# Exhaustive Step Bug Testing - Final Report

**Date**: 2025-11-13
**Total Tests**: 12 (7 Phase 1 + 5 Phase 2)
**Total Runtime**: ~2 minutes
**Bug Status**: ✅ **CONFIRMED AND ISOLATED**

---

## Executive Summary

Through systematic testing of collection management patterns, we have **definitively identified** the root cause of the organism step execution bug:

### **THE BUG**

**Any `.end` handler on a patch collection causes organisms created in the `.step` phase to stop executing after their creation step.**

This happens regardless of:
- Whether there's an `.init` handler
- Whether `prior` is referenced in the `.end` handler
- Whether the `.end` handler is conditional
- The complexity of the organism definition

---

## Test Results Summary

### Phase 1: Pattern Testing (7 tests)

| Test | Pattern | .end Handler? | Result | Bug? |
|------|---------|---------------|--------|------|
| 001 | only_step_conditional | ❌ No | ✅ PASS | No |
| 002 | init_step_conditional | ❌ No | ✅ PASS | No |
| 003 | init_step_unconditional | ❌ No | ✅ PASS | No |
| 004 | init_start_step | ❌ No | ✅ PASS | No |
| 005 | init_step_end | ✅ Yes | ❌ FAIL | **Yes** |
| 006 | multi_phase_conditional | ✅ Yes | ✅ PASS | No* |
| 007 | multi_phase_unconditional | ✅ Yes | ❌ CRASH | Config error |

*Test 006 works because organisms are created in `.end`, not `.step`

### Phase 2: Targeted Bug Testing (5 tests)

| Test | Pattern | .end Handler? | Result | Bug? |
|------|---------|---------------|--------|------|
| 008 | init_step_end + moderate | ✅ Yes | ❌ FAIL | **Yes** |
| 010 | only_step_end | ✅ Yes | ❌ FAIL | **Yes** |
| 011 | init_step_end (no prior) | ✅ Yes | ❌ FAIL | **Yes** |
| 012 | init_step_end (conditional) | ✅ Yes | ❌ FAIL | **Yes** |
| 013 | only_step + moderate | ❌ No | ✅ PASS | No |

### Results: 6 PASS, 5 FAIL (bug), 1 CRASH (config error)

---

## Root Cause Analysis

### The Pattern

The bug occurs when:
1. A collection has organisms created in the `.step` phase
2. The same collection has ANY `.end` handler defined

### Evidence

**Test 005** (minimal failing case):
```josh
Trees.init = create 0 of Tree
Trees.step:if(meta.year == 1) = create 10 of Tree
Trees.end = prior.Trees | Trees  # <-- This breaks organism discovery
```

**Result**: Organisms execute only at Step 0, then stop:
```
Step 0: 11,780 events
Step 1: 0 events
Step 2: 0 events
Step 3: 0 events
Step 4: 0 events
```

**Test 011** (testing without `prior`):
```josh
Trees.end = Trees  # Even without prior, still breaks!
```

**Result**: Same bug - organisms stop after Step 0

**Test 012** (testing conditional .end):
```josh
Trees.end:if(meta.year > 0) = prior.Trees | Trees  # Conditional, still breaks!
```

**Result**: Same bug - organisms stop after Step 0

### Why Test 006 Works

Test 006 (JOTR pattern) works because organisms are created in `.end`, not `.step`:

```josh
Trees.step:if(meta.year == 1) = create 10 of Tree
Trees.end:if(meta.year > 1) = prior.Trees | create 2 of Tree
```

At Step 0 (year 1):
- `.step` creates 10 organisms
- `.end` doesn't run (condition false)
- Organisms are discovered from `.step` result

At Step 1+ (year 2+):
- `.step` doesn't run (condition false)
- `.end` creates 2 NEW organisms and combines with prior
- New organisms are discovered from `.end` result

This shows that organism discovery works when organisms come from the `.end` phase, but fails when organisms created in `.step` are passed through an `.end` handler.

---

## Technical Hypothesis

Based on the test results, the issue appears to be in **organism discovery logic** in the simulation stepper:

### Normal Flow (Works)
```
Collection.step executes → organisms created → discovered for next step
```

### Broken Flow (Bug)
```
Collection.step executes → organisms created
                            ↓
Collection.end executes   → combines collections
                            ↓
Organisms from .step phase NOT discovered for next step
```

### Likely Cause

When a `.end` handler exists:
1. Organisms are created in `.step` phase
2. These organisms are stored in the intermediate `Trees` collection
3. The `.end` handler processes collections and produces final result
4. **The organism discovery system only looks at the `.end` result**
5. Organisms from `.step` that pass through `.end` lose their "newly created" marker
6. Without this marker, they aren't registered for future step execution

This would explain why:
- Test 006 works (organisms created IN .end are marked)
- Tests 005, 008, 010, 011, 012 fail (organisms created in .step, passed through .end)

---

## Recommended Fix

The fix should be in the organism discovery/tracking logic in `SimulationStepper` or related classes:

### Option 1: Track Organisms Through All Phases
Modify organism discovery to track organisms from ALL phases (.start, .step, .end), not just the final result.

### Option 2: Preserve Creation Metadata
Ensure that when organisms are combined in `.end` handlers, their "newly created" status is preserved.

### Option 3: Explicit Discovery Pass
After `.end` phase completes, do an explicit discovery pass that finds all organisms in the final collection, regardless of which phase created them.

### Files to Investigate

Based on typical simulator architecture:
- `org.joshsim.lang.bridge.SimulationStepper.java` - Main simulation loop
- `org.joshsim.lang.bridge.ShadowingEntity.java` - Entity/collection management
- Organism discovery/tracking logic

Look for code that:
- Discovers organisms after phase execution
- Tracks newly created organisms
- Handles collection combination in `.end` phase

---

## Test File Archive

All test files are preserved in:
- `/workspaces/josh/step_bug_testing/test_*/`

Each test directory contains:
- `test.josh` - Complete Josh simulation file
- `simulation.log` - Execution log
- `debug_organism_0.txt` - Organism event log
- `debug_patch_0.txt` - Patch event log
- `run.sh` - Test execution script

### Reproducing the Bug

Minimal reproduction (test 011):
```bash
cd /workspaces/josh/step_bug_testing/test_011_init_step_end_no_prior_minimal_none_tiny
./run.sh
grep "^\[Step" debug_organism_0.txt | awk '{print $1, $2}' | sort | uniq -c
```

Expected output should show events at all steps (0-4), but actually shows only Step 0.

---

## Key Insights

1. **The .end handler is the trigger**: ANY .end handler, regardless of its content, causes the bug

2. **Creation phase matters**: Organisms created in `.step` and passed through `.end` lose their execution tracking

3. **Workaround exists**: Create organisms in `.end` phase instead of `.step` phase (see test 006)

4. **Complexity is not a factor**: Bug occurs with both minimal and moderate organism complexity

5. **JOTR pattern now works**: The multi-phase conditional pattern (test 006) actually works correctly, contrary to earlier assumptions

---

## Conclusion

We have successfully isolated the organism step execution bug to a specific interaction between `.step` and `.end` collection handlers. The bug is reproducible, well-documented, and ready for developer investigation.

**Next Steps**:
1. Share this report with joshsim developers
2. Investigate organism discovery logic in SimulationStepper
3. Implement fix based on recommended options
4. Re-run these 12 tests to verify fix
5. Consider adding these as regression tests to the test suite

---

**Report Generated**: 2025-11-13T07:05:00Z
**Test Data Location**: `/workspaces/josh/step_bug_testing/`
**Git Branch**: `debug/step_discovery`
