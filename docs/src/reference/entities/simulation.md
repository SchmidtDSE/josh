---
title: "Declaring a simulation grid"
description: >-
  Setting the size of a patch and the two corners that bound the space a model runs over.
order: 10
tags: [entities, simulation, grid]
---

A `simulation` stanza describes the space a model runs over. `grid.size` sets the edge length of one
patch, and `grid.low` / `grid.high` give two opposite corners as a latitude and a longitude.

Corners may be written either as a single value per axis, as here, or split across `grid.low.x` and
`grid.low.y`. A complete run also needs `steps.low` and `steps.high` to bound the clock and a
`grid.patch` naming the patch type to tile the grid with; this snippet declares only the geometry.
