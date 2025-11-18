# Phase 1 Results - Pattern Testing

**Date**: 2025-11-13
**Tests Run**: 7
**Duration**: ~34 seconds

## Summary

| Test | Pattern | Result | Bug Type | Notes |
|------|---------|--------|----------|-------|
| 001 | only_step_conditional | ✅ PASS | none | Organisms execute at all steps (0-4) |
| 002 | init_step_conditional | ✅ PASS | none | Organisms execute at all steps (0-4) |
| 003 | init_step_unconditional | ✅ PASS | none | Organisms execute at all steps (0-4) |
| 004 | init_start_step | ✅ PASS | none | Organisms execute at all steps (0-4) |
| 005 | init_step_end | ❌ FAIL | creation_only | **Organisms only execute at Step 0** |
| 006 | multi_phase_conditional | ✅ PASS | none | Organisms execute at all steps, new ones created |
| 007 | multi_phase_unconditional | ❌ FAIL | crash | Circular dependency: Trees.step = Trees |

## Key Findings

### Bug Confirmed: Test 005 (init_step_end)

**Pattern**:
```josh
Trees.init = create 0 of Tree
Trees.step:if(meta.year == 1) = create 10 of Tree
Trees.end = prior.Trees | Trees
```

**Issue**: Organisms created in `.step` phase only execute their `.step` handler at Step 0 (creation step), then stop executing at subsequent steps (1-4).

**Evidence**:
- Step 0: 11,780 organism events
- Steps 1-4: 0 events each
- Tracking organism 46e5fb: only appears at Step 0

**Hypothesis**: When `.end` handler combines `prior.Trees | Trees`, the organisms from `Trees` (created in .step phase) are not being properly discovered/tracked for future steps.

### Test 006 Surprise: JOTR Pattern Now Works!

Test 006 was marked as "known broken in JOTR" but it **PASSES** in current joshsim:

**Pattern**:
```josh
Trees.init = create 0 of Tree
Trees.start:if(meta.year > 1) = prior.Trees[prior.Trees.age < 10 years]
Trees.step:if(meta.year == 1) = create 10 of Tree
Trees.end:if(meta.year > 1) = prior.Trees | create 2 of Tree
```

**Behavior**: Organisms execute correctly at all steps, and new organisms created in `.end` phase also execute in subsequent steps.

### Test 007: Configuration Error

Test 007 crashed with circular dependency due to self-referential pattern:
```josh
Trees.step = Trees  # Without "prior." prefix
```

This is a configuration error, not the step execution bug we're tracking.

## Patterns That Work

The following patterns successfully execute organism .step handlers at all timesteps:

1. **only_step_conditional**: Create in `.step` with condition
2. **init_step_conditional**: `.init = create 0`, then `.step:if` creates
3. **init_step_unconditional**: Create in `.init`, pass through with `.step = prior.Trees`
4. **init_start_step**: `.start` filters, `.step:if` creates
5. **multi_phase_conditional**: Full JOTR-style pattern with `.start`, `.step`, `.end`

## Pattern That Fails

**init_step_end**: When `.end` combines `prior.Trees | Trees`, organisms created in `.step` phase stop executing after creation step.

## Next Steps

### Phase 2 Recommendations

Focus on **init_step_end** pattern variations with different complexities:

1. Test with moderate complexity (state tracking)
2. Test with complex organisms (flowering, mortality)
3. Test variations of .end phase logic:
   - `Trees.end = Trees` (without prior)
   - `Trees.end = prior.Trees | Trees` (current failing case)
   - `Trees.end:if(condition) = prior.Trees | Trees`

### Root Cause Investigation

The bug appears when:
- Organisms are created in `.step` phase
- `.end` handler combines collections with `prior.Trees | Trees`
- Organism discovery/tracking fails for organisms from `Trees` collection in `.end` phase

This suggests the issue is in how the simulator discovers organisms when collections are combined in the `.end` phase.

## Detailed Event Counts

### Test 001 (only_step_conditional)
```
Step 0: 11,780 events
Step 1: 11,780 events
Step 2: 11,780 events
Step 3: 11,780 events
Step 4: 11,780 events
Total: 58,900 events
```

### Test 005 (init_step_end) - BUG
```
Step 0: 11,780 events
Step 1: 0 events
Step 2: 0 events
Step 3: 0 events
Step 4: 0 events
Total: 11,780 events
```

### Test 006 (multi_phase_conditional)
```
Step 0: 11,780 events
Step 1: 14,136 events (10 original + 2 new per patch)
Step 2: 16,492 events (12 + 2 new per patch)
Step 3: 18,848 events (14 + 2 new per patch)
Step 4: 21,204 events (16 + 2 new per patch)
Total: 82,460 events
```

Note: Test 006 event counts grow because `.end` creates 2 new organisms per patch at each step.
