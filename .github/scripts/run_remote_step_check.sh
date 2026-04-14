#!/bin/bash
# License: BSD-3-Clause
# Run the remote step-label check simulation and verify results.
#
# Usage: run_remote_step_check.sh

set -e

java -Xmx2g -jar build/libs/joshsim-fat.jar runRemote \
  examples/test/remote_step_check.josh \
  StepCheck \
  --endpoint http://localhost:8085/runReplicates \
  --api-key testkey \
  --replicates 5

python3 .github/scripts/check_remote_step.py
