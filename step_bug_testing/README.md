# Exhaustive Step Bug Testing

This directory contains a comprehensive test suite that identified and isolated the organism step execution bug in joshsim.

## Quick Start

### View Results
```bash
cat step_bug_testing/summary/quick_summary.txt
cat step_bug_testing/summary/FINAL_REPORT.md
```

### Reproduce the Bug (Minimal Test Case)

```bash
cd step_bug_testing/test_011_init_step_end_no_prior_minimal_none_tiny
./run.sh
```

Check organism execution:
```bash
grep "^\[Step" debug_organism_0.txt | awk '{print $1, $2}' | sort | uniq -c
```

**Expected**: Events at Steps 0, 1, 2, 3, 4
**Actual**: Events only at Step 0 (BUG!)

### Compare with Working Test

```bash
cd ../test_001_only_step_minimal_none_tiny
./run.sh
grep "^\[Step" debug_organism_0.txt | awk '{print $1, $2}' | sort | uniq -c
```

**Result**: Events at all steps 0-4 (WORKS CORRECTLY)

## Bug Summary

**Root Cause**: ANY `.end` handler on a patch collection causes organisms created in the `.step` phase to stop executing after their creation step.

**Trigger Conditions**:
1. Organisms created in `.step` phase of a collection
2. Same collection has a `.end` handler (any content)

**Does NOT matter**:
- Whether `.init` handler exists
- Whether `prior` is used in `.end`
- Whether `.end` is conditional
- Organism complexity level

## Test Structure

- **test_001-007**: Phase 1 - Pattern testing
- **test_008, 010-013**: Phase 2 - Focused bug isolation
- **summary/**: Analysis results and reports
- **common/**: Shared scripts and config

## Results

- **Total**: 12 tests
- **Pass**: 6 tests (no .end handler OR organisms created in .end)
- **Fail**: 5 tests (organisms created in .step, passed through .end)
- **Crash**: 1 test (configuration error, not the bug)

## Files

Each test directory contains:
- `test.josh` - Complete simulation file
- `run.sh` - Execution script
- `simulation.log` - Execution output
- `debug_organism_0.txt` - Organism event log (CHECK THIS for bug)
- `debug_patch_0.txt` - Patch event log

## Key Tests

- **test_005**: First identified the bug (init+step+end pattern)
- **test_011**: Minimal reproduction (.end without prior)
- **test_001**: Baseline working case (no .end handler)
- **test_006**: Shows organisms created IN .end work correctly

## For Developers

See `summary/FINAL_REPORT.md` for:
- Full technical analysis
- Root cause hypothesis
- Recommended fix approaches
- Code areas to investigate

The likely issue is in organism discovery logic when collections are combined in `.end` phase handlers.
