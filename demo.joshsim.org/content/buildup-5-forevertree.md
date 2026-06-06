::: {.buildup-lede}
With the climate drivers in place, define how a ForeverTree actually grows each year.
:::

Each organism tracks its `age` and `height` over time. A `stochastic` term
samples a normal distribution each step for natural variability, `newGrowth`
multiplies the maximum growth rate by the temperature, precipitation, and
stochastic factors, and `height.step` accumulates that growth from the prior
step onward.

So the annual increment combines three unitless factors — a unimodal
temperature impact that peaks mid-window and is zero at the edges, a saturating
precipitation impact, and a multiplicative noise term — and growth is choked off
whenever either climate driver is unfavorable.
