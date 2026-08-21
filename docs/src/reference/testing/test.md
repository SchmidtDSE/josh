---
title: "Test stanzas (reserved)"
description: >-
  Why start test is not valid syntax, and where a model's assertions are written instead.
order: 10
tags: [testing, reserved]
status: reserved
reason: >-
  `entityStanzaType` in JoshLang.g4 has no `test` alternative, so `start test <Name>` fails to parse
  with "no viable alternative at input 'start test'"; assertions are written as `assert.*` handlers
  on an entity instead.
---

A `test` stanza would bundle a grid, a step range, and one or more `assert` expressions into a
self-contained check. The grammar does not have it: `entityStanzaType` admits `disturbance`,
`external`, `organism`, `management`, `patch`, and `simulation`, and nothing else.

Assertions today are written as `assert.<name>` handlers on an entity, which the runner treats as
failures when they evaluate false. [spinup.josh](../time/spinup.josh) is a working example, and the
conformance suite under `josh-tests/` is built entirely from that form.

This unit had been checked by nothing at all before the migration — it was never added to
`examples/validate.sh`'s list, so the fact that it does not parse went unrecorded.
