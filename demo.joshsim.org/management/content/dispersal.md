::: {.buildup-lede}
Grass does not stay put — it spreads into the cleared cells from its neighbours.
:::

A burned patch is not invaded only from within; grass arrives from nearby. We let
each patch send a share of its cover out to its neighbours and receive the sum of
what its own neighbours send. To find them, a patch looks within
`dispersalRadius` — which must be larger than the 16 km cell size, or it would
only find itself.

The bookkeeping uses the `.end` phase so every patch reads a settled value:
`coverShare` is what a patch emits per neighbour, and `coverImmigration` is the
total it receives, picked up on the following step (`prior.coverImmigration`) and
added to its own growth. After a fire, this is what lets the invasion sweep
across the scar rather than waiting in place.
