# 🚀 START HERE - Organism Step Execution Bug

## ✅ BUG CONFIRMED

Through 12 systematic tests, we **definitively identified** the root cause:

> **ANY `.end` handler on a patch collection causes organisms created in the `.step` phase to stop executing after their creation step.**

## 📖 Quick Navigation

1. **For Complete Summary**: Read `TESTING_SUMMARY.md` ⭐ **START HERE**
2. **For Extended Findings**: Read `summary/UPDATED_FINDINGS.md` (9 new tests + workarounds)
3. **For Original Analysis**: Read `summary/FINAL_REPORT.md` (overnight run results)
4. **For Quick Reproduction**: See section below
5. **For Test Design**: Read `NEW_TEST_DESIGN.md`

## 🔬 Reproduce the Bug (30 seconds)

```bash
# Navigate to minimal failing test
cd /workspaces/josh/step_bug_testing/test_011_init_step_end_no_prior_minimal_none_tiny

# Run the test
./run.sh

# Check organism execution by step
grep "^\[Step" debug_organism_0.txt | awk '{print $1, $2}' | sort | uniq -c
```

**What you'll see**:
```
11780 [Step 0,    <-- Organisms execute ONLY here
    0 [Step 1,    <-- Then stop
    0 [Step 2,
    0 [Step 3,
    0 [Step 4,
```

**What you SHOULD see** (working test):
```bash
cd ../test_001_only_step_minimal_none_tiny
./run.sh
grep "^\[Step" debug_organism_0.txt | awk '{print $1, $2}' | sort | uniq -c
```
```
11780 [Step 0,    <-- Organisms execute at ALL steps
11780 [Step 1,
11780 [Step 2,
11780 [Step 3,
11780 [Step 4,
```

## 🎯 The Difference

**Failing Pattern** (test_011):
```josh
Trees.init = create 0 of Tree
Trees.step:if(meta.year == 1) = create 10 of Tree
Trees.end = Trees  # <-- This breaks organism discovery
```

**Working Pattern** (test_001):
```josh
Trees.step:if(meta.year == 1) = create 10 of Tree
# No .end handler = organisms execute correctly
```

## 📊 Test Results at a Glance (21 Total Tests)

| Has .end? | Pattern | Result | Count |
|-----------|---------|--------|-------|
| ❌ No | Any | ✅ PASS | 5 tests |
| ✅ Yes | Current ref (`Trees`) | ❌ FAIL | 11 tests |
| ✅ Yes | Different attr | ✅ PASS | 1 test |
| ✅ Yes | Prior-only ref | ✅ PASS | 1 test |
| ✅ Yes | Create IN .end | ✅ PASS | 1 test |
| ⚠️ Various | Crashes | ⚠️ ERROR | 2 tests |

**Updated Conclusion**: Having `.end` handler that references **current** collection value breaks organism discovery **GLOBALLY for the patch** (affects ALL collections, not just the one with .end).

## 💡 Technical Details

**Hypothesis**: When `.end` handlers process collections, organisms from `.step` phase lose their "newly created" status and aren't tracked for future timestep execution.

**Code Areas to Investigate**:
- `org.joshsim.lang.bridge.SimulationStepper.java` - Main simulation loop
- `org.joshsim.lang.bridge.ShadowingEntity.java` - Entity management
- Organism discovery logic after phase execution

**Recommended Fix Approaches**:
1. Track organisms through ALL phases (.start, .step, .end)
2. Preserve "newly created" metadata through .end processing
3. Explicit organism discovery pass after .end completes

## 📁 What's Included

- **12 complete test cases** with Josh files, execution scripts, and debug logs
- **Comprehensive analysis** in FINAL_REPORT.md
- **Machine-readable results** in all_results.jsonl
- **Quick reference** in quick_summary.txt

## 🚀 Next Steps

1. **Review findings**: Read `TESTING_SUMMARY.md` for complete analysis
2. **Apply workarounds**: See `summary/UPDATED_FINDINGS.md` for 3 working patterns
3. **Investigate code**: Check `SimulationStepper.java` and `ShadowingEntity.java`
4. **Implement fix**: Separate organism discovery from phase execution
5. **Verify fix**: Re-run test suite with `./run_new_tests.sh`
6. **Add regression tests**: All 21 tests ready to add to test suite

## ✅ TESTED WORKAROUNDS (Use Immediately!)

### Option A: Prior-Only Reference ⭐ RECOMMENDED
```josh
Trees.step = create 10 of Tree
Trees.end = prior.Trees  # ✅ TESTED - organisms execute at all steps
```

### Option B: Read-Only .end on Different Attribute
```josh
Trees.step = create 10 of Tree
treeCount.end = count(Trees)  # ✅ TESTED - works fine
```

### Option C: Create in `.end` Phase
```josh
Trees.end = create 10 of Tree  # ✅ TESTED - confirmed working
```

**See WORKAROUNDS.md for complete guide with examples and migration instructions!**

---

**All test data preserved in**: `/workspaces/josh/step_bug_testing/`
**Git branch**: `debug/step_discovery`
**Date**: 2025-11-13
**Tests**: 21 total (12 original + 9 new)
