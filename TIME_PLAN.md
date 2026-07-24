# Explicit Simulation Time and JSHD Time Axes — Complete Implementation Plan

## Status and review context

The initial implementation is on PR [#492](https://github.com/SchmidtDSE/josh/pull/492), targeting `dev`.

Already implemented:

- JSHD v2 time-axis serialization with v1 read compatibility.
- Count and ISO range/instant metadata in `TimeAxis`.
- Preprocess CLI declarations for count and ISO axes.
- Initial ISO simulation clock and explicit external lookup syntax.
- Count/ISO examples and preprocessing-to-run integration tests.

This plan supersedes the original implementation details where PR review identified correctness or maintainability issues.

## Goals

Separate three currently conflated concepts:

1. **Engine-loop position:** `meta.stepCount` remains a zero-based loop counter.
2. **Simulation clock:** legacy count mode remains default; ISO mode is opt-in.
3. **External-data coordinate:** JSHD stores declared coordinates and resolves them exactly.

The engine owns clocks, serialized external metadata, exact coordinate lookup, validation, and warnings. Model authors retain ownership of `state`, spin-up/spindown, forcing policy, scenarios, and calendar-like model attributes.

## User-facing contract

### Count mode — default and backward compatible

Without `time.type`, existing `steps.low`/`steps.high` behavior is unchanged.

```bash
java -jar joshsim.jar preprocess sim.josh Main temp.nc temperature K temperature.jshd \
  --time-type count --time-start 2015 --time-unit year --time-count 86
```

```josh
external temperature at index forcingIndex
external temperature at year forcingYear
```

Count metadata stores the coordinate name, unit, start, increment, count, and whether the resource is a range or instant.

### ISO mode — explicit date-only calendar clock

```josh
start simulation Main
  time.type = "ISO"
  time.low = "2026-01-01"
  time.high = "2100-12-01"
  time.interval = "P1M"
end simulation
```

```bash
java -jar joshsim.jar preprocess sim.josh Main rainfall.nc rain mm rainfall.jshd \
  --time-type ISO --time-start 2026-01-01 --time-interval P1M --time-count 900
```

```josh
external rainfall at time meta.time
```

Initial ISO scope is date-only: `java.time.LocalDate` and `java.time.Period` support ISO dates and periods such as `P1Y`, `P1M`, and `P1D`. No dependency is added.

### `meta.year` and `meta.time`

- `meta.time` is the explicit ISO coordinate in ISO mode.
- `meta.year` retains its pre-feature raw-timestep fallback. It must **not** become ISO-derived.
- Normal model-defined `year` attributes continue to resolve before the built-in fallback.
- The built-in `meta.year` fallback emits a once-per-run warning because authors likely intended a declared clock or model-owned calendar attribute.

## JSHD v2 format

Extend `src/main/java/org/joshsim/precompute/JshdUtil.java` with optional v2 temporal metadata:

- type: `COUNT` or `ISO`;
- kind: `RANGE` or `INSTANT`;
- coordinate/source name;
- count: unit, start, increment, count;
- ISO: start date and period/count, or instant date.

Requirements:

- JSHD v1 files load as timeless and retain index access compatibility.
- Timeless files reject metadata query and coordinate access with actionable errors.
- JSHD and JSHDZ carry identical metadata.
- Coordinate lookup is exact: no nearest-neighbor lookup, implicit resampling, repeated-year expansion, or index fallback.

## Required refactors

### Preprocess time-axis construction

In `src/main/java/org/joshsim/command/PreprocessUtil.java`:

1. Keep one mode dispatch point in `buildTimeAxis(...)`.
2. Move count logic into `buildCountTimeAxis(...)`.
3. Move ISO logic into `buildIsoTimeAxis(...)`.
4. Keep shared declaration detection and count validation separate.

Validation rules:

| Axis form | Required options | Invalid combinations |
|---|---|---|
| Count range | `--time-start`, `--time-unit`, `--time-count` | `--time-interval` |
| Count instant | `--time-instant`, `--time-unit` | `--time-start`, `--time-count` |
| ISO range | `--time-start`, `--time-interval`, `--time-count` | `--time-unit`, `--time-increment` |
| ISO instant | `--time-instant` | `--time-start`, `--time-count` |

The declared count must equal generated grid slices. ISO bounds must align exactly to the generated sequence. Source `--time-dim` selects source slice order only; preprocessing does not infer CF calendar semantics, aggregate source values, or translate raw CF time coordinates.

### Amend behavior

Replace current rejection of temporal-metadata amend operations with composition validation:

- same mode;
- same coordinate unit or ISO period;
- compatible axis kind;
- no overlap;
- exact contiguity;
- merged axis count equals merged grid slice count.

Reject invalid composition before writing output and preserve metadata after a valid grid combine.

### Simulation-clock initialization

In `src/main/java/org/joshsim/lang/bridge/MinimalEngineBridge.java`:

1. Keep a single `time.type` dispatch point.
2. Extract `initializeCountClock()` for legacy `steps.low`/`steps.high` behavior.
3. Extract `initializeIsoClock()` for `time.low`/`time.high`/`time.interval` behavior.
4. Remove duplicated constructor branches by using a compact immutable clock configuration or common initialization helper.

ISO mode derives engine steps `0..count-1`. Count mode remains exactly compatible with existing simulations.

## Typed ISO-date boundary

Raw ISO strings must not cross bridge APIs.

1. Change `IsoSimulationClock` to return `LocalDate`.
2. Change `EngineBridge.getCurrentIsoTime()` to return `Optional<LocalDate>`.
3. Change `EngineBridge.getExternalAtIsoTime(...)` to take `LocalDate`.
4. Parse a Josh string scalar into `LocalDate` once in `SingleThreadEventHandlerMachine`.
5. Format `LocalDate` back to a string scalar only for `meta.time` compatibility.
6. Keep `TimeAxis` and JSHD ISO resolution typed as `LocalDate`.

Invalid author strings must fail with a resource-aware message such as:

```text
Invalid ISO date for external rainfall: 2026-13-01
```

## Type-safe count-coordinate reads

The original implementation incorrectly stripped a coordinate expression to its raw number and reattached the unit named in `at <unit>`. Replace this with normal Josh conversion.

Required conversion pipeline:

```text
expression actual unit
  -> `at <unit>` clause unit
  -> persisted JSHD axis unit
  -> exact coordinate index
```

Implementation:

1. In `SingleThreadEventHandlerMachine.pushExternalAtCoordinate(...)`, call `bridge.convert(coordinate, Units.of(clauseUnit))` instead of rebuilding an `EngineValue` from `getAsDecimal()`.
2. In `MinimalEngineBridge.getExternalAtCoordinate(...)`, keep conversion from clause unit to `TimeAxis` unit.
3. Use existing `MapConverter` alias, direct, inverse, and transitive conversion behavior.
4. Fail if either conversion is unavailable.

Required tests:

- alias success: `years -> year -> yr`;
- active conversion that changes numeric magnitude;
- source-expression to clause conversion failure;
- clause to JSHD-axis conversion failure;
- ISO lookup bypasses numeric unit conversion.

## Runtime warning system

### Infrastructure

1. Add `OutputOptions.printWarning(...)` in `src/main/java/org/joshsim/util/OutputOptions.java`.
2. Add a run-scoped `SimulationWarningReporter` with no-op default and deduplication keys.
3. Thread the reporter through `RunUtil`, facade startup, bridge construction, and compatible MCP/browser adapters.
4. Keep low-level constructors usable with a no-op reporter.

Warnings should use normal command output routing and be test-capturable. They should not be emitted through ad hoc `System.err` calls from the engine.

### `meta.year` warning

On actual built-in fallback resolution only, emit once per run:

```text
Warning: meta.year is using raw simulation timestep 42, not a declared calendar.
Define time.type = "ISO" with time.low, time.high, and time.interval,
or use an explicit model-owned calendar attribute.
```

No warning when a model-defined `year` attribute resolves normally.

### Implicit external-index warnings

| Form | Meaning | Warning |
|---|---|---|
| `external X` | current raw simulation timestep used as an index | yes, once per resource |
| legacy `external X at expr` | `expr` interpreted as raw zero-based index | yes, once per resource |
| `external X at index expr` | explicit raw index | no |
| `external X at unit expr` | typed count coordinate | no |
| `external X at time expr` | typed ISO date coordinate | no |

When metadata exists, include mode, unit/period, available range, coordinate count, and a safe suggested replacement:

```text
Warning: external precipitation at forcingStep is interpreted as a zero-based JSHD index.
Resource precipitation declares count axis time: 2015 year through 2100 year, 86 coordinates.
Prefer external precipitation at year forcingYear or external precipitation at index forcingStep.
```

For JSHD v1/timeless data, state that no declared time axis is available and recommend explicit `at index` access.

## Explicit external syntax and metadata queries

Maintain these forms in `src/main/antlr/org/joshsim/lang/antlr/JoshLang.g4`:

```josh
external precipitation at index 10
external precipitation at year forcingYear
external rainfall at time meta.time
first year of external precipitation
last year of external precipitation
length of external precipitation
unit of external precipitation
```

Metadata queries fail clearly for legacy/timeless JSHD. Exact typed reads fail with requested coordinate, resource name, mode, and available span.

## Test matrix

### Serialization

- v1 JSHD timeless load compatibility.
- v2 count range, count instant, ISO range, ISO instant round trips.
- JSHDZ parity.
- malformed metadata rejection.

### Preprocessing

- valid count and ISO command paths;
- flag incompatibilities;
- slice-count mismatch;
- malformed date/period;
- non-aligned ISO endpoint;
- instant with incorrect slice count;
- valid and invalid amend-axis composition.

### Simulation and external reads

- count mode remains unchanged;
- ISO yearly, monthly, daily, and instant clocks;
- month-end behavior;
- `meta.time` progression;
- unchanged `meta.year` fallback in count and ISO modes;
- model-owned `year` override;
- exact count and ISO hits/misses;
- JSHD v1 coordinate-read failure;
- alias and conversion behavior.

### Warnings

- `meta.year` fallback warning once only;
- no warning for model-defined `year`;
- bare external warning once per resource;
- legacy `at expr` warning once per resource;
- metadata-rich and timeless warning messages;
- no warning for explicit index, count-coordinate, or ISO-coordinate reads.

### End-to-end

Extend `src/test/java/org/joshsim/command/ExternalTimeAxisIntegrationTest.java` to preprocess fixture data, run count and ISO simulations, assert exported values, and capture warnings.

Run `./gradlew test` and the existing TeaVM/WebAssembly-safe build path after every implementation phase.

## Documentation and examples

Update:

- `llms-full.txt`;
- `README.md`;
- `TIME.md`;
- `examples/features/external_time_axis_count.josh`;
- `examples/features/external_time_axis_iso.josh`.

Document default count compatibility, explicit ISO scope, exact-match behavior, JSHD v1 limitations, warning migration guidance, unit-alias requirements, prepared-data/CF boundary, and spin-up patterns.

## Non-goals for the first release

- Engine-owned eras, spin-up, spindown, or forcing policies.
- Automatic CF/UDUNITS parsing, calendar conversion, aggregation, or source time-dimension inference.
- Time-of-day, time zones, non-Gregorian calendars, and general duration values.
- Implicit resampling, nearest-neighbor lookup, or automatic annual-to-monthly repetition.
- Removal of legacy implicit-index syntax; it remains supported with warnings.

## Delivery sequence

1. Refactor preprocessing and bridge branches.
2. Establish typed `LocalDate` bridge boundary.
3. Restore `meta.year` semantics.
4. Correct count-coordinate unit conversion.
5. Add warning infrastructure and warning behavior.
6. Implement amend composition.
7. Expand tests and update docs/examples.
8. Push review-sized commits to PR #492 and reply to review threads with tests.
