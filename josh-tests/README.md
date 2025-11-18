# Josh Conformance Test Suite

This directory contains Josh language conformance tests that are automatically discovered and executed by JUnit 5.

## Test Organization

```
josh-tests/
├── conformance/               # Language spec compliance tests
│   ├── core/                 # Core language features
│   ├── types/                # Type system tests
│   ├── spatial/              # Spatial query tests
│   ├── temporal/             # Temporal logic tests
│   ├── stochastic/           # Stochastic mechanics tests
│   └── io/                   # I/O and assertions tests
├── regression/               # Bug-specific regression tests
├── integration/              # Full simulation tests
└── README.md                 # This file
```

##  Test Naming Convention

All test files must follow the naming pattern: `test_*.josh`

Example: `test_lifecycle_init_step_end.josh`

## Test Metadata

Each test can include metadata in comments at the top of the file:

```josh
# @category: lifecycle
# @subcategory: events  
# @priority: critical
# @issue: #123
# @description: Entities should execute at all timesteps when using .end handlers

start simulation MyTest
  # ... test content ...
end simulation
```

## Running Tests

### All Conformance Tests
```bash
./gradlew test --tests "org.joshsim.conformance.JoshConformanceTest.runConformanceTest"
```

### Critical Tests Only
```bash
./gradlew test --tests "org.joshsim.conformance.JoshConformanceTest.runCriticalTest"
```

### IDE Integration

Tests appear as individual test cases in IntelliJ IDEA and Eclipse test runners, allowing you to:
- Run/debug single tests
- View test results in the IDE
- Re-run failed tests

## Test Methodology

### Assertion-Based Testing (Recommended)

Tests should use native Josh assertions which fail immediately and produce clear error messages:

```josh
start patch Default
  Trees.init = create 10 count of Tree
  
  # Assert trees were created
  assert.treesCreated.init = count(Trees) == 10 count
  
  # Assert trees persist
  assert.treesPersist.step = count(Trees) == 10 count
end patch
```

### Exit Code Validation

The test runner executes each test via:
```bash
java -jar build/libs/joshsim-fat.jar run <test-file> <simulation-name>
```

- Exit code 0 = test passed (all assertions succeeded)
- Non-zero exit code = test failed (with output captured for debugging)

## Performance Tracking

All tests are tracked for performance via the `PerformanceTracker` JUnit extension:
- Execution times are recorded to `build/test-results/performance-history.csv`
- Performance regressions trigger warnings (but don't fail tests)
- Statistical analysis detects slowdowns >30% or z-score >2.0

## Adding New Tests

1. Create a `.josh` file starting with `test_` in the appropriate category directory
2. Add metadata comments (optional but recommended)
3. Include Josh assertions to validate behavior
4. Run the test to ensure it passes

The test will be automatically discovered and executed by the JUnit test runner.
