---
title: "Binomial draws for cohort survival"
description: >-
  Drawing the number of survivors out of a thousand interchangeable individuals in one call,
  rather than instantiating a thousand entities.
order: 40
tags: [distributions, stochastic, performance]
---

`sample binomial with n of ... p of ...` draws the number of successes out of `n` independent trials
in one call. Modelling 1000 individuals through an 80% survival rate this way costs a single draw per
step, where instantiating 1000 organisms and rolling each one costs a thousand.

Use this when the individuals are interchangeable and only the count matters. When they differ from
each other — in height, age, or position — they have to be real entities, and
[eval_duration.josh](../performance/eval_duration.josh) covers how to find out whether that is
costing you.
