import numpy as np
import xarray as xr
import re
from pathlib import Path
import collections

def distance(val, ref):
    return abs(ref - val)
vectDistance = np.vectorize(distance)

def cmap_xmap(function, cmap):
    """ Applies function, on the indices of colormap cmap. Beware, function
    should map the [0, 1] segment to itself, or you are in for surprises.

    See also cmap_xmap.
    """
    cdict = cmap._segmentdata
    function_to_map = lambda x : (function(x[0]), x[1], x[2])
    for key in ('red','green','blue'):
        cdict[key] = map(function_to_map, cdict[key])
#        cdict[key].sort()
#        assert (cdict[key][0]<0 or cdict[key][-1]>1), "Resulting indices extend out of the [0, 1] segment."
    return matplotlib.colors.LinearSegmentedColormap('colormap',cdict,1024)

def getClosest(sortedMatrix, column, val):
    while len(sortedMatrix) > 3:
        half = int(len(sortedMatrix) / 2)
        sortedMatrix = sortedMatrix[-half - 1:] if sortedMatrix[half, column] < val else sortedMatrix[: half + 1]
    if len(sortedMatrix) == 1:
        result = sortedMatrix[0].copy()
        result[column] = val
        return result
    else:
        safecopy = sortedMatrix.copy()
        safecopy[:, column] = vectDistance(safecopy[:, column], val)
        minidx = np.argmin(safecopy[:, column])
        safecopy = safecopy[minidx, :].A1
        safecopy[column] = val
        return safecopy

def convert(column, samples, matrix):
    return np.matrix([getClosest(matrix, column, t) for t in samples])

def valueOrEmptySet(k, d):
    return (d[k] if isinstance(d[k], set) else {d[k]}) if k in d else set()

def mergeDicts(d1, d2):
    """
    Creates a new dictionary whose keys are the union of the keys of two
    dictionaries, and whose values are the union of values.

    Parameters
    ----------
    d1: dict
        dictionary whose values are sets
    d2: dict
        dictionary whose values are sets

    Returns
    -------
    dict
        A dict whose keys are the union of the keys of two dictionaries,
    and whose values are the union of values

    """
    res = {}
    for k in d1.keys() | d2.keys():
        res[k] = valueOrEmptySet(k, d1) | valueOrEmptySet(k, d2)
    return res

def extractCoordinates(filename):
    """
    Scans the header of an Alchemist file in search of the variables.

    Parameters
    ----------
    filename : str
        path to the target file
    mergewith : dict
        a dictionary whose dimensions will be merged with the returned one

    Returns
    -------
    dict
        A dictionary whose keys are strings (coordinate name) and values are
        lists (set of variable values)

    """
    with open(filename, 'r') as file:
#        regex = re.compile(' (?P<varName>[a-zA-Z._-]+) = (?P<varValue>[-+]?\d*\.?\d+(?:[eE][-+]?\d+)?),?')
        regex = r"(?P<varName>[a-zA-Z._-]+) = (?P<varValue>[^,]*),?"
        dataBegin = r"\d"
        is_float = r"[-+]?\d*\.?\d+(?:[eE][-+]?\d+)?"
        for line in file:
            match = re.findall(regex, line.replace('Infinity', '1e30000'))
            if match:
                return {
                    var : float(value) if re.match(is_float, value)
                        else bool(re.match(r".*?true.*?", value.lower())) if re.match(r".*?(true|false).*?", value.lower())
                        else value
                    for var, value in match
                }
            elif re.match(dataBegin, line[0]):
                return {}

def extractVariableNames(filename):
    """
    Gets the variable names from the Alchemist data files header.

    Parameters
    ----------
    filename : str
        path to the target file

    Returns
    -------
    list of list
        A matrix with the values of the csv file

    """
    with open(filename, 'r') as file:
        dataBegin = re.compile('\d')
        lastHeaderLine = ''
        for line in file:
            if dataBegin.match(line[0]):
                break
            else:
                lastHeaderLine = line
        if lastHeaderLine:
            regex = re.compile(' (?P<varName>\S+)')
            return regex.findall(lastHeaderLine)
        return []

def openCsv(path):
    """
    Converts an Alchemist export file into a list of lists representing the matrix of values.

    Parameters
    ----------
    path : str
        path to the target file

    Returns
    -------
    list of list
        A matrix with the values of the csv file

    """
    regex = re.compile('\d')
    with open(path, 'r') as file:
        lines = filter(lambda x: regex.match(x[0]), file.readlines())
        return [[float(x) for x in line.split()] for line in lines]

def beautifyValue(v):
    """
    Converts an object to a better version for printing, in particular:
        - if the object converts to float, then its float value is used
        - if the object can be rounded to int, then the int value is preferred

    Parameters
    ----------
    v : object
        the object to try to beautify

    Returns
    -------
    object or float or int
        the beautified value
    """
    try:
        v = float(v)
        if v.is_integer():
            return int(v)
        return v
    except:
        return v

if __name__ == '__main__':
    # CONFIGURE SCRIPT
    # Where to find Alchemist data files
    directory = 'data'
    # Where to save charts
    output_directory = 'charts'
    # How to name the summary of the processed data
    pickleOutput = 'data_summary'
    # Experiment prefixes: one per experiment (root of the file name)
    experiments = ['simulation']
    floatPrecision = '{: 0.3f}'
    # Number of time samples 
    timeSamples = 400
    # time management
    minTime = 400
    maxTime = 4000
    timeColumnName = 'time'
    logarithmicTime = False
    # One or more variables are considered random and "flattened"
    seedVars = ['seed']
    # Label mapping
    class Measure:
        def __init__(self, description, unit = None):
            self.__description = description
            self.__unit = unit
        def description(self):
            return self.__description
        def unit(self):
            return '' if self.__unit is None else f'({self.__unit})'
        def derivative(self, new_description = None, new_unit = None):
            def cleanMathMode(s):
                return s[1:-1] if s[0] == '$' and s[-1] == '$' else s
            def deriveString(s):
                return r'$d ' + cleanMathMode(s) + r'/{dt}$'
            def deriveUnit(s):
                return f'${cleanMathMode(s)}' + '/{s}$' if s else None
            result = Measure(
                new_description if new_description else deriveString(self.__description),
                new_unit if new_unit else deriveUnit(self.__unit),
            )
            return result
        def __str__(self):
            return f'{self.description()} {self.unit()}'
    
    centrality_label = 'H_a(x)'
    def expected(x):
        return r'\mathbf{E}[' + x + ']'
    def stdev_of(x):
        return r'\sigma{}[' + x + ']'
    def mse(x):
        return 'MSE[' + x + ']'
    def cardinality(x):
        return r'\|' + x + r'\|'

    labels = {
        'nodeCount': Measure(r'$n$', 'nodes'),
        'harmonicCentrality[Mean]': Measure(f'${expected("H(x)")}$'),
        'meanNeighbors': Measure(f'${expected(cardinality("N"))}$', 'nodes'),
        'speed': Measure(r'$\|\vec{v}\|$', r'$m/s$'),
        'msqer@harmonicCentrality[Max]': Measure(r'$\max{(' + mse(centrality_label) + ')}$'),
        'msqer@harmonicCentrality[Min]': Measure(r'$\min{(' + mse(centrality_label) + ')}$'),
        'msqer@harmonicCentrality[Mean]': Measure(f'${expected(mse(centrality_label))}$'),
        'msqer@harmonicCentrality[StandardDeviation]': Measure(f'${stdev_of(mse(centrality_label))}$'),
        'org:protelis:tutorial:distanceTo[max]': Measure(r'$m$', 'max distance'),
        'org:protelis:tutorial:distanceTo[mean]': Measure(r'$m$', 'mean distance'),
        'org:protelis:tutorial:distanceTo[min]': Measure(r'$m$', ',min distance'),
    }
    def derivativeOrMeasure(variable_name):
        if variable_name.endswith('dt'):
            return labels.get(variable_name[:-2], Measure(variable_name)).derivative()
        return Measure(variable_name)
    def label_for(variable_name):
        return labels.get(variable_name, derivativeOrMeasure(variable_name)).description()
    def unit_for(variable_name):
        return str(labels.get(variable_name, derivativeOrMeasure(variable_name)))
    
    # Setup libraries
    np.set_printoptions(formatter={'float': floatPrecision.format})
    # Read the last time the data was processed, reprocess only if new data exists, otherwise just load
    import pickle
    import os
    if os.path.exists(directory):
        newestFileTime = max([os.path.getmtime(directory + '/' + file) for file in os.listdir(directory)], default=0.0)
        try:
            lastTimeProcessed = pickle.load(open('timeprocessed', 'rb'))
        except:
            lastTimeProcessed = -1
        shouldRecompute = not os.path.exists(".skip_data_process") and newestFileTime != lastTimeProcessed
        if not shouldRecompute:
            try:
                means = pickle.load(open(pickleOutput + '_mean', 'rb'))
                stdevs = pickle.load(open(pickleOutput + '_std', 'rb'))
            except:
                shouldRecompute = True
        if shouldRecompute:
            timefun = np.logspace if logarithmicTime else np.linspace
            means = {}
            stdevs = {}
            for experiment in experiments:
                # Collect all files for the experiment of interest
                import fnmatch
                allfiles = filter(lambda file: fnmatch.fnmatch(file, experiment + '_*.csv'), os.listdir(directory))
                allfiles = [directory + '/' + name for name in allfiles]
                allfiles.sort()
                # From the file name, extract the independent variables
                dimensions = {}
                for file in allfiles:
                    dimensions = mergeDicts(dimensions, extractCoordinates(file))
                dimensions = {k: sorted(v) for k, v in dimensions.items()}
                # Add time to the independent variables
                dimensions[timeColumnName] = range(0, timeSamples)
                # Compute the matrix shape
                shape = tuple(len(v) for k, v in dimensions.items())
                # Prepare the Dataset
                dataset = xr.Dataset()
                for k, v in dimensions.items():
                    dataset.coords[k] = v
                if len(allfiles) == 0:
                    print("WARNING: No data for experiment " + experiment)
                    means[experiment] = dataset
                    stdevs[experiment] = xr.Dataset()
                else:
                    varNames = extractVariableNames(allfiles[0])
                    for v in varNames:
                        if v != timeColumnName:
                            novals = np.ndarray(shape)
                            novals.fill(float('nan'))
                            dataset[v] = (dimensions.keys(), novals)
                    # Compute maximum and minimum time, create the resample
                    timeColumn = varNames.index(timeColumnName)
                    allData = { file: np.matrix(openCsv(file)) for file in allfiles }
                    computeMin = minTime is None
                    computeMax = maxTime is None
                    if computeMax:
                        maxTime = float('-inf')
                        for data in allData.values():
                            maxTime = max(maxTime, data[-1, timeColumn])
                    if computeMin:
                        minTime = float('inf')
                        for data in allData.values():
                            minTime = min(minTime, data[0, timeColumn])
                    timeline = timefun(minTime, maxTime, timeSamples)
                    # Resample
                    for file in allData:
    #                    print(file)
                        allData[file] = convert(timeColumn, timeline, allData[file])
                    # Populate the dataset
                    for file, data in allData.items():
                        dataset[timeColumnName] = timeline
                        for idx, v in enumerate(varNames):
                            if v != timeColumnName:
                                darray = dataset[v]
                                experimentVars = extractCoordinates(file)
                                darray.loc[experimentVars] = data[:, idx].A1
                    # Fold the dataset along the seed variables, producing the mean and stdev datasets
                    mergingVariables = [seed for seed in seedVars if seed in dataset.coords]
                    means[experiment] = dataset.mean(dim = mergingVariables, skipna=True)
                    stdevs[experiment] = dataset.std(dim = mergingVariables, skipna=True)
            # Save the datasets
            pickle.dump(means, open(pickleOutput + '_mean', 'wb'), protocol=-1)
            pickle.dump(stdevs, open(pickleOutput + '_std', 'wb'), protocol=-1)
            pickle.dump(newestFileTime, open('timeprocessed', 'wb'))
    else:
        means = { experiment: xr.Dataset() for experiment in experiments }
        stdevs = { experiment: xr.Dataset() for experiment in experiments }

    # QUICK CHARTING

    import matplotlib
    import matplotlib.pyplot as plt
    import matplotlib.cm as cmx
    matplotlib.rcParams.update({'axes.titlesize': 12})
    matplotlib.rcParams.update({'axes.labelsize': 10})
    
    def make_line_chart(
        xdata,
        ydata,
        title=None,
        ylabel=None,
        xlabel=None,
        colors=None,
        linewidth=1,
        error_alpha=0.2,
        figure_size=(6, 4)
    ):
        fig = plt.figure(figsize = figure_size)
        ax = fig.add_subplot(1, 1, 1)
        ax.set_title(title)
        ax.set_xlabel(xlabel)
        ax.set_ylabel(ylabel)
#        ax.set_ylim(0)
#        ax.set_xlim(min(xdata), max(xdata))
        index = 0
        for (label, (data, error)) in ydata.items():
#            print(f'plotting {data}\nagainst {xdata}')
            lines = ax.plot(xdata, data, label=label, color=colors(index / (len(ydata) - 1)) if colors else None, linewidth=linewidth)
            index += 1
            if error is not None:
                last_color = lines[-1].get_color()
                ax.fill_between(
                    xdata,
                    data+error,
                    data-error,
                    facecolor=last_color,
                    alpha=error_alpha,
                )
        return (fig, ax)
    def generate_all_charts(means, errors = None, basedir=''):
        viable_coords = { coord for coord in means.coords if means[coord].size > 1 }
        for comparison_variable in viable_coords - {timeColumnName}:
            mergeable_variables = viable_coords - {timeColumnName, comparison_variable}
            for current_coordinate in mergeable_variables:
                merge_variables = mergeable_variables - { current_coordinate }
                merge_data_view = means.mean(dim = merge_variables, skipna = True)
                merge_error_view = errors.mean(dim = merge_variables, skipna = True)
                for current_coordinate_value in merge_data_view[current_coordinate].values:
                    beautified_value = beautifyValue(current_coordinate_value)
                    for current_metric in merge_data_view.data_vars:
                        title = f'{label_for(current_metric)} for diverse {label_for(comparison_variable)} when {label_for(current_coordinate)}={beautified_value}'
                        for withErrors in [True, False]:
                            fig, ax = make_line_chart(
                                title = title,
                                xdata = merge_data_view[timeColumnName],
                                xlabel = unit_for(timeColumnName),
                                ylabel = unit_for(current_metric),
                                ydata = {
                                    beautifyValue(label): (
                                        merge_data_view.sel(selector)[current_metric],
                                        merge_error_view.sel(selector)[current_metric] if withErrors else 0
                                    )
                                    for label in merge_data_view[comparison_variable].values
                                    for selector in [{comparison_variable: label, current_coordinate: current_coordinate_value}]
                                },
                            )
                            ax.set_xlim(minTime, maxTime)
                            ax.legend()
                            fig.tight_layout()
                            by_time_output_directory = f'{output_directory}/{basedir}/{comparison_variable}'
                            Path(by_time_output_directory).mkdir(parents=True, exist_ok=True)
                            figname = f'{comparison_variable}_{current_metric}_{current_coordinate}_{beautified_value}{"_err" if withErrors else ""}'
                            for symbol in r".[]\/@:":
                                figname = figname.replace(symbol, '_')
                            fig.savefig(f'{by_time_output_directory}/{figname}.pdf')
                            plt.close(fig)
    for experiment in experiments:
        current_experiment_means = means[experiment]
        current_experiment_errors = stdevs[experiment]
        generate_all_charts(current_experiment_means, current_experiment_errors, basedir = f'{experiment}/all')
        
from pathlib import Path
import numpy as np
import matplotlib.pyplot as plt
from matplotlib.lines import Line2D
from scipy import stats

def weibull_frequency(variability, scale=1.0, loc=0.0, time_range=None):
    """
    Calculate the frequency (probability density) of WeibullTimeSimplified distribution.
    
    Parameters:
    -----------
    variability : float
        Shape parameter (k) of the Weibull distribution
    scale : float, default=1.0
        Scale parameter (lambda) of the Weibull distribution  
    loc : float, default=0.0
        Location parameter of the Weibull distribution
    time_range : array-like, optional
        Time points to evaluate. If None, uses a default range
    
    Returns:
    --------
    tuple: (time_points, frequency_values)
    """
    if time_range is None:
        time_range = np.linspace(0.1, 5, 1000)  # Avoid 0 for Weibull
    
    # Weibull distribution using scipy.stats
    # Note: scipy uses 'c' for shape parameter, 'scale' for scale parameter
    frequency = stats.weibull_min.pdf(time_range, c=variability, scale=scale, loc=loc)
    
    return time_range, frequency


def plot_disagreement_bar_chart(means_data, output_suffix=""):
    """
    Create a grouped bar chart showing MAX disagreement rate for different configurations.
    Groups by connection_range_percentage with variability as sub-groups.
    
    Parameters
    ----------
    means_data : xr.Dataset
        The dataset containing simulation means
    output_suffix : str
        Optional suffix for output filename
    """
    ds = means_data
    
    # Check for disagreement variable
    disagreement_var = next((v for v in ["disagreementRate", "disagrementRate"] if v in ds.data_vars), None)
    
    if disagreement_var is None:
        print("WARNING: No disagreement variable found in dataset")
        return None
    
    # Take MAX over history_length and time to get peak disagreement rate
    ds_summary = ds.max(dim=["history_length", "time"], skipna=True)
    
    # Get all combinations of connection_range and variability
    conn_ranges = np.asarray(ds_summary["connection_range_percentage"].values, dtype=float)
    variabilities = np.asarray(ds_summary["variability"].values, dtype=float)
    
    # Prepare data for grouped bar chart
    # Structure: {conn_range: [disagree_var1, disagree_var2, ...]}
    data_by_conn = {}
    for conn in conn_ranges:
        data_by_conn[conn] = []
        for var in variabilities:
            disagree_val = ds_summary[disagreement_var].sel(
                connection_range_percentage=conn,
                variability=var
            ).values
            data_by_conn[conn].append(float(disagree_val))
    
    # Create grouped bar chart
    fig, ax = plt.subplots(figsize=(10, 6))
    print(conn_ranges)
    num_conn_ranges = len(conn_ranges)
    num_variabilities = len(variabilities)
    bar_width = 0.8 / num_variabilities
    x_positions = np.arange(num_conn_ranges)
    
    # Plot bars for each variability within each connection range group
    colors = plt.cm.Set2(np.linspace(0, 1, num_variabilities))
    
    for i, var in enumerate(variabilities):
        var_data = [data_by_conn[conn][i] for conn in conn_ranges]
        x_offset = (i - num_variabilities/2 + 0.5) * bar_width
        bars = ax.bar(x_positions + x_offset, var_data, bar_width,
                     label=f'variability={var:.0f}',
                     color=colors[i], alpha=0.8, edgecolor='black', linewidth=0.5)
        
        # Add value labels on top of bars
        for bar, val in zip(bars, var_data):
            height = bar.get_height()
            ax.text(bar.get_x() + bar.get_width()/2., height,
                    f'{val:.3f}',
                    ha='center', va='bottom', fontsize=7)
    
    ax.set_xlabel("Connection Range Percentage", fontsize=11)
    ax.set_ylabel("Max Disagreement Rate", fontsize=11)
    ax.set_title("Maximum Disagreement Rate by Configuration", fontsize=12)
    ax.set_xticks(x_positions)
    ax.set_xticklabels([f'{conn:.1f}' for conn in conn_ranges], fontsize=9)
    ax.legend(fontsize=9, loc='best')
    ax.grid(True, alpha=0.3, axis='y')
    ax.tick_params(axis="both", labelsize=9)
    
    plt.tight_layout()
    
    out_dir = Path("charts")
    out_dir.mkdir(parents=True, exist_ok=True)
    file = f"disagreement_bar_chart_max{output_suffix}"
    fig.savefig(f"{out_dir}/{file}.png", dpi=300, bbox_inches="tight")
    fig.savefig(f"{out_dir}/{file}.pdf", bbox_inches="tight")
    plt.close(fig)
    
    print(f"Saved to: {out_dir}/{file}.png and {out_dir}/{file}.pdf")
    
    return fig


def plot_weibull_frequencies_only(means_data, connection_range_percentage, output_suffix=""):
    """
    Plot only the Weibull frequency distributions in a separate chart.
    
    Parameters
    ----------
    means_data : xr.Dataset
        The dataset containing simulation means
    connection_range_percentage : float
        The connection range percentage to select
    output_suffix : str
        Optional suffix for output filename
    """
    ds = means_data.sel(connection_range_percentage=connection_range_percentage)
    
    # Get variabilities from the dataset
    variabilities = np.asarray(ds["variability"].values, dtype=float)

    def fmt_var(v: float) -> str:
        return str(int(v)) if float(v).is_integer() else f"{v:g}"

    # distinct colors per variability
    cmap = plt.cm.viridis if len(variabilities) <= 10 else plt.cm.turbo
    colors = {v: cmap(i / max(1, len(variabilities) - 1)) for i, v in enumerate(variabilities)}

    fig, ax = plt.subplots(figsize=(4.5, 4))
    
    # Plot Weibull frequencies
    weibull_time_range = np.linspace(0.1, 3, 500)
    
    for var in variabilities:
        weibull_time, weibull_freq = weibull_frequency(var, scale=1.0, loc=0.0, time_range=weibull_time_range)
        ax.plot(weibull_time, weibull_freq, color=colors[var], linewidth=2, alpha=0.9, 
               label=f"k={fmt_var(var)}")
    
    ax.set_title("WeibullTimeSimplified Frequency Distributions", fontsize=15)
    ax.set_xlabel("time", fontsize=13)
    ax.set_ylabel("Frequency", fontsize=13)
    ax.grid(True, alpha=0.15)
    ax.tick_params(axis="both", labelsize=12)
    ax.legend(fontsize=12)

    plt.tight_layout()

    out_dir = Path("charts")
    out_dir.mkdir(parents=True, exist_ok=True)
    file = f"weibull_frequencies_conn{connection_range_percentage}{output_suffix}"
    fig.savefig(f"{out_dir}/{file}.png", dpi=300, bbox_inches="tight")
    fig.savefig(f"{out_dir}/{file}.pdf", bbox_inches="tight")
    plt.close(fig)

    print(f"Saved to: {out_dir}/{file}.png and {out_dir}/{file}.pdf")

    return fig


def plot_combined_states_and_weibull(means_data, connection_range_percentage, state_vars_to_show=4, output_suffix=""):
    """
    Plot state variables and Weibull frequency distributions in a combined chart.
    Shows state variables on the left axis and Weibull frequencies on the right axis.
    
    Parameters
    ----------
    means_data : xr.Dataset
        The dataset containing simulation means
    connection_range_percentage : float
        The connection range percentage to select
    state_vars_to_show : int
        Number of state variables to show (default: 4)
    output_suffix : str
        Optional suffix for output filename
    """
    ds = means_data.sel(connection_range_percentage=connection_range_percentage)
    
    # Average over history_length to focus on variability comparison
    ds_avg = ds.mean(dim="history_length", skipna=True)

    # Resolve variable names present in the dataset
    disagreement_var = next((v for v in ["disagreementRate", "disagrementRate"] if v in ds_avg.data_vars), None)
    history_size_var = next(
        (v for v in ["historySize", "history_size", "history_size[mean]", "historySize[mean]"] if v in ds_avg.data_vars),
        None,
    )

    requested_state_vars = ["wandering[mean]", "defending[mean]", "wait[mean]", "solving[mean]"]
    if disagreement_var is not None:
        requested_state_vars.append(disagreement_var)
    if history_size_var is not None:
        requested_state_vars.append(history_size_var)

    state_vars = [v for v in requested_state_vars if v in ds_avg.data_vars][:state_vars_to_show]

    # overlays (avg over variability)
    overlay_vars = [v for v in ["alarm[max]", "attacked[max]"] if v in ds_avg.data_vars]
    overlay_avg = ds_avg[overlay_vars].mean(dim="variability", skipna=True) if overlay_vars else None

    variabilities = np.asarray(ds_avg["variability"].values, dtype=float)
    time = ds_avg["time"].values

    def fmt_var(v: float) -> str:
        return str(int(v)) if float(v).is_integer() else f"{v:g}"

    # distinct colors per variability
    cmap = plt.cm.viridis if len(variabilities) <= 10 else plt.cm.turbo
    colors = {v: cmap(i / max(1, len(variabilities) - 1)) for i, v in enumerate(variabilities)}

    overlay_style = {
        "alarm[max]": dict(color="red", linestyle="--", linewidth=1.2, alpha=0.6),
        "attacked[max]": dict(color="orange", linestyle=":", linewidth=1.2, alpha=0.6),
    }

    # Create subplots - add one extra for Weibull frequencies
    fig, axes = plt.subplots(
        nrows=max(1, len(state_vars) + 1),  # +1 for Weibull plot
        ncols=1,
        figsize=(5.6, 0.85 * max(1, len(state_vars) + 1)),
        sharex=True,
    )
    axes = np.atleast_1d(axes)

    # Plot state variables
    for ax, v in zip(axes[:-1], state_vars):  # All except last axis
        is_disagreement = v in {"disagreementRate", "disagrementRate"}
        skip_overlays = (history_size_var is not None and v == history_size_var)

        # variability-colored lines
        for var in variabilities:
            y = ds_avg[v].sel(variability=var).values
            if is_disagreement:
                y = (y > 0).astype(int)
            ax.plot(time, y, color=colors[var], linewidth=1.2, alpha=0.9, zorder=2)

        # overlays on every subplot except history size
        if (not skip_overlays) and overlay_avg is not None:
            for ov in overlay_vars:
                ax.plot(
                    overlay_avg["time"].values,
                    overlay_avg[ov].values,
                    zorder=0,
                    **overlay_style.get(ov, dict(color="0.25", linewidth=0.7, alpha=0.45)),
                )

        if is_disagreement:
            ax.set_title(f"{v.removesuffix('[mean]')} (>0 → 1 else 0)", fontsize=9)
            ax.set_ylim(-0.05, 1.05)
            ax.set_yticks([0, 1])
        else:
            ax.set_title(v.removesuffix("[mean]"), fontsize=9)
        ax.grid(True, alpha=0.15)
        ax.tick_params(axis="both", labelsize=8)

    # Plot Weibull frequencies on the last axis
    weibull_ax = axes[-1]
    weibull_time_range = np.linspace(0.1, max(time[-1], 10), 500)
    
    for var in variabilities:
        weibull_time, weibull_freq = weibull_frequency(var, scale=1.0, loc=0.0, time_range=weibull_time_range)
        weibull_ax.plot(weibull_time, weibull_freq, color=colors[var], linewidth=1.2, alpha=0.9, 
                       label=f"variability={fmt_var(var)}")
    
    weibull_ax.set_title("WeibullTimeSimplified Frequency", fontsize=9)
    weibull_ax.set_ylabel("Frequency", fontsize=8)
    weibull_ax.grid(True, alpha=0.15)
    weibull_ax.tick_params(axis="both", labelsize=8)

    axes[-1].set_xlabel("time", fontsize=9)

    # single global legend: variabilities + overlays (if any)
    var_handles = [
        Line2D([0], [0], color=colors[v], lw=1.2, label=f"variability={fmt_var(v)}")
        for v in variabilities
    ]
    overlay_handles = []
    if overlay_vars:
        overlay_handles = [
            Line2D([0], [0], label=f"{ov} (avg)", **overlay_style.get(ov, dict(color="0.25", linewidth=0.7, alpha=0.45)))
            for ov in overlay_vars
        ]

    fig.legend(
        handles=var_handles + overlay_handles,
        loc="upper center",
        ncol=min(len(var_handles + overlay_handles), 5),
        fontsize=7,
        frameon=False,
    )

    plt.tight_layout(rect=(0, 0, 1, 0.99))

    out_dir = Path("charts")
    out_dir.mkdir(parents=True, exist_ok=True)
    file = f"states_and_weibull_conn{connection_range_percentage}_by_variability"
    fig.savefig(f"{out_dir}/{file}.png", dpi=300, bbox_inches="tight")
    fig.savefig(f"{out_dir}/{file}.pdf", bbox_inches="tight")
    plt.close(fig)

    print(f"Saved to: {out_dir}/{file}.png and {out_dir}/{file}.pdf")

    return fig

def plot_states_by_history(means_data, connection_range_percentage, variability, stdevs_data=None, output_suffix="", show_legend=True):
    """
    Plot state variables for a given connection_range_percentage and variability.
    
    Parameters
    ----------
    means_data : xr.Dataset
        The dataset containing simulation means
    connection_range_percentage : float
        The connection range percentage to select
    variability : float
        The variability to select
    stdevs_data : xr.Dataset, optional
        The dataset containing standard deviations for confidence intervals
    output_suffix : str
        Optional suffix for output filename
    show_legend : bool
        Whether to show the legend (default: True)
    """
    ds = means_data.sel(connection_range_percentage=connection_range_percentage, variability=variability)
    ds_std = stdevs_data.sel(connection_range_percentage=connection_range_percentage, variability=variability) if stdevs_data is not None else None

    # Resolve variable names present in the dataset
    disagreement_var = next((v for v in ["disagreementRate", "disagrementRate"] if v in ds.data_vars), None)
    history_size_var = next(
        (v for v in ["historySize", "history_size", "history_size[mean]", "historySize[mean]"] if v in ds.data_vars),
        None,
    )

    requested_state_vars = ["wandering[mean]", "defending[mean]", "wait[mean]", "solving[mean]"]
    if disagreement_var is not None:
        requested_state_vars.append(disagreement_var)
    if history_size_var is not None:
        requested_state_vars.append(history_size_var)

    state_vars = [v for v in requested_state_vars if v in ds.data_vars]

    # overlays (avg over history_length)
    overlay_vars = [v for v in ["alarm[max]", "attacked[max]"] if v in ds.data_vars]
    overlay_avg = ds[overlay_vars].mean(dim="history_length", skipna=True) if overlay_vars else None

    histories = np.asarray(ds["history_length"].values, dtype=float)
    time = ds["time"].values

    def fmt_history(h: float) -> str:
        if h == 6000:
            return "∞"
        return str(int(h)) if float(h).is_integer() else f"{h:g}"

    # distinct colors per history
    cmap = plt.cm.tab10 if len(histories) <= 10 else plt.cm.turbo
    colors = {h: cmap(i / max(1, len(histories) - 1)) for i, h in enumerate(histories)}

    overlay_style = {
        "alarm[max]": dict(color="red", linestyle="--", linewidth=1.2, alpha=0.6),
        "attacked[max]": dict(color="orange", linestyle=":", linewidth=1.2, alpha=0.6),
    }

    fig, axes = plt.subplots(
        nrows=max(1, len(state_vars)),
        ncols=1,
        figsize=(5.6, 1.25 * max(1, len(state_vars))),
        sharex=True,
    )
    axes = np.atleast_1d(axes)

    for ax, v in zip(axes, state_vars):
        is_disagreement = v in {"disagreementRate", "disagrementRate"}
        skip_overlays = (history_size_var is not None and v == history_size_var)

        # history-colored lines
        for h in histories:
            y = ds[v].sel(history_length=h).values
            if is_disagreement:
                y = (y > 0).astype(int)
            ax.plot(time, y, color=colors[h], linewidth=1.2, alpha=0.9, zorder=2)
            
            # Add 95% confidence interval (1.96 * std)
            if ds_std is not None and v in ds_std.data_vars:
                std = ds_std[v].sel(history_length=h).values
                ci = 1.96 * std
                if not is_disagreement:
                    # Check if variable is a proportion (values between 0 and 1)
                    is_proportion = (np.nanmin(y) >= 0 and np.nanmax(y) <= 1)
                    lower = np.clip(y - ci, 0, 1) if is_proportion else y - ci
                    upper = np.clip(y + ci, 0, 1) if is_proportion else y + ci
                    ax.fill_between(time, lower, upper, color=colors[h], alpha=0.2, zorder=1)

        # overlays on every subplot except history size
        if (not skip_overlays) and overlay_avg is not None:
            for ov in overlay_vars:
                ax.plot(
                    overlay_avg["time"].values,
                    overlay_avg[ov].values,
                    zorder=0,
                    **overlay_style.get(ov, dict(color="0.25", linewidth=0.7, alpha=0.45)),
                )

        ax.set_title(v.removesuffix("[mean]"), fontsize=18)
        ax.grid(True, alpha=0.15)
        ax.tick_params(axis="both", labelsize=15)

    axes[-1].set_xlabel("time", fontsize=18)

    # single global legend: histories + overlays (if any)
    history_handles = [
        Line2D([0], [0], color=colors[h], lw=1.2, label=f"H={fmt_history(h)}")
        for h in histories
    ]
    overlay_handles = []
    if overlay_vars:
        overlay_handles = [
            Line2D([0], [0], label=f"{ov.removesuffix('[max]')}", **overlay_style.get(ov, dict(color="0.25", linewidth=0.7, alpha=0.45)))
            for ov in overlay_vars
        ]

    if show_legend:
        fig.legend(
            handles=history_handles + overlay_handles,
            loc="upper center",
            ncol=min(len(history_handles + overlay_handles), 4),
            fontsize=12,
            frameon=False,
        )

    plt.tight_layout(rect=(0, 0, 1, 0.98) if show_legend else None)

    out_dir = Path("charts")
    out_dir.mkdir(parents=True, exist_ok=True)
    legend_suffix = "_no_legend" if not show_legend else ""
    suffix = f"_conn{connection_range_percentage}_var{variability}{output_suffix}{legend_suffix}"
    base = out_dir / f"states_plus_disagreement_plus_historysize_by_history{suffix}"
    fig.savefig(base.with_suffix(".png"), dpi=300, bbox_inches="tight")
    fig.savefig(base.with_suffix(".pdf"), bbox_inches="tight")
    plt.close(fig)

    print(f"Saved to: {base}.png and {base}.pdf")

    if history_size_var is None:
        print("WARNING: history size variable not found (tried: historySize, history_size, history_size[mean], historySize[mean])")

    return fig


def plot_legend_only_for_history(means_data, connection_range_percentage, variability, output_suffix=""):
    """
    Create a standalone legend chart for plot_states_by_history.
    
    Parameters
    ----------
    means_data : xr.Dataset
        The dataset containing simulation means
    connection_range_percentage : float
        The connection range percentage to select
    variability : float
        The variability to select
    output_suffix : str
        Optional suffix for output filename
    """
    ds = means_data.sel(connection_range_percentage=connection_range_percentage, variability=variability)
    
    # overlays (avg over history_length)
    overlay_vars = [v for v in ["alarm[max]", "attacked[max]"] if v in ds.data_vars]
    
    histories = np.asarray(ds["history_length"].values, dtype=float)
    
    def fmt_history(h: float) -> str:
        if h == 6000:
            return "∞"
        return str(int(h)) if float(h).is_integer() else f"{h:g}"
    
    # distinct colors per history
    cmap = plt.cm.tab10 if len(histories) <= 10 else plt.cm.turbo
    colors = {h: cmap(i / max(1, len(histories) - 1)) for i, h in enumerate(histories)}
    
    overlay_style = {
        "alarm[max]": dict(color="red", linestyle="--", linewidth=1.2, alpha=0.6),
        "attacked[max]": dict(color="orange", linestyle=":", linewidth=1.2, alpha=0.6),
    }
    
    # Create figure with no axes, just for legend
    fig = plt.figure(figsize=(8, 2))
    
    # Create legend handles
    history_handles = [
        Line2D([0], [0], color=colors[h], lw=1.2, label=f"H={fmt_history(h)}")
        for h in histories
    ]
    overlay_handles = []
    if overlay_vars:
        overlay_handles = [
            Line2D([0], [0], label=f"{ov.removesuffix('[max]')}", **overlay_style.get(ov, dict(color="0.25", linewidth=0.7, alpha=0.45)))
            for ov in overlay_vars
        ]
    
    fig.legend(
        handles=history_handles + overlay_handles,
        loc="center",
        ncol=min(len(history_handles + overlay_handles), 6),
        fontsize=12,
        frameon=False,
    )
    
    # Remove axes
    fig.patch.set_visible(False)
    
    out_dir = Path("charts")
    out_dir.mkdir(parents=True, exist_ok=True)
    suffix = f"_conn{connection_range_percentage}_var{variability}{output_suffix}"
    base = out_dir / f"legend_only_by_history{suffix}"
    fig.savefig(base.with_suffix(".png"), dpi=300, bbox_inches="tight")
    fig.savefig(base.with_suffix(".pdf"), bbox_inches="tight")
    plt.close(fig)
    
    print(f"Saved legend to: {base}.png and {base}.pdf")
    
    return fig


def plot_states_by_variability(means_data, connection_range_percentage, stdevs_data=None, state_vars_to_show=4):
    """
    Plot state variables comparing different variability values.
    Shows only the first N state variables with all variabilities overlaid.
    
    Parameters
    ----------
    means_data : xr.Dataset
        The dataset containing simulation means
    connection_range_percentage : float
        The connection range percentage to select
    stdevs_data : xr.Dataset, optional
        The dataset containing standard deviations for confidence intervals
    state_vars_to_show : int
        Number of state variables to show (default: 4)
    """
    ds = means_data.sel(connection_range_percentage=connection_range_percentage)
    ds_std = stdevs_data.sel(connection_range_percentage=connection_range_percentage) if stdevs_data is not None else None
    
    # Average over history_length to focus on variability comparison
    ds_avg = ds.mean(dim="history_length", skipna=True)
    ds_std_avg = ds_std.mean(dim="history_length", skipna=True) if ds_std is not None else None

    # Resolve variable names present in the dataset
    disagreement_var = next((v for v in ["disagreementRate", "disagrementRate"] if v in ds_avg.data_vars), None)
    history_size_var = next(
        (v for v in ["historySize", "history_size", "history_size[mean]", "historySize[mean]"] if v in ds_avg.data_vars),
        None,
    )

    requested_state_vars = ["wandering[mean]", "defending[mean]", "wait[mean]", "solving[mean]"]
    if disagreement_var is not None:
        requested_state_vars.append(disagreement_var)
    if history_size_var is not None:
        requested_state_vars.append(history_size_var)

    state_vars = [v for v in requested_state_vars if v in ds_avg.data_vars][:state_vars_to_show]

    # overlays (avg over variability)
    overlay_vars = [v for v in ["alarm[max]", "attacked[max]"] if v in ds_avg.data_vars]
    overlay_avg = ds_avg[overlay_vars].mean(dim="variability", skipna=True) if overlay_vars else None

    variabilities = np.asarray(ds_avg["variability"].values, dtype=float)
    time = ds_avg["time"].values

    def fmt_var(v: float) -> str:
        return str(int(v)) if float(v).is_integer() else f"{v:g}"

    # distinct colors per variability
    cmap = plt.cm.viridis if len(variabilities) <= 10 else plt.cm.turbo
    colors = {v: cmap(i / max(1, len(variabilities) - 1)) for i, v in enumerate(variabilities)}

    overlay_style = {
        "alarm[max]": dict(color="red", linestyle="--", linewidth=1.2, alpha=0.6),
        "attacked[max]": dict(color="orange", linestyle=":", linewidth=1.2, alpha=0.6),
    }

    fig, axes = plt.subplots(
        nrows=max(1, len(state_vars)),
        ncols=1,
        figsize=(5.6, 1.25 * max(1, len(state_vars))),
        sharex=True,
    )
    axes = np.atleast_1d(axes)

    for ax, v in zip(axes, state_vars):
        is_disagreement = v in {"disagreementRate", "disagrementRate"}
        skip_overlays = (history_size_var is not None and v == history_size_var)

        # variability-colored lines
        for var in variabilities:
            if var==3: continue
            y = ds_avg[v].sel(variability=var).values
            if is_disagreement:
                y = (y > 0).astype(int)
            ax.plot(time, y, color=colors[var], linewidth=1.2, alpha=0.9, zorder=2)
            
            # Add 95% confidence interval (1.96 * std)
            if ds_std_avg is not None and v in ds_std_avg.data_vars:
                std = ds_std_avg[v].sel(variability=var).values
                ci = 1.96 * std
                if not is_disagreement:
                    # Check if variable is a proportion (values between 0 and 1)
                    is_proportion = (np.nanmin(y) >= 0 and np.nanmax(y) <= 1)
                    lower = np.clip(y - ci, 0, 1) if is_proportion else y - ci
                    upper = np.clip(y + ci, 0, 1) if is_proportion else y + ci
                    ax.fill_between(time, lower, upper, color=colors[var], alpha=0.2, zorder=1)

        # overlays on every subplot except history size
        if (not skip_overlays) and overlay_avg is not None:
            for ov in overlay_vars:
                ax.plot(
                    overlay_avg["time"].values,
                    overlay_avg[ov].values,
                    zorder=0,
                    **overlay_style.get(ov, dict(color="0.25", linewidth=0.7, alpha=0.45)),
                )

        if is_disagreement:
            ax.set_title(f"{v.removesuffix('[mean]')} (>0 → 1 else 0)", fontsize=16.5)
            ax.set_ylim(-0.05, 1.05)
            ax.set_yticks([0, 1])
        else:
            ax.set_title(v.removesuffix("[mean]"), fontsize=16.5)

        ax.grid(True, alpha=0.15)
        ax.tick_params(axis="both", labelsize=15)

    axes[-1].set_xlabel("time", fontsize=18)

    # single global legend: variabilities + overlays (if any)
    var_handles = [
        Line2D([0], [0], color=colors[v], lw=1.2, label=f"k={fmt_var(v)}")
        for v in variabilities
    ]
    overlay_handles = []
    if overlay_vars:
        # remove also [max] from label (ov)
        overlay_handles = [
            Line2D([0], [0], label=f"{ov.removesuffix('[max]')}", **overlay_style.get(ov, dict(color="0.25", linewidth=0.7, alpha=0.45)))
            for ov in overlay_vars
        ]

    fig.legend(
        handles=var_handles + overlay_handles,
        loc="upper center",
        ncol=min(len(var_handles + overlay_handles), 7),
        fontsize=10.5,
        frameon=False,
    )

    plt.tight_layout(rect=(0, 0, 1, 0.98))

    out_dir = Path("charts")
    out_dir.mkdir(parents=True, exist_ok=True)
    print()
    file = f"states_comparison_conn{connection_range_percentage}_by_variability"
    fig.savefig(f"{out_dir}/{file}.png", dpi=300, bbox_inches="tight")
    fig.savefig(f"{out_dir}/{file}.pdf", bbox_inches="tight")
    plt.close(fig)

    print(f"Saved to: {out_dir}/{file}.png and {out_dir}/{file}.pdf")

    return fig


# Example usage: plot for specific connection_range and variability
plot_states_by_history(means["simulation"], connection_range_percentage=0.2, variability=10.0, stdevs_data=None, show_legend=False)
# Example usage: plot for specific connection_range and variability
plot_states_by_history(means["simulation"], connection_range_percentage=0.15000000000000002, variability=10.0, stdevs_data=None, show_legend=False)
# Example usage: plot for specific connection_range and variability
plot_states_by_history(means["simulation"], connection_range_percentage=0.15000000000000002, variability=10.0, stdevs_data=None, show_legend=False)

# Example usage: plot without legend
plot_states_by_history(means["simulation"], connection_range_percentage=0.2, variability=10.0, stdevs_data=None, show_legend=False)

# Example usage: create standalone legend
plot_legend_only_for_history(means["simulation"], connection_range_percentage=0.2, variability=10.0)

# Example usage: plot comparing variabilities (first 4 state vars)
plot_states_by_variability(means["simulation"], connection_range_percentage=0.2, stdevs_data=None, state_vars_to_show=4)

# Example usage: plot disagreement rate as bar chart for all configurations
plot_disagreement_bar_chart(means["simulation"])

# Example usage: plot only Weibull frequency distributions
plot_weibull_frequencies_only(means["simulation"], connection_range_percentage=0.2)
