# Expected Behavior Specification

This document specifies the exact expected behavior for both test cases.

---

## Test Case 1: broken.josh (FAILURE - Demonstrates Bug)

### Simulation Parameters
- Grid: 1 patch (single cell)
- Steps: 0-4 (5 total timesteps)
- Organisms: 10 SimpleTree instances
- Creation: Step 1 via conditional handler `Trees.step:if(meta.year == 1)`

### Expected Debug Output Pattern

#### Step 0 (year = 0)
```
PATCH_STEP geoKey=... year:0
TREE_COUNT geoKey=... count:0
```
**Counts**:
- PATCH_STEP events: 1
- TREE_COUNT events: 1 (showing count=0)
- ORG_STEP events: 0 (no trees exist yet)

#### Step 1 (year = 1)
```
PATCH_STEP geoKey=... year:1
TREES_CREATED geoKey=... created 10 trees
TREE_COUNT geoKey=... count:10
ORG_STEP geoKey=... age:0 years year:1
ORG_STEP geoKey=... age:0 years year:1
... (8 more ORG_STEP events, one per tree)
```
**Counts**:
- PATCH_STEP events: 1
- TREES_CREATED events: 1
- TREE_COUNT events: 1 (showing count=10)
- ORG_STEP events: 10 (trees execute ONCE)

#### Step 2 (year = 2) - BUG MANIFESTATION
```
PATCH_STEP geoKey=... year:2
TREE_COUNT geoKey=... count:10
```
**Counts**:
- PATCH_STEP events: 1
- TREE_COUNT events: 1 (showing count=10 - trees persist)
- ORG_STEP events: 0 ❌ **BUG**: Trees persist but don't execute

**Bug Evidence**:
- Tree count shows 10 (organisms persist in memory)
- No ORG_STEP events (organisms not discovered/executed)
- Age remains 0 years (no increment since Step 1)

#### Step 3 (year = 3) - Bug Continues
```
PATCH_STEP geoKey=... year:3
TREE_COUNT geoKey=... count:10
```
**Counts**:
- PATCH_STEP events: 1
- TREE_COUNT events: 1 (showing count=10)
- ORG_STEP events: 0 ❌ BUG

#### Step 4 (year = 4) - Bug Continues
```
PATCH_STEP geoKey=... year:4
TREE_COUNT geoKey=... count:10
```
**Counts**:
- PATCH_STEP events: 1
- TREE_COUNT events: 1 (showing count=10)
- ORG_STEP events: 0 ❌ BUG

### Total Event Counts (broken.josh)

| Event Type     | Expected Count | Explanation                              |
|----------------|----------------|------------------------------------------|
| PATCH_STEP     | 5              | 1 per step (0-4)                        |
| TREE_COUNT     | 5              | 1 per step (0-4)                        |
| TREES_CREATED  | 1              | Only at Step 1                          |
| ORG_STEP       | **10**         | Only at Step 1 (10 trees × 1 execution) |

**What it SHOULD be**:
- ORG_STEP: **40** (10 trees × 4 steps, Steps 1-4 after creation)

**Bug Impact**:
- Missing 30 organism executions (75% of expected)
- Organisms frozen after creation
- No state transitions possible
- Agent-based modeling completely broken

### Verification Commands

```bash
# Run broken version
java -jar build/libs/joshsim-fat.jar run test-cases/minimal-repro/broken.josh

# Count events in debug output
grep "ORG_STEP" test-cases/minimal-repro/debug_organism_broken_0.txt | wc -l
# Expected: 10 (should be 40)

grep "PATCH_STEP" test-cases/minimal-repro/debug_patch_broken_0.txt | wc -l
# Expected: 5

grep "TREE_COUNT.*count:10" test-cases/minimal-repro/debug_patch_broken_0.txt | wc -l
# Expected: 4 (Steps 1-4 show 10 trees persist)
```

---

## Test Case 2: working.josh (SUCCESS - Correct Behavior)

### Simulation Parameters
- Grid: 1 patch (single cell)
- Steps: 0-4 (5 total timesteps)
- Organisms: 10 SimpleTree instances
- Creation: Init phase via unconditional handler `Trees.init`

### Expected Debug Output Pattern

#### Step 0 (year = 0)
```
PATCH_STEP geoKey=... year:0
TREE_COUNT geoKey=... count:10
ORG_STEP geoKey=... age:0 years year:0
ORG_STEP geoKey=... age:0 years year:0
... (8 more ORG_STEP events, one per tree)
```
**Counts**:
- PATCH_STEP events: 1
- TREE_COUNT events: 1 (showing count=10)
- ORG_STEP events: 10 ✅ (trees execute)

#### Step 1 (year = 1)
```
PATCH_STEP geoKey=... year:1
TREE_COUNT geoKey=... count:10
ORG_STEP geoKey=... age:1 years year:1
ORG_STEP geoKey=... age:1 years year:1
... (8 more ORG_STEP events, one per tree)
```
**Counts**:
- PATCH_STEP events: 1
- TREE_COUNT events: 1 (showing count=10)
- ORG_STEP events: 10 ✅ (trees execute, ages increment)

#### Step 2 (year = 2)
```
PATCH_STEP geoKey=... year:2
TREE_COUNT geoKey=... count:10
ORG_STEP geoKey=... age:2 years year:2
ORG_STEP geoKey=... age:2 years year:2
... (8 more ORG_STEP events, one per tree)
```
**Counts**:
- PATCH_STEP events: 1
- TREE_COUNT events: 1 (showing count=10)
- ORG_STEP events: 10 ✅ (trees execute, ages increment)

#### Step 3 (year = 3)
```
PATCH_STEP geoKey=... year:3
TREE_COUNT geoKey=... count:10
ORG_STEP geoKey=... age:3 years year:3
ORG_STEP geoKey=... age:3 years year:3
... (8 more ORG_STEP events, one per tree)
```
**Counts**:
- PATCH_STEP events: 1
- TREE_COUNT events: 1 (showing count=10)
- ORG_STEP events: 10 ✅ (trees execute, ages increment)

#### Step 4 (year = 4)
```
PATCH_STEP geoKey=... year:4
TREE_COUNT geoKey=... count:10
ORG_STEP geoKey=... age:4 years year:4
ORG_STEP geoKey=... age:4 years year:4
... (8 more ORG_STEP events, one per tree)
```
**Counts**:
- PATCH_STEP events: 1
- TREE_COUNT events: 1 (showing count=10)
- ORG_STEP events: 10 ✅ (trees execute, ages increment)

### Total Event Counts (working.josh)

| Event Type     | Expected Count | Explanation                              |
|----------------|----------------|------------------------------------------|
| PATCH_STEP     | 5              | 1 per step (0-4)                        |
| TREE_COUNT     | 5              | 1 per step (0-4)                        |
| ORG_STEP       | **50**         | All steps (10 trees × 5 steps)          |

**Success Evidence**:
- ORG_STEP events at ALL steps
- Ages increment correctly (0→1→2→3→4)
- Trees persist AND execute
- Agent-based modeling works correctly

### Verification Commands

```bash
# Run working version
java -jar build/libs/joshsim-fat.jar run test-cases/minimal-repro/working.josh

# Count events in debug output
grep "ORG_STEP" test-cases/minimal-repro/debug_organism_working_0.txt | wc -l
# Expected: 50

grep "PATCH_STEP" test-cases/minimal-repro/debug_patch_working_0.txt | wc -l
# Expected: 5

grep "TREE_COUNT.*count:10" test-cases/minimal-repro/debug_patch_working_0.txt | wc -l
# Expected: 5 (all steps show 10 trees)

# Verify age progression
grep "ORG_STEP.*age:0" test-cases/minimal-repro/debug_organism_working_0.txt | wc -l
# Expected: 10 (Step 0 only)

grep "ORG_STEP.*age:4" test-cases/minimal-repro/debug_organism_working_0.txt | wc -l
# Expected: 10 (Step 4 only)
```

---

## Comparison: Broken vs Working

### Key Differences

| Aspect                    | broken.josh (BUG)           | working.josh (CORRECT)      |
|---------------------------|-----------------------------|-----------------------------|
| **Handler Syntax**        | Conditional `:if(year==1)` | Unconditional `.init`       |
| **Creation Phase**        | `.step` at Step 1          | `.init` at Step 0           |
| **ORG_STEP Events**       | 10 (Step 1 only)           | 50 (all steps)              |
| **Age Progression**       | Stuck at 0 years           | 0→1→2→3→4 years            |
| **Discovery Works**       | Step 1 only                | All steps                   |
| **Organisms Execute**     | Once                       | Every step                  |

### What This Proves

1. **Conditional Syntax Breaks Discovery**:
   - Broken version uses `:if(meta.year == 1)` on collection attribute
   - Working version uses unconditional `.init`
   - ONLY difference is conditional syntax

2. **Organisms Persist But Don't Execute**:
   - Both versions show count=10 after creation
   - Broken version: 0 ORG_STEP events after Step 1
   - Working version: 10 ORG_STEP events every step

3. **Root Cause Confirmed**:
   - Static cache marks `Trees.step` as "has handler"
   - Fast-path NOT taken (cache says handler exists)
   - Conditional evaluates FALSE at Step 2+
   - Handler doesn't execute, attribute UNSET
   - Discovery finds 0 organisms in unset attribute

---

## Success Criteria

### Test PASSES if:

1. **Broken Version Shows Bug**:
   - Total ORG_STEP events: 10 (not 40)
   - ORG_STEP events only at Step 1
   - TREE_COUNT shows 10 at Steps 1-4 (persistence)
   - No ORG_STEP events at Steps 2-4 (no execution)

2. **Working Version Shows Correct Behavior**:
   - Total ORG_STEP events: 50
   - ORG_STEP events at ALL steps (0-4)
   - TREE_COUNT shows 10 at all steps
   - Ages increment correctly (0→4)

3. **Clear Contrast**:
   - 5x difference in ORG_STEP events (10 vs 50)
   - Only difference is conditional handler syntax
   - Proves bug is in conditional handler mechanism

### Test FAILS if:

1. **Broken Version Works**:
   - ORG_STEP events appear at Steps 2-4
   - Total events approach 40-50
   - Bug not reproduced

2. **Working Version Broken**:
   - ORG_STEP events missing at any step
   - Total events less than 50
   - Unconditional pattern also broken

3. **No Clear Difference**:
   - Both versions show similar behavior
   - Root cause not isolated
   - Test case too complex or incorrect

---

## Related Documentation

**Task File**: `/workspaces/josh/.claude/tasks/organism-step-debug-investigation.md`
**Test Plan**: `/workspaces/josh/test-cases/minimal-repro/PLAN.md`
**Runtime Code**:
- Static cache: `EntityBuilder.java:203-220`
- Fast-path: `ShadowingEntity.java:532`
- Discovery: `InnerEntityGetter.java:39-78`
