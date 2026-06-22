::: {.buildup-lede}
First we give each patch an invasive grass, tracked as the fraction of ground it covers.
:::

Not everything on the landscape is a tree. We add a patch-level attribute,
`invasiveCover`, running from 0% to 100% — how much of the patch the invasive
grass holds. It is not an organism we track individually; it is a property of the
place.

Grass does well on open ground and poorly under a closed canopy, so its yearly
growth scales with how *open* the patch is. We measure openness from the number
of live trees: at zero live trees the patch is fully open, and by
`treesForFullSuppression` live trees the canopy shuts grass growth down. Cover
never drops below `invasiveMinCover` on its own. For now the grass just grows —
in the next step we let it push back on the trees.
