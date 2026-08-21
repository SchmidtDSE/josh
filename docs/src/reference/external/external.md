---
title: "Declaring an external data source"
description: >-
  Naming a dataset, where it comes from, and what units its values carry, so that handlers can
  read it.
order: 10
tags: [external, data]
---

An `external` stanza names a dataset the model reads and says where it comes from, what format it is
in, and what units its values carry. Once declared, handlers read it as `external <Name>`.

`source.location` is a template: `{{ current.year }}` is substituted from the stanza's own
attributes, so one declaration covers a directory of per-year rasters. Declaring `source.units` is
what lets the engine convert the incoming values into whatever unit the model does its arithmetic
in.

External data is read from preprocessed `.jshd` files at run time rather than from the raw raster;
`preprocess` produces them.
