---
title: "Entities that reference each other"
description: >-
  Two entity types that each read the other, resolved without the author ordering them by hand.
order: 20
tags: [queries, spatial]
---

Two entity types may each read the other. Grass shades itself out when the cover trees above it
exceed a foot, and the trees grow independently — a dependency the author does not have to order by
hand.

Note the units: the trees grow in inches, the threshold is in feet, and the grass responds in
centimetres. Comparison and arithmetic convert as needed, so a model can be written in whatever unit
each measurement is naturally reported in.
