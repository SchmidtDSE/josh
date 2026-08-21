---
title: "Binding a distribution to a const"
description: >-
  A distribution is a value: it can be named and passed around before anything is drawn from it.
order: 30
tags: [distributions, stochastic]
---

A distribution expression written without `sample` produces a distribution value, which can be bound
to a `const` and passed around before any draw is taken. Here `normal with mean of 5 count std of
1 count` is bound to `cnt` while the `create` uses a literal count.

This is the other half of the pair with [autobox_scalar.josh](autobox_scalar.josh): a scalar and a
distribution are both values, and it is `sample` — not the binding — that collapses one to the other.
