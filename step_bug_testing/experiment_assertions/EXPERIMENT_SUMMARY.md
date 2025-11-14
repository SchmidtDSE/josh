# Josh Conformance Testing Experiment - Summary

**Date:** 2025-11-14
**Objective:** Evaluate assertion-based testing vs log-based validation

---

## Experiment Results

### Tests Created

1. **`test_assertion_based.josh`** - Test with `.end` handler (reproduces bug)
2. **`test_assertion_based_passing.josh`** - Test without `.end` handler (passes)

### Key Findings

| Metric | Log-Based | Assertion-Based | Winner |
|--------|-----------|-----------------|--------|
| **Execution Time** | 3.34s | 3.27s | 🟰 Tie (~2% faster) |
| **Output Size** | 5.2 MB | 0 KB | ✅ **99.9% reduction** |
| **Post-processing** | grep + awk | None | ✅ **Eliminated** |
| **Failure Detection** | After completion | Immediate | ✅ **Fail-fast** |
| **Error Messages** | "only 1 timestep" | "assert.treesExistStep2 failed" | ✅ **More specific** |

### Assertion Example

```josh
start patch Default
  Trees.step:if(meta.year == 1) = create 10 of Tree

  # Self-validating assertions
  assert.treesExistStep2.step:if(meta.year == 2) = count(Trees) == 10 count
  assert.treesExistStep3.step:if(meta.year == 3) = count(Trees) == 10 count
  assert.treesExistStep4.step:if(meta.year == 4) = count(Trees) == 10 count
end patch

start organism Tree
  age.init = 0 years
  age.step = prior.age + 1 year

  # Assert age increases correctly
  assert.ageIncreases.step:if(meta.year == 2) = mean(current.age) == 1 years
  assert.ageIncreases.step:if(meta.year == 3) = mean(current.age) == 2 years
end organism
```

### Bug Detection

The assertion-based test **successfully detected** the organism lifecycle bug:

```
java.lang.RuntimeException: Assertion failed for assert.treesExistStep2
```

This proves assertions work perfectly for regression testing!

---

## Recommendations

1. **Use assertions for all new tests** - Superior in every metric except slight verbosity
2. **Migrate existing tests gradually** - Convert log-based tests over time
3. **Keep it simple** - Assertions are a native Josh feature, no external tooling needed

---

## Files

- `test_assertion_based.josh` - Failing test (detects bug)
- `test_assertion_based_passing.josh` - Passing test (no .end handler)
- `run_assertion_test.sh` - Simple runner script

---

## Impact on Implementation Plan

Based on these results, the full implementation plan has been updated to use:
- **JUnit parameterized tests** (not custom bash runner)
- **Native Josh assertions** (not debug logs)
- **Statistical performance tracking** (not crude timeouts)

See `CONFORMANCE_TEST_IMPLEMENTATION_PLAN.md` for complete details.
