---
title: "Reading other entities on the same patch"
description: >-
  Reading the entities that share a patch with this one, and aggregating them into a value a
  handler can branch on.
order: 10
tags: [queries, spatial]
---

`here.<Type>` is the collection of entities of that type on the patch the current entity occupies.
Aggregate it — `max`, `mean`, `sum` — to get a value a handler can branch on.

Combined with a [selector](../syntax/selector.josh), this makes a handler that fires only under a
condition and otherwise leaves the attribute alone: `seedBank` is set to `"seed"` on the steps where
a fire is present, and is untouched on every other step.
