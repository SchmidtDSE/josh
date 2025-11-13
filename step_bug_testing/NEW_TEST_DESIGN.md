# New Test Cases Based on Overnight Agent Discoveries

**Date**: 2025-11-13
**Purpose**: Expand test coverage based on refined understanding of the bug

## Key Insights from Overnight Run

1. **ANY `.end` handler causes the bug** - doesn't matter what it contains
2. **Doesn't require `prior` reference** - test_011 showed `Trees.end = Trees` still breaks
3. **Conditional `.end` still breaks** - test_012 showed conditions don't help
4. **Creation location matters** - test_006 works because organisms created IN `.end`, not passed through

## New Test Hypotheses to Validate

### Category A: Alternative Collection Patterns (Test if self-reference is the issue)

#### Test 013: Separate Collection for New Items
**Hypothesis**: Using a separate attribute for newly created organisms avoids the self-reference ambiguity

**Pattern**:
```josh
newTrees.step:if(meta.year == 1) = create 10 of Tree
Trees.end = prior.Trees | newTrees
```

**Expected**: ✅ Should PASS (no self-reference in .end)
**Rationale**: If the bug is about `Trees` referencing itself, this should work

---

#### Test 014: Two-Step Collection Pattern
**Hypothesis**: Explicit intermediate collection makes the phase boundary clear

**Pattern**:
```josh
Trees.step:if(meta.year == 1) = create 10 of Tree
stepResult.end = Trees
allTrees.end = prior.allTrees | stepResult
```

**Expected**: ❓ Unknown
**Rationale**: Tests if we can "extract" the .step result explicitly

---

### Category B: Different Phase Combinations (Test if .end specifically is the problem)

#### Test 015: Start Handler Instead of End
**Hypothesis**: The bug is specific to `.end`, not other phases

**Pattern**:
```josh
Trees.step:if(meta.year == 1) = create 10 of Tree
Trees.start = prior.Trees
```

**Expected**: ✅ Should PASS (test_004 showed .start works)
**Rationale**: Confirms .end is specifically the problem

---

#### Test 016: Only Start and End (No Step Creation)
**Hypothesis**: Creating in `.start` and processing in `.end` works

**Pattern**:
```josh
Trees.start:if(meta.year == 1) = create 10 of Tree
Trees.end = prior.Trees | Trees
```

**Expected**: ❓ Unknown
**Rationale**: Tests if the bug is .step->.end specifically or any creation->.end

---

### Category C: Collection Operation Variations (Test what .end operations break)

#### Test 017: Filtering in End Instead of Combining
**Hypothesis**: The bug is about the union operator `|`, not `.end` in general

**Pattern**:
```josh
Trees.init = create 0 of Tree
Trees.step:if(meta.year == 1) = create 10 of Tree
Trees.end = Trees[age >= 0 years]
```

**Expected**: ❌ Likely FAIL (any .end handler seems to break)
**Rationale**: Tests if filtering avoids the bug

---

#### Test 018: Count Only in End
**Hypothesis**: Read-only access in `.end` doesn't break organism tracking

**Pattern**:
```josh
Trees.init = create 0 of Tree
Trees.step:if(meta.year == 1) = create 10 of Tree
treeCount.end = count(Trees)
```

**Expected**: ✅ Should PASS (different collection in .end)
**Rationale**: Tests if ANY collection with .end breaks, or just Trees

---

#### Test 019: End Handler on Different Collection
**Hypothesis**: `.end` handler on OTHER collection doesn't affect Trees

**Pattern**:
```josh
Trees.step:if(meta.year == 1) = create 10 of Tree
OtherStuff.end = Trees  # Different collection references Trees
```

**Expected**: ✅ Should PASS
**Rationale**: Tests if the bug is about having .end on the SAME collection

---

### Category D: Timing and Frequency Variations

#### Test 020: Create at Step 0 (year == 0)
**Hypothesis**: Bug appears regardless of which step organisms are created

**Pattern**:
```josh
Trees.init = create 0 of Tree
Trees.step:if(meta.year == 0) = create 10 of Tree
Trees.end = prior.Trees | Trees
```

**Expected**: ❌ Should FAIL (but different execution pattern)
**Rationale**: Validates bug isn't specific to step 1

---

#### Test 021: Unconditional Step Creation
**Hypothesis**: Creating every step shows if bug affects ongoing creation

**Pattern**:
```josh
Trees.init = create 0 of Tree
Trees.step = create 1 of Tree
Trees.end = prior.Trees | Trees
```

**Expected**: ❌ Should FAIL
**Rationale**: Shows if bug affects incremental organism creation

---

#### Test 022: Multiple Creation Events
**Hypothesis**: Creating in multiple steps tests cumulative behavior

**Pattern**:
```josh
Trees.init = create 0 of Tree
Trees.step:if(meta.year == 1 or meta.year == 3) = create 5 of Tree
Trees.end = prior.Trees | Trees
```

**Expected**: ❌ Should FAIL (both batches likely affected)
**Rationale**: Tests if second batch of organisms also breaks

---

### Category E: Multiple Collections (Test scope of bug)

#### Test 023: Two Collections Both with Step+End
**Hypothesis**: Bug affects all collections independently

**Pattern**:
```josh
Trees.step:if(meta.year == 1) = create 10 of Tree
Trees.end = prior.Trees | Trees

Shrubs.step:if(meta.year == 1) = create 5 of Shrub
Shrubs.end = prior.Shrubs | Shrubs
```

**Expected**: ❌ Both should FAIL
**Rationale**: Confirms bug is per-collection, not global

---

#### Test 024: One Collection with End, One Without
**Hypothesis**: Collections without `.end` work even if others have `.end`

**Pattern**:
```josh
Trees.step:if(meta.year == 1) = create 10 of Tree
Trees.end = prior.Trees | Trees

Shrubs.step:if(meta.year == 1) = create 5 of Shrub
# No Shrubs.end
```

**Expected**: Trees ❌ FAIL, Shrubs ✅ PASS
**Rationale**: Confirms bug is isolated to collections with .end

---

### Category F: Edge Cases and Boundary Conditions

#### Test 025: Empty End Handler (Pass-through)
**Hypothesis**: Even empty/trivial .end handler triggers bug

**Pattern**:
```josh
Trees.step:if(meta.year == 1) = create 10 of Tree
Trees.end = Trees  # Simple pass-through
```

**Expected**: ❌ Should FAIL (test_011 confirmed this)
**Rationale**: Validates test_011 finding

---

#### Test 026: Prior-Only End Handler
**Hypothesis**: .end that only references prior doesn't break

**Pattern**:
```josh
Trees.step:if(meta.year == 1) = create 10 of Tree
Trees.end = prior.Trees  # Only prior, not Trees
```

**Expected**: ❓ Unknown (might work, might lose new organisms)
**Rationale**: Tests if combining is required or if any .end breaks

---

#### Test 027: End with Create (Like test_006)
**Hypothesis**: Creating in .end works (already confirmed by test_006)

**Pattern**:
```josh
Trees.init = create 0 of Tree
Trees.end:if(meta.year == 1) = create 10 of Tree
```

**Expected**: ✅ Should PASS (test_006 pattern)
**Rationale**: Reconfirms that creation IN .end works

---

## Test Priority

**High Priority** (Most likely to reveal new insights):
1. Test 013 - Separate collection (tests self-reference theory)
2. Test 017 - Filtering instead of combining
3. Test 018 - Count only (read vs write)
4. Test 019 - End on different collection
5. Test 024 - One with end, one without

**Medium Priority** (Validates understanding):
6. Test 020 - Create at step 0
7. Test 021 - Unconditional creation
8. Test 023 - Two collections both affected
9. Test 026 - Prior-only in end

**Low Priority** (Edge cases):
10. Test 014 - Two-step pattern
11. Test 016 - Start and end only
12. Test 022 - Multiple creation events
13. Test 025 - Already validated by test_011
14. Test 027 - Already validated by test_006

## Implementation Plan

1. **Generate test files** using same structure as existing tests
2. **Run in batches** of 5 to avoid overwhelming execution
3. **Analyze results** after each batch
4. **Update hypothesis** based on findings
5. **Document** new insights in updated FINAL_REPORT

## Expected Outcomes

**If Test 013 PASSES**: Bug is about self-reference ambiguity in phase resolution
**If Test 013 FAILS**: Bug is deeper - any .end handler affects organism tracking

**If Test 017 PASSES**: Bug is specific to collection combination operator
**If Test 017 FAILS**: Bug is any .end operation on collections with organisms

**If Test 018 PASSES**: Bug is about write operations in .end
**If Test 018 FAILS**: Bug is about organism tracking through ANY .end handler

**If Test 019 PASSES**: Bug is isolated to collection that has .end
**If Test 019 FAILS**: Bug affects organism tracking globally when ANY .end exists

## Success Criteria

- Generate at least 10 new test cases (high + medium priority)
- Execute all tests successfully
- Identify at least one pattern that avoids the bug (workaround)
- Refine the technical hypothesis about root cause
- Update FINAL_REPORT with new findings
