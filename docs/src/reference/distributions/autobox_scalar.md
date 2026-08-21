---
title: "Sampling a scalar"
description: >-
  Why sampling a plain scalar works, and what that makes substitutable — a constant today, a
  distribution tomorrow, with no other change.
order: 20
tags: [distributions, stochastic]
---

`sample` accepts a plain scalar as well as a distribution: sampling `5 count` yields `5 count`. A
scalar behaves as a distribution with all its mass at one point, so a handler can be written against
`sample` without knowing whether the value it was handed is stochastic.

That is what makes a parameter substitutable — a model that samples a constant today can be handed a
`normal` tomorrow with no other change.
