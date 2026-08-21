---
title: "Configuration variables"
description: >-
  Reading a value from a .jshc namespace, so one model runs against several parameter sets
  without being edited.
order: 10
tags: [config]
---

`config <namespace>.<name>` reads a value supplied outside the model. The namespace selects a
`.jshc` file — `example.testVar1` comes from `example.jshc` — so the same model runs against
different parameter sets without being edited.

A `.jshc` is a flat list of `name = value` assignments, and unlike a `.josh` model it is the right
place for comments: it is what someone tuning the model reads. The file beside this model declares
`testVar1`, `testVar2`, and an unused `testVar3`.

`joshsim discoverConfig <model>` lists the variables a model reads, which is how a caller finds out
what a `.jshc` has to supply. This model is not marked runnable here because running it requires
that `.jshc` to be on the working path; `examples/validate.sh` exercises the `discoverConfig`
behaviour against it directly.
