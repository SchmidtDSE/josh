# Minimal Reproduction Test Case for Organism Discovery Bug

**Date**: 2025-11-12
**Bug**: Conditional handlers on organism collections prevent discovery after first execution
**Root Cause**: Static handler caching + fast-path optimization + discovery mechanism interaction

---

## Overview

This test case provides the **simplest possible demonstration** of the organism discovery bug. It uses a single organism type, minimal timesteps, and clear debug output to show that organisms execute once (when created via conditional handler) but never again in subsequent steps.

---

## Test Strategy

### Design Principles

1. **Minimal Complexity**:
   - Single organism type (`SimpleTree`)
   - Single patch type (`Default`)
   - 5 timesteps (Steps 0-4)
   - No external data dependencies
   - Minimal organism logic (just age tracking)

2. **Clear Reproduction**:
   - Conditional handler creates organisms at Step 1: `Trees.step:if(meta.year == 1) = create 10 of SimpleTree`
   - Debug logging tracks organism execution: `log.step = debug(geoKey, "ORG_STEP", "age:", age)`
   - Expected: ORG_STEP events at Step 1 only
   - Actual behavior clearly visible in debug output

3. **Control Comparison**:
   - Working version uses unconditional handler: `Trees.init = create 10 of SimpleTree`
   - Identical organism logic and debug logging
   - Expected: ORG_STEP events at ALL steps (0-4)
   - Proves issue is conditional handler syntax, not logic

---

## Test Files

### 1. `broken.josh` - Demonstrates the Bug

**Key Features**:
- Creates organisms using conditional handler: `Trees.step:if(meta.year == 1) = create 10 of SimpleTree`
- Organisms execute `.step` logic at Step 1 (creation time)
- Organisms DO NOT execute `.step` logic at Steps 2-4
- Debug output shows clear pattern

**Expected Debug Output**:
```
Step 0: year=0
  - PATCH_STEP events: 1 (patch executes)
  - ORG_STEP events: 0 (no trees yet)

Step 1: year=1
  - PATCH_STEP events: 1 (patch executes)
  - TREES_CREATED events: 1 (showing count=10)
  - ORG_STEP events: 10 (trees execute ONCE)

Step 2: year=2
  - PATCH_STEP events: 1 (patch executes)
  - ORG_STEP events: 0 ❌ BUG: Trees persist but don't execute

Step 3: year=3
  - PATCH_STEP events: 1 (patch executes)
  - ORG_STEP events: 0 ❌ BUG: Trees persist but don't execute

Step 4: year=4
  - PATCH_STEP events: 1 (patch executes)
  - ORG_STEP events: 0 ❌ BUG: Trees persist but don't execute
```

**Total Expected Events**:
- PATCH_STEP: 5 events (Steps 0-4)
- ORG_STEP: 10 events (all at Step 1)
- Total ORG_STEP should be 50 (10 trees × 5 steps) but is only 10

### 2. `working.josh` - Demonstrates Correct Behavior

**Key Features**:
- Creates organisms using unconditional handler: `Trees.init = create 10 of SimpleTree`
- Organisms execute `.step` logic at ALL steps
- Debug output shows organisms persist AND execute

**Expected Debug Output**:
```
Step 0: year=0
  - PATCH_STEP events: 1 (patch executes)
  - ORG_STEP events: 10 ✅ (trees execute)

Step 1: year=1
  - PATCH_STEP events: 1 (patch executes)
  - ORG_STEP events: 10 ✅ (trees execute)

Step 2: year=2
  - PATCH_STEP events: 1 (patch executes)
  - ORG_STEP events: 10 ✅ (trees execute)

Step 3: year=3
  - PATCH_STEP events: 1 (patch executes)
  - ORG_STEP events: 10 ✅ (trees execute)

Step 4: year=4
  - PATCH_STEP events: 1 (patch executes)
  - ORG_STEP events: 10 ✅ (trees execute)
```

**Total Expected Events**:
- PATCH_STEP: 5 events (Steps 0-4)
- ORG_STEP: 50 events (10 trees × 5 steps)

---

## Simulation Design

### Simulation Parameters

```josh
start simulation MinimalRepro
  # Minimal grid (single patch)
  grid.size = 1000 m
  grid.low = 34.0 degrees latitude, -116.0 degrees longitude
  grid.high = 34.01 degrees latitude, -116.01 degrees longitude

  # 5 timesteps (0-4)
  steps.low = 0 count
  steps.high = 4 count

  # Year counter (starts at 0)
  year.init = 0 count
  year.step = prior.year + 1 count

  # Debug output
  debugFiles.organism = "file:////workspaces/josh/test-cases/minimal-repro/debug_organism_{replicate}.txt"
  debugFiles.patch = "file:////workspaces/josh/test-cases/minimal-repro/debug_patch_{replicate}.txt"
end simulation
```

### Patch Design

**Broken Version**:
```josh
start patch Default
  # CONDITIONAL: Creates trees at Step 1 only
  Trees.step:if(meta.year == 1) = create 10 of SimpleTree

  # Debug: Track tree creation
  log.step = {
    const count = count(Trees)
    return debug(geoKey, "PATCH_STEP", "year:", meta.year, "treeCount:", count)
  }
end patch
```

**Working Version**:
```josh
start patch Default
  # UNCONDITIONAL: Creates trees at init
  Trees.init = create 10 of SimpleTree

  # Debug: Track tree persistence
  log.step = {
    const count = count(Trees)
    return debug(geoKey, "PATCH_STEP", "year:", meta.year, "treeCount:", count)
  }
end patch
```

### Organism Design (Same for Both)

```josh
start organism SimpleTree
  # Track age
  age.init = 0 years
  age.step = prior.age + 1 year

  # Debug: Track organism execution
  log.step = debug(geoKey, "ORG_STEP", "age:", age, "year:", meta.year)
end organism
```

---

## Running the Tests

### Command Line

```bash
# Run broken version (demonstrates bug)
cd /workspaces/josh
java -jar build/libs/joshsim-fat.jar run test-cases/minimal-repro/broken.josh

# Run working version (demonstrates correct behavior)
java -jar build/libs/joshsim-fat.jar run test-cases/minimal-repro/working.josh
```

### Expected Runtime

- Each simulation: < 1 second
- Total test time: < 5 seconds

### Output Files

```
test-cases/minimal-repro/
├── debug_organism_0.txt    (broken version - 10 ORG_STEP events)
├── debug_patch_0.txt       (broken version - 5 PATCH_STEP events)
├── debug_organism_0.txt    (working version - 50 ORG_STEP events)
├── debug_patch_0.txt       (working version - 5 PATCH_STEP events)
```

---

## Verification Criteria

### Success Criteria (Working Version)

1. **Organism Execution**:
   - ORG_STEP events appear at ALL steps (0-4)
   - Total ORG_STEP events: 50 (10 trees × 5 steps)
   - Ages increase from 0 to 4 years

2. **Patch Execution**:
   - PATCH_STEP events appear at ALL steps (0-4)
   - Tree count remains 10 across all steps

### Failure Criteria (Broken Version)

1. **Organism Execution**:
   - ORG_STEP events appear ONLY at Step 1
   - Total ORG_STEP events: 10 (10 trees × 1 step)
   - Ages stuck at 0 years after Step 1

2. **Patch Execution**:
   - PATCH_STEP events appear at ALL steps (0-4)
   - Tree count shows 10 at Steps 1-4 (trees persist)
   - BUT: Trees don't execute their `.step` logic

---

## What This Test Proves

### Key Observations

1. **Organisms Persist**: Tree count shows 10 at Steps 2-4 in broken version
2. **Organisms Don't Execute**: No ORG_STEP events after Step 1 in broken version
3. **Conditional Handler is Culprit**: Only difference is `:if` syntax
4. **Pattern Works Unconditionally**: Working version shows organisms execute every step

### Root Cause Confirmation

This test isolates the THREE interacting bug components:

1. **Static Handler Cache**:
   - `Trees.step` marked as "has handler" at definition time
   - Cache doesn't distinguish between conditional and unconditional

2. **Fast-Path Skip**:
   - Fast-path NOT taken because cache says "has handler"
   - Code goes through full handler execution path

3. **Discovery Failure**:
   - Step 1: Handler executes, attribute SET, organisms discovered ✅
   - Step 2+: Handler doesn't execute (condition false), attribute UNSET, organisms NOT discovered ❌

---

## Next Steps

After confirming this minimal reproduction:

1. **Add Runtime Logging**:
   - Log `EntityBuilder.getAttributesWithoutHandlersBySubstep()` output
   - Log `ShadowingEntity.hasNoHandlers()` calls
   - Log `InnerEntityGetter.getInnerEntities()` discoveries
   - Track conditional handler execution

2. **Propose Fix Options**:
   - Option A: Auto-copy prior value when conditional is false
   - Option B: Mark conditional handlers differently in cache
   - Option C: Check conditional in discovery mechanism
   - Option D: Deprecate conditional syntax on collections

3. **Implement Fix**:
   - Choose best approach based on performance/maintainability
   - Add tests to prevent regression
   - Update documentation

---

## Related Files

**Task File**: `/workspaces/josh/.claude/tasks/organism-step-debug-investigation.md`

**Runtime Code**:
- `/workspaces/josh/src/main/java/org/joshsim/engine/entity/base/EntityBuilder.java:203-220` (static cache)
- `/workspaces/josh/src/main/java/org/joshsim/lang/bridge/ShadowingEntity.java:532` (fast-path)
- `/workspaces/josh/src/main/java/org/joshsim/lang/bridge/InnerEntityGetter.java:39-78` (discovery)
- `/workspaces/josh/src/main/java/org/joshsim/engine/entity/handler/EventHandler.java` (conditional storage)

**Example Simulations**:
- `/workspaces/josh/jotr/historic/two_trees.josh` (working pattern)
- `/workspaces/josh/jotr/historic/stochastic_flowering_updated.josh` (broken pattern)
