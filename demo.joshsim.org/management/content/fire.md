::: {.buildup-lede}
A fire sweeps through part of the map in one year, killing most trees it reaches.
:::

Where the fire burns is given to us as a static map — the `burned` mask, read
with `external burned at 0`, just like the climate layers. A simulation-level
`fire` flag turns it on in a single year, `fireStep`, and a patch knows it is in
the fire's path when `onFire` is true.

Inside the footprint, each tree draws its own fate: with probability
`burnLikelihood` (90% here, so roughly one in ten survives) it moves to the
**Burned** stage and stops growing. That is why we built the life stages first —
fire is simply a transition into a terminal stage. With most trees gone, the
patch is suddenly wide open, and the grass is ready to take advantage.
