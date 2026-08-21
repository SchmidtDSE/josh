---
title: "Disturbance stanzas"
description: >-
  The entity type for something that happens to a patch — fire, flood, harvest — rather than
  something that lives in it.
order: 30
tags: [entities, disturbance]
---

A `disturbance` is an entity type for events that act on a patch rather than living in it — fire,
flood, harvest. It takes the same `init` / `step` handlers as an organism.

This one is active on the step it is created and inactive on every step after, which is the shape of
a one-off event. Organisms observe it the same way they observe each other, through `here.Fire`;
[here.josh](../queries/here.josh) shows a handler that fires only when a disturbance is present.
