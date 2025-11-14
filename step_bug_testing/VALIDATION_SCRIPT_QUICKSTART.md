# Validation Script Quick Start Guide

## Overview

The `validate_organism_execution.sh` script analyzes organism execution patterns in debug output files to detect common organism lifecycle bugs.

**Location**: `/workspaces/josh/step_bug_testing/validate_organism_execution.sh`

---

## Quick Usage

### Basic Command

```bash
./validate_organism_execution.sh <debug_organism_0.txt>
```

### Example

```bash
./validate_organism_execution.sh test_005_init_step_end_minimal_none_tiny/debug_organism_0.txt
```

---

## What It Checks

1. ✅ **Multi-Timestep Execution**: Organisms execute at 2+ timesteps (not just creation)
2. ✅ **Event Consistency**: Event counts are consistent across timesteps
3. ❌ **IllegalMonitorStateException**: No lock/unlock mismatches
4. ❌ **CircularDependencyException**: No circular dependencies

---

## Exit Codes

- **0**: All checks pass OR file is empty (expected)
- **1**: Critical bug detected

Safe for CI/CD pipelines!

---

## Common Use Cases

### After Running a Test

```bash
cd test_005_init_step_end_minimal_none_tiny
bash run.sh
cd ..
./validate_organism_execution.sh test_005_init_step_end_minimal_none_tiny/debug_organism_0.txt
```

### Batch Validation

```bash
for test in test_*/debug_organism_0.txt; do
  echo "Checking: $test"
  ./validate_organism_execution.sh "$test"
  echo ""
done
```

### CI/CD Integration

```bash
if ./validate_organism_execution.sh test_005_*/debug_organism_0.txt; then
  echo "✅ Validation passed"
else
  echo "❌ Validation failed"
  exit 1
fi
```

---

## Expected Output

### Normal Execution

```
Analyzing organism execution in: test_005_init_step_end_minimal_none_tiny/debug_organism_0.txt

Event counts by timestep:
  11780 [Step 0,
  11780 [Step 1,
  11780 [Step 2,
  11780 [Step 3,
  11780 [Step 4,

✅ Organisms execute at multiple timesteps
✅ Event counts are consistent across timesteps

✅ All validation checks passed
```

### Varying Event Counts (OK)

```
Event counts by timestep:
  17670 [Step 0,
  11780 [Step 1,
  11780 [Step 2,
  11780 [Step 3,
  11780 [Step 4,

✅ Organisms execute at multiple timesteps
⚠️  Event counts vary across timesteps (may be intentional)

✅ All validation checks passed
```

### Critical Bug Detected

```
Event counts by timestep:
  11780 [Step 0,

❌ CRITICAL BUG: Organisms only execute at 1 timestep!
```

---

## Troubleshooting

### "Error: File not found"

Check the file path:
```bash
ls -la test_005_init_step_end_minimal_none_tiny/debug_organism_0.txt
```

### "Empty debug file"

This is normal if:
- Test doesn't create organisms
- Debug logging is disabled
- Simulation failed before creating organisms

Check the simulation log:
```bash
cat test_005_init_step_end_minimal_none_tiny/simulation.log
```

### "Only 1 timestep detected"

This is the organism lifecycle bug! The script caught it.

Debug steps:
1. Check recent changes to ShadowingEntity.java or SimulationStepper.java
2. Review organism discovery logic
3. Run full regression suite: `./run_regression_suite.sh --full`
4. See `/workspaces/josh/.claude/tasks/ORGANISM_LIFECYCLE_ARCHITECTURE_FIX.md`

---

## Integration with Test Suite

The validation script complements the automated test runner:

**run_regression_suite.sh**:
- Runs tests AND validates inline
- Good for automated testing
- Returns pass/fail for each test

**validate_organism_execution.sh**:
- Analyzes existing output files
- Good for debugging
- Shows detailed event counts

Use both together for comprehensive validation!

---

## Related Documentation

- **COMPONENT_1_3_IMPLEMENTATION.md**: Full implementation details
- **REGRESSION_SUITE_USAGE.md**: Test suite usage guide
- **TEST_MANIFEST.md**: Test descriptions and success criteria
- **INTEGRATION_TESTING_WORKFLOW_PLAN.md**: Original specification (lines 188-251)

---

**Last Updated**: 2025-11-14
**Script Version**: 1.0
**Status**: Production Ready
