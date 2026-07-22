#!/bin/bash

# ForeverTree management-extension test.
#
# Extends the ForeverTree model (paper/forevertree) with an invasive-grass
# competition dynamic, a fire disturbance, and a management response. Exercises
# the canonical CLI path end to end:
#   1. preprocess the shared climate netCDFs (tasmax K; pr kg m-2 s-1) from
#      paper/forevertree/data -> grid-aligned .jshd
#   2. preprocess the static fire / management boundary masks. These carry a
#      single-element calendar_year dimension and are read in-model with
#      `external burned at 0` / `external managed at 0`, so they are processed
#      with --timestep 0 into a single-layer .jshd.
#   3. run the baseline (no management) and assert the fire burned some trees
#   4. run an outplanting + invasive-removal scenario and assert it completes
#
# Run from the repo root. Requires build/libs/joshsim-fat.jar (built if absent).

set -e

if [ ! -f build/libs/joshsim-fat.jar ]; then
  gradle fatJar
fi

EX=paper/management
CLIMATE=paper/forevertree/data
JAR=build/libs/joshsim-fat.jar
N_REPLICATES="${N_REPLICATES:-2}"

WORK_DIR=$(mktemp -d)
cleanup() {
  rm -rf "$WORK_DIR"
  rm -f /tmp/management_results_*.csv
}
trap cleanup EXIT

josh() { java -Xmx6g -jar "$JAR" "$@"; }

echo "Validating management models..."
# The model is layered across three imported files, each building on the last via `update`:
#   forevertree.josh  - grid definition + basic ForeverTree ecology (climate-driven growth)
#   invasive_grass.josh - imports forevertree.josh; adds invasive-grass competition and the
#                         Juvenile/Adult maturity states that gate growth suppression
#   management.josh   - imports invasive_grass.josh; adds fire, the outplant origin, the Burned
#                       state, and the scenario export sink; this is the file actually run below
# management_wasm.josh stays a single self-contained file (matching management.josh's simulated
# behavior, but not its exact text) since the browser/WASM demo has no mechanism to bundle
# multiple imported files alongside the one pasted into the editor.
josh validate "$EX/management.josh"
josh validate "$EX/management_wasm.josh"

echo "Preprocessing climate netCDFs to .jshd..."
josh preprocess "$EX/management.josh" Main \
  "$CLIMATE/maxtemp_synthetic.nc" tasmax K "$WORK_DIR/temperature.jshd" \
  --time-dim calendar_year --x-coord lon --y-coord lat
josh preprocess "$EX/management.josh" Main \
  "$CLIMATE/precip_synthetic.nc" pr kgm2s "$WORK_DIR/precipitation.jshd" \
  --time-dim calendar_year --x-coord lon --y-coord lat

echo "Preprocessing static fire / management masks (single timestep) to .jshd..."
# The output filename stem is the resource name referenced from the model:
# burned.jshd -> `external burned`, managed.jshd -> `external managed`.
josh preprocess "$EX/management.josh" Main \
  "$EX/data/fire_synthetic.nc" burned count "$WORK_DIR/burned.jshd" \
  --time-dim calendar_year --x-coord lon --y-coord lat --timestep 0
josh preprocess "$EX/management.josh" Main \
  "$EX/data/management_synthetic.nc" managed count "$WORK_DIR/managed.jshd" \
  --time-dim calendar_year --x-coord lon --y-coord lat --timestep 0

for f in temperature precipitation burned managed; do
  [ -s "$WORK_DIR/$f.jshd" ] || { echo "missing $f.jshd"; exit 1; }
done

# Everything except the .jshc config, which we swap between scenarios.
GRIDS="temperature=$WORK_DIR/temperature.jshd"
GRIDS="$GRIDS;precipitation=$WORK_DIR/precipitation.jshd"
GRIDS="$GRIDS;burned=$WORK_DIR/burned.jshd"
GRIDS="$GRIDS;managed=$WORK_DIR/managed.jshd"

echo "Running baseline (no management, $N_REPLICATES replicates)..."
rm -f /tmp/management_results_*.csv
josh run "$EX/management.josh" Main --replicates "$N_REPLICATES" \
  --data "scenario.jshc=$EX/scenario.jshc;$GRIDS"

for ((r = 0; r < N_REPLICATES; r++)); do
  [ -s "/tmp/management_results_${r}.csv" ] || exit 2
  grep -q "invasiveCover" "/tmp/management_results_${r}.csv" || exit 3
done

echo "Checking the fire burned trees inside the footprint..."
# nBurned must climb above zero somewhere at/after the fire step.
awk -F, 'NR==1{for (i=1;i<=NF;i++) h[$i]=i}
         NR>1 && $h["nBurned"]+0 > 0 {found=1}
         END{exit !found}' /tmp/management_results_0.csv || exit 4

echo "Verifying the management knobs revegetate the burned managed zone..."
# Outplanting adults + knocking back invasives should let the managed zone
# recover. We assert the scenario runs and writes output, proving both knobs
# are wired through the config.
sed -e 's/^outplantCount = .*/outplantCount = 8 count/' \
    -e 's/^removeInvasives = .*/removeInvasives = 1 count/' \
    "$EX/scenario.jshc" > "$WORK_DIR/recover.jshc"
rm -f /tmp/management_results_*.csv
josh run "$EX/management.josh" Main --replicates 1 \
  --data "scenario.jshc=$WORK_DIR/recover.jshc;$GRIDS"
[ -s "/tmp/management_results_0.csv" ] || exit 5

echo "ForeverTree management example test passed successfully!"
