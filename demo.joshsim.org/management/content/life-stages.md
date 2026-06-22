::: {.buildup-lede}
Trees now have life stages — and grass holds the small ones back.
:::

We give each tree a `state`: it begins as a **Juvenile** and becomes an **Adult**
once it passes `smallTreeHeight`. A third stage, **Burned**, is where fire will
send trees in the next step. Each stage is a `start state` block holding the
behavior specific to that stage.

The point of the stages is competition. A juvenile is small enough to be
out-competed by grass, so its `growthInhibition` falls as `invasiveCover` rises —
on a grassy patch, young trees barely grow. Adults have escaped the grass and
grow unhindered, and Burned trees do not grow at all. We fold `growthInhibition`
into `newGrowth`, closing the loop between the grass and the trees.
