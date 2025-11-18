# Regression Suite Usage Guide

## Overview

The `run_regression_suite.sh` script is an automated test runner for the organism lifecycle regression test suite. It validates that organisms execute correctly across multiple timesteps.

## Location

```
/workspaces/josh/step_bug_testing/run_regression_suite.sh
```

## Usage

### Quick Mode (Critical Tests Only)

Runs only the 5 critical regression tests:

```bash
cd /workspaces/josh/step_bug_testing
./run_regression_suite.sh --quick
```

**Critical Tests:**
- test_005_init_step_end_minimal_none_tiny
- test_012_init_step_end_conditional_minimal_none_tiny
- test_023_two_collections_both_end_minimal_none_tiny
- test_024_one_with_end_one_without_minimal_none_tiny
- test_026_prior_only_end_minimal_none_tiny

**Runtime:** ~30-60 seconds

### Full Mode (All Tests)

Runs all available tests (currently 26 tests):

```bash
cd /workspaces/josh/step_bug_testing
./run_regression_suite.sh --full
```

Or simply:

```bash
./run_regression_suite.sh
```

**Runtime:** ~5-10 minutes

## Test Validation Logic

For each test, the script:

1. Runs the simulation: `bash run.sh`
2. Checks for `debug_organism_0.txt` output
3. Validates organism execution at multiple timesteps:
   - Extracts unique step identifiers: `grep "^\[Step" debug_organism_0.txt | awk '{print $1, $2}' | sort -u | wc -l`
   - **PASS**: If step_count >= 2 (organisms execute at multiple timesteps)
   - **FAIL**: If step_count == 1 (organisms only execute once)
   - **SKIP**: If no organism output exists

## Output

### Color-Coded Results

- 🟢 **PASS** (Green): Test succeeded, organisms executed at multiple timesteps
- 🔴 **FAIL** (Red): Test failed, organisms stopped after first timestep
- 🟡 **SKIP** (Yellow): Test skipped, no organism output or test directory missing

### Summary Report

The script provides a clear summary at the end:

```
============================================
TEST RESULTS SUMMARY
============================================
✅ Passed:  5
❌ Failed:  0
⚠️  Skipped: 0
📊 Total:   5
```

### Critical Test Results (Full Mode Only)

When running in full mode, the script also displays results for critical tests:

```
============================================
CRITICAL TEST RESULTS
============================================
✅ PASS: test_005_init_step_end_minimal_none_tiny
✅ PASS: test_012_init_step_end_conditional_minimal_none_tiny
...
```

## Exit Codes

- **Exit 0**: All tests passed
- **Exit 1**: One or more tests failed

## Example Usage in CI/CD

```bash
#!/bin/bash
# Run critical regression tests before deployment

cd /workspaces/josh/step_bug_testing

if ./run_regression_suite.sh --quick; then
  echo "✅ Regression tests passed - safe to deploy"
  exit 0
else
  echo "❌ Regression tests failed - DO NOT deploy"
  exit 1
fi
```

## Test Categories

### Current Categories

1. **CRITICAL_TESTS**: Essential tests that must pass (5 tests)
2. **NESTED_ORGANISM_TESTS**: Future tests for parent-child organisms (placeholder)
3. **ALL_TESTS**: Pattern-based discovery of all test_[0-9]* directories (26 tests)

### Adding New Tests

To add a test to the critical test suite:

1. Edit `run_regression_suite.sh`
2. Add the test name to the `CRITICAL_TESTS` array:

```bash
CRITICAL_TESTS=(
  "test_005_init_step_end_minimal_none_tiny"
  # ... existing tests ...
  "test_NEW_your_test_name"  # Add here
)
```

## Troubleshooting

### Test Directory Not Found

If you see:
```
⚠️  SKIP: test_XXX (directory not found)
```

Verify the test directory exists:
```bash
ls -la /workspaces/josh/step_bug_testing/test_XXX
```

### No Organism Output

If you see:
```
⚠️  SKIP (no organism output)
```

This means `debug_organism_0.txt` was not created. Check:
- Does the test create organisms?
- Did the simulation run successfully?
- Check `simulation.log` for errors

### Execution Error

If you see:
```
❌ FAIL (execution error)
```

Check the test's `simulation.log`:
```bash
cat /workspaces/josh/step_bug_testing/test_XXX/simulation.log
```

## Validation Script

The suite includes a standalone validation script for analyzing organism execution patterns:

```bash
./validate_organism_execution.sh <debug_organism_0.txt>
```

### Features

- Displays event counts by timestep
- Validates organisms execute at multiple timesteps
- Checks for consistent event counts
- Detects critical exceptions (IllegalMonitorStateException, CircularDependencyException)

### Example Usage

```bash
# Validate a specific test output
./validate_organism_execution.sh test_005_init_step_end_minimal_none_tiny/debug_organism_0.txt

# Validate multiple tests
for test in test_*/debug_organism_0.txt; do
  echo "Validating $test"
  ./validate_organism_execution.sh "$test"
done
```

### Exit Codes

- **Exit 0**: All validations pass OR file is empty (expected for some tests)
- **Exit 1**: Critical bug detected (only 1 timestep, exceptions found)

## Related Documentation

- **TEST_MANIFEST.md**: Detailed test descriptions and success criteria
- **START_HERE.md**: Quick reproduction cases
- **TESTING_SUMMARY.md**: Comprehensive test results and analysis
- **WORKAROUNDS.md**: Patterns to use/avoid
- **validate_organism_execution.sh**: Standalone validation script (Component 1.3)

---

**Last Updated**: 2025-11-14
**Components**: 1.2 (Automated Test Runner), 1.3 (Validation Script)
**Status**: Production Ready
