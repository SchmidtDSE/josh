::: {.buildup-lede}
Real forests respond to real weather — so we feed in actual temperature and rainfall maps.
:::

Josh reads gridded climate rasters aligned to the model. `clampedTemp` keeps
temperature within a survivable range, `temperatureImpact` turns that into a
growth multiplier with a quadratic curve, and `precipImpact` does the same for
rainfall with a smooth (sigmoid) curve.

Under the hood, climate rasters (geotiff/COG, NetCDF) are preprocessed once into
grid-aligned `.jshd` layers, so at runtime `external` is a plain per-patch read;
the user never writes alignment or interpolation logic.

<div class="ext-figures">
<figure><img src="img/eco_temp_spatial.png" alt="Map of input temperature in Kelvin across the simulation grid, warmer to the south"></figure>
<figure><img src="img/eco_temp_domain.png" alt="Temperature growth response: a quadratic curve peaking near 300 K and zero outside 270 to 330 K"></figure>
<figure><img src="img/eco_precip_spatial.png" alt="Map of input precipitation in millimeters per year across the grid, wetter to the west"></figure>
<figure><img src="img/eco_precip_domain.png" alt="Precipitation growth response: a sigmoid rising with precipitation between 300 and 500 mm"></figure>
</div>

<p class="ext-figures-caption">The maps show the gridded climate Josh reads in; the curves show how each value becomes a growth multiplier — so you can see why mid-range temperatures and wetter patches drive more growth.</p>
