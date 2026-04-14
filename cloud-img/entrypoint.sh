#!/bin/sh
# Batch job entrypoint for K8s pods.
# Expects env vars: JOSH_MINIO_PREFIX, JOSH_SIMULATION
# MinIO creds (MINIO_ENDPOINT, etc.) are picked up automatically
# by HierarchyConfig from the environment.
#
# Usage: /app/entrypoint.sh [jar_path]
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

java -jar "$JAR" run "$SCRIPT" "$JOSH_SIMULATION" --replicates=1
