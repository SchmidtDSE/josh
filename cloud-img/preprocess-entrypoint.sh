#!/bin/sh
# Preprocess batch entrypoint for K8s pods.
# Stages inputs from MinIO, finds .josh script, runs preprocessing,
# uploads the resulting .jshd back to MinIO.
#
# Expects env vars:
#   JOSH_MINIO_PREFIX, JOSH_SIMULATION, JOSH_JOB_ID,
#   JOSH_DATA_FILE, JOSH_VARIABLE, JOSH_UNITS, JOSH_OUTPUT_FILE
# Optional env vars:
#   JOSH_CRS, JOSH_X_COORD, JOSH_Y_COORD, JOSH_TIME_DIM,
#   JOSH_TIMESTEP, JOSH_DEFAULT_VALUE, JOSH_PARALLEL, JOSH_AMEND
# MinIO creds (MINIO_ENDPOINT, etc.) are picked up automatically
# by HierarchyConfig from the environment.
#
# Usage: /app/preprocess-entrypoint.sh [jar_path]
#   jar_path defaults to /app/joshsim-fat.jar

set -e

JAR="${1:-/app/joshsim-fat.jar}"
WORK_DIR="/tmp/work"

# Stage inputs (josh script + data file)
java -jar "$JAR" stageFromMinio \
  --prefix="$JOSH_MINIO_PREFIX" \
  --output-dir="$WORK_DIR"

SCRIPT=$(find "$WORK_DIR" -name '*.josh' -type f | head -1)

if [ -z "$SCRIPT" ]; then
  echo "ERROR: No .josh file found in $WORK_DIR" >&2
  exit 1
fi

# Build optional flags
OPTS=""
if [ -n "$JOSH_CRS" ]; then
  OPTS="$OPTS --crs=$JOSH_CRS"
fi
if [ -n "$JOSH_X_COORD" ]; then
  OPTS="$OPTS --x-coord=$JOSH_X_COORD"
fi
if [ -n "$JOSH_Y_COORD" ]; then
  OPTS="$OPTS --y-coord=$JOSH_Y_COORD"
fi
if [ -n "$JOSH_TIME_DIM" ]; then
  OPTS="$OPTS --time-dim=$JOSH_TIME_DIM"
fi
if [ -n "$JOSH_TIMESTEP" ]; then
  OPTS="$OPTS --timestep=$JOSH_TIMESTEP"
fi
if [ -n "$JOSH_DEFAULT_VALUE" ]; then
  OPTS="$OPTS --default-value=$JOSH_DEFAULT_VALUE"
fi
if [ "$JOSH_PARALLEL" = "true" ]; then
  OPTS="$OPTS --parallel"
fi
if [ "$JOSH_AMEND" = "true" ]; then
  OPTS="$OPTS --amend"
fi

# Run preprocessing
# shellcheck disable=SC2086
java -jar "$JAR" preprocess "$SCRIPT" "$JOSH_SIMULATION" \
  "$WORK_DIR/$JOSH_DATA_FILE" "$JOSH_VARIABLE" "$JOSH_UNITS" \
  "$WORK_DIR/$JOSH_OUTPUT_FILE" \
  $OPTS

# Upload result .jshd to MinIO
java -jar "$JAR" stageToMinio \
  --input-dir="$WORK_DIR" \
  --prefix="batch-jobs/$JOSH_JOB_ID/outputs/"
