---
title: "Management stanzas"
order: 40
tags: [entities, management]
---

A `management` entity represents a deliberate intervention — planting, thinning, prescribed burning
— as opposed to a `disturbance`, which represents something that happens to the system.

Like any entity it can read the patch it sits in, so `mean(here.PlantingMap)` draws the planting
intensity from another entity present on the same patch rather than from a constant.
