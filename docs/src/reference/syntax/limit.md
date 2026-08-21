---
title: "Bounding a value with limit"
description: >-
  Clamping a value into a range, with either end left open when only one side needs a bound.
order: 50
tags: [syntax, values]
---

`limit <value> to [<low>, <high>]` clamps a value into a range. Either bound may be omitted to leave
that side unbounded, so `[,10 m]` caps the height at 10 m with no floor, and `[0,]` would floor it
at zero with no cap.

Bounds carry units and must agree with the value being clamped.
[sample.josh](../distributions/sample.josh) uses the open-lower form to keep a normal draw from
going negative, which is the most common reason to reach for `limit`.
