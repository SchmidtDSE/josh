---
title: "Loading a config file from the model (reserved)"
order: 20
tags: [config, reserved]
status: reserved
reason: >-
  The `config "<url>" as <name>` statement parses but is rejected by the interpreter with
  "Configuration statements reserved for future use"; config values are supplied through a `.jshc`
  namespace instead.
---

Josh's grammar accepts a `config "<url>" as <name>` statement, but the interpreter refuses it — this
form is reserved for a future release. It is kept here so the syntax is recorded rather than
rediscovered.

Configuration today is supplied by namespace, as in
[config_example.josh](config_example.josh): the model names `config example.testVar1` and the runner
resolves `example` to a `.jshc` file. Nothing is validated or run for a reserved unit, so this file
cannot silently start passing without someone noticing the status here is stale.
