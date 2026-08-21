---
title: "Spin-up and spin-down as a modelling recipe"
description: >-
  Expressing burn-in and cool-down with ordinary attributes, because the engine has no dedicated
  machinery for either.
order: 10
tags: [time, spinup]
runnable: true
assert: true
simulation: SpinupExample
seed: 42
---

Spin-up is not engine machinery in Josh — it is something a model expresses with the tools it
already has. Widen `steps.low` and `steps.high` to cover the burn-in and cool-down, then use an
ordinary attribute, here called `state`, to mark which phase each step belongs to.

`observedStep` re-anchors the clock so the observed window is numbered from zero, the way it would
be if spin-up had never been simulated, and `year` draws from a different range in each phase.
Because `state` is just an attribute, downstream handlers branch on it with the same selectors they
use for anything else.

The `assert.*` handlers on the patch check the phase boundaries hold — the spin-up steps land at
negative observed indices, the observed steps at 0 through 4, the spin-down steps above 4. That
makes this a self-validating model, so it runs in the conformance suite rather than as a line in a
shell script.
