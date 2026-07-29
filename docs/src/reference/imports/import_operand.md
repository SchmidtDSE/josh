---
title: "A file of shared unit definitions"
order: 10
tags: [imports, units]
---

A `.josh` file need not declare a simulation. This one declares only units — `year` with its aliases
and its conversion to `month`, and `month` itself — so several models can share one definition
instead of repeating it.

It is a complete, valid model on its own terms, which is why it validates here rather than only as
the operand of the import beside it.
