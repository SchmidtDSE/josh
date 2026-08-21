---
title: "External time axis on a count clock"
description: >-
  Querying a dataset's time axis with at year, on a run whose clock counts steps from a calendar
  anchor.
order: 20
tags: [external, data, temporal]
---

External data can carry a declared time axis, which the model queries with `at year <value>` and
interrogates with `first year of`, `last year of`, `length of`, and `unit of`.

This model runs on the default count clock, using `steps.low = 2024 count` as the calendar anchor so
that `meta.year` is a real year rather than an offset. The `year` unit is declared at the bottom of
the file because the unit named in the `at year` clause is resolved through that alias before being
converted to the unit persisted in the `.jshd`.

Running it needs a preprocessed dataset. The command that produces one is recorded at the top of the
model:

```
java -jar joshsim-fat.jar preprocess external_time_axis_count.josh Main \
  maxtemp_tulare_annual.nc Maximum_air_temperature_at_2m K temperature.jshd \
  --time-type count --time-start 2024 --time-unit year --time-count 3
```
