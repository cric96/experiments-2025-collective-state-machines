# Collective Finite State Machines

This repository contains the source code and experiments for the paper:

> **Macroscopic Design of Swarms with Collective State Machines**

The goal of this work is to validate the concept of **collective finite state machines (cFSMs)** in the context of aggregate programming. A cFSM extends classical finite state machines so that states denote *collective behaviours* and transitions represent *collective agreements* on strategy evolution, enabling a multi-agent system to behave as a single coordinated entity.

## Overview

The cFSM model is formalised on augmented event structures and operationalised within the [ScaFi](https://github.com/scafi/scafi) aggregate programming framework. The experimental evaluation targets three properties:

1. **Correctness** — all agents eventually converge to a consistent collective state.
2. **Resilience** — agreement is maintained or recovered under asynchronous execution and conflicting events.
3. **Practical history independence** — the system operates continuously with bounded memory via compressed histories.

## Requirements

- A working **Java** installation (JDK) > 18
- **Gradle** (the included Gradle wrapper `gradlew` is sufficient)

## Case Study Description

The case study simulates a **search-and-rescue mission** performed by a swarm of **40 autonomous drones** in a military-inspired scenario. The drones are deployed in a **500 × 500 m²** operational area, with a maximum speed of **10 m/s** and a variable communication radius *R*.

The swarm collectively transitions among four states:

| State | Description |
|-------|-------------|
| **Wait** | Drones stay at the base station (e.g., recharging or maintenance). |
| **Wander** | Drones explore the area to locate a target (e.g., a lost soldier). |
| **Solve** | Drones converge on the target location to perform rescue. |
| **Defend** | Drones return to protect the base station from an attack. |

Two types of events drive the mission: **alarm events** (triggering target search) and **base attacks** (emergency interrupts with higher priority). A task is considered complete when all 40 drones converge within 100 m of the relevant location and hold position for 60 s. During the simulation, a deliberate conflict is introduced — a base attack occurs while a search mission is active — to assess how the cFSM resolves competing stimuli via its priority mechanism.

### Simulation Parameters

| Parameter | Values |
|-----------|--------|
| Communication radius *R* | 75 m, 100 m |
| History time window *H* | 1200 s, ∞ |
| Weibull shape *k* (execution frequency variability) | 4, 7, 10 |
| Number of independent runs per configuration | 128 |
| Simulation duration | 4000 s |

The simulation is powered by [Alchemist](https://alchemistsimulator.github.io/), a stochastic discrete-event simulator for pervasive computing. Drone movement is handled by [MacroSwarm](https://github.com/scafi/macro-swarm), a swarm robotics library built on top of ScaFi.

## How to Run the Experiments

Run all experiments in batch mode (headless):

```bash
./gradlew runCaseStudyMachineBatch
```

To launch a single run with the graphical visualisation:

```bash
./gradlew runCaseStudyMachineGraphic
```

## How to Generate Charts

To generate the plots from the experimental data, run:

```bash
# TODO: add the chart generation command here
```

