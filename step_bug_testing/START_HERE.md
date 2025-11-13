# 🚀 START HERE - Organism Step Execution Bug

## ✅ BUG CONFIRMED

Through 12 systematic tests, we **definitively identified** the root cause:

> **ANY `.end` handler on a patch collection causes organisms created in the `.step` phase to stop executing after their creation step.**

## 📖 Quick Navigation

1. **For Executive Summary**: Read `summary/quick_summary.txt`
2. **For Full Analysis**: Read `summary/FINAL_REPORT.md`
3. **For Quick Reproduction**: See section below
4. **For All Results**: See `summary/all_results.jsonl`

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

## 📊 Test Results at a Glance

| Has .end? | Organisms in .step? | Result | Count |
|-----------|---------------------|--------|-------|
| ❌ No | ✅ Yes | ✅ PASS | 6 tests |
| ✅ Yes | ✅ Yes | ❌ FAIL | 5 tests |
| ✅ Yes | ❌ No (in .end) | ✅ PASS | 1 test |

**Conclusion**: The `.end` handler is the trigger. It breaks organism discovery for organisms created in `.step`.

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

1. Review `summary/FINAL_REPORT.md` for full technical analysis
2. Investigate organism discovery code in SimulationStepper
3. Implement fix based on recommended approaches
4. Re-run test suite: `./common/test_runner.sh`
5. Add these as regression tests

---

**All test data preserved in**: `/workspaces/josh/step_bug_testing/`
**Git branch**: `debug/step_discovery`
**Date**: 2025-11-13
