"""
Script to load and aggregate simulation data.
Groups CSV files by parameters dynamically extracted from filenames, and provides aggregation functions.
"""

import pandas as pd
import numpy as np
from pathlib import Path
import re
from collections import defaultdict
from typing import Dict, List, Tuple, Any
from io import StringIO


class SimulationDataLoader:
    """Loads and organizes simulation data."""
    
    def __init__(self, data_dir: str = "data"):
        self.data_dir = Path(data_dir)
        self.grouped_data = defaultdict(list)
        self.parameters = set()
        self.param_names = []  # Names of parameters (excluding 'seed')
        
    def parse_filename(self, filename: str) -> Tuple[Dict[str, float], float]:
        """Extracts parameters dynamically from the filename.
        
        Expects format: simulation_param1-value1_param2-value2_..._seed-value.csv
        
        Returns:
            Tuple[Dict of parameters (excluding seed), seed value]
            Returns (None, None) if parsing fails
        """
        # Pattern to match: simulation_key-value pairs separated by underscores
        pattern = r"simulation_(.+)\.csv"
        match = re.match(pattern, filename)
        if not match:
            return None, None
        

        # Extract all key-value pairs
        params_str = match.group(1)
        # Split by underscores first, then parse each param-value pair
        param_pairs = params_str.split('_')
        
        params = {}
        seed = None
        
        for pair in param_pairs:
            if '-' in pair:
                key, value = pair.split('-', 1)
                val = float(value)
                if key == 'seed':
                    seed = val
                else:
                    params[key] = val
        if seed is None:
            return None, None
            
        return params, seed
    
    def load_csv(self, filepath: Path) -> pd.DataFrame:
        """Loads a single CSV file, using the line starting with '# time' as the header."""
        header = None
        data_lines = []
        with open(filepath, 'r') as f:
            for line in f:
                if line.strip().startswith('# time'):
                    header = line.strip().lstrip('#').strip().split()
                elif not line.strip().startswith('#'):
                    data_lines.append(line)
        data_string = ''.join(data_lines)
        # Use sep='\s+' as it is more robust for space-separated data
        df = pd.read_csv(StringIO(data_string), sep=r'\s+', header=None, names=header)
        return df
    
    def load_all_data(self) -> Dict:
        """Loads all CSV files and organizes them by parameters.
        
        Returns:
            Dict with structure: {tuple of parameter values: [df1, df2, ...]}
        """
        if not self.data_dir.exists():
            raise FileNotFoundError(f"Directory {self.data_dir} not found")
        
        csv_files = list(self.data_dir.glob("simulation_*.csv"))
        print(f"Found {len(csv_files)} CSV files")
        
        for filepath in csv_files:
            params_dict, seed = self.parse_filename(filepath.name)
            
            if params_dict is not None:
                # Set parameter names from first file
                if not self.param_names:
                    self.param_names = sorted(params_dict.keys())
                
                df = self.load_csv(filepath)
                # Add metadata to dataframe
                for param_name, param_value in params_dict.items():
                    df[param_name] = param_value
                df['seed'] = seed
                
                # Group by parameter values (as tuple for hashability)
                key = tuple(params_dict[k] for k in self.param_names)
                self.grouped_data[key].append(df)
                self.parameters.add(key)
        
        print(f"Parameter names: {self.param_names}")
        print(f"Parameter combinations found: {sorted(self.parameters)}")
        return self.grouped_data
    
    def get_combined_dataframe(self, **param_filters) -> pd.DataFrame:
        """Combines all dataframes for specific parameters.
        
        Args:
            **param_filters: Keyword arguments for parameter filters (e.g., grid_size=10.0, range=1.5)
                           If a parameter is not specified, all values for that parameter are included
            
        Returns:
            Combined DataFrame with all seeds matching the filters
        """
        if not self.grouped_data:
            self.load_all_data()
        
        dfs_to_combine = []
        for key, dfs in self.grouped_data.items():
            # Create dict from key tuple
            params = {name: value for name, value in zip(self.param_names, key)}
            
            # Check if all filters match
            match = all(
                params.get(filter_name) == filter_value
                for filter_name, filter_value in param_filters.items()
            )
            
            if match:
                dfs_to_combine.extend(dfs)
        
        if not dfs_to_combine:
            return pd.DataFrame()
        
        return pd.concat(dfs_to_combine, ignore_index=True)
    
    def get_by_parameters(self, **params) -> List[pd.DataFrame]:
        """Gets the list of dataframes for specific parameters.
        
        Args:
            **params: Keyword arguments for exact parameter values (e.g., grid_size=10.0, range=1.5)
                     All parameter names must be specified
            
        Returns:
            List of DataFrames (one per seed)
        """
        if not self.grouped_data:
            self.load_all_data()
        
        # Create key tuple in correct order
        try:
            key = tuple(params[name] for name in self.param_names)
        except KeyError as e:
            available = ', '.join(self.param_names)
            raise ValueError(f"Missing parameter: {e}. Available parameters: {available}")
        
        return self.grouped_data.get(key, [])


class SimulationAggregator:
    
    @staticmethod
    def convergence_time_statistics(dfs: List[pd.DataFrame],
                                   metric: str = 'state[mean]',
                                   threshold: float = 1.0,
                                   group_by_params: List[str] = None) -> pd.DataFrame:
        """Calculates convergence time statistics grouped by parameters.
        
        Convergence time is defined as the first time when the metric reaches the threshold.
        
        Args:
            dfs: List of DataFrames to analyze
            metric: Column name to check for convergence
            threshold: Value that indicates convergence
            group_by_params: List of parameter names to group by (e.g., ['grid_size', 'range'])
                           If None, will detect and use all parameter columns
            
        Returns:
            DataFrame with mean, min, max convergence time for each parameter combination
        """
        if not dfs:
            return pd.DataFrame()
        
        # Combine all dataframes
        combined = pd.concat(dfs, ignore_index=True)
        
        # Detect parameter columns if not specified
        if group_by_params is None:
            exclude_cols = {'seed', 'time'}
            group_by_params = []
            for col in combined.columns:
                if col not in exclude_cols and combined[col].nunique() <= len(combined) / 10:
                    group_by_params.append(col)
        
        # Calculate convergence time for each seed
        convergence_times = {}
        for df in dfs:
            params = tuple(df[param].iloc[0] for param in group_by_params)
            if(params not in convergence_times):
                convergence_times[params] = []
            
            # Find first time metric reaches threshold
            # skip the first tick
            df = df[df['time'] > 1]
            conv_time = df[df[metric] == threshold]['time']
            if not conv_time.empty:
                convergence_times[params].append(conv_time.iloc[0])
            else:
                convergence_times[params].append(np.nan)  # Did not converge    
        ## Aggregate statistics
        records = []
        for params, times in convergence_times.items():
            times_clean = [t for t in times if not np.isnan(t)]
            if times_clean:
                mean_time = np.mean(times_clean)
                min_time = np.min(times_clean)
                max_time = np.max(times_clean)
            else:
                mean_time = min_time = max_time = np.nan
            
            record = {param: value for param, value in zip(group_by_params, params)}
            record.update({
                'mean_convergence_time': mean_time,
                'min_convergence_time': min_time,
                'max_convergence_time': max_time
            })
            records.append(record)
        return pd.DataFrame(records)

def example_usage():
    """Example usage of the classes."""
    
    loader = SimulationDataLoader()
    loader.load_all_data()
    aggregator = SimulationAggregator()
    print("\n=== Convergence Time Statistics ===")
    all_dfs = [df for dfs_list in loader.grouped_data.values() for df in dfs_list]
    conv_stats = aggregator.convergence_time_statistics(
        all_dfs,
        metric='state[mean]',
        threshold=1.0,
        group_by_params=['size', 'range']
    )
    print(conv_stats)
    
    return loader, aggregator


if __name__ == "__main__":
    loader, aggregator = example_usage()
    
    # You can then use loader and aggregator for other analyses
    print("\n=== Available objects ===")
    print("- loader: SimulationDataLoader")
    print("- aggregator: SimulationAggregator")
    print("\nUse loader.grouped_data to access grouped data")
    print("Use aggregator.* for various aggregation functions")
