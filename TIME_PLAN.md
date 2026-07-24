# Time Axis and External Metadata Implementation Plan

## Goal

Separate three concepts that are presently coupled through `meta.stepCount`:

1. engine-loop position;
2. simulation calendar coordinate; and
3. external-data slice selection.

The engine will provide an explicit, validated clock and external temporal metadata. Model authors retain ownership of spin-up, spindown, state transitions, scenarios, and forcing policies.

## Decisions

### Two explicit time modes

| Mode | Default | Purpose | Clock behavior |
|---|---:|---|---|
| `count` | Yes | Preserve existing simulations and support model-native numeric coordinates | `meta.stepCount` remains the loop/index coordinate |
| `ISO` | No | Support annual, monthly, daily, and dated-event series without ordinal month counters | Engine advances exact ISO calendar coordinates |

No `time.type` declaration means `count` mode. This is the compatibility guarantee for existing simulations and JSHD v1 files.

### Boundary of responsibility

- **Preprocessing author:** prepares/aggregates raw sources, selects source slices, and declares one canonical output axis.
- **JSHD/JSHDZ:** persists the declared axis with the output grid.
- **Engine:** validates the selected mode, advances the ISO clock when enabled, resolves metadata, and rejects unavailable external coordinates.
- **Model author:** defines `state`, spin-up/spindown behavior, forcing selection, and external-read policy.

Preprocessing does not automatically interpret CF calendars, convert raw CF coordinates to a Josh calendar, aggregate slices, or infer a source time dimension. A source dimension may be selected with `--time-dim` only to control source slice order.

## Author-facing contracts

### Count mode

Count mode uses numeric coordinates with a declared Josh unit. The unit is part of the JSHD axis contract, not a special language feature.

```josh
start simulation Main
  steps.low = 0 count
  steps.high = 85 count
end simulation

scenarioStart.constant = first year of external precipitation
scenarioEnd.constant = last year of external precipitation
precip.step = external precipitation at year meta.forcingYear
```

```bash
java -jar joshsim.jar preprocess sim.josh Main futureTemp.nc temp celsius futureTemp.jshdz \
  --time-type count --time-start 2015 --time-unit year --time-count 86
```

An external coordinate request is converted through Josh's ordinary unit system, then must match a stored coordinate exactly. Aliases such as `yr` and `years` work only when the simulation unit definitions make them aliases/conversions of the declared axis unit; source dimension names never participate in matching.

### ISO mode

ISO mode is enabled explicitly in the simulation and preprocessing declarations.

```josh
start simulation Main
  time.type = "ISO"
  time.low = "2026-01-01"
  time.high = "2100-12-01"
  time.interval = "P1M"
end simulation

rain.step = external rainfall at time meta.time
forcingYear.step = meta.year
```

```bash
java -jar joshsim.jar preprocess sim.josh Main rainfall.nc rain mm rainfall.jshdz \
  --time-type ISO --time-start 2026-01-01 --time-interval P1M --time-count 900
```

For a dated snapshot or event:

```bash
java -jar joshsim.jar preprocess sim.josh Main fire.tiff rbr rbr fire.jshdz \
  --time-type ISO --time-instant 2020-09-01
```

ISO mode uses date-only ISO-8601 values for the first release:

- coordinates: `YYYY-MM-DD`;
- intervals: ISO periods accepted by `java.time.Period`, such as `P1Y`, `P1M`, `P7D`, or `P1D`;
- `meta.time`: current ISO coordinate;
- `meta.year`: year extracted from `meta.time`, returned as a Josh `year` value.

`time.low` and `time.high` alone are insufficient for monthly data; they must be full ISO dates so every simulated coordinate is unambiguous.

## External read syntax and compatibility

New explicit forms:

```josh
external precipitation at index 10
external precipitation at year 2050
external rainfall at time meta.time
first year of external precipitation
last year of external precipitation
length of external precipitation
```

Semantics:

- `at index N`: explicit zero-based record position; works for every JSHD.
- `at <unit> value`: count-mode coordinate lookup, converted to the JSHD declared axis unit and matched exactly.
- `at time value`: ISO-mode coordinate lookup, matched exactly against a stored ISO date.
- `first`, `last`, and `length`: JSHD temporal metadata queries; unavailable for legacy/timeless JSHD resources.

Compatibility forms remain valid but warn once per source location:

```josh
external precipitation
external precipitation at 10
```

They are interpreted as index reads. The warning states the interpretation and points authors to `at index`, `at <unit>`, or `at time`.

Every failed coordinate lookup is a hard error that includes the resource name, requested value, selected mode, and available index/coordinate range. There is no nearest-neighbor or silent fallback behavior.

## JSHD v2 temporal metadata

Extend the existing v1 binary format in `src/main/java/org/joshsim/precompute/JshdUtil.java` to version 2. The v2 header carries an optional temporal-axis block before grid values.

The metadata model contains:

- presence flag;
- `timeType`: `COUNT` or `ISO`;
- `axisKind`: `RANGE` or `INSTANT`;
- coordinate/dimension name for provenance;
- for count mode: unit, numeric start, numeric increment, count;
- for ISO mode: ISO start, ISO interval, count, or ISO instant;
- enough canonical coordinate information to validate exact lookup and amend operations.

A v1 JSHD deserializes with no temporal metadata. It preserves legacy index behavior and produces a clear error for metadata-query or coordinate-based reads.

The same serialized v2 payload must work through uncompressed JSHD and compressed JSHDZ paths.

## Implementation sequence

1. **Metadata domain model**
   - Add immutable time-axis types near `src/main/java/org/joshsim/precompute/DoublePrecomputedGrid.java`.
   - Validate count and ISO invariants independently.
   - Use `java.time.LocalDate`, `java.time.Period`, and `java.time.format.DateTimeFormatter`; add no library dependency.

2. **Format and serialization**
   - Bump serialization in `src/main/java/org/joshsim/precompute/JshdUtil.java` to v2.
   - Retain complete v1 deserialization.
   - Thread metadata through `src/main/java/org/joshsim/precompute/BinaryGridSerializationStrategy.java` and the JSHDZ serialization path.

3. **Preprocessing declaration**
   - Extend `src/main/java/org/joshsim/command/PreprocessCommand.java` and `src/main/java/org/joshsim/command/PreprocessUtil.java` with `--time-type`.
   - Count mode: accept `--time-start`, `--time-unit`, `--time-count`, and optional numeric increment.
   - ISO mode: accept `--time-start`, `--time-interval`, `--time-count`, or mutually exclusive `--time-instant`.
   - Validate that the declared count equals output slices; validate ISO sequence bounds and interval progression.
   - In amend mode, require mode-compatible, contiguous, non-overlapping axes.

4. **Simulation ISO clock**
   - Interpret `time.type`, `time.low`, `time.high`, and `time.interval` in the simulation stanza.
   - Update bridge/clock infrastructure to derive step count, `meta.time`, and `meta.year` in ISO mode.
   - Preserve `steps.low`/`steps.high`, `meta.stepCount`, and all count-mode behavior unchanged.
   - Keep the ISO implementation isolated and verify the used `java.time` subset under the existing TeaVM build; provide a narrow compatible implementation only if needed.

5. **Runtime external metadata and lookup**
   - Extend `src/main/java/org/joshsim/lang/bridge/ExternalResourceGetter.java`, `src/main/java/org/joshsim/lang/bridge/EngineBridge.java`, and their implementations to cache resource plus metadata.
   - Add exact count-coordinate and ISO-date-to-index resolution.
   - Surface stored metadata through `src/main/java/org/joshsim/geo/external/readers/JshdExternalDataReader.java` and inspection tooling.

6. **Grammar, interpreter, and diagnostics**
   - Extend `src/main/antlr/org/joshsim/lang/antlr/JoshLang.g4` for explicit index, unit-coordinate, ISO-time, and metadata-query expressions.
   - Implement actions in `src/main/java/org/joshsim/lang/interpret/visitor/delegates/JoshExternalVisitor.java`.
   - Add runtime operations in `src/main/java/org/joshsim/lang/interpret/machine/SingleThreadEventHandlerMachine.java`.
   - Emit migration warnings for bare and legacy implicit-index reads without changing their semantics.

7. **Tests**
   - JSHD v1 read compatibility and v2 count/ISO/instant round trips.
   - JSHDZ parity.
   - Count- and ISO-mode preprocessing validation, including malformed dates/periods, count mismatch, and amend discontinuity.
   - ISO annual, monthly, daily, and instant clocks; inclusive endpoint behavior; `meta.time` and `meta.year` values.
   - Exact coordinate success and unavailable-coordinate failures.
   - Parser/visitor coverage for new syntax and warnings.
   - Preserve existing behavior covered by `src/test/java/org/joshsim/command/ExternalTimestepAlignmentIntegrationTest.java`.
   - Run tests on JVM and the existing WebAssembly-safe path.

8. **Documentation**
   - Update `llms-full.txt`, `README.md`, and `TIME.md` with time modes, preprocessing options, JSHD compatibility guarantees, warnings, and spin-up examples.
   - Document the intentional prepared-data boundary: raw CF calendar conversion and aggregation occur before preprocessing, not during simulation runtime.

## Non-goals for the first release

- Engine-owned eras, spin-up, spindown, or forcing policies.
- Automatic CF/UDUNITS parsing, calendar conversion, aggregation, or time-dimension inference.
- Time-of-day, time zones, ISO durations, or non-Gregorian calendars in ISO mode.
- Implicit resampling, nearest-neighbor time lookup, or repeating annual values across monthly steps.
- Removal of existing bare or `at <expression>` external reads.
