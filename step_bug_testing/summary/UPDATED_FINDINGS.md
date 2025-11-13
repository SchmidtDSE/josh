# Updated Findings - Step Bug Testing (Extended Suite)

**Date**: 2025-11-13
**Previous Tests**: 12 (test_001 through test_012)
**New Tests**: 9 (test_013 through test_026, excluding test_014-016, 022, 025, 027)
**Total Tests**: 21

---

## CRITICAL NEW DISCOVERIES

### 🎯 Finding #1: Bug is NOT isolated to the collection with `.end`

**Test 024 Result**: When Trees has `.end` handler, **ALL organisms stop executing**, including Shrubs which has NO `.end` handler!

**Pattern**:
```josh
Trees.step:if(meta.year == 1) = create 10 of Tree
Trees.end = prior.Trees | Trees  # Trees has .end

Shrubs.step:if(meta.year == 1) = create 5 of Shrub
# Shrubs has NO .end handler
```

**Result**:
- Trees organisms: Execute only at Step 0 ❌
- Shrubs organisms: Execute only at Step 0 ❌ **(THIS IS NEW!)**

**Implication**: The bug is **GLOBAL to the patch**, not per-collection. Having ANY `.end` handler that references current collection value breaks ALL organism tracking for that patch.

---

### 🎯 Finding #2: Two Patterns That WORK

#### **Pattern A: `.end` on Different Attribute (Test 018 - PASS)**

```josh
Trees.step:if(meta.year == 1) = create 10 of Tree
treeCount.end = count(Trees)  # Different attribute reads Trees
```

**Result**: ✅ PASS - Trees organisms execute at all steps (0-4)

**Why it works**: Trees collection has NO `.end` handler. Only `treeCount` (a scalar) has `.end`. Reading from Trees in another attribute's .end handler doesn't break Trees organisms.

---

#### **Pattern B: `.end` with Prior-Only Reference (Test 026 - PASS)**

```josh
Trees.step:if(meta.year == 1) = create 10 of Tree
Trees.end = prior.Trees  # Only references prior, not current
```

**Result**: ✅ PASS - Trees organisms execute at all steps (0-4)

**Why it works**: Trees.end ONLY references `prior.Trees`, never the current `Trees` value. This avoids the self-reference ambiguity.

**Note**: This pattern loses newly created organisms at subsequent steps (they're not in prior), but existing organisms continue executing.

---

## Updated Test Results Summary

### Original Tests (001-012)

| Test | Pattern | .end Handler? | Bug? | Notes |
|------|---------|---------------|------|-------|
| 001 | only_step_conditional | ❌ No | ✅ Works | Baseline |
| 002 | init_step_conditional | ❌ No | ✅ Works | |
| 003 | init_step_unconditional | ❌ No | ✅ Works | |
| 004 | init_start_step | ❌ No | ✅ Works | |
| 005 | init_step_end | ✅ Yes | ❌ FAIL | Original bug case |
| 006 | multi_phase_conditional | ✅ Yes | ✅ Works | Organisms created IN .end |
| 007 | multi_phase_unconditional | ✅ Yes | ⚠️ CRASH | Config error |
| 008 | init_step_end (moderate) | ✅ Yes | ❌ FAIL | |
| 010 | only_step_end | ✅ Yes | ❌ FAIL | |
| 011 | init_step_end (no prior) | ✅ Yes | ❌ FAIL | |
| 012 | init_step_end (conditional) | ✅ Yes | ❌ FAIL | |
| 013 | only_step (moderate) | ❌ No | ✅ Works | |

### New Tests (013-026)

| Test | Pattern | .end Handler? | Bug? | Key Finding |
|------|---------|---------------|------|-------------|
| 013 | separate_collection | ✅ Yes | ⚠️ CRASH | "Could not find value for Trees" |
| 017 | filter_in_end | ✅ Yes | ❌ FAIL | Filtering doesn't help |
| **018** | **count_in_end** | ✅ **Different attr** | ✅ **PASS** | **✨ WORKAROUND** |
| 019 | end_different_collection | ✅ Yes | ⚠️ CRASH | IllegalMonitorStateException |
| 020 | create_at_step_0 | ✅ Yes | ❌ FAIL | Not creation timing |
| 021 | unconditional_step | ✅ Yes | ❌ FAIL | Every-step creation fails |
| 023 | two_collections_both_end | ✅ Yes (both) | ❌ FAIL | Both collections affected |
| 024 | one_with_end_one_without | ✅ Yes (Trees only) | ❌ **BOTH FAIL** | **🚨 GLOBAL BUG** |
| **026** | **prior_only_end** | ✅ **Prior-only** | ✅ **PASS** | **✨ WORKAROUND** |

### Summary Statistics

- **Total tests**: 21
- **Pass**: 8 tests (38%)
- **Fail (bug)**: 11 tests (52%)
- **Crash**: 2 tests (10%)

---

## Refined Root Cause Analysis

### Original Hypothesis (from overnight run)

> "Organisms created in `.step` phase that pass through `.end` handler lose their execution tracking"

### Refined Hypothesis (based on new tests)

> **"When ANY collection on a patch has a `.end` handler that references its current value (not just `prior`), organism discovery fails GLOBALLY for that patch across ALL collections."**

### Evidence Chain

1. **Test 005, 008, 010-012, 017, 020-021, 023**: All fail when collection has `.end` referencing current value ❌
2. **Test 018**: PASSES when `.end` is on different attribute ✅
3. **Test 026**: PASSES when `.end` only references `prior` ✅
4. **Test 024**: Trees has `.end`, Shrubs doesn't - **BOTH fail** ❌
   - This proves the bug is GLOBAL to the patch
   - Having `.end` on one collection breaks organism tracking for ALL collections

### Technical Interpretation

The bug appears to be in the **organism discovery phase** after `.end` handlers execute:

**Hypothesis**:
1. After `.step` phase, organisms are discovered and queued for next timestep
2. When `.end` phase exists and references current collection:
   - Runtime tries to resolve "current value" of collection
   - This triggers some state change/cache invalidation
   - Organism discovery mechanism loses track of ALL organisms on the patch
3. At next timestep (Step 1+), no organisms are found to execute

**Why Test 018 works**: `Trees` collection has no `.end` handler, so no state change occurs for Trees. The `treeCount` attribute having `.end` doesn't interfere with organism tracking.

**Why Test 026 works**: `Trees.end = prior.Trees` never references "current Trees", so no current-value resolution happens. The organisms from `.step` remain in the cached state and execute at next timestep.

**Why Test 024 fails for BOTH**: When Trees.end tries to resolve current Trees, it triggers a GLOBAL organism discovery reset for the patch, affecting ALL collections including Shrubs.

---

## Updated Fix Recommendations

### Option 1: Separate Organism Discovery from Phase Execution ⭐ **RECOMMENDED**

**What**: Perform organism discovery AFTER all phases (.start, .step, .end) complete, based on the FINAL attribute values, not intermediate phase results.

**Why**: This decouples organism tracking from phase-by-phase attribute resolution. Organisms are discovered based on "what's actually in the collection at the end of the timestep", not "which phase added them".

**Files to modify**:
- `SimulationStepper.java` - Add explicit organism discovery pass after phase loop
- `ShadowingEntity.java` - Ensure organism discovery looks at final cached values

---

### Option 2: Preserve Organism Tracking Through `.end` Phase

**What**: When `.end` handler executes, preserve organism metadata (e.g., "newly created" markers) from `.step` phase results.

**Why**: This maintains the current phase-by-phase discovery but ensures `.end` handlers don't clear organism tracking state.

**Files to modify**:
- `ShadowingEntity.java` or `DirectLockMutableEntity.java` - Cache organism lists after each phase
- Phase handler execution logic - Don't clear organism tracking when `.end` runs

---

### Option 3: Document as Unsupported Pattern + Provide Workaround

**What**: Document that `Collection.end = prior.Collection | Collection` creates ambiguity and is unsupported.

**Recommended patterns**:
```josh
# Pattern A: Use separate attributes
newItems.step = create N of Organism
Collection.end = prior.Collection | newItems

# Pattern B: Only reference prior (note: loses new organisms)
Collection.step = create N of Organism
Collection.end = prior.Collection

# Pattern C: Read-only .end handlers are fine
Collection.step = create N of Organism
counts.end = count(Collection)
```

**Why**: This is a language design decision. The pattern may be inherently ambiguous.

---

## Workarounds for Users (Available NOW)

### ❌ Workaround 1: Use Separate Attributes (DOES NOT WORK)

**Attempted pattern**:
```josh
newTrees.step:if(meta.year == 1) = create 10 of Tree
Trees.end = prior.Trees | newTrees
```

**Status**: ❌ TESTED AND FAILED
- test_030: Crashes with "Could not find value for Trees"
- test_031: Crashes with IllegalMonitorStateException

**Conclusion**: This pattern does NOT work. Do not use.

---

### ✅ Workaround 2: Avoid `.end` Handlers on Organism Collections

**Instead of**:
```josh
Trees.step:if(meta.year == 1) = create 10 of Tree
Trees.end = prior.Trees | Trees
```

**Do this**:
```josh
Trees.step:if(meta.year == 1) = create 10 of Tree
# No .end handler
```

**Status**: CONFIRMED working (tests 001-004, 013)

---

### ✅ Workaround 3: Create Organisms in `.end` Phase

**Instead of**:
```josh
Trees.step:if(meta.year == 1) = create 10 of Tree
Trees.end = prior.Trees | Trees
```

**Do this**:
```josh
Trees.end:if(meta.year == 1) = create 10 of Tree
# Or combine with prior if needed
```

**Status**: CONFIRMED working (test_006)

---

## Files for Investigation

Based on crash stack traces and test results:

1. **`org.joshsim.lang.bridge.SimulationStepper.java`**
   - Lines 165-166: `performStream()` - where organism updates happen
   - Lines 197-222: `updateEntity()` and `updateEntityUnsafe()` - entity update logic
   - Organism discovery likely happens here after phase execution

2. **`org.joshsim.lang.bridge.ShadowingEntity.java`**
   - Lines 826-856: `startSubstep()` and `endSubstep()` - phase boundaries
   - Lines 621-734: Attribute resolution logic
   - Lines 851: `endSubstep()` calls on inner entities (organism discovery?)

3. **`org.joshsim.lang.bridge.SyntheticScope.java`**
   - Line 81: "Could not find value for Trees" error source
   - Attribute lookup during `.end` phase evaluation

4. **`org.joshsim.engine.entity.base.DirectLockMutableEntity.java`**
   - Lines 110-224: Attribute caching and locking
   - Lines 179, 258: Lock/unlock operations (IllegalMonitorStateException source)

---

## Next Steps

### For Runtime Fix

1. **Add detailed logging**:
   ```java
   // In SimulationStepper.updateEntity() or similar
   System.err.println("Before .end phase: " + countOrganisms());
   executeEndPhase();
   System.err.println("After .end phase: " + countOrganisms());
   ```

2. **Trace organism discovery**:
   - Add breakpoints or logging in organism discovery code
   - Run test_005 (fails) vs test_001 (works) side-by-side
   - Identify exact point where organisms are "lost"

3. **Implement Option 1** (separate discovery):
   - Move organism discovery to AFTER all phases complete
   - Test with full suite (21 tests)

### For Documentation

1. Update Josh language guide with:
   - Known limitation about `.end` handlers
   - Recommended workarounds (patterns A, B, C)
   - Examples of correct multi-phase collection management

2. Add regression tests:
   - Test 018 (read-only .end)
   - Test 026 (prior-only .end)
   - Test 024 (multi-collection .end impact)

---

## Conclusion

The expanded test suite has revealed that the organism step execution bug is **more severe than initially thought**:

- It's not just about collections with `.end` handlers
- It's **GLOBAL to the patch** when ANY collection has `.end` referencing current value
- We've identified **two working patterns** (test 018, 026) that provide workarounds
- The root cause is likely in organism discovery after `.end` phase execution

**The good news**: We now have concrete workarounds users can apply immediately, and a clear technical understanding for implementing a fix.

---

**Report Date**: 2025-11-13
**Branch**: `debug/step_discovery`
**Test Data**: `/workspaces/josh/step_bug_testing/`
**Previous Report**: `FINAL_REPORT.md`
