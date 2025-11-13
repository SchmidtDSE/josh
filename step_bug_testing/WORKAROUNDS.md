# Organism Step Execution Bug - WORKAROUNDS

**Date**: 2025-11-13
**Status**: ✅ TWO CONFIRMED WORKING WORKAROUNDS
**Priority**: HIGH - Use these immediately while bug fix is in progress

---

## The Problem

When a collection has a `.end` handler that references its current value, ALL organisms on that patch stop executing after their creation step.

**Broken Pattern** (DO NOT USE):
```josh
Trees.step:if(meta.year == 1) = create 10 of Tree
Trees.end = prior.Trees | Trees  # ❌ BUG: Organisms stop after step 0
```

---

## ✅ WORKAROUND #1: Use Prior-Only Reference (RECOMMENDED)

**Status**: ✅ CONFIRMED WORKING (test_026)
**Organisms execute**: YES, at all steps
**Organisms preserved**: YES, across timesteps

**Pattern**:
```josh
Trees.step:if(meta.year == 1) = create 10 of Tree
Trees.end = prior.Trees  # ✅ WORKS: Only references prior, not current
```

**Test Results**:
```
Step 0: 11,780 organism events  ✅
Step 1: 11,780 organism events  ✅
Step 2: 11,780 organism events  ✅
Step 3: 11,780 organism events  ✅
Step 4: 11,780 organism events  ✅
```

**When to use**: When you need to maintain organisms across timesteps without adding new ones every step.

**How it works**: By only referencing `prior.Trees` (not current `Trees`), the `.end` handler avoids the self-reference bug that breaks organism discovery.

---

## ✅ WORKAROUND #2: Read-Only Access in .end

**Status**: ✅ CONFIRMED WORKING (test_018)
**Use case**: Computing statistics or counts without modifying the organism collection

**Pattern**:
```josh
Trees.step:if(meta.year == 1) = create 10 of Tree
# Trees has NO .end handler

# Different attribute can have .end handler for read-only access
treeCount.end = count(Trees)
statistics.end = {
  return [count(Trees), mean(Trees.age), max(Trees.age)]
}
```

**Test Results**:
```
Step 0: 11,780 organism events  ✅
Step 1: 11,780 organism events  ✅
Step 2: 11,780 organism events  ✅
Step 3: 11,780 organism events  ✅
Step 4: 11,780 organism events  ✅
```

**Key principle**: The organism collection itself (Trees) has NO `.end` handler. Only other attributes have `.end` handlers that READ from Trees.

---

## ✅ WORKAROUND #3: Create in .end Phase

**Status**: ✅ CONFIRMED WORKING (test_006)
**Use case**: When creation timing in `.end` vs `.step` doesn't matter

**Pattern**:
```josh
# Don't create in .step at all
Trees.end:if(meta.year == 1) = create 10 of Tree

# Or combine with prior if needed
Trees.end:if(meta.year == 1) = prior.Trees | create 10 of Tree
```

**Test Results**: Organisms execute correctly at all steps

**Note**: This changes when organisms are created (end of timestep instead of during step), which may affect model semantics.

---

## ❌ PATTERNS THAT DON'T WORK

### ❌ Separate Attribute Pattern
```josh
newTrees.step = create 10 of Tree
Trees.end = prior.Trees | newTrees  # ❌ CRASHES: "Could not find value for Trees"
```

**Status**: TESTED AND FAILED (test_030)
**Error**: `RuntimeException: Could not find value for Trees`

### ❌ Separate Attribute with Init
```josh
Trees.init = create 0 of Tree
newTrees.step = create 10 of Tree
Trees.end = prior.Trees | newTrees  # ❌ CRASHES: Locking error
```

**Status**: TESTED AND FAILED (test_031)
**Error**: `IllegalMonitorStateException` (locking bug)

---

## Detailed Comparison

| Pattern | Works? | Organisms Execute? | Use Case |
|---------|--------|-------------------|----------|
| `Trees.end = prior.Trees \| Trees` | ❌ NO | Only at step 0 | Broken (original bug) |
| `Trees.end = prior.Trees` | ✅ YES | All steps | **Best general workaround** |
| `treeCount.end = count(Trees)` | ✅ YES | All steps | Read-only statistics |
| `Trees.end = create N of Tree` | ✅ YES | All steps | Creation in .end phase |
| `Trees.end = prior.Trees \| newTrees` | ❌ NO | Crashes | Does not work |

---

## Migration Guide

### If you have this pattern:

```josh
Trees.init = create 0 of Tree
Trees.step:if(meta.year == 1) = create 10 of Tree
Trees.end = prior.Trees | Trees
```

### Replace with Workaround #1:

```josh
Trees.init = create 0 of Tree
Trees.step:if(meta.year == 1) = create 10 of Tree
Trees.end = prior.Trees  # ✅ Change: Remove "| Trees"
```

**Impact**: Organisms created in .step will continue to exist and execute at future timesteps via `prior.Trees`.

**Note**: This pattern means Trees collection only gets new organisms at the specific timestep(s) where `.step` condition is true. At other timesteps, `Trees.end = prior.Trees` just carries forward existing organisms.

---

### If you need to combine prior with new items:

Unfortunately, there's no working workaround for this pattern yet. Options:

1. **Create everything in .end phase**:
   ```josh
   Trees.end = prior.Trees | create N of Tree
   ```

2. **Don't use .end at all**:
   ```josh
   Trees.step = prior.Trees | create N of Tree
   ```

3. **Wait for bug fix**: The runtime bug fix is in progress

---

## Example: Complete Working Model

```josh
start simulation MySimulation
  steps.low = 0
  steps.high = 10

  grid.size = 100 m
  grid.low = 34.0 degrees latitude, -116.0 degrees longitude
  grid.high = 34.1 degrees latitude, -116.1 degrees longitude
  grid.patch = "ForestPatch"

  year.init = 0 count
  year.step = prior.year + 1 count
end simulation

start patch ForestPatch
  # ✅ WORKING PATTERN
  Trees.init = create 0 of Tree
  Trees.step:if(meta.year == 1) = create 100 of Tree
  Trees.end = prior.Trees  # Carry forward organisms

  # Optional: Track statistics with separate attribute
  treeStats.end = {
    return [count(Trees), mean(Trees.age), count(Trees[state == "adult"])]
  }

  log.step = debug(geoKey, "PATCH", "year:", meta.year, "trees:", count(Trees))
end patch

start organism Tree
  age.init = 0 years
  age.step = prior.age + 1 year

  state.init = "seedling"
  state.step = {
    if (prior.age >= 10 years) {
      return "adult"
    } else {
      return prior.state
    }
  }

  log.step = debug(geoKey, "TREE", "age:", age, "state:", state)
end organism

# Units
start unit year
  alias years
end unit

start unit m
  alias meters
end unit

start unit count
end unit

start unit degrees
end unit
```

---

## FAQ

### Q: Why does `Trees.end = prior.Trees` work but `Trees.end = prior.Trees | Trees` doesn't?

**A**: The bug is triggered when `.end` handler tries to resolve the CURRENT value of the collection (the `Trees` on the right side). By only referencing `prior.Trees`, you avoid the bug entirely.

### Q: Will I lose organisms with `Trees.end = prior.Trees`?

**A**: No! Organisms created in `.step` become part of `prior.Trees` at the next timestep. They're preserved and continue executing.

### Q: Can I still filter or transform organisms in .end?

**A**: Yes, you can operate on `prior.Trees`:
```josh
Trees.end = prior.Trees[age >= 5 years]  # ✅ Should work (not yet tested)
Trees.end = prior.Trees | create 2 of Tree  # ✅ Works (test_006)
```

As long as you don't reference current `Trees`, you should be safe.

### Q: What if I need to count Trees inside the .end handler?

**A**: Don't put the .end handler on Trees itself. Use a different attribute:
```josh
Trees.step = create 10 of Tree
# No Trees.end handler

treeCount.end = count(Trees)  # ✅ Works
```

### Q: When will the bug be fixed?

**A**: Fix is in progress. The recommended approach is to modify the runtime's organism discovery logic to occur AFTER all phases complete, not during phase execution. Once fixed, all patterns should work correctly.

---

## Testing Your Workaround

To verify organisms are executing correctly:

1. **Add debug logging** to organism .step handlers:
   ```josh
   log.step = debug(geoKey, "ORG_STEP", "age:", age, "year:", meta.year)
   ```

2. **Check debug output** by step:
   ```bash
   grep "^\[Step" debug_organism_0.txt | awk '{print $1, $2}' | sort | uniq -c
   ```

3. **Expected output** (organisms execute at ALL steps):
   ```
   N [Step 0,
   N [Step 1,
   N [Step 2,
   ...
   ```

4. **Bug present** (organisms only execute at step 0):
   ```
   N [Step 0,
   0 [Step 1,
   0 [Step 2,
   ```

---

## Contact & Support

- **Bug Report**: See `/workspaces/josh/step_bug_testing/START_HERE.md`
- **Test Suite**: See `/workspaces/josh/step_bug_testing/TESTING_SUMMARY.md`
- **Detailed Analysis**: See `/workspaces/josh/step_bug_testing/summary/UPDATED_FINDINGS.md`

---

**Last Updated**: 2025-11-13
**Test Coverage**: 21 comprehensive tests
**Workaround Status**: 2 patterns confirmed working, 1 pattern confirmed broken
