---
title: "Defining custom units"
order: 20
tags: [units, conversion]
---

A `unit` stanza introduces a unit the engine does not already know. `alias` gives it the spellings a
model may use — `year`, `years`, `yr`, `yrs` all name the same unit — and a conversion line states
its relationship to another unit, so `month = 12 * current` inside `start unit year` reads "one year
is twelve months".

`current` in a conversion means the unit being defined. Once both units are declared, `as months`
converts across them, and adding `1 year` to a value in months is well defined.
