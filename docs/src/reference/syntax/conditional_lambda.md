---
title: "Inline conditional expressions"
description: >-
  The one-line conditional, for a handler whose every branch is a single value and which needs no
  body at all.
order: 30
tags: [syntax, conditionals]
---

`X if (condition) else Y` is an expression, not a statement, so it can appear anywhere a value can
— including directly on the right of a handler with no `{ }` body at all.

This is the compact counterpart to [conditional_full.josh](conditional_full.josh): it fits when
each branch is a single value and there is nothing else for the handler to do.
