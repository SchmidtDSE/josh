---
title: "Josh Language Specification 2.0"
header-includes: |
  \usepackage{float}
  \floatplacement{figure}{H}
date: Mar 6, 2025
author:
  - A Samuel Pottinger
  - Nick Gondek
  - Lucia Layritz
  - Maya Zomer
  - Maya Weltman-Fahs
---

Focused on vegetation, the Josh language allows for the description of multi-occupancy patch-based ecological simulations in which multiple species occupying a grid cell describing a small segment of a community can be described through individual behaviors with optional state changes.


# Purpose
This language specifically seeks to support the nexus between science, policy, and software engineering by executing vegetation-focused ecological simulations to support management decisions. This specification prefers a SmallTalk / HyperTalk-like language that prioritizes an imperative design with proceedural elements that may operate at a community level in simulation execution but allows for specification of behavior at the level of an individual organism.

## Audience
The purpose of this specification suggests a heterogenous audience: those expressing the logic of these simulations and those reading that logic come from diverse academic and intellectual backgrounds where we believe those identifying as professional software engineers may possibly represent a minority. In this community, we do not expect that a single general purpose programming language is known by the vast majority of participants. Therefore, in addition to the detailed prioritization provided elsewhere within this Motivation section, this specification specifically focus on constructs which support that broad user base over providing computational tools which may be required by software engineers to provide very specific fine-tuning of computational flow, parallelization, optimization, etc. As this specification further describes, we find that a domain specific language instead of a library in a pre-existing general purpose language better satisfies the goals of this effort and the needs of its audience.

## Systems in scope
Different forms of modeling may intersect to underpin the ecological simulation practically needed to inform management decisions. Josh considers support for two types of computational systems. In these simulations, individual behaviors have may be evaluated at varying degrees of fidelity extending all the way to the individual organism. This requires language feature for behaviors expressed both at levels of a community and an individual. In all situations, these systems respond to disturbances such as fire or may be influenced by intentional management choices.  Additionally, many of these systems include stochastic elements which, via Monte Carlo, can create distributional outputs.

### Independent patches
In some cases, communities or “patches” representing very large numbers of individuals may be described as a series of interconnected equations independent of other patches. Though co-occupying species may interact, these descriptions form a directed acyclic graph of formulas which may ultimately depend on nothing else other than time. Highly amenable to parallelized computation, these systems can often evaluate very quickly. This perspective motivates the inclusion of aggregated communities into the language. In structuring computation, this perspective also embeds the idea that organism movement is secondary (vegetation communities move but vegetation individual are less likely to move).

### Interacting patches
In other ecosystems, cells existing within some form of geospatial grid may interact with outside simulation results like CMIP outputs or with each other through time as individuals move between discrete states. For example, some simulations may be model the spread of seeds. Due to interactions both spatially and temporally, these systems often require more intensive computation. This perspective motivates the inclusion of agents with individual behaviors into the language. 

## Design priorities
In these systems, output correctness is may be easiest to understand at the level of an individual and a very large ecosystem. While these models produce outputs at intermediate scales essential for management decisions, this liminal space of confirmability may become more acceptable as trust grows in system behaviors at those very small and very large scales.

### Readability over compactness
Building this trust requires that code exhibits a high degree of interpretability for a broad audience. This includes those who may be less familiar with traditional software development that need to enter into dialogue with the simulation logic in order to both understand simulation mechanics and defend resulting management choices. This need becomes especially acute given that the programming languages which actually execute these simulations are more likely to be high-performance systems-level languages where higher order ideas around pointers, objects, matrix operations, and graph traversal algorithms may inhibit a broader ecosystem of practitioners to participate. In short, the dual goals of running simulations and reading the logic of those simulations stand on equal footing. In response, this specification takes inspiration from SmallTalk / HyperTalk-derivative languages which emphasize object-oriented readability over a compact representation in code. In other words, use of “programming characters” like curly braces are minimized to make more “natural language” statements.

### Imperative over procedural
The idea of a “serialized” set of discrete steps inherent in many general purpose programming languages contrasts with natural systems where all organisms are taking action or being acted upon in parallel in response to simultaneously occurring environmental factors. Therefore, this specification prioritizes the description of entity behavior over allowing for specification of the order in which computation running those behaviors is actually executed. This means that, unlike general purpose languages, control flow is decided by the interpreter or compiler and not the simulation author. This makes this language more like an imperative language with procedural elements than a procedural one with imperative elements.

### Behavioral over computational description
Given the heterogeneity of the anticipated user base, this specification prioritizes fluency in expressing the logic of a simulation over specification of the specifics for how that computation is optimized or parallelized. In the same way that general purpose languages perform optimizations at compile-time the source code for simulations may speak to behavior at the level of an individual organism even as operations are likely to become optimized within the interpreter / compiler to avoid intractable expensive computation that could potentially involve the simulation of millions of individual agents.

### Broadcast information not agents
Simulations create graphs of computation where some formulas are evaluated as part of solving others. This specification enforces non-cyclicality to avoid the need for embedded numerical optimization. This specification resolves this tension by enforcing a Markov-like property where information needed to update a cell can only rely on one of the following:

 - Current state of the cell absent circular dependencies for formulas used within a cell.
 - State of another cell from only the immediate prior time step.

Practicall speaking, this means that a cell may update its own state baesd on these two pieces of information but may not update the state of another cell. This concept is raised to simulation authors.

## Stochasticity
Just as organisms and communities need to be the primary “nouns” that a user manipulates, stochastic elements also must be a primary “type” that the user can express and engage with. In contrast to many general purpose languages, working with probability distributions needs to be similarly fluent as working with discrete numbers.


# Structures
Similar to other SmallTalk / HyperTalk-like languages, this specification uses a stanza approach. Each stanza can update attributes and attributes may have events. Each stanza defines an entity (object).

## Stanzas
Each stanza has a start and end keyword. The language is whitespace agnostic and each statement has regular syntax such that end statement characters like semicolons are not required. Names may be alphanumeric.

```
start organism JoshuaTree

  # ...

end organism
```

## Attributes
Each stanza can manipulate attributes. These are variables attached to a specific agent like a specific tree. This can take a lambda-like notation:

```
start organism JoshuaTree

  age.init = sample ageGeotiff

end organism
```

This can also take a full body notation:

```
start organism JoshuaTree

  age.step = {
    const currentAge = self.age
    const newAge = currentAge + 1 year
    return newAge
  } 

end organism
```

Local variables are not supported outside full body notation.

## Events
Attributes have different events such as init and step. These can be conditional like on other entities being present in the same patch or cell:

```
start organism Deciduous

  cover.end:if(max(Conifer.cover) > 0%) = {
    const maxCover = map mean(Conifer.cover) from [0%, 90%] to [0%, 100%]
    return limit self.cover to [,maxCover]
  }

end organism
```

This can also be stochastic:

```
start organism Confier

  cover.step:if(sample uniform from 0% to 100% > 50%) = {
    const growth = sample normal with mean of 5% std of 1%
    const newCover = limit self.cover + growth to [0%, 100%]
    return newCove
  }

end organism
```

Finally, this can depend on external variables like for environment:

```
start organism Confier

  cover.step:if(mean(growSeasonPreciptation) > 1 in) = {
    const growth = sample normal with mean of 5% std of 1%
    const prior = self.cover
    const new = limit prior + growth to [0%, 100%]
    return new
  }

end organism
```


# Entities
There are disctinct types of entites that should be treated differently by the compiler / interpreter.

## Simulation
Settings which dictate behavior of the entire simulation are dictated in a Simulation entity.

```
start simulation Detailed

  grid.size = 1km
  grid.start = 34 degrees latitude, -116 degrees longitude
  grid.end = 35 degrees latitude, -115 degrees longitude

end simulation
```

This can include generalized sampling behavior:

```
start simulation Coarse

  grid.size = 1km
  grid.start = 34 degrees longitude, -116 degrees latitude
  grid.end = 35 degrees longitude, -115 degrees latitude
  
  sampling = 1%  # Sample 1% of individuals in each patch

end simulation
```

Alternatively, sampling can be specified per agent:

```
start simulation Coarse

  grid.size = 1km
  grid.start = 34 degrees longitude, -116 degrees latitude
  grid.end = 35 degrees longitude, -115 degrees latitude
  
  sampling.JoshuaTree = 1%  # Sample 1% of individuals in each patch
  sampling.ShrubGrass = 100 count  # Sample

end simulation
```

Finally, simulations can also control the sampling behavior for distribution arithmetic:

```
start simulation Coarse

  grid.size = 1km
  grid.start = 34 degrees latitude -116 degrees longitude
  grid.end = 35 degrees latitude -115 degrees longitude
  
  sampling.general = 1000 count

end simulation
```

The user should specify the name of the simulation to execute.

## Patch
Patches are the grid cells in which communities of organisms may be present.

```
start patch Default

  location = all
  JoshuaTree.init = create sum(locationsGeotiff) JoshuaTree
  JoshuaTree.step = JoshuaTree - JoshuaTree[JoshuaTree.state == "dead"]
  JoshuaTree.step = {
    const new = create 1 count if sum(JoshuaTree) < 10 count else 0 JoshuaTree
    const prior = JoshuaTree
    return prior + new
  }

end patch
```

Some simulations may represent simple occupancy as a binary where only one agent represent a community. In this case, create can take a boolean value.

```
start patch Default

  location = all
  JoshuaTree.init = {
    const numNew = 1 count if sum(locationsGeotiff) > 0 count else 0 count
    const new = create numNew JoshuaTree
    return new
  }

end patch
```

Attributes may also be added to patches. This can be used in exporting and visualizing results:

```
start patch Default

  location = all

  # ...

  occupationScore.step = sum(JoshuaTree.cover)

end patch
```

Location of all will use this patch across the entire simulation. However, patches may vary geographically.

```
start patch Wet

  location = mean(percepitationGeotiff) > 5 in

end patch
```

The location "all" will apply if no other patch value matches. Note that, if more than one patch applies, only one patch will be made at each location per the grid size but the patch type chosen is not defined. Some implementations may raise an exception.

## Organism
Organisms can define attributes which have event handlers. These can have conditional behaviors:

```
start organism Conifer

  state.step:if(max(Fire.cover) > 0%) = "stump"

end organism
```

These conditions can be strung together in if / else relationships:

```
start organism Conifer

  state.step
    :if(max(Fire.cover) > 50%) = "dead"
    :elif(max(Fire.cover) > 0%) = "stump"

end organism
```

The keywords if, elif (else if), and else are available. Only one else is allowed per conditional group. Note that handlers can be exected in parallel:

```
start organism Deciduous

  seedBank.step = self.seedBank + 5%
  seedBank.step:if(max(Fire.cover) > 0%) = "seed"

end organism
```

Limited control is available over ordering of events. See events.

## Disturbance
In addition to "organism" one can specify other types of agents representing disturbances:

```
start disturbance Fire

  cover.init = 0%
  cover.step = self.cover + 5%

end disturbance
```

Disturbances are the same as organisms but an alternative label is provided for convienence.

## Management
Management agents can be created through user configuration or user interactions.

```
start management Planting

  cover.init: mean(plantingMap)

end management
```

These are typically markers used that other agents can react to

## External
External data layers can also add additional information that can be used by agents. Data inside these layers cannot be edited.

```
start external growingSeasonPrecipitation

  year.init = meta.stepCount + 2050 count  # Simulation starts in 2050
  source.location = "file://precipitation/{{ self.year }}.geotiff"
  source.format = "geotiff"
  source.units = "in / month"

end external
```

These may be used for environmental data such as temperature projections. Location may support different handlers such as `https://`.

## Events
The init event will only be executed at the initalization of a simulation or the creation of an entity. The remove event will only be executed when an entity is removed from a grid cell. There are also start, step, and end events which correspond to when in a simulation timestep the event handler should be invoked. All start will execute before step which will all execute before end though order of execution is not guaranteed within these groups. A modifier of `:if(meta.stepCount == 0 count)` can be used to determine if init is being run as part of simulation initalization (prior to starting) or if an agent is made while the simulation is running.

## States
All entities have a default state but custom states can be added. This can be used to specify behavior which only happens during certain states.

```
start organism Conifer

  state.step:if(max(Fire.cover) > 0%) = "stump"

  start state "stump"
    state.step:if(mean(precipitationGeotiff) > 2 in) = "default"
  end state

end organism
```

This can be modified using the state attribute. All event handlers on the default state will be executed followed by the state-specific handlers.


# Type system
The type system supports typed units such as 50% or 10 inches. These types need to be defined though they may be imported.

## Scalar
This specification calls individual numbers with units as scalars. Regular arithmetic operations (+, -, *, /, ^) will work for this type with automated unit conversion if numbers of non-identical units encountered. This is typically accessed via the `self` keyword:

```
start organism JoshuaTree

  age.step = self.age + 1 year

end organism
```

In general, accessing scalar attributes of individual non-self agents is discouraged. Scalar values can be made through a number followed by units.

## Distribution
A distibution can be created a few ways. First, it can be a set of individual numbers (such as an array) read from a file called a realized distribution:

```
const distribution = ageGeotiff
```

Altnernatively, it can be a formally described distribution created in code called a virtual distribution:

```
const distribution = normal with mean of 5% std of 1%
```

Distributions can be combined (|) which will concatenate if both operands are realized. Otherwise, random sampling as specified in the simulation will combine the distributions.

### Reduction operations
Distributions can be converted to scalars via reduction operations:

 - **mean**: Get the mean of the distribution.
 - **std**: Get the standard deviation of the distribution.
 - **min**: Get the minimum value of the distribution.
 - **max**: Get the maximum value of the distribution.
 - **count**: Get the count of values in the distribution.
 - **sum**: The arithmetic sum of all values in the distribution.

If using virtualized distributions, operations may perform sampling as controlled by the Simulation to proivde an estimation. If nested distributions (distribution of distributions) is provided, they are flattened and then the operation is performed.

### Stochastic operations
Drawing values from distributions randomly is available through the sample command which, by default, will draw a single value.

```
const randomAge = sample ageGeotiff
```

Drawing multiple times with replacement:

```
const randomAge = sample 5 count from ageGeotiff
```

Drawing multiple times without replacement:

```
const randomAge = sample 5 count from ageGeotiff without replacement
```

The count can be a variable with a count number.

### Distribution arithmetic
Automated conversion will be applied such that all members of a distribution have the same units. Arithmetic operations (+, -, *, /, ^) perform scalar broadcast if one operand is a scalar, pairwise arithmetic if the operands are distributions of the same size and realized, and sampling followed by pairwise arithmetic otherwise.

### Set
Sets are a realized distribution that automatically removes duplicates. At this time, they are used for occupancy on patches only. They support only concat and removal (+, -).

### Indexing
For realized scalars only, subsets can be retrieved through indexing:

```
JoshuaTree[JoshuaTree.state == "dead"]
```

This returns a new distribution that meets the given condition applied per element. If applied to a virtualized distribution, it will be sampled.

## Units
Numbers have units with conversion between units allowed via the as keyword:

```
const numberAsMeters = 24 m
const numberAsKilometers = numberAsMeters as km
```

Units are specified as the name after a numeric literal. Forced conversion is possible as well:

```
const numberAsMeters = 24 m
const numberAsKilometers = force numberAsMeters / 1000 as km
```

### Built in
A limited number of units are provided by default:

 - Internally required units of percentage (%, percent) and count (count, counts) are defined which are used for some operation. These are non-convertable by default though may be interpreted by different functions.
 - Degrees (degrees, degree) for location (have latitude and longitude) are available and are uni-directionally convertable to meters by haversine distance.
 - Meters (alias: meter, m) and kilometers (alias: kilometer, km) are defined by default and mutually convertable.
 - Some internal types are available: bool (bool) for boolean (1 for true / 0 for false if force cast), and string for string literals.

### User defined
Users may add additional units with aliases:

```
start unit inch
  in = self
  inches = self
end unit
```

Conversions can also be created:

```
start unit inch
  in = self * 1
  inches = self * 1
  feet = self / 12
end unit

start unit foot
  feet = self
  yard = self / 3
end unit
```

These conversions can depend on where the conversion is taking place:

```
start unit percent
  ft.Conifer.cover = self / 100 * 300
end unit
```

When a conversion is attempted for which logic is not provided, an exception should be thrown. If multiple definitions for a unit exist, they will combine but those specified in larger line numbers will override conversions from those matching in smaller line numbers.

## String
Strings have a built in unit called string. The only operation support for strings are + and | which both do concatenation.

## Location
Location is on every agent that has a latitude / longitude pair in degrees. These individual values cannot be accessed but location can be used for operations like difference. If converted to another type like meters, this will become a scaler.


# Features
The following language features are defined though some are only available in full bodies as opposed to lambdas.

## Comments
Any text after a hash mark will be ignored until a newline is encountered.

```
const a = 5  # test comment
```

## Conditionals
Conditional statements can be applied in event definitions and in the event handlers themselves.

### Boolean expressions
C-style equality operators (>, >=, <, <=, ==, !=) yield bool scalars when applied scalars:

```
const a = 5 units
const b = 7 units
const c = b > c
```

Approximately equals also provided with configurable tollerance:

```
const a = 1.234 meters
const b = 1.235 meters
const c = b ~= c with 0.01 tollerance
```

These operators yield realized distributions of bool values (sampling if applied to virtualized). Python-style logical operators are provided to combine bools (and, or, xor):

```
const a = 1
const b = 0
const c = a or b
```

Other arithmetic operators are not defined for bool (+, -, *, /, ^).

### Modifiers
Though additional modifiers may be added in the future, the following modifiers like `:if` are avialable:

 - **if**: Executed if the argument is true. Only one allowed per modifier chain and must be the first.
 - **elif**: Executed if the argument is true and all prior modifiers in the same chain did not execute.
 - **else**: Executed if prior modifiers in the same chain did not execute. Only one allowed per modifier chain and must be the last.

This acts as a function call which takes a single argument which is of type bool.

### Within event handlers
If statements are defined as following a Python-like turninary operation:

```
const a = 1 if b = 2 else 3
```

These can be full bodies:

```
const a = {
    return 1
  } if b = 2 else {
    return 3
  }
```

## Mapping
Mapping one from set of numbers to another is available though only linear mapping is supported at this time:

```
const a = map b from [0 in, 100 in] to [0 %, 100%]
```

This will extraplote linearlly in the range if an input outside the domain is provided.

## Limit
Limits can be used to enforce a min, max, or range:

```
const a = 10 %;
const withMin = limit a to [15%,]
const withMax = limit a to [,5%]
const withRange = limit a to [5%, 15%]
```

## Full bodies
Full bodies provide a small procedural script optionally with a return statement. If no return, a full body will yield a None.

### Constants
Local constants are available only in full bodies:

```
{
  const a = 5
}
```

These variables are lost after leaving the body though nested variables are supported:

```
{
  const a = 5
  return {
    const b = a + 5
    return b
  }
}
```

These definitions are similar to const in JavaScript and cannot be rewritten:

```
{
  const a = 5
  a = 10  # Leads to error
}
```

### Other variables
Outside variables can be read inside full bodies:

```
start organism JoshuaTree

  age.step = {
    const currentAge = self.age
    const newAge = currentAge + 1 year
    return newAge
  } 

end organism
```

However, these are like const and cannot be written:

```
start organism JoshuaTree

  age.step = {
    const currentAge = self.age
    const newAge = currentAge + 1 year
    self.age = newAge  # Results in an error
  } 

end organism
```

## Built-in functions
The following which take a single argument are defined for both scalars and distributions to return the same type (scalar, distribution) and unit as the input:

 - **abs**: Take the absolute value of a scalar or all elements in a distribution.
 - **log10**: Apply a logarithm (base 10) to a scalar or all elements in a distribution.
 - **ln**: Apply a logarithm (base e) to a scalar or all elements in a distribution.
 - **round**: Round to the nearest whole number such that a fractional component of 0.5 or higher is rounded up an all else is rounded down. Applies to a scalar or all elements in a distribution.
 - **ceil**: Round up to the nearest whole number regardless of fractional component value. Applies to a scalar or all elements in a distribution.
 - **floor**: Round down to the nearest whole number regardless of fractional component value. Applies to a scalar or all elements in a distribution.

The following take two arguments:

 - **difference**: Returns the absolute difference between values. Applies to a scalar or all elements in a distribution (pairwise).

The following which take a single argument are only available for distributions and return scalars as described elsewhere: min, mean, max, sum, and std.

## Temporal and spatial queries
Information from other time steps and cells can be queried through the at and within keywords. These always return realized distributions.

### Temporal queries
At this time, only prior for timestep minus 1 is suppored for the at keyword.

```
const priorCount = sum(JoshuaTree.count at prior)
```

### Spatial queries
At this time, only spatial queries by radial distance are supported.

```
const nearbyCount = sum(JoshuaTree.count within 30 m radial)
```

This results in a set for which const can be used:

```
const treesNearby = JoshuaTree within 30 m radial
const treesOn = JoshuaTree
const nearbyButNotOn = treesNearby - treesOn
const nearbyButNotOnCount = sum(nearbyButNotOn.count)
```

## Imports
Imports will cause another script to get executed as if the contents were found at the import location.

```
import "file://other.josh"
```

These statements must be at top level (not within any stanzas). Paths can be specified with different protocols like `https://`.

## Interactivity and configuration
In this phase of the specification, limited configuration is available and must be loaded at top level (outside stanzas):

```
config "file://config.json"
```

The JSON file at the location (additoinal protocols like `https://` may be made avialable) should have an object with string keys and string values where the string values should be a numeric followed by a units and msut have valid variable names similar to const:

```
{
  "val1": "5 count"
}
```

These values are read only and become available via a config object where config statements at higher line numbers will override config statements at lower line numbers if names collide.

```
const a = config.val1 + 1 count
```

This JSON may, in practice, be generated from user interface elements like sliders depending on the interpreter / compiler.

# Reservations, Conventions, and Defaults
The following conventions are recommended. Unless specified otherwise, violations should not result in an exception raised from the interpreter / compiler.

## Local variables
Local variables like defined via const are recommended to follow `camelCase` with leading lowercase letter. Note that only alphanumeric names are supported and the first character cannot be a number where violations should result in an exception.

## Attributes
Attribute names are recommended to follow `camelCase` with leading lowercase letter. Note that only alphanumeric names are supported and the first character cannot be a number where violations should result in an exception.

## Entities
Groups of agents present on the same patch can be found by specifying the name of the agent:

```
const alsoOnCell = JoshuaTree
```

Distributions of values can be found on those groups:

```
const countsAlsoOnCell = JoshuaTree.count
```

To help improve readability, it is recommended that entity names are `CamelCase` with leading upper case character.

## Reserved names
The following are used by the system and it is not recommended that they be used for names of any user defined entities or variables and should throw an exception: as, const, disturbance, elif, else, end, if, management, limit, map, return, start, state, step, within. The following are reserved for future use: gaussian, linearly, logrithmically, logistically.


# Example
This simple Joshua Tree-inspired example demonstrates basic mechanics.

## Setup
This implementation starts with a grid where each cell or patch is 30 meters by 30 meters centered at a given geographic location. without sampling.

```
start simulation Example

  grid.size = 30 m
  grid.start = 34 degrees longitude, -116 degrees latitude
  grid.end = 35 degrees longitude, -115 degrees latitude

end simulation
```

This demonstration will also use local geotiff with estimated counts of trees and ages with geographic specificity:

```
start external observedAges

  source.location = "file://obsevations.geotiff"
  source.format = "geotiff"
  source.units = "years"
  source.band = 0

end external

start external observedCounts

  source.location = "file://obsevations.geotiff"
  source.format = "geotiff"
  source.units = "count"
  source.band = 1

end external
```

Unit definitions are also provided.

```
start unit years

  year = self
  yr = self
  yrs = self

end unit
```

Finally, uniform patches are defined.

```
start patch Default

  location = all
  JoshuaTree.init = create sum(observedCounts) JoshuaTree

end patch
```

This patch is further modified after additional definition

## Organism behavior
This organism will primarily be defined through changes in state. Note that initial state is set based on age.

```
start organism JoshuaTree

  age.init
    :if(meta.stepCount == 0 count) = sample observedAges
    :else = 0 years

  age.step = self.age + 1 year

  state.init
    :if(self.age > 30 years) = "adult"
    :elif(self.age > 2 years) = "juvenile"
    :elif(self.age > 0 years) = "seedling"
    :else = "seed"

  seedCache.init
    :if(self.age > 30 years) = self.age * (5% / 1 year)
    :else = 0%

  start state "seed"

    state.step
      :if(sample uniform from 0% to 100% > 50%) = "seedling"
      :elif(sample uniform from 0% to 100% > 50%) = "dead"
      :elif(self.age > 3 years) = "dead"
  
  end state

  start state "seedling"

    state.step
      :if(sample uniform from 0% to 100% < 20%) = "dead"
      :elif(self.age > 2 years) = "juvenile"

  end state

  start state "juvenile"

    state.step
      :if(sample uniform from 0% to 100% < 10%) = "dead"
      :elif(self.age > 30 years) = "adult"

  end state

  start state "adult"

    seedCache.step = limit 1% + seedCache to [0%, 100%]
    state.step:if(sample uniform from 0% to 100% < 5%) = "dead"

  end state

end organism
```

The starting implementation simply updates age and state without seed dispersal.

## Dispersal
Seed dispersal means that new Joshua Trees may be created if the space is not too crowded. This is handled in the patch:

```
start patch Default

  location = all
  JoshuaTree.init = create sum(observedCounts) JoshuaTree
  JoshuaTree.start = JoshuaTree - JoshuaTree[JoshuaTree.state == "dead"]
  JoshuaTree.step = {
    const prior = JoshuaTree
    const neighbors = JoshuaTree within 30 m radial
    const adultNeighbors = neighbors[neighbors.state == "adult"]
    const seedDensity = sum(adultNeighbors.seedCache) / 10% * 1 count
    const newCount = floor(sample uniform from 0 count to seedDensity)
    const newCountCapped = limit newCount to [0, 30 - count(prior)]
    const new = create newCountCapped JoshuaTree
    return new + prior
  }

end patch
```

## Disturbances
We include a simple fire disturbance at random locations.

### Use of a disturbance agent
The first approach involves an agent.

```
start disturbance Fire

  active.init = true
  active.step = false

end disturbance
```

This is used as a simple marker:

```
start patch Default

  location = all

  JoshuaTree.init = create sum(observedCounts) JoshuaTree
  JoshuaTree.start = JoshuaTree - JoshuaTree[JoshuaTree.state == "dead"]
  JoshuaTree.step = {
    const prior = JoshuaTree
    const neighbors = JoshuaTree within 30 m radial
    const adultNeighbors = neighbors[neighbors.state == "adult"]
    const seedDensity = sum(adultNeighbors.seedCache) / 10% * 1 count
    const newCount = floor(sample uniform from 0 count to seedDensity)
    const newCountCapped = limit newCount to [0, 30 - count(prior)]
    const new = create newCountCapped JoshuaTree
    return new + prior
  }

  Fire.step = create (1 if sample uniform from 0% to 100% < 5%) else 0) Fire
  Fire.start = Fire - JoshuaTree[Fire.active == false]

end patch
```

Finally, the JoshuaTree can respond to fire:

```
start organism JoshuaTree

  # ... prior ...

  state.step:if(count(Fire) > 0 and sample uniform 0% to 100% < 90%) = "dead"

  # ... states ...

end organism
```

### Without a disturbance agent
This second approach simply uses the patch itself.

```
start patch Default

  location = all

  JoshuaTree.init = create sum(observedCounts) JoshuaTree
  JoshuaTree.start = JoshuaTree - JoshuaTree[JoshuaTree.state == "dead"]
  JoshuaTree.step = {
    const prior = JoshuaTree
    const neighbors = JoshuaTree within 30 m radial
    const adultNeighbors = neighbors[neighbors.state == "adult"]
    const seedDensity = sum(adultNeighbors.seedCache) / 10% * 1 count
    const newCount = floor(sample uniform from 0 count to seedDensity)
    const newCountCapped = limit newCount to [0, 30 - count(prior)]
    const new = create newCountCapped JoshuaTree
    return new + prior
  }

  onFire.start = sample uniform from 0% to 100% < 5%

end patch
```

Then, the JoshuaTree can respond to fire:

```
start organism JoshuaTree

  # ... prior ...

  state.step:if(patch.onFire and sample uniform 0% to 100% < 90%) = "dead"

  # ... states ...

end organism
```

# Implementation
Given this language structure and the earlier described motivation, the following optional recommendations are given for those implementing this language.

## Tunable sampling
Some simulations operate at the millions of individuals, possibly making computation intractable. That said, many individual organisms in a patch will exhibit very similar attributes. Analogous to instance weights in the machine learning community, this specification suggests that implementing compilers and interpreters offer tunable granularity where, instead of doing individual computation for one million agents within a patch, computation may be done for one thousand sampled agents and the resulting distribution used to express the full community. This also allows simulation users to run “coarse” simulations for quick feedback to aid their developer loop, saving a longer running “granular” simulation after trust has been established in the simulation design.

## Interpretation and compilation
This language could compile to source code in a different language which potentially uses a complementary library written in that language. This allows for the user to leave the DSL and continue work in a host language beyond the scope of this project. That said, weary of the security and stability implications of an "eval" approach, an interpreter may be appropriate for the workbench in which authors build simulations.

## Parallelism
At minimum, each simulation trial is independent of each other and the specification recommends running each trial in Monte Carlo in parallel when supported by the host language. This isolation is amenable to distributed computing. Furthermore, simulations follow a Markov property where only zero or one prior time steps must be maintained to evaluate the current time step. Therefore, updates to cells can be evaluated independently of each other. However, in some simulations, years can also be evaluated indepedently. This can be determined at compile-time by evaluating if the simulation uses "neighbor queries" in which one cell inspects another.

## Circular dependency
Checking for and raising an exception on circular dependency in the computational graph is required for a valid implementation.
