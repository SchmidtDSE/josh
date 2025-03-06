Focused on vegetation, the Josh language allows for the description of multi-occupancy patch-based ecological simulations in which multiple species occupying a grid cell describing a small segment of a community can be described through individual behaviors with optional state changes.

# Purpose
This language specifically seeks to support the nexus between science, policy, and software engineering by executing vegetation-focused ecological simulations to support management decisions. This specification prefers a SmallTalk / HyperTalk-like language that prioritizes an imperative design that may operate at a community level in simulation execution but allows for specification of behavior at the level of an individual organism.

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
The idea of a “serialized” set of discrete steps inherent in many general purpose programming languages contrasts with natural systems where all organisms are taking action or being acted upon in parallel in response to simultaneously occurring environmental factors. Therefore, this specification prioritizes the description of entity behavior over allowing for specification of the order in which computation running those behaviors is actually executed. This means that, unlike general purpose languages, control flow is decided by the interpreter or compiler and not the simulation author. This makes this language more like an imperative language than a procedural one.

### Behavioral over computational description
Given the heterogeneity of the anticipated user base, this specification prioritizes fluency in expressing the logic of a simulation over specification of the specifics for how that computation is optimized or parallelized. In the same way that general purpose languages perform optimizations at compile-time the source code for simulations may speak to behavior at the level of an individual organism even as operations are likely to become optimized within the interpreter / compiler to avoid intractable expensive computation that could potentially involve the simulation of millions of individual agents.

## Stochasticity
Just as organisms and communities need to be the primary “nouns” that a user manipulates, stochastic elements also must be a primary “type” that the user can express and engage with. In contrast to many general purpose languages, working with probability distributions needs to be similarly fluent as working with discrete numbers.

# Structures

## Stanzas

## Attributes

## Events

## Interaction


# Entities

## Communities

## Organism

## States

## Disturbance and management

## Environment


# Features

## Conditionals

## External data

## Mapping

## Stochasticity

## Other built-ins


# Unit conversion


# Interactivity


# Example

## Description

## Baseline implementation

## Disturbances

## Interventions


# Implementation
Given this language structure and the earlier described motivation, the following optional recommendations are given for those implementing this language.

## Tunable sampling
Some simulations operate at the millions of individuals, possibly making computation intractable. That said, many individual organisms in a patch will exhibit very similar attributes. Analogous to instance weights in the machine learning community, this specification suggests that implementing compilers and interpreters offer tunable granularity where, instead of doing individual computation for one million agents within a patch, computation may be done for one thousand sampled agents and the resulting distribution used to express the full community. This also allows simulation users to run “coarse” simulations for quick feedback to aid their developer loop, saving a longer running “granular” simulation after trust has been established in the simulation design.

## Interpretation and compilation
This language could compile to source code in a different language which potentially uses a library construct 

## Parallelism

### Local concurrency

### Distribution

## Host language


# Future
TKTK
