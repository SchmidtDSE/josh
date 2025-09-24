# Two Trees

In our [previous tutorial](grass_shrub_fire.md), we explored patch-based modeling. In this next step, we will look at modeling where we are simulating individual organisms.

## Table of Contents

- [Overview](#overview)
- [Data](#data)
- [Simulation](#simulation)
- [Init](#init)
- [Death](#death)
- [Extremes](#extremes)
- [Birth](#birth)
- [SpeciesA](#species-a)
- [SpeciesB](#species-b)
- [Wrap](#wrap)

## Overview

You are already becoming an expert in Josh!

This next example is just using what you already learned but making it a little bigger. Specifically, we are going to run a simulation with lots of agents with each representing a single tree. This can sometimes be useful to include dynamics which are difficult to describe at the patch.

As with the other tutorials, this is a toy model using fake species. The use of specific location is just so that we can use external data.

## Data

This time we are going to use data from [Cal-Adapt](https://cal-adapt.org/) which provides its data as netCDF files for Tulare County, California. The data comes from the FGOALS-g3 climate model under the SSP2-4.5 emissions scenario (2015-2100). Again, a [later tutorial will go into the details of preprocessing](data.md) for speed.

The climate data uses custom unit conversions in Josh. Temperature data is in Kelvin (K), while precipitation data is provided in kg m^-2 s^-1 and automatically converted to mm/year using a custom `kgm2s` unit definition.

For now, go ahead and grab our preprocessed [temperature data](/guides/two_trees/temperatureTulare.jshd) and [precipitation data](/guides/two_trees/precipitationTulare.jshd). Then, delete the data from the prior tutorial and upload these new files.

### Data Sources

- **Cal-Adapt:** [Cal-Adapt](https://cal-adapt.org/) - Climate data under [no restrictions](https://data.ca.gov/dataset/10-model-ensemble-30-year-named-climate-period-average-precipitation)
- **Model:** FGOALS-g3 SSP2-4.5 scenario for Tulare County (FIPS 06107)

## Simulation

The simulation here will look pretty similar. Let's go ahead and define our boundaries and time range.

```joshlang
start simulation Main

  # Specify where the simulation runs
  grid.size = 200 m
  grid.low = 36.52 degrees latitude, -118.68 degrees longitude
  grid.high = 36.42 degrees latitude, -118.45 degrees longitude

  # Specify when the simulation runs (2024 to 2054)
  steps.low = 0 count
  steps.high = 30 count

  startYear.init = 2024
  year.init = startYear
  year.step = prior.year + 1

  # Indicate ecological and spatial limits
  constraints.maxOccupants = 30 count
  constraints.minViableTemperature = 273 K
  constraints.maxViableTemperature = 373 K

  # Indicate exports
  exportFiles.patch = "memory://editor/patches"

end simulation
```

As [before](grass_shrub_fire.md), we have some constants that we will use across multiple organisms.

## Init

Let's start with initializing occupancy of these patches. We want to have this step use the maximum occupancy from the simulation definition.

```joshlang
start patch Default

  # Randomize starting occupancy
  countOccupancy.init = sample uniform from 0 count to meta.constraints.maxOccupants
  initialACount.init = (sample uniform from 0% to 100%) * countOccupancy
  initialBCount.init = countOccupancy - initialACount

  # Create initial occupants
  SpeciesA.init = create initialACount of SpeciesA
  SpeciesB.init = create initialBCount of SpeciesB

end patch
```

## Death

Unlike our ForeverTree, these trees can die. We will set a property on our agents called dead and remove those from the patch that have dead == true.

```joshlang
start patch Default

  # ... prior code here ...

  # Remove dead occupants at start of simulation step
  SpeciesA.start = prior.SpeciesA[prior.SpeciesA.dead == false]
  SpeciesB.start = prior.SpeciesB[prior.SpeciesB.dead == false]
  speciesACount.start = count(SpeciesA)
  speciesBCount.start = count(SpeciesB)

end patch
```

This is mostly similar to what you've seen before but you may notice that there is also a start event handler type in addition to init and step handlers. Each step is actually broken into three phases: start, step, and end. This lets you run multiple calculations on a variable within a single step if needed where you can control the order of those calculations. Also, this is the first time we see a slice. For example, in "SpeciesA[prior.SpeciesA.dead == false]" at the start of each step we remove dead trees by filtering for those that aren't dead and saving the resulting distribution "slice" back to the SpeciesA attribute on Default patch.

## Extremes

Next, we will also check for extreme temperature events which our organisms will respond to.

```joshlang
start patch Default

  # ... prior code here ...

  # Determine if in an extreme temperature event
  inExtremeTemperature.step = {
    const currentTemperature = external temperatureTulare
    const inExtremeCold = currentTemperature < meta.constraints.minViableTemperature
    const inExtremeHot = currentTemperature > meta.constraints.maxViableTemperature
    return inExtremeCold or inExtremeHot
  }

end patch
```

Don't worry! You will see the organisms (agents) use this soon.

## Birth

Next, we also want to take care of making new trees.

```joshlang
start patch Default

  # ... prior code here ...

  # Calculate num new occupants end step (to have their first step on next step)
  totalCount.start = speciesACount + speciesBCount
  newCount.end = {
    const probabilityNewRaw = (-1 count * (totalCount - 5 count) ^ 2 / 20 count) + 110 %
    const probabilityNew = limit probabilityNewRaw to [0%, 100%]
    const maxNewCount = (meta.constraints.maxOccupants - totalCount) * probabilityNew
    return sample uniform from 0 count to maxNewCount
  }

  # If empty, split evenly
  newSpeciesACount.end = {
    if (totalCount > 0) {
      return floor((speciesACount / totalCount) * newCount)
    } else {
      return newCount * 50%
    }
  }

  newSpeciesBCount.end = {
    if (totalCount > 0) {
      return floor((speciesBCount / totalCount) * newCount)
    } else {
      return newCount * 50%
    }
  }

  # Make the new agents and concatenate to ("|" or add to end of) the current list.
  SpeciesA.end = prior.SpeciesA | create newSpeciesACount of SpeciesA
  SpeciesB.end = prior.SpeciesB | create newSpeciesBCount of SpeciesB

  # Export counts
  export.newSpeciesACount.end = newSpeciesACount
  export.newSpeciesBCount.end = newSpeciesBCount

end patch
```

Here, we are getting a closer look at distributions in Josh. These are collections of numbers similar to lists or arrays in other languages. However, they may also be formal distributions like normal or uniform. We can combine two distributions together using the pipe character ("|") and we can filter the elements within a distribution using square brackets like we saw in our discussion of slices.

Before finishing up this step, note that SpeciesA is a variable or attribute of the Default patch. By convention, it uses the same name as the organism. That said, you could also call it "SpeciesAPresent" and things would work out.

### More about conventions

Conventions are informal rules that a community follows even if they are not strictly enforced by the programming language itself. In the Josh language, we recommend use leading lower camelCase to refer to normal variables, leading capital CamelCase to refer to entities, and leading capital CamelCase of the same name as the entity to refer to sub-entities like SpeciesA individuals inside a Default patch. All this in mind, these are just suggestions. You can change this as you like!

## SpeciesA

That was most of the new stuff! Let's go ahead and use some of our prior tools to define these two tree species.

Each of these will work by looking at by how much growing conditions deviate from species-specific optimal values. These are defined through a quadratic for temperature and sigmoid for precipiation. We use these to determine what percent of optimal growth each agent sees in each time step. This is done by multiplying two "impact" values together as a scale variable.

```joshlang
start organism SpeciesA

  # Start a zero height except when if this is the simulation start
  dead.init = false
  height.init = {
    const isFirstYear = meta.startYear == meta.year
    const heightSample = sample normal with mean of 20 meters std of 5 meters
    const heightSampleLimit = limit heightSample to [0 meters, 40 meters]
    return heightSampleLimit if isFirstYear else 0 meters
  }

  # At each step, determine impact of temperature
  temperatureImpact.step = map external temperatureTulare
    from [270 K, 330 K]
    to [0%, 100%]
    quadratic

  # At each step, determine impact of precipitation
  precipitationImpact.step = map external precipitationTulare
    from [300 mm, 500 mm]
    to [0%, 100%]
    sigmoid

  # At each step, determine overall growth
  newGrowth.step = 1 m * temperatureImpact * precipitationImpact
  height.step = prior.height + newGrowth

  # At each step end, determine death if over survival probability
  dead.end = {
    const deadByChance = (sample uniform from 0% to 100%) > 30%
    const deadByExtremeTemp = here.inExtremeTemperature
    return deadByChance or deadByExtremeTemp
  }

end organism
```

There are two things to highlight in this code. First, we can see now where the variable in the slice used above gets used. Second, like how we did a map with a sigmoid before, we use a quadratic here. By default, quadratic puts the center of the range at the maximum (upside down parabola) but this can be modified by using "quadratic(false)" instead.

## SpeciesB

In practice, different species will probably vary quite a bit from each other in their defintion. However, for this toy example, SpeciesB will be about the same as SpeciesA just with some different parameters. Let's take a look!

```joshlang
start organism SpeciesB

  # Start a zero height except when if this is the simulation start
  dead.init = false
  height.init = {
    const isFirstYear = meta.startYear == meta.year
    const heightSample = sample normal with mean of 20 meters std of 5 meters
    const heightSampleLimit = limit heightSample to [0 meters, 40 meters]
    return heightSampleLimit if isFirstYear else 0 meters
  }

  # At each step, determine impact of temperature
  temperatureImpact.step = map external temperatureTulare
    from [290 K, 350 K]
    to [0%, 100%]
    quadratic

  # At each step, determine impact of precipitation
  precipitationImpact.step = map external precipitationTulare
    from [200 mm, 400 mm]
    to [0%, 100%]
    sigmoid

  # At each step, determine overall growth
  newGrowth.step = 1 m * temperatureImpact * precipitationImpact
  height.step = prior.height + newGrowth

  # At each step end, determine death if over survival probability
  dead.end = {
    const deadByChance = (sample uniform from 0% to 100%) > 30%
    const deadByExtremeTemp = here.inExtremeTemperature
    return deadByChance or deadByExtremeTemp
  }

end organism
```

Everything is about the same here except for the parameterization of those important temperature and precipitation impact variables.

## Wrap

Let's wrap this up! We just need our units. This time, we will also demonstrate use of unit conversions. Try changing one of the temperature values above to celcius to give it a shot.

```joshlang
start unit kgm2s
  # kg m-2 s-1 - precipitation rate used in climate data
  # To convert to mm/year, multiply by seconds per year (31,536,000)
  # since 1 kg/m² of water = 1 mm of precipitation

  mm = current * 31536000

end unit

start unit K

  alias Kelvins
  alias Kelvin

end unit


start unit C

  alias Celcius

  K = 272.15 + current

end unit


start unit mm

  alias millimeters
  alias millimeter

  m = current / 1000

end unit
```

Go ahead and give it a run. This one might take a little longer! If you have [Josh Cloud](/community.html), this might be a good time to give it a shot. When ready, let's move forward to a [tutorial on data preprocessing](data.md).

[Download Complete Code](/examples/guide/two_trees.josh)