---
title: "Creating entities in a patch"
description: >-
  Populating a grid cell with create, and how the attribute a create is assigned to becomes
  the name the rest of the model queries.
order: 20
tags: [entities, patch]
---

A patch holds the organisms, disturbances, and management actions that occupy one cell of the grid.
`create <n> of <Type>` instantiates a count of them; `create <Type>` with no count instantiates one.

The attribute a `create` is assigned to is what the rest of the model queries, so `TreeA.init`
populates the collection later referred to as `here.TreeA`.
