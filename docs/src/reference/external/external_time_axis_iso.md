---
title: "External time axis on an ISO clock"
description: >-
  Putting a simulation on a calendar clock with ISO dates, and what that changes about the
  temporal queries.
order: 30
tags: [external, data, temporal]
---

Setting `time.type = "ISO"` puts the simulation on a calendar clock: `time.low`, `time.high`, and an
ISO 8601 `time.interval` replace the step bounds, and `meta.time` carries the current date.

The temporal queries are the same as on a [count clock](external_time_axis_count.josh) with `year`
replaced by `time`. `meta.year` still exists on an ISO run but remains the raw timestep — 0, 1, 2 —
so calendar semantics have to come from `meta.time` or from a model attribute.

Running it needs a preprocessed dataset built with matching ISO metadata:

```
java -jar joshsim-fat.jar preprocess external_time_axis_iso.josh Main \
  maxtemp_tulare_annual.nc Maximum_air_temperature_at_2m K temperature.jshd \
  --time-type ISO --time-start 2024-01-01 --time-interval P1Y --time-count 3
```
