---
title: "Debug output with debugFiles"
order: 10
tags: [debugging, export]
runnable: true
simulation: Main
exports: [patch]
overlay: hello_debug_ci.josh
---

`debug(...)` writes a message from inside a handler, and `debugFiles.<entity>` says where those
messages go — a separate stream from `exportFiles`, so tracing a model does not disturb the data it
produces. This is the [Hello Grid](../../guides/hello/hello.josh) model with tracing added: the patch
reports its step, and each tree reports its age and height.

The value of a handler is what it returns, so a debug call is assigned to an attribute like anything
else. `dbg.step = debug("age:", current.age, ...)` exists to be evaluated, not to be read.

## Why this has an overlay

`debugFiles` targets have the same browser-versus-CLI split as `exportFiles`: `memory://editor/debug`
is what the editor reads, and the JVM rejects it. `exports:` only retargets `exportFiles`, so the
`debugFiles` line is redeclared by [hello_debug_ci.josh](hello_debug_ci.josh) instead — the case that
the `overlay:` field was added for.

This model was previously `examples/guide/hello_debug.josh`, paired with a `_cli` twin. Neither was
referenced by any script, workflow, or page, so nothing had ever checked either of them.
