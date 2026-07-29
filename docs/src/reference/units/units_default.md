---
title: "Converting between built-in units"
order: 10
tags: [units, conversion]
---

`as` converts a value to another unit, and the engine refuses conversions it cannot justify. `height
as km` is accepted because metres and kilometres are related by a known factor.

`force ... as` performs the conversion the arithmetic has already made dimensionally correct:
`height / 1000 m` is a dimensionless ratio, so relabelling it as kilometres is an assertion the
author is making rather than one the engine can derive. Reach for `force` only when you can say why
the plain `as` was rejected.
