# joshpy Handoff: `preprocess` Contract

The contract joshpy must satisfy when driving `java -jar joshsim-fat.jar preprocess`. Covers the
new `--no-time-dim` flag and every time-related behavior of preprocessing.

Companion docs: [TIME.md](TIME.md) for the engine-side read semantics (`external X at time ...`),
[TIME_PLAN.md](TIME_PLAN.md) for the design rationale and explicit non-goals.

---

## 1. Invocation shape

```
preprocess <script.josh> <simulation> <dataFile> <variable> <units> <output.jshd|.jshdz> [options]
```

All six positionals are required and order-sensitive.

| Positional | Meaning | joshpy responsibility |
|---|---|---|
| `script.josh` | Josh script defining the grid and step range | Must parse, and must declare the named simulation |
| `simulation` | Simulation name within the script | Must exist or the run fails |
| `dataFile` | Source raster/table (`.nc`, `.tif`, `.csv`, `.jshd`) | Reader is chosen by file extension |
| `variable` | Variable name, or band number for GeoTIFF | Must exist in the source |
| `units` | Units the values carry inside simulations | **This** sets the output grid's units — see §5 |
| `output` | Destination path | `.jshdz` selects xz compression; any other extension writes uncompressed |

Spatial options: `--crs` (default `EPSG:4326`), `--x-coord` (default `lon`), `--y-coord`
(default `lat`).

---

## 2. Source time selection — pick exactly one mode

This selects **which slice of the source** is read. It is independent of §3.

### Mode A — named time dimension

```
--time-dim time
```

The name must exist in the source as a dimension variable. NetCDF fails hard if it does not:

```
Preprocessing failed: Failed to set dimensions: Time dimension variable not found: <name>
```

This hard failure is deliberate — it is what turns a typo into an error instead of a silent
single-slice read. **`--time-dim` defaults to `calendar_year`**, a Josh fixture convention that is
almost never what an external source uses.

> **joshpy must always pass `--time-dim` explicitly** for time-series sources. Do not rely on the
> default.

### Mode B — no time dimension (new)

```
--no-time-dim
```

For flat rasters: a 2D NetCDF, a GeoTIFF, a CSV. Before this flag existed there was **no way** to
preprocess a timeless NetCDF — the defaulted `calendar_year` was required to exist, and
`--time-dim ""` failed the same way.

- Takes precedence over `--time-dim` if both are passed. Not an error; documented precedence.
- Every grid timestep reads the single available slice, so a multi-step grid gets the same values
  at every step.
- GeoTIFF and CSV readers ignore the time-dimension name entirely, so they worked without the flag
  before and still do. Passing `--no-time-dim` for them is harmless and more honest.

> **Do not** emit `--time-dim ""` as a substitute. The empty string is an internal encoding between
> the CLI and `ExternalGeoMapper`, not part of this contract, and the Kubernetes entrypoint's `-n`
> guard would drop it.

### Mode C — forced single timestep

```
--timestep N
```

Reads source index `N` only, and writes it to grid timestep `N` (see §4 — this is the one case that
does **not** rebase the index). Combine with `--time-dim`; produces exactly one output slice.

---

## 3. Declared JSHD time axis

Bakes coordinate metadata into the output. Without it, the output is a legacy index-only grid:
still readable by bare `external X` and `external X at index N`, but every coordinate and metadata
query below fails.

Enabled only by a declared axis:

```josh
external temperature at year 2050      # exact coordinate read, never nearest-neighbor
external rainfall at time meta.time    # ISO axis
length of external temperature         # slice count
unit of external temperature           # declared axis unit
first year of external temperature     # first / last coordinate
last year of external temperature
```

**The axis is written only if at least one of** `--time-start`, `--time-unit`, `--time-count`,
`--time-interval`, `--time-instant` **is non-empty.** `--time-type` alone does nothing.

### `--time-type count` (default)

| Form | Required | Must be absent |
|---|---|---|
| Range | `--time-start`, `--time-unit`, `--time-count` | `--time-interval` |
| Instant | `--time-instant`, `--time-unit` | `--time-start`, `--time-count`, `--time-interval` |

`--time-increment` is optional for a range and defaults to `1`.

```
--time-type count --time-start 2015 --time-unit year --time-count 86 --time-increment 1
```

### `--time-type ISO`

| Form | Required | Must be absent |
|---|---|---|
| Range | `--time-start` (ISO date), `--time-interval` (ISO period), `--time-count` | `--time-unit`, `--time-increment` |
| Instant | `--time-instant` (ISO date) | `--time-start`, `--time-count`, `--time-unit`, `--time-increment` |

```
--time-type ISO --time-start 2024-01-01 --time-interval P1Y --time-count 3
```

Dates are `LocalDate` (date-only) and intervals are positive `Period` values (`P1D`, `P1M`, `P1Y`).
No times of day, time zones, or non-Gregorian calendars.

### Unit conversion caveat for count axes

A count-coordinate read converts the expression to the read clause's unit, then to the JSHD axis
unit. Every hop needs a declared conversion or alias in the `.josh` — e.g. reading
`at year meta.year` against an axis declared with `--time-unit year` still needs a `year` unit
declared in the script (`start unit year / alias years / end unit`). Pick `--time-unit` to match
what the script declares.

### The count invariant

```
--time-count  ==  steps.high - steps.low + 1     (from the .josh simulation)
```

Violating it is a hard error: `--time-count must equal the number of output slices`. An instant form
requires exactly one output slice.

> **joshpy must derive `--time-count` from the same step range it wrote into the generated `.josh`**,
> not from the source file's time dimension length. They are frequently different, and the mismatch
> fails at preprocess time rather than silently.

---

## 4. Slice index mapping

For each grid timestep `t` in `[steps.low, steps.high]`:

```
sourceIndex = t - steps.low          # normal case: rebased to 0-based source order
sourceIndex = t                      # when --timestep was given: absolute
```

The rebasing is what lets a calendar-style `steps.low` line up with a 0-based source. With
`steps.low = 2024`, `steps.high = 2026`, grid steps 2024/2025/2026 read source indices 0/1/2, and a
declared count axis of `--time-start 2024 --time-count 3` labels them 2024/2025/2026.

`--time-dim` **only selects source slice order.** Josh does not read, parse, or translate CF time
coordinates, UDUNITS strings, or calendars — the raw values in the source's time variable are never
consulted to place slices. Alignment is entirely the caller's responsibility via §3 and §4.

---

## 5. Units

- The positional `units` argument sets the output grid's units. This is the value the simulation
  sees.
- The source's CF `units` **attribute is not used for the output.** Values are taken as decimals and
  the attribute is discarded.
- A source variable with **no** `units` attribute is fine as of this change. Previously it failed
  with `Error interpolating value for patch: <Patch@hash>`, which is the bug that prompted this
  handoff. joshpy does not need to synthesize a `units` attribute when generating test fixtures.
- Separately: `grid.size` inside the `.josh` **must** be meters (`m` / `meter` / `meters`).
  Anything else is rejected up front — `km` would otherwise be silently treated as meters and
  produce a grid orders of magnitude too fine.

---

## 6. Coverage and fill

A patch whose center falls outside the source's coordinate bounds **plus a 10% per-axis buffer**
gets no value, and the grid keeps its default.

- `--default-value V` sets that default; without it, uncovered cells are `0`.
- `--default-value` is ignored when `--amend` is set.

This matters for small test fixtures: a 30 m grid over a 2×2-point raster rounds up to more rows
than the data covers, so edge cells legitimately read `0`. Assert on populated cells, not every
cell.

---

## 7. Amend mode

`--amend` combines the new grid with an existing output file. Time-axis rules:

- Both grids must have an axis, or neither. Mixing is an error.
- Slice ranges must not overlap and must be **exactly contiguous**
  (`existing.max + 1 == added.min`).
- The merged axis count must equal the merged slice count.

---

## 8. Errors and exit codes

| Exit | Meaning |
|---|---|
| `0` | Success. Stdout: `Successfully preprocessed data to <path>` |
| `1` | `IllegalArgumentException` — bad arguments, failed invariants, unsupported grid units |
| `148` | Any other exception |

**`148` is not arbitrary**: the command returns `404`, and the OS truncates exit status modulo 256
(`404 % 256 == 148`). joshpy should treat `148` as "unknown preprocessing error" and surface the
stderr message, which carries the real detail.

Error messages now include the underlying cause. `ExternalGeoMapper` previously wrapped failures in
`Error interpolating value for patch: <identity hash>` while discarding the cause text, and
`PreprocessCommand` prints only `getMessage()` — so the actual failure was invisible. joshpy should
surface stderr verbatim on non-zero exit.

---

## 9. joshpy checklist

1. Always pass `--time-dim <name>` for time-series sources, or `--no-time-dim` for flat ones. Never
   rely on the `calendar_year` default; never send `--time-dim ""`.
2. Derive `--time-count` from the generated `.josh` step range, not the source's time length.
3. Keep `--time-type` consistent: `count` uses `--time-unit`/`--time-increment`; `ISO` uses
   `--time-interval`. Crossing them is an error.
4. Emit `grid.size` in meters.
5. Choose `.jshdz` vs `.jshd` by intent — the extension alone decides compression.
6. Treat exit `1` as a usage/validation error and `148` as an internal error; surface stderr either
   way.
7. Do not require a `units` attribute on generated NetCDF fixtures.
8. When asserting preprocessed values, ignore cells outside the source's coverage.

---

## 10. Verification

The minimal repro that drove this work, and the shape of a joshpy round-trip test:

```sh
# flat raster — no time dimension anywhere
java -jar joshsim-fat.jar preprocess sim.josh Preprocess temp2d.nc temperature celsius out.jshdz \
  --x-coord lon --y-coord lat --no-time-dim

# time series with a declared count axis
java -jar joshsim-fat.jar preprocess sim.josh Preprocess temp.nc temperature celsius out.jshdz \
  --x-coord lon --y-coord lat \
  --time-dim time --time-type count --time-start 2015 --time-unit year \
  --time-count 2 --time-increment 1
```

Java-side coverage for these paths lives in
[PreprocessNetcdfMetadataIntegrationTest](src/test/java/org/joshsim/command/PreprocessNetcdfMetadataIntegrationTest.java)
and [ExternalTimeAxisIntegrationTest](src/test/java/org/joshsim/command/ExternalTimeAxisIntegrationTest.java).
