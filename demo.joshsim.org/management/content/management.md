::: {.buildup-lede}
Inside a smaller managed zone, we can act: plant trees and push the grass back.
:::

A second static map — the `managed` mask, read with `external managed at 0` —
marks a zone inside the burn where we intervene, once, in `managementStep`. Two
levers, both toggled from the config:

**Outplanting.** When `outplantCount` is above zero, the patch creates that many
new ForeverTrees in the managed zone. Outplanted trees are not seedlings: they
start at `outplantAge` and `outplantHeight`, so if they are tall enough they
begin as Adults — already past the stage where grass would hold them back. That
is why the tree's `age` and `height` now branch on whether the simulation is at
its very first step (the original cohort) or later (an outplant).

**Invasive removal.** When `removeInvasives` is on, the managed zone's
`invasiveCover` is knocked back to `invasiveRemovalTarget`, giving young trees
room. Planting and removal help through different mechanisms — and they combine.
