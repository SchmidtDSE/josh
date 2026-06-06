::: {.buildup-lede}
First, tell Josh where in the world the model lives — and how finely to slice it up.
:::

Every Josh simulation runs on a spatial grid. `grid.size` sets the resolution
of each patch (here 16 km × 16 km cells), `grid.top_left` and
`grid.bottom_right` place the bounding box on a real map — our forest sits in
the Sierra Nevada — and `grid.patch` names the patch type that fills every cell.

Concretely, this lays out 16 km square patches over a fixed lat/lon bounding box
(36.73, -119.52 to 35.80, -117.98 degrees).
