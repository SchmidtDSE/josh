#!/bin/bash
# License: BSD-3-Clause
# Run a local profiler test and verify profiler output.
#
# Usage: run_profiler_local.sh <replicates> <on|off> [--enable-profiler]
#   replicates      -- number of replicates to pass as --replicates=N
#   on|off          -- passed to check_profiler.py to assert timings are present or absent
#   --enable-profiler -- optional; appended to the run command when provided

set -e

REPLICATES="$1"
MODE="$2"
PROFILER_FLAG="$3"

java -Xmx2g -jar build/libs/joshsim-fat.jar run \
  --replicates="$REPLICATES" \
  $PROFILER_FLAG \
  examples/simulations/profiler_multi.josh \
  ProfilerMultiExample

python3 .github/scripts/check_profiler.py "$MODE"
rm -f /tmp/profiler_multi_josh.csv
