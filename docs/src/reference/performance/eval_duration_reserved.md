---
title: "evalDuration cannot be assigned"
description: >-
  Why the engine rejects a model that writes to an attribute the engine itself supplies, kept as a
  standing check that it still does.
order: 30
tags: [performance, profiling]
expect: parse-error
---

`evalDuration` is supplied by the engine, not by the model, so a handler that assigns to it is
rejected: *Cannot use reserved attribute evalDuration*.

This file is expected to fail. The harvest asserts it still does, which is the point of recording it
here — a reserved name that quietly became assignable would otherwise be found by whoever wrote a
model that depended on it.
