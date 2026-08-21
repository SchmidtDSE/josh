---
title: "Querying the previous step"
description: >-
  Reading state as of the end of the previous step, and how that composes with reading the current
  patch.
order: 40
tags: [queries, temporal]
---

`prior.` reads the state as of the end of the previous step. It composes with `here.`, so
`prior.here.TreeA.count` is the count of `TreeA` that was on this patch one step ago.

This is the same model as [query_spatial.josh](query_spatial.josh) with the radius removed: one
looks outward in space, the other backward in time, and both are ways of reading state that is not
the entity's own.
