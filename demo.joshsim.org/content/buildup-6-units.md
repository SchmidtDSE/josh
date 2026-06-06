::: {.buildup-lede}
Mixing meters, millimeters, and Kelvin? Josh handles the unit conversions for you.
:::

Rather than tracking conversions by hand, you declare rules in `start unit`
blocks. Here `kgm2s` (the climate data's precipitation-flux unit) converts to
`mm` per year, and `mm` converts to `m`; Josh applies these automatically
whenever a value is used where a different unit is expected.

This is built-in support for automatic unit conversions between compatible types
(like Fahrenheit to Celsius). The `as mm` cast invokes the registered conversion
— kg m⁻²s⁻¹ × 31536000 → mm yr⁻¹ — applied automatically wherever a target unit
is named.
