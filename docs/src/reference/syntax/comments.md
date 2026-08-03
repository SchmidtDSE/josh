---
title: "Comments"
order: 10
tags: [syntax]
---

Everything after a `#` on a line is a comment. Comments may follow an expression, as they do inside
the `age.step` block here, or occupy a line of their own.

Model files in this repository are otherwise kept comment-free: an explanation of *why* a model is
written a certain way belongs in this sidecar, and a tunable parameter belongs in a `.jshc` config
file where whoever is tuning it will look. `#` is for the rare note that only makes sense at the
exact line it sits on.
