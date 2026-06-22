::: {.buildup-lede}
We start where the intro left off: a forest whose growth is driven by climate.
:::

This is the ForeverTree model from the intro demo, in brief. Every patch — the
smallest ecologically meaningful spatial entity on our grid — starts with ten
trees, and each tree grows a little each year. How much depends on the
climate it reads from `external` data: a quadratic temperature response
(`temperatureImpact`) that peaks in the mid-range, a smooth precipitation
response (`precipImpact`), and a touch of natural variability (`stochastic`).

We keep this base intact and extend it. Over the next steps we add an invasive
grass, life stages for the trees, a fire, the way grass spreads, and finally the
management we can apply in response.
