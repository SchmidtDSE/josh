---
title: "Filtering a collection with a slice"
description: >-
  Selecting the members of a collection that satisfy a predicate, such as the dead trees out of
  all the trees on a patch.
order: 70
tags: [syntax, values, collections]
---

Square brackets after a collection filter it by a predicate evaluated per member, so
`current.JoshuaTrees[current.JoshuaTrees.state == "dead"]` is the subset whose `state` attribute is
`"dead"`.

Note that a string attribute is compared against a quoted literal here. That comparison works
against values the model itself assigned; strings that reach the model from the engine are not
always interchangeable with literals, so prefer comparing against attributes the model sets.
