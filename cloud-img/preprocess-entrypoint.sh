#!/bin/sh
# Preprocess batch entrypoint for K8s pods.
# Stages inputs from MinIO, resolves the entry .josh script, runs preprocessing,
# uploads the resulting .jshd back to MinIO.
#
# Expects env vars:
#   JOSH_MINIO_PREFIX, JOSH_SIMULATION, JOSH_JOB_ID,
#   JOSH_DATA_FILE, JOSH_VARIABLE, JOSH_UNITS, JOSH_OUTPUT_FILE
# Optional env vars:
#   JOSH_CRS, JOSH_X_COORD, JOSH_Y_COORD, JOSH_TIME_DIM,
#   JOSH_TIMESTEP, JOSH_DEFAULT_VALUE, JOSH_PARALLEL, JOSH_AMEND,
#   JOSH_TIME_OPTS
#   JOSH_SCRIPT names the entry script when the staged layout is not the usual
#   "sim.josh (or the one root-level .josh) plus imported overlays".
# MinIO creds (MINIO_ENDPOINT, etc.) are picked up automatically
# by HierarchyConfig from the environment.
#
# Usage: /app/preprocess-entrypoint.sh [jar_path]
#   jar_path defaults to /app/joshsim-fat.jar

set -e

JAR="${1:-/app/joshsim-fat.jar}"
WORK_DIR="/tmp/work"

# GKE Autopilot pods sometimes have a brief window after start where DNS
# isn't usable yet. Probe the object store we are about to stage from so a
# flaky resolver doesn't kill stageFromMinio after we've paid full JAR
# startup cost. Probing the configured endpoint rather than a fixed host
# keeps this useful off GCP (NRP Ceph, in-cluster MinIO, EKS).
MINIO_HOST=$(echo "${MINIO_ENDPOINT:-}" | sed -e 's|^[a-zA-Z][a-zA-Z0-9+.-]*://||' -e 's|[:/].*$||')
if [ -n "$MINIO_HOST" ]; then
  for attempt in 1 2 3 4 5 6 7 8 9 10; do
    if getent hosts "$MINIO_HOST" >/dev/null 2>&1; then
      break
    fi
    sleep 2
  done
fi

# Stage inputs (josh script + data file), with retry for transient hiccups.
STAGE_OK=0
for attempt in 1 2 3; do
  if java -jar "$JAR" stageFromMinio \
       --prefix="$JOSH_MINIO_PREFIX" \
       --output-dir="$WORK_DIR"; then
    STAGE_OK=1
    break
  fi
  echo "stageFromMinio attempt $attempt failed, retrying..." >&2
  sleep $((attempt * 5))
done
if [ "$STAGE_OK" -ne 1 ]; then
  echo "ERROR: stageFromMinio failed after retries" >&2
  exit 1
fi

# Resolve the entry script. A model that uses `import` stages its overlay files
# alongside the entry at their relative import path (canonical/exports.josh and
# friends), so a recursive `find ... | head -1` can return an overlay instead of
# the entry -- filesystem traversal order decides, which is neither alphabetical
# nor stable. Running an overlay standalone fails with "Cannot update entity
# ...; no prior definition exists" because it only means anything when imported.
# Resolve deterministically instead, most explicit first:
#   1. JOSH_SCRIPT, when the dispatcher names the entry outright.
#   2. sim.josh at the work dir root, the name joshpy's staging always uses.
#   3. The sole root-level *.josh. Imports live in subdirectories, so limiting
#      this to depth 1 excludes overlays, and it keeps the name-agnostic
#      `stageToMinio --input-dir=./sim/` workflow working. More than one
#      candidate is an error rather than a coin flip.
if [ -n "${JOSH_SCRIPT:-}" ]; then
  case "$JOSH_SCRIPT" in
    /*) SCRIPT="$JOSH_SCRIPT" ;;
    *) SCRIPT="$WORK_DIR/$JOSH_SCRIPT" ;;
  esac
  if [ ! -f "$SCRIPT" ]; then
    echo "ERROR: JOSH_SCRIPT=$JOSH_SCRIPT does not resolve to a file ($SCRIPT)" >&2
    exit 1
  fi
elif [ -f "$WORK_DIR/sim.josh" ]; then
  SCRIPT="$WORK_DIR/sim.josh"
else
  SCRIPT=""
  for candidate in "$WORK_DIR"/*.josh; do
    [ -f "$candidate" ] || continue
    if [ -n "$SCRIPT" ]; then
      echo "ERROR: multiple .josh files at the root of $WORK_DIR and no" \
        "sim.josh to disambiguate. Set JOSH_SCRIPT to name the entry file." >&2
      ls -1 "$WORK_DIR"/*.josh >&2
      exit 1
    fi
    SCRIPT="$candidate"
  done
  if [ -z "$SCRIPT" ]; then
    echo "ERROR: No .josh file at the root of $WORK_DIR" >&2
    exit 1
  fi
fi

echo "Entry script: $SCRIPT"

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
elif [ -n "${JOSH_TIME_DIM+set}" ]; then
  # Set but empty means the dispatcher declared a source with no time dimension. Leaving the
  # flag off instead would silently fall back to the jar's default dimension name.
  OPTS="$OPTS --no-time-dim"
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
# JOSH_TIME_OPTS already holds --time-* flags, built by TimeAxisParams.toCliFlags(). The dispatcher
# owns that encoding so this script needs no knowledge of the individual time axis fields. Values
# are single tokens (counts, units, ISO dates and periods), so appending is safe to word-split.
if [ -n "$JOSH_TIME_OPTS" ]; then
  OPTS="$OPTS $JOSH_TIME_OPTS"
fi

# Run preprocessing
# shellcheck disable=SC2086
java -XX:+ExitOnOutOfMemoryError -jar "$JAR" preprocess "$SCRIPT" "$JOSH_SIMULATION" \
  "$WORK_DIR/$JOSH_DATA_FILE" "$JOSH_VARIABLE" "$JOSH_UNITS" \
  "$WORK_DIR/$JOSH_OUTPUT_FILE" \
  $OPTS

# Upload result .jshd to MinIO, with retry to avoid losing a completed
# preprocess run to a transient network hiccup.
UPLOAD_OK=0
for attempt in 1 2 3; do
  if java -jar "$JAR" stageToMinio \
       --input-dir="$WORK_DIR" \
       --prefix="batch-jobs/$JOSH_JOB_ID/outputs/"; then
    UPLOAD_OK=1
    break
  fi
  echo "stageToMinio attempt $attempt failed, retrying..." >&2
  sleep $((attempt * 5))
done
if [ "$UPLOAD_OK" -ne 1 ]; then
  echo "ERROR: stageToMinio failed after retries" >&2
  exit 1
fi
