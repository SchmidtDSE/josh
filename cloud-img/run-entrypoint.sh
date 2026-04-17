#!/bin/sh
# Run batch entrypoint for K8s pods.
# Stages inputs from MinIO, finds .josh script, runs simulation.
# Expects env vars: JOSH_MINIO_PREFIX, JOSH_SIMULATION
# MinIO creds (MINIO_ENDPOINT, etc.) are picked up automatically
# by HierarchyConfig from the environment.
#
# Usage: /app/run-entrypoint.sh [jar_path]
#   jar_path defaults to /app/joshsim-fat.jar

set -e

JAR="${1:-/app/joshsim-fat.jar}"
WORK_DIR="/tmp/work"

java -jar "$JAR" stageFromMinio \
  --prefix="$JOSH_MINIO_PREFIX" \
  --output-dir="$WORK_DIR"

SCRIPT=$(find "$WORK_DIR" -name '*.josh' -type f | head -1)

if [ -z "$SCRIPT" ]; then
  echo "ERROR: No .josh file found in $WORK_DIR" >&2
  exit 1
fi

# JOB_COMPLETION_INDEX is set by K8s for indexed Jobs (0, 1, 2, ...).
# Each pod runs one replicate at its assigned index so {replicate}
# template paths resolve to unique filenames.
REPLICATE_INDEX="${JOB_COMPLETION_INDEX:-0}"
java -jar "$JAR" run "$SCRIPT" "$JOSH_SIMULATION" --replicate-index="$REPLICATE_INDEX"
