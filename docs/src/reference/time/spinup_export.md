---
title: "Exporting the spin-up phase"
description: >-
  Two ways to separate the observed window from the burn-in around it: a phase column in the CSV,
  or narrowing the export.
order: 20
tags: [time, spinup, export]
runnable: true
simulation: SpinupExport
---

The companion to [spinup.josh](spinup.josh), showing what the phase marker is for once the run is
over. `export.state.step = meta.state` puts the phase in the exported CSV as an ordinary column, so a
caller can select the spin-up, observed, or spin-down rows after the fact.

That is the alternative to filtering during the run. `--output-steps 0-2` narrows the export to the
observed window using the inclusive range form, which scales to long runs in a way that listing each
index does not. `examples/validate.sh` exercises both paths against this model and checks which
phases appear in the resulting CSV.
