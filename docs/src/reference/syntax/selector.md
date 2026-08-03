---
title: "Selectors on event handlers"
order: 40
tags: [syntax, conditionals]
---

A handler can carry its own conditions with `:if(...)`, `:elif(...)`, and `:else`, each introducing
a separate assignment. The engine picks the first selector whose condition holds and evaluates only
that one.

This differs from the conditional forms in intent rather than in result. `if`/`elif`/`else` branch
*within* one handler; selectors declare several handlers for the same attribute and event, each
guarded. A selector may also be used alone, as in [here.josh](../queries/here.josh), where a single
`:if(...)` makes the handler fire only under a condition and leave the attribute untouched
otherwise.
