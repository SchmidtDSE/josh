::: {.buildup-lede}
We record the ecology we care about, and expose the management choices as knobs.
:::

The `export.*` lines choose which output variables to record each year. Alongside
the year and mean height, we now record the live, burned, **juvenile**, and
**adult** tree counts and the invasive cover — so the results show not just how
tall the forest is, but its age structure and how the grass and fire reshaped it.

As in the intro demo, the tunable values live in a companion `.jshc` file rather
than the model, so we can re-run with new choices without touching the code. The
management knobs are the interesting ones to try next:

<div class="config-example">
<div class="config-example-label">scenario.jshc</div>
<pre><code class="language-joshlang"># Outplanting: how many trees to add
#   (0 disables outplanting)
outplantCount = 0 count
# height >= smallTreeHeight => planted
#   as Adults
outplantHeight = 3 m

# Invasive removal: 1 = knock grass
#   back, 0 = off
removeInvasives = 0 count
invasiveRemovalTarget = 5 %</code></pre>
</div>
