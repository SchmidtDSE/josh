---
title: "Measuring where time goes with evalDuration"
order: 10
tags: [performance, profiling]
---

Every attribute carries an `evalDuration` alongside its value, holding how long that attribute took
to evaluate. Exporting `sum(Tree.height.evalDuration)` next to the value itself puts the cost of each
part of the model in the same CSV as its output.

This model is the "before" half of a pair: 200 trees and 5000 shrubs per patch, with the shrub
cover computed per individual. The exported `shrubCoverMs` column is what makes the cost of those
5000 entities visible rather than inferred. [eval_duration_optimized.josh](eval_duration_optimized.josh)
is the same model with that cost removed.
