::: {.buildup-lede}
Every Josh model is built from a few labeled blocks. Before adding behavior, we sketch the empty shell.
:::

A Josh model has three kinds of entities: a **simulation** (global settings and
the grid), a **patch** (a location on that grid), and an **organism**
(something that lives there — here, a ForeverTree). We start with three empty
blocks and fill them in over the next steps.

More precisely: **simulation** defines the spatial extent, temporal range,
export targets, and landscape-level logic; **patch** describes a grid cell, an
initialization of organisms, per-step computations, and exported summary
statistics; and **organism** defines the attributes and per-step behavior of an
individual organism ("agent"), optionally including state transitions.
