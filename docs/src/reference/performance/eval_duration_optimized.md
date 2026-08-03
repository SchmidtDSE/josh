---
title: "Acting on an evalDuration measurement"
order: 20
tags: [performance, profiling]
---

The "after" half of the pair with [eval_duration.josh](eval_duration.josh). The 5000 `Shrub`
entities are gone, replaced by a single `shrubPctCover` attribute on the patch: the shrubs were
interchangeable and only their aggregate cover was ever read, so representing them individually
bought nothing.

The trees are untouched, because they are not interchangeable — each carries its own height. The
`export.shrubCoverMs` column stays in place so the two runs can be compared directly; the same
reasoning applied to a survival process gives
[binomial.josh](../distributions/binomial.josh).
