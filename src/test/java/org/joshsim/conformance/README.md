# Josh Conformance Test Infrastructure

This directory contains the JUnit infrastructure for running Josh language conformance tests.

## Test Discovery

The `JoshConformanceTest` class automatically discovers and runs all `.josh` test files located in `josh-tests/conformance/`. Test files are identified by:

1. Being located under the `josh-tests/` directory
2. Having a filename starting with `test_`
3. Having a `.josh` extension

Each test file must contain a `start simulation` declaration. The simulation name is extracted automatically and used when invoking the test.

## Running Tests

```bash
# Run all conformance tests
./gradlew conformanceTest

# Run only critical-priority tests
./gradlew conformanceTest --tests "*runCriticalTest*"

# Run a specific test
./gradlew conformanceTest --tests "*test_collections_filter_basic*"
```

Tests are executed by invoking the Josh jar with `--seed 42` for reproducibility.

## Test Metadata Format

Test files should include metadata in comment headers at the top of the file:

```josh
# @category: types
# @subcategory: conversions
# @priority: critical
# @issue: #123
# @description: Test implicit unit conversions in arithmetic
```

### Metadata Tags

| Tag | Required | Description | Values |
|-----|----------|-------------|--------|
| `@category` | No | Top-level test category | `control`, `core`, `entities`, `io`, `spatial`, `stochastic`, `temporal`, `types` |
| `@subcategory` | No | Sub-category within the category | Varies by category |
| `@priority` | No | Test importance for filtering | `critical`, `high`, `medium`, `low` |
| `@issue` | No | Related GitHub issue | e.g., `#123` |
| `@description` | No | Human-readable test description | Free text |

The `@priority: critical` tag is used by `discoverCriticalTests()` to identify tests that should pass for basic functionality.

## Test File Structure

Test files are self-validating Josh simulations that use `assert.*` attributes:

```josh
# @category: core
# @subcategory: lifecycle
# @priority: critical
# @description: Test basic organism creation and aging

start simulation TestOrganismLifecycle
  grid.size = 10 m
  grid.low = 0 degrees latitude, 0 degrees longitude
  grid.high = 1 degrees latitude, 1 degrees longitude
  grid.patch = "Default"
  steps.low = 0 count
  steps.high = 5 count
end simulation

start patch Default
  Tree.init = create 1 count of Tree
end patch

start organism Tree
  age.init = 0 year
  age.step = prior.age + 1 year

  # Assertions validate expected behavior
  assert.ageStep3.step:if(meta.stepCount == 3 count) = current.age == 3 years
end organism

start unit year
  alias years
end unit
```

A test passes if the simulation completes with exit code 0. Assertion failures cause non-zero exit codes.

## Classes

| Class | Purpose |
|-------|---------|
| `JoshConformanceTest` | JUnit test runner with parameterized test discovery |
| `TestMetadata` | Parses metadata tags from test file headers |
| `TestDataGenerator` | Generates GeoTIFF/NetCDF/JSHD test data files |
| `PerformanceTracker` | JUnit extension for tracking test execution times |
