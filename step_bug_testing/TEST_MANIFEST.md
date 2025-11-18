# Test Manifest - Organism Lifecycle Test Suite

**Date**: 2025-11-14
**Purpose**: Document critical regression tests for organism lifecycle behavior
**Status**: Active test suite for preventing regressions

---

## Critical Regression Tests (Must Pass)

### Category: Basic Lifecycle

Tests that verify fundamental organism creation and execution across timesteps.

- **test_005** - `test_005_init_step_end_minimal_none_tiny`
  - Tests: init, step, and end phases with minimal configuration
  - Expected: Organisms execute at all timesteps (0-4)
  - Validates: Basic lifecycle completeness

- **test_012** - `test_012_init_step_end_conditional_minimal_none_tiny`
  - Tests: Conditional organism creation with all three phases
  - Expected: Organisms execute at all timesteps (0-4)
  - Validates: Conditional logic does not break lifecycle

- **test_026** - `test_026_prior_only_end_minimal_none_tiny`
  - Tests: Prior-only pattern in .end handler (workaround pattern)
  - Expected: 11,780 organism events per step
  - Validates: Prior-only pattern works correctly

---

### Category: Multi-Collection

Tests that verify behavior when multiple collections are involved.

- **test_023** - `test_023_two_collections_both_end_minimal_none_tiny`
  - Tests: Two separate collections, both with .end handlers
  - Expected: Organisms execute at all timesteps (0-4)
  - Validates: Multiple collections can coexist with .end handlers

- **test_024** - `test_024_one_with_end_one_without_minimal_none_tiny`
  - Tests: Two collections, only one has .end handler
  - Expected: Organisms execute at all timesteps (0-4)
  - Validates: Mixed .end handler presence doesn't cause issues

---

### Category: Nested Organisms

Tests that verify parent-child organism relationships and attribute inheritance.

- **test_parent_lineage** - Parent organism with lineage tracking
  - Tests: Nested organisms with parent lineage
  - Expected: Organisms execute at all timesteps (0-4)
  - Validates: Parent-child relationships maintain lifecycle

- **test_parent_geokey** - Parent organism with geoKey references
  - Tests: Nested organisms accessing parent geoKey
  - Expected: Organisms execute at all timesteps (0-4)
  - Validates: GeoKey inheritance and access patterns

- **test_parent_synthetic** - Parent organism with synthetic attributes
  - Tests: Nested organisms with computed parent attributes
  - Expected: Organisms execute at all timesteps (0-4)
  - Validates: Synthetic attribute computation in parent context

---

### Category: Edge Cases

Tests that verify correct behavior in unusual or boundary conditions.

- **test_017** - `test_017_filter_in_end_minimal_none_tiny`
  - Tests: Filter operations in .end phase
  - Expected: Organisms execute at all timesteps (0-4)
  - Validates: Collection filtering doesn't break lifecycle

- **test_033** - `test_033_multiple_current_refs`
  - Tests: Multiple references to current collection values
  - Expected: Organisms execute at all timesteps (0-4)
  - Validates: Complex reference patterns work correctly

---

### Category: State Transitions (FAILING - Known Bugs)

Tests that verify organism state machine transitions.

- **test_037** - `test_037_state_transition_bug` ❌ **CURRENTLY FAILING**
  - Tests: Age-based state transitions (seedling → juvenile → adult)
  - Expected: Organisms transition states when age conditions met
  - **Current Status**: BUG CONFIRMED - States never transition
  - **Bug Report**: STATE_TRANSITION_BUG_INVESTIGATION.md
  - **Evidence**: Organisms age correctly (1→7 years) but remain "seedling" forever
  - **Transitions Expected**:
    - Age ≥ 2 years: seedling → juvenile
    - Age ≥ 5 years: juvenile → adult
  - **Validates**: State machine functionality (CURRENTLY BROKEN)

---

## Test Success Criteria

For each test to be considered passing, verify the following:

### 1. Organism Execution at All Timesteps

**Command**:
```bash
grep "^\[Step" debug_organism_0.txt | awk '{print $1, $2}' | sort | uniq -c
```

**Expected Output** (for 10 organisms across 5 steps):
```
N [Step 0,
N [Step 1,
N [Step 2,
N [Step 3,
N [Step 4,
```

**Failure Indicator**:
```
N [Step 0,
0 [Step 1,   # ❌ Organisms stopped executing!
```

---

### 2. Event Counts Consistent Across Timesteps

**What to Check**:
- Count organism lifecycle events (init, step, end) per timestep
- Verify consistency except where intentional variations exist
- Look for unexpected drops or increases

**Expected Behavior**:
- Event counts should be stable across timesteps
- Any variation should match test design (e.g., conditional creation)

---

### 3. No IllegalMonitorStateException Errors

**Command**:
```bash
grep "IllegalMonitorStateException" *.log
```

**Expected**: No matches

**What It Means**: This error indicates organism locking/discovery issues during phase execution

---

### 4. No Circular Dependency Errors

**Command**:
```bash
grep -i "circular" *.log
```

**Expected**: No matches

**What It Means**: Circular dependencies prevent proper attribute resolution

---

### 5. Organism Counts Match Expectations

**What to Verify**:
- Initial organism count matches creation logic
- Organism count persists across timesteps (unless intentionally modified)
- No unexpected organism duplication or loss

**Method**:
```bash
# Count unique organisms per step
grep "^\[Step" debug_organism_0.txt | awk '{print $1, $2}' | sort | uniq -c
```

---

## Expected Event Counts

### Standard 10-Organism Test (5 Timesteps)

For a typical test with 10 organisms executing across 5 timesteps (0-4):

**Event Count Per Step**: 11,780 events

**Breakdown**:
- Step 0: 11,780 events
- Step 1: 11,780 events
- Step 2: 11,780 events
- Step 3: 11,780 events
- Step 4: 11,780 events

**Consistency**: All steps should have identical event counts unless the test design intentionally varies behavior.

---

### Variations and Intentional Differences

**Document any test-specific variations here:**

- **test_020** (create_at_step_0): May have different counts at step 0 vs. subsequent steps
- **Conditional creation tests**: Event counts may vary based on condition evaluation
- **Filter/count tests**: May have reduced organism counts after filtering

**Note**: Any deviation from standard counts should be explicitly documented in the individual test's README or comments.

---

## Test Execution

### Running Individual Tests

```bash
cd /workspaces/josh/step_bug_testing/test_XXX_name
./run.sh
```

### Running Full Test Suite

```bash
cd /workspaces/josh/step_bug_testing
./run_all_tests.sh
```

### Analyzing Results

```bash
# Check organism execution pattern
grep "^\[Step" debug_organism_0.txt | awk '{print $1, $2}' | sort | uniq -c

# Count total events per step
grep "^\[Step" debug_organism_*.txt | awk '{print $2}' | sort | uniq -c

# Check for errors
grep -i "exception\|error" *.log
```

---

## Regression Prevention

This test suite serves as a regression prevention mechanism. Before merging any changes to the organism lifecycle system:

1. Run all critical regression tests
2. Verify all success criteria pass
3. Document any new expected behaviors
4. Update this manifest if test expectations change

---

## Related Documentation

- **IMMEDIATE_ACTION_PLAN.md** - Current bug status and workarounds
- **WORKAROUNDS.md** - Patterns to use/avoid
- **TESTING_SUMMARY.md** - Comprehensive test results and analysis
- **START_HERE.md** - Quick reproduction cases

---

**Last Updated**: 2025-11-14
**Test Count**: 12 tests (11 passing + 1 known bug)
**Coverage**: Basic lifecycle, multi-collection, nested organisms, edge cases, state transitions
