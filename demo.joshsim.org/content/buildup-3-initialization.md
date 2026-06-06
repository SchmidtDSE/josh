::: {.buildup-lede}
Now populate the landscape — every patch starts with a small stand of trees.
:::

Patches come to life through initialization. `ForeverTree.init` is evaluated
once at the start and creates 10 ForeverTree organisms in every patch; Josh then
tracks and updates each one independently across every time step.

In other words, there are ten ForeverTree agents per patch at t0 (no mortality,
so the count stays constant).
