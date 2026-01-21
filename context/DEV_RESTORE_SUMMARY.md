# Dev-Restore Branch Summary

**Branch:** `dev-restore`
**Base:** `main` (at commit `a6b96f85` - "Bug/inner entities not updating #319")
**Target:** Merge into `dev`, then `dev` into `main`
**Last Updated:** January 21, 2026

---

## PR Summary Table

| PR | Title | Impact | What Changed |
|----|-------|--------|--------------|
| #333 | Conformance Tests | Additive | 117 test files + JUnit infrastructure |
| #334 | Debug Output System | Additive + Engine | New `debug()` function, grammar changes, machine methods |
| #335 | Timestamp Templating | Additive | `{timestamp}` in `TemplateStringRenderer` |
| #336 | Random Seed | Engine | `--seed` CLI flag, `SharedRandom` utility, serial execution |
| #337 | Test Data Generation | Additive | Gradle tasks for GeoTIFF/NetCDF/JSHD generation |
| #338 | Lambda Scope Fix | Engine | Fix filter expression scope in `SingleThreadEventHandlerMachine` |
| #339 | Meta Synthetic Variables | Engine | `meta.stepCount`, `meta.year` in `SyntheticScope` |
| #340 | Documentation | Docs only | `LanguageSpecification.md`, `llms-full.txt` |
| #341 | Boolean Distribution Count | Engine | Fix `count()` on boolean distributions in `RealizedDistribution` |
| #342 | Built-ins for 'here' | Engine | `here.attr` resolution in `ValueResolver` |
| #343 | Prior Attribute Value Fix | Engine | Prior value consistency in `ShadowingEntity` |
| #344 | Compound Units | Engine | Unit tracking in arithmetic ops (`CompoundUnits`) |
| #345 | Boolean Epsilon | Engine | Float comparison tolerance in `EngineValue` |
| #346 | Map/Distribution Expansion | Engine | Element-wise ops, map strategy in `RealizedDistribution` |
| #347 | Inner Entity Deduplication | Engine | Identity-based dedup in `InnerEntityGetter` |
| #348 | Boolean Distribution Units | Engine | `Units.EMPTY` for boolean results in `RealizedDistribution` |

**Legend:**
- **Additive** = New files/features, no core engine changes
- **Engine** = Changes to core simulation engine files
- **Docs only** = Documentation changes only

---

## Executive Summary

The `dev-restore` branch is a **clean re-implementation** of 4 valuable features from the contaminated `dev` branch. The original `dev` branch contained buggy fix attempts (PRs #311, #314, #315) interleaved with good features, making cherry-picking impossible. Instead, we re-implemented each feature from scratch on `main`.

**Result:** All 4 features are complete and working. The conformance test suite validates the engine with **111 of 117 tests passing** (94.9%). The 6 remaining failures are all design decisions, not bugs.

---

## Why This Branch Exists

### The Problem

The `dev` branch accumulated several issues:
1. **Buggy fix attempts** - PRs #311, #314, #315 tried to fix state evaluation order bugs but introduced new issues
2. **Contaminated files** - `ShadowingEntity.java`, `SimulationStepper.java`, and `SingleThreadEventHandlerMachine.java` had buggy changes interleaved with good features
3. **The actual fix** - PR #319 on `main` contained the correct 10-line fix

### The Solution

Rather than attempting to cherry-pick good commits from the polluted `dev` branch, we:
1. Started fresh from `main` (which had the correct fix)
2. Re-implemented each feature independently
3. Validated with the 117-test conformance suite

---

## PRs in This Branch

### PR #333: Conformance Tests

**Purpose:** Establish a comprehensive test suite for Josh language behavior

**What was changed:**
- **New files only** - No engine changes
- `josh-tests/conformance/` - 117 `.josh` test files
- `src/test/java/org/joshsim/conformance/` - JUnit infrastructure
- `build.gradle` - New `conformanceTest` task

**Key files:**
```
josh-tests/conformance/           # 117 test files
src/test/java/org/joshsim/conformance/
├── JoshConformanceTest.java      # Parameterized test runner
├── TestMetadata.java             # Parses test metadata from .josh files
└── PerformanceTracker.java       # JUnit extension
```

**Validating tests:** All 117 tests in `josh-tests/conformance/`

---

### PR #334: Debug Output System

**Purpose:** Allow simulation authors to output debug messages to configurable destinations

**What was changed:**
- **Grammar:** Added `DEBUG_` token, `expressionList`, `variadicFunctionCall` to `JoshLang.g4`
- **Machine:** Added `writeDebug()`, `debugVariadic()` methods to `SingleThreadEventHandlerMachine.java`
- **New IO files:**
  ```
  src/main/java/org/joshsim/lang/io/
  ├── DebugOutputFacade.java           # Core async writer
  ├── CombinedDebugOutputFacade.java   # Routes by entity type
  ├── DebugOutputFacadeBuilder.java    # Reads debugFiles.* config
  └── StdoutOutputStreamStrategy.java  # stdout:// support
  ```
- **Wiring:** Modified `BridgeGetter`, `FutureBridgeGetter`, `JoshSimFacadeUtil`

**Example usage:**
```josh
start simulation Example
  debugFiles.organism = "file:///tmp/debug.txt"
end simulation

start organism Tree
  dbg.step = debug("age:", current.age, "height:", current.height)
end organism
```

**Output format:**
```
[Step 0, organism @ 6c1bfb54 (48.5, 23.5)] age: 1 height: 0.6611
```

**Validating tests:** Manual testing via `examples/guide/hello_debug_cli.josh`

---

### PR #335: Timestamp Templating

**Purpose:** Add `{timestamp}` placeholder for output path templating

**What was changed:**
- **Modified:** `TemplateStringRenderer.java` - Added timestamp field and processing
- **Modified:** `JvmExportFacadeFactory.java` - Backward compatibility in `getPathLegacy()`
- **Tests:** 8 new tests in `TemplateStringRendererTest.java`

**Example usage:**
```josh
exportFiles.patch = "file:///results/{timestamp}/replicate_{replicate}.csv"
```

**Validating tests:** `TemplateStringRendererTest.java`

---

### PR #336: Random Seed

**Purpose:** Enable deterministic, reproducible simulations via `--seed` CLI flag

**What was changed:**
- **CLI:** Added `--seed` option to `RunCommand.java`
- **New utility:** `SharedRandom.java` - ThreadLocal random access
- **Modified:** `SimulationStepper.java` - Forces serial patch execution when seed is set
- **Modified:** `SingleThreadEventHandlerMachine.java` - Uses shared random

**Example usage:**
```bash
java -jar joshsim-fat.jar run simulation.josh Main --seed 42
```

**Validating tests:** All conformance tests use `--seed 42`

---

### PR #337: Test External Data Generation

**Purpose:** Generate test data files for external data conformance tests

**What was changed:**
- **New files only** - No engine changes
- `build.gradle` - Added `generateRawTestData`, `preprocessTestData` tasks
- `src/test/java/.../TestDataGenerator.java` - GeoTIFF/NetCDF generation

**Validating tests:**
- `test_external_geotiff_checkerboard`, `test_external_geotiff_sequential`
- `test_external_jshd_basic`, `test_external_jshd_large`
- `test_external_netcdf_precipitation`, `test_external_netcdf_temperature`

---

### PR #338: Lambda Scope Fix

**Purpose:** Port fix for filter expression scope issues

**What was changed:**
- **Modified:** `SingleThreadEventHandlerMachine.java` - Fixed scope resolution in filter lambdas

**Validating tests:** `test_collections_filter_basic`, `test_collections_filter_complex`

---

### PR #339: Meta Synthetic Variables

**Purpose:** Enable `meta.stepCount` and `meta.year` access in Josh code

**What was changed:**
- **Modified:** `SyntheticScope.java` - Added `meta` namespace resolution
- **Modified:** `SimulationStepper.java` - Provides step count to scope

**Example usage:**
```josh
age.init
  :if(meta.stepCount == 0 count) = sample external ObservedAges
  :else = 0 years
```

**Validating tests:** `test_temporal_queries_meta*` (5 tests)

---

### PR #340: Documentation Updates

**Purpose:** Document debug output functionality

**What was changed:**
- **Docs only** - No code changes
- `LanguageSpecification.md` - Debug output section
- `llms-full.txt` - Updated for AI assistant context

---

### PR #341: Boolean Distribution Count

**Purpose:** Fix `count()` function on boolean distributions

**What was changed:**
- **Modified:** `RealizedDistribution.java` - Fixed count calculation for boolean values

**Validating tests:** `test_distributions_count`

---

### PR #342: Built-ins for 'here'

**Purpose:** Enable `here.attr` access patterns for patch attributes

**What was changed:**
- **Modified:** `ValueResolver.java` - Added `here` resolution logic
- **Modified:** `SyntheticScope.java` - `here` as alias for containing patch

**Validating tests:** `test_spatial_patches_attributes`

---

### PR #343: Prior Attribute Value Fix

**Purpose:** Fix inconsistent prior attribute values

**What was changed:**
- **Modified:** `ShadowingEntity.java` - Fixed prior value caching and retrieval

**Validating tests:** `test_temporal_prior_basic`, `test_temporal_prior_chained`, `test_temporal_prior_collections`

---

### PR #344: Compound Units

**Purpose:** Support compound units from multiplication/division

**What was changed:**
- **Modified:** `CompoundUnits.java` - Unit tracking through arithmetic
- **Modified:** `EngineValue.java` - Arithmetic operators preserve/combine units

**Validating tests:** `test_scalars_arithmetic_multiplication`, `test_scalars_arithmetic_mixed`

---

### PR #345: Boolean Epsilon

**Purpose:** Add epsilon tolerance for floating-point boolean comparisons

**What was changed:**
- **Modified:** `EngineValue.java` - Added epsilon in `equals()` comparison
- **Modified:** `DecimalScalar.java` - Float comparison with tolerance

**Validating tests:** Most assertion-based tests rely on this fix

---

### PR #346: Map/Distribution Expansion

**Purpose:** Support element-wise operations and improve map strategy

**What was changed:**
- **Modified:** `RealizedDistribution.java` - Element-wise arithmetic
- **Modified:** `SingleThreadEventHandlerMachine.java` - Map function improvements

**Validating tests:** `test_map_linear`, `test_map_quadratic`, `test_map_sigmoid`, `test_distributions_arithmetic_*`

---

### PR #347: Inner Entity Set Deduplication

**Purpose:** Fix duplicate entity processing in filtered collections

**What was changed:**
- **Modified:** `InnerEntityGetter.java` - Identity-based deduplication using `IdentityHashMap`

**Problem:** Patches with both `Trees` and `oldTrees = Trees[condition]` caused `IllegalMonitorStateException` (double-unlock)

**Validating tests:** `test_collections_filter_basic`, `test_collections_filter_complex`

---

### PR #348: Test Tweaks and Boolean Distribution Units

**Purpose:** Fix test bugs and boolean distribution unit handling

**What was changed:**
- **Modified:** `RealizedDistribution.java` - Boolean comparison results use `Units.EMPTY`
- **Modified tests:** Fixed assertions comparing independent random samples

**Validating tests:** `test_stochastic_arithmetic_scalar`, `test_stochastic_sampling_with_replacement`, `test_collections_filter_complex`

---

## Conformance Test Status

**Current:** 111 passing / 6 failing (94.9%)

The 6 failing tests all require **design decisions**, not bug fixes:

| Test | Issue | Design Question |
|------|-------|-----------------|
| `test_collections_chained` | `Pine \| Fir` fails | Support heterogeneous collections? |
| `test_collections_combine` | Same | Same |
| `test_scalars_units_percentage` | `km * ratio` fails | Auto-simplify dimensionless units? |
| `test_temporal_queries_meta_cross_attribute` | `mm * proportion` fails | Same |
| `test_functions_power` | `4 m ^ 0.5` fails | Support fractional exponents with units? |
| `test_functions_chained` | Same | Same |

See `CONFORMANCE_ANALYSIS.md` for detailed analysis.

---

## Files Changed Summary

From `main`:
- **New files:** ~160 (conformance tests + debug infrastructure)
- **Modified files:** ~40 (engine + IO)
- **Lines added:** ~10,000
- **Lines removed:** ~300

---

## Verification Commands

```bash
# Build
./gradlew fatJar

# Run all tests
./gradlew test

# Run conformance tests
./gradlew conformanceTest

# Run single conformance test
java -jar build/libs/joshsim-fat.jar run josh-tests/conformance/<path>.josh <SimName> --seed 42

# Test debug output
java -jar build/libs/joshsim-fat.jar run examples/guide/hello_debug_cli.josh Main
```
