#!/bin/bash
# License: BSD-3-Clause
# Run a remote profiler test against a running Josh server and verify profiler output.
#
# Usage: run_profiler_remote.sh <on|off> [--enable-profiler]
#   on|off          -- passed to check_profiler.py to assert timings are present or absent
#   --enable-profiler -- optional; appended to the runRemote command when provided

set -e

MODE="$1"
PROFILER_FLAG="$2"

java -Xmx2g -jar build/libs/joshsim-fat.jar runRemote \
  examples/simulations/profiler_multi.josh \
  ProfilerMultiExample \
  --endpoint http://localhost:8085/runReplicates \
  --api-key testkey \
  --replicates=1 \
  $PROFILER_FLAG

python3 .github/scripts/check_profiler.py "$MODE"
rm -f /tmp/profiler_multi_josh.csv
