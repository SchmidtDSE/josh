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

# Prepare a tiny, self-contained simulation in a temp dir.
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT
RUN_JOSH="$WORK/run.josh"
RUN_OUT="$WORK/run_out.csv"
cat > "$RUN_JOSH" <<JOSH
start simulation TestSim
  grid.size = 100 m
  grid.low = 0 degrees latitude, 0 degrees longitude
  grid.high = 0.1 degrees latitude, 0.1 degrees longitude
  grid.patch = "Default"
  steps.low = 0 count
  steps.high = 2 count
  exportFiles.patch = "file://$RUN_OUT"
end simulation
start patch Default
  Tree.init = create 2 count of Tree
  export.treeCount.step = count(Tree)
end patch
start organism Tree
  age.init = 0 count
  age.step = prior.age + 1 count
end organism
JOSH

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
  --tool-arg script="$RUN_JOSH" --tool-arg simulation=TestSim)
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

echo "=== MCP Inspector CLI smoke test passed ==="
