# Hello Grid!

Josh is a simulation engine that includes everything you need from handling stochastic properties to working with geospatial data formats. In this first tutorial, we will explore a simple getting started example that introduces the basic concepts of Josh and also Josh Lang, the configuration language used to define simulations.

## Table of Contents

- [Overview](#overview)
- [Editor](#editor)
- [Simulation](#simulation)
- [Patch](#patch)
- [Organism](#organism)
- [Units](#units)
- [Exports](#exports)
- [Visualizing](#visualizing)
- [Replicates](#replicates)
- [Wrap](#wrap)

## Overview

Josh aims to make it easy to build spatial vegetation models. It is intended to help with the logistics of simulations including agents, geospatial data, scalability, and stochastic behavior. In these tutorials, we will explore these [features](/purpose.html) in hands-on practical examples.

### Why Josh Lang?

Sometimes expressing these systems in general purpose languages like Python can require a lot of code. We built Josh Lang to be a configuration language which is designed specifically for this use case and which includes syntax that, not only cutting down on development time, tries to improve clarity in the code defining how these systems work.

With these goals in mind, we are going to simulate a very basic system: a ForeverTree. This fake species for this toy demo just grows and gets older. While this seems simple, this basic entry point will enable us to explore all of the core concepts of Josh.

## Editor

The easiest way to use Josh is to take advantage of [our online development environment](https://editor.joshsim.org). You don't need to install any software and you can run simulations without leaving your browser. This will not share your simulation or data with us (unless you opt to run on [Josh Cloud](/community.html)). Also, your work is saved on your machine. All of this means that you can program in safety and privacy but also don't need to configure your computer.

While we will discuss [running simulations outside your browser](cli.md) later and this can be much faster, we will assume that you are using the [Josh IDE](https://editor.joshsim.org) for the purposes of this guide.

## Simulation

It all starts with a simulation. This is where we tell Josh where in the world we are running our model and for how long. Let's define a 1 kilometer grid that is around Joshua Tree National Park. We will simulate from 2025 to 2035. Let's take a look at the code.

```joshlang
start simulation Main

  grid.size = 1000 m
  grid.low = 33.7 degrees latitude, -115.4 degrees longitude
  grid.high = 34.0 degrees latitude, -116.4 degrees longitude
  grid.patch = "Default"

  steps.low = 0 count
  steps.high = 10 count

end simulation
```

We called this simulation Main but you could call it anything you like. Note that Josh runs simulations where it computes everything within our simulated area once per step. So, `steps.low` and `steps.high` tells Josh for how many timesteps we want to execute our work. In our case, we will assume that each step is a year and we run from year 0 to year 11. However, you can run simulations where each step is a day or a month.

All together, this means that Josh will simulate each 1 km "patch" within our simulation area for each year from year 0 to year 10.

## Patch

Next, we need to tell Josh about our grid cells. These are referred to as patches. Here, we will create ten trees on each patch. Josh knows which kind of patch to use because of the name we specified above. Here we encounter our first use of init.

```joshlang
start patch Default

  ForeverTree.init = create 10 count of ForeverTree

end patch
```

Init tells Josh that you want some calculation to run when something is made. In this case, when the patch is created in the simulation. In other words, we are telling Josh that, when a patch is created within our simulation, we should place 10 forever trees in that patch.

## Organism

We've told Josh about our grid and we've told it about the patches we want to fill that grid. At last, we can tell Josh how we want our organism (ForeverTree) to behave.

```joshlang
start organism ForeverTree

  age.init = 0 year
  age.step = prior.age + 1 year

  height.init = 0 meters
  height.step = prior.height + sample uniform from 0 meters to 1 meters

end organism
```

When we create a new ForeverTree, we give it an age. In this case, it starts as zero years old. However, we now see not just the use of the init event callback but the step event.

Using this step callback, Josh will add one year to the tree's age (prior means the value of the variable prior to that step starting). Something similar happens for height but with a twist: we are sampling the annual growth for each tree from a random uniform distribution.

### About random distributions

We can sample different kinds of distributions in our simulations to try to capture real-world behaviors. This also lets us use Monte Carlo which involves running many simulations with lots of random sampling to see a universe of future possibilities. A uniform distribution means that every number has the same probability of being selected. So, a uniform distribution from 1 to 5 means 2 and 3 have the same probability of being selected. Later tutorials will look at other random distributions like the normal distribution.

## Units

So far, we've been giving Josh not just numbers like 33.7 but those numbers have units like degrees. Your simulations can do unit dimensional analysis for you, performing conversions if needed. Josh has meters (meter, meters, m), count (count, counts), and degrees (degree, degrees) built in. However, it doesn't know about years. Let's define it.

```joshlang
start unit year

  alias years
  alias yr
  alias yrs

end unit
```

A later tutorial will discuss unit conversions. For example, you might want to define months and tell Josh that a year is equivalent to 12 months. However, for now, this code snippet simply tells Josh that years is a valid unit and it may go by different names: year, years, yr, yrs.

## Run

You have everything you need to run your first simulation! Go ahead and copy the four stanzas above into the text editor and run in your browser. Give it a minute or two to do its job. You are simulating over 30,000 trees! Then, let's look at the output.

## Exports

Your simulation probably ran but you likely didn't see any outputs. We have to tell Josh which variables to export. You can send data from your simulations to CSV files which you can open in spreadsheet software, COGs / geotiffs, or netCDF files. You can also do these exports for variables on simulations, patches, or organisms.

All of these options in mind, one of the most useful export targets it the Josh IDE itself. You can send these data into the editor for immediate visualization and, if desired, export. This can help refine a simulation as you are building it. Let's start there by changing our definition of the Default patch, indicating what data we want to export.

```joshlang
start patch Default

  ForeverTree.init = create 10 count of ForeverTree

  export.averageAge.step = mean(ForeverTree.age)
  export.averageHeight.step = mean(ForeverTree.height)

end patch
```

The code we wrote tells Josh to record the average age of the trees in each patch along with the average height. However, we also need to tell Josh to send these results to the editor for visualization. That is done on the simulation.

```joshlang
start simulation TestSimpleSimulation

  grid.size = 1000 m
  grid.low = 33.7 degrees latitude, -115.4 degrees longitude
  grid.high = 34.0 degrees latitude, -116.4 degrees longitude

  steps.low = 0 count
  steps.high = 10 count

  exportFiles.patch = "memory://editor/patches"

end simulation
```

This says to send patches to the editor "in memory" which just means it isn't being saved to your computer. Note that the Josh IDE can only use memory targets. If you want to use disk targets, you need to [run simulations outside your browser](cli.md). Anyway, with that, give it another shot.

## Visualizing

You should see tools for visualization including a bar chart showing the overall simulation-wide values over time. You can change the timestep shown by clicking on the different bars. Go ahead and try a few different options in the dropdown menus and see how values change over time in the simulation!

Also, if you have a [Mapbox API Key](https://www.mapbox.com), you can also add satellite imagery or reference points underneath your heatmap. Simply click the map button. Then, choose one of the basemap options.

We are almost done but lets finish off our introduction by looking at replicates.

## Replicates

So far, we've only run with a single replicate. However, if you go back to the run dialog, you can indicate that you want more than one replicate. This can be a bit slow to execute in your browser. If you are [running Josh locally](cli.md), have access to [Josh Cloud](/community.html), or have an [IT team which has set up a data center](/use.html), you can run multiple replicates in parallel.

If you have access to one of those other systems, give it a try now with 10 replicates. Otherwise, go ahead and run in your browser with 2.

## Export

After running for a larger number of replicates, try clicking the export button. This will provide CSV files which can be opened in your spreadsheet software. For exporting geotiffs and netCDF files, see [running Josh locally](cli.md).

## Wrap

That's it for our first introduction. In the [next tutorial](grass_shrub_fire.md), we will explore some of the tools that Josh makes available for real world simulation including use of external data and some syntax which makes it easier to define these mathematical systems.

[Download Complete Code](/examples/guide/hello.josh)