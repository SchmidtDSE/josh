# Josh Simulation Engine Guide

Welcome to the Josh Simulation Engine tutorial series. These tutorials will guide you through creating ecological simulations using Josh, a domain-specific language designed for vegetation and agent-based modeling.

## Tutorial Series

### Beginner Tutorials
Learn the basics of Josh interface and core concepts.

1. **[Hello Grid](hello/hello.md)** - Introduction to Josh interface and basic simulation concepts
2. **Data Integration** — working with external geospatial data sources. Not written yet; the stub is [_data.md](_data.md).

### Intermediate Tutorials
Explore multi-species modeling and spatial interactions.

3. **[Two Trees](two_trees/two_trees.md)** - Multiple species modeling and competitive interactions
4. **State Management** — entity states and behavioral transitions. Not written yet; the stub is [_states.md](_states.md).

### Advanced Tutorials
Master complex dynamics and tool integration workflows.

5. **[Fire Dynamics](grass_shrub_fire/grass_shrub_fire.md)** - Disturbance modeling and fire effects
6. **Python Integration** — analysis workflows using Python. Not written yet; the stub is [_python.md](_python.md).
7. **Command Line Tools** — preprocessing and batch execution. Not written yet; the stub is [_cli.md](_cli.md).

Four of the seven tutorials have never been more than a heading and an "under construction" line. They are listed without links so the series reads honestly, and their files carry a leading underscore, which is what tells the harvester they are prose with no model rather than a unit missing one.

## Getting Help

- **Language Reference**: See [llms-full.txt](../../../llms-full.txt) for complete Josh language specification
- **Examples**: Browse [the reference library](../reference/) for feature-by-feature examples
- **Community**: Visit the project repository for discussions and support

## Prerequisites

- Basic understanding of ecological modeling concepts
- Familiarity with text editing and file management
- Optional: Knowledge of Python for advanced analysis workflows

## Tutorial Structure

Each tutorial includes:
- **Overview**: Learning objectives and prerequisites
- **Step-by-step instructions**: Guided implementation
- **Code examples**: Complete Josh simulation files
- **Visualization**: Understanding simulation outputs
- **Extensions**: Ideas for further exploration

Start with [Hello Grid](hello/hello.md) if you're new to Josh, or jump to any tutorial that matches your interests and experience level.

## How a tutorial is stored

Each written tutorial is a directory holding the model, its prose, and any overlay it needs:

```
hello/hello.josh              the model, which is the only copy of this code
hello/hello.md                the prose, and the front-matter that says how to run it
two_trees/two_trees_ci.josh   an `update` stanza shortening the run for CI
```

The model is never transcribed into the prose. Snippets inside a tutorial are excerpts that build it up step by step; the complete model is the `.josh` beside them, and `./gradlew harvestDocs` validates it on every push.

## Where these are published

`./gradlew renderDocs` turns this directory into [joshsim.org/library/guides/](https://joshsim.org/library/guides/), and CI does the same on every push. The rendered page shows the complete model by reading the `.josh` from disk, so a page cannot disagree with the file the build validates. The hand-written pages that used to live in `landing/guides/` are gone; only redirects remain, because they had drifted from the models and nothing compared them.
