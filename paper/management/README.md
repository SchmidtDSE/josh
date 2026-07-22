# ForeverTree management extension

An extension of the [ForeverTree example](../forevertree) that adds an invasive
competition dynamic, a fire disturbance, and a management response. It keeps the
base model's climate-driven growth (quadratic temperature response, sigmoid
precipitation response, Gaussian noise) and layers three mechanics on top — the
model behind `demo.joshsim.org/management`.

## The three mechanics

1. **Invasive grass competition.** A patch-level `invasiveCover` (0–100%) grows
   fast on empty patches and is held down where trees are dense: annual growth
   scales from `invasiveBaseGrowth` at zero trees to nothing at
   `treesForFullSuppression` live trees, and never falls below `invasiveMinCover`
   on its own. Grass also suppresses the growth of **small** trees only (height
   below `smallTreeHeight`) — large trees out-compete it. Tree life stage is a
   `Juvenile` → `Adult` transition by height.
2. **Fire.** A static fire footprint (`data/fire_synthetic.nc`, the `burned`
   mask) marks where fire can occur; the sim-level `fire` flag fires it on
   `fireStep`. Inside the footprint each tree burns with probability
   `burnLikelihood` (90% → ~1 of 10 survives), moving to a terminal `Burned`
   life stage. With the trees gone, grass invades the cleared cells, partly by
   **dispersal** from neighbouring cells.
3. **Management.** A smaller managed boundary inside the burn
   (`data/management_synthetic.nc`, the `managed` mask) is where two
   config-toggled interventions act once, on `managementStep`: **outplanting**
   (`outplantCount` trees at `outplantAge`/`outplantHeight` — tall enough plants
   start as Adults) and **invasive removal** (`removeInvasives` knocks cover back
   to `invasiveRemovalTarget`). Together they let the managed zone revegetate.

## Layout

| File | What it is |
|------|------------|
| `forevertree.josh` | Base layer: grid definition + basic ForeverTree ecology (climate-driven growth only). |
| `invasive_grass.josh` | Imports `forevertree.josh`; `update`s in invasive-grass competition and the `Juvenile`/`Adult` maturity states that gate growth suppression. |
| `management.josh` | Imports `invasive_grass.josh`; `update`s in fire, the outplant origin, the terminal `Burned` state, and the `file://` export sink — this is the file actually run from the CLI. |
| `management_wasm.josh` | Single self-contained file (not built via import/update, since the browser/WASM demo can't bundle multiple files) with the same simulated behavior as `management.josh` but a `memory://editor/patches` export sink instead. |
| `scenario.jshc` | Tunable scenario knobs (named `scenario` because `management` is a reserved word). |
| `data/fire_synthetic.nc` | Static fire footprint (`burned` mask, 1 = burned). |
| `data/management_synthetic.nc` | Static managed boundary (`managed` mask, 1 = managed; south/hot half of the burn). |

Climate inputs are **reused** from `../forevertree/data/` (`maxtemp_synthetic.nc`,
`precip_synthetic.nc`); `test.sh` preprocesses them from there.

## Run

```sh
# from the repo root, after building build/libs/joshsim-fat.jar
bash paper/management/test.sh          # 2 replicates (fast)
N_REPLICATES=100 bash paper/management/test.sh
```

The test preprocesses the climate netCDFs and the two masks to `.jshd`, runs the
baseline scenario, checks the fire burned trees, then runs an outplanting +
invasive-removal scenario to prove the management knobs are wired through.

## How the static masks are read

The fire and managed boundaries are time-invariant 2-D fields. Josh has no purely
dimensionless external path, so each mask carries a **single-element
`calendar_year`** dimension; it is preprocessed with `--timestep 0` into a
one-layer `.jshd` and read inline at every step with `external burned at 0` /
`external managed at 0` (the same idiom the Joshua Tree model uses for its static
burn-severity field). The mask `.nc` must therefore be shaped
`(calendar_year, lat, lon)` with `calendar_year` length ≥ 1 and be aligned
cell-for-cell with the climate netCDFs.

## Notes / gotchas

- Same grid as ForeverTree (`grid.size = 16000 m`, NW `36.73°/−119.52°` →
  SE `35.80°/−117.98°`, steps 0–10 ≈ years 2024–2034).
- `dispersalRadius` (20 km) must exceed the 16 km cell size, or the neighbour
  query finds only the patch itself and no grass disperses.
- Inside `:if(...)` selectors, `and` binds tighter than `==`/`<`, so wrap each
  comparison in parentheses (e.g. `a and (b == c)`).
- Combine entity collections with `|` (union), not `+`.
- `exportFiles.patch` must be an absolute `file:///…` URL — hence the separate
  `_wasm` twin with the `memory://` sink.
- `--data` takes the explicit `name=path;name=path` form.
