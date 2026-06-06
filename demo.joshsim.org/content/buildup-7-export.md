::: {.buildup-lede}
Finally, choose how long to run, expose the knobs, and say what to record.
:::

`steps.low` and `steps.high` set the time range (years 0–10),
`exportFiles.patch` names where results go, and the three `export.*` lines
record the year, tree count, and mean height at every step — the data we
visualize next. Stochastic replicates share the same climate forcing and initial
conditions, writing one row per (patch, year, replicate).

The `config` keyword keeps the tunable knobs out of the model and in a companion
`.jshc` file, so collaborators can re-run with new values without touching the
code. Here it supplies `minPrecipImpactPct` and `maxNewGrowth`:

<div class="config-example">
<div class="config-example-label">forevertree.jshc</div>
<pre><code class="language-joshlang"># How much can a ForeverTree grow
# in one year?
maxNewGrowth = 1 m

# How much is growth rate reduced
# by drought in a given year?
#  (at 0%, drought halts growth)
#  (at 100%, no effect on growth)
minPrecipImpactPct = 0 %</code></pre>
</div>
