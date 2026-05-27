#!/bin/bash
# MCP server regression smoke test, driven by the official MCP Inspector CLI.
#
# Exercises the stdio MCP server end-to-end the way a real MCP client would:
#   1. tools/list                — all four tools are advertised
#   2. validate_simulation       — cheap parse/validate path
#   3. run_simulation            — local run path (writes a CSV)
#   4. preprocess_data           — local preprocess path (writes a .jshd)
#
# Requires `node`/`npx` on PATH (preinstalled on GitHub-hosted ubuntu runners)
# and a built fat jar at build/libs/joshsim-fat.jar.
set -euo pipefail

JAR="build/libs/joshsim-fat.jar"
# Pin a version here (e.g. @modelcontextprotocol/inspector@0.x.y) once a known-good
# release is confirmed, so the inspector's own CLI changes can't silently break CI.
INSPECTOR="@modelcontextprotocol/inspector"

if [ ! -f "$JAR" ]; then
  echo "ERROR: $JAR not found. Build it with: ./gradlew fatJar"
  exit 1
fi

echo "node: $(node --version), npm: $(npm --version)"
echo "=== MCP Inspector CLI smoke test ==="

# Helper: run the inspector against the stdio server with the given inspector args.
inspect() {
  npx --yes "$INSPECTOR" --cli java -jar "$JAR" mcp "$@"
}

# 1. tools/list — every tool must be advertised.
echo "--- tools/list ---"
LIST=$(inspect --method tools/list)
echo "$LIST"
for tool in validate_simulation discover_config preprocess_data run_simulation; do
  echo "$LIST" | grep -q "\"$tool\"" || { echo "FAIL: tool '$tool' not advertised"; exit 1; }
done
echo "PASS: all four tools advertised"

# Use an existing self-contained example (no external data) for validate + run.
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT
RUN_JOSH="examples/simulations/simple.josh"
RUN_SIM="TestSimpleSimulation"
RUN_OUT="/tmp/simple_josh.csv"  # export path declared inside simple.josh
rm -f "$RUN_OUT"

# 2. validate_simulation — should report a successful validation.
echo "--- validate_simulation ---"
VALIDATE=$(inspect --method tools/call --tool-name validate_simulation \
  --tool-arg script="$RUN_JOSH")
echo "$VALIDATE"
echo "$VALIDATE" | grep -qi "Validated Josh script" || { echo "FAIL: validate_simulation"; exit 1; }
echo "PASS: validate_simulation"

# 3. run_simulation — should complete and write the export CSV.
echo "--- run_simulation ---"
RUN=$(inspect --method tools/call --tool-name run_simulation \
  --tool-arg script="$RUN_JOSH" --tool-arg simulation="$RUN_SIM")
echo "$RUN"
echo "$RUN" | grep -qi "completed" || { echo "FAIL: run_simulation did not complete"; exit 1; }
[ -s "$RUN_OUT" ] || { echo "FAIL: run_simulation produced no CSV output"; exit 1; }
echo "PASS: run_simulation (CSV written)"

# 4. preprocess_data — convert the committed test GeoTIFF into a .jshd.
echo "--- preprocess_data ---"
PREP_OUT="$WORK/precip.jshd"
PREPROCESS=$(inspect --method tools/call --tool-name preprocess_data \
  --tool-arg script=examples/test/test_basic_preprocess.josh \
  --tool-arg simulation=Main \
  --tool-arg dataFile=src/test/resources/cog/CHC-CMIP6_SSP245_CHIRPS_2008_annual.tif \
  --tool-arg variable=0 \
  --tool-arg unitsStr=mm \
  --tool-arg outputFile="$PREP_OUT")
echo "$PREPROCESS"
echo "$PREPROCESS" | grep -qi "Successfully preprocessed" \
  || { echo "FAIL: preprocess_data"; exit 1; }
[ -s "$PREP_OUT" ] || { echo "FAIL: preprocess_data produced no .jshd output"; exit 1; }
echo "PASS: preprocess_data (.jshd written)"

# 5. run_simulation with an explicit data mapping (the MCP equivalent of `--data`).
# The .jshd from step 4 lives in a temp dir outside the working directory, and the script
# (test_basic_preprocess.josh) references it via `external data`. With no `data.jshd` in the CWD,
# only the data mapping can satisfy that reference — so a completed run proves the mapping is
# used, not working-directory resolution. The object-valued arg uses the inspector's JSON form.
echo "--- run_simulation with data mapping ---"
RUN_DATA=$(inspect --method tools/call --tool-name run_simulation \
  --tool-arg script=examples/test/test_basic_preprocess.josh \
  --tool-arg simulation=Main \
  --tool-arg "data={\"data.jshd\":\"$PREP_OUT\"}")
echo "$RUN_DATA"
echo "$RUN_DATA" | grep -qi "completed" \
  || { echo "FAIL: run_simulation with data mapping"; exit 1; }
echo "PASS: run_simulation with data mapping"

echo "=== MCP Inspector CLI smoke test passed ==="
