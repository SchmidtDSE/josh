---
title: "Branching with if, elif, and else"
order: 20
tags: [syntax, conditionals]
---

Inside a `{ }` handler body, `if` / `elif` / `else` branch between statements, and each branch
returns its own value. The conditions are evaluated in order, so this handler grows a tree by
0.1 m once it is at least 10 m tall, by 0.5 m once it is at least 5 m, and by 1 m otherwise.

Use this form when a branch needs more than one statement. When every branch is a single
expression, the inline form in
[conditional_lambda.josh](conditional_lambda.josh) says the same thing in one line, and
[selector.josh](selector.josh) moves the conditions onto the handler itself.
