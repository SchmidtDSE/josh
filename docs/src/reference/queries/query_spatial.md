---
title: "Querying a spatial neighbourhood"
description: >-
  Reaching past the current patch to every entity within a radius, and why that read is taken as
  of the previous step.
order: 30
tags: [queries, spatial]
---

`<Type>.<attribute> within <distance> radial at <time>` reaches past the current patch to every
entity inside a radius. Here the count of `TreeA` within 30 m of the current position is summed and
used to depress the height of a neighbouring `TreeB`.

`at prior` selects the previous step's state. Reading the neighbourhood at `prior` rather than
`current` is what keeps the result independent of the order in which patches are evaluated within a
step.
