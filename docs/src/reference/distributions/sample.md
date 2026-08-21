---
title: "Sampling from a distribution"
description: >-
  Drawing a value from normal or uniform, the units the draw carries, and the bound that keeps
  it non-negative.
order: 10
tags: [distributions, stochastic]
---

`sample` draws one value from a distribution. `normal with mean of ... std of ...` and
`uniform from ... to ...` are the two most common; both carry units, and the draw carries them too.

A normal draw is unbounded, so the initial height here is wrapped in
[`limit ... to [0,]`](../syntax/limit.josh) to keep it non-negative. The step handler uses
[selectors](../syntax/selector.josh) to draw growth from a different uniform range depending on how
tall the tree already is.

Runs that sample are reproducible only against a fixed seed; the conformance runner and the `run`
command both take `--seed`.
