# ForeverTree example

A self-contained Josh implementation of the ForeverTree model — a spatial
vegetation simulation whose per-year growth is driven by external temperature
and precipitation, with a quadratic temperature response, a logistic
(sigmoid) precipitation response, and small Gaussian growth noise. This is the
model used in the paper.

## Layout

| File | What it is |
|------|------------|
| `forevertree.josh` | The model, with a `file://` export sink — run from the CLI. |
| `forevertree_wasm.josh` | Byte-for-byte twin with a `memory://editor/patches` sink for the Josh web (WASM) editor. The two files differ **only** in the `exportFiles.patch` line. |
| `forevertree.jshc` | Tunable parameters (`minPrecipImpactPct`, `maxNewGrowth`). |
| `data/*.nc` | CF-1.8 synthetic climate inputs (`tasmax` in K; `pr` flux in `kg m⁻² s⁻¹`). |

## Tunable parameters (`forevertree.jshc`)

Key model behavior is decomposed into `forevertree.jshc` so it can be swept
without editing the model:

- **`minPrecipImpactPct`** — the precipitation-impact floor (the bottom of the
  sigmoid's output range). Slide it to see how strongly precipitation gates
  growth in the temperature/precipitation interaction:
  `0%` makes precipitation fully gate growth (strong precip dependence), while
  `100%` removes precipitation as a limiter so growth is driven by temperature
  alone. In the synthetic data this moves final mean height from ~4.8 m at `0%`
  to ~9.9 m at `100%`.
- **`maxNewGrowth`** — the maximum new growth per year, realized when both the
  temperature and precipitation impacts are at 100%.

## Run

```sh
# from the repo root, after building build/libs/joshsim-fat.jar
bash paper/forevertree/test.sh          # 2 replicates (fast)
N_REPLICATES=100 bash paper/forevertree/test.sh   # the paper's workload
```

The test preprocesses the climate netCDFs to `.jshd`, runs the simulation with
those `.jshd` and `forevertree.jshc` supplied via `--data`, and checks the
per-replicate CSV exports (`/tmp/forevertree_results_{replicate}.csv`).

## How the precipitation units work

`pr` is a CF precipitation flux in `kg m⁻² s⁻¹`. That CF string isn't a legal
Josh identifier, so it's aliased to `kgm2s` at preprocess time, and a
`start unit kgm2s` block in the model converts it to the working unit `mm` — no
python, no intermediate netCDF:

```
start unit kgm2s
  mm = current * 31536000     # kg m⁻² s⁻¹ flux → mm/year depth
end unit
```

## Notes / gotchas

- The `.jshd` are aligned to the model's grid (`grid.size = 16000 m`, bounding
  box 36.73°/−119.52° NW → 35.80°/−117.98° SE).
- Write grid size in **meters** (`16000 m`), not `km` — with degree corners,
  meters trigger the Haversine conversion; `km` parses but fails at run.
- `grid.top_left` (alias of `grid.low`) is the **north-west** corner, so its
  latitude must be the **larger** value; `grid.bottom_right` (alias of
  `grid.high`) is the south-east corner.
- `exportFiles.patch` must be an absolute `file:///…` URL — relative `file://`
  paths are misparsed (the path after `file://` is read as the URI authority).
  That's why `forevertree.josh` carries an absolute output path and there's a
  separate `_wasm` twin with the `memory://` sink instead of rewriting the line.
- `--data` takes the explicit `name=path;name=path` form — a bare directory is
  misparsed as a job-variation spec.
