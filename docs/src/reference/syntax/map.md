---
title: "Rescaling a value with map"
description: >-
  Rescaling a value linearly from one range onto another, and why the result is not clamped to the
  destination.
order: 60
tags: [syntax, values]
---

`map <value> from [<low>, <high>] to [<low>, <high>]` rescales a value linearly from one range onto
another. Here a conifer cover between 0% and 90% is rescaled onto 0% to 100%, and the result is used
as the cap for a `limit`.

`map` does not clamp: a value outside the source range maps to a value outside the destination
range. Compose it with [limit](limit.josh) when the result has to stay inside the destination bounds.
