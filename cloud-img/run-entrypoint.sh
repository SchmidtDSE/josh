#!/bin/sh
# Run batch entrypoint for K8s pods.
# Stages inputs from MinIO, resolves the entry .josh script, runs simulation.
# Expects env vars: JOSH_MINIO_PREFIX, JOSH_SIMULATION
# Optional: JOSH_SCRIPT names the entry script when the staged layout is not
# the usual "sim.josh (or the one root-level .josh) plus imported overlays".
# MinIO creds (MINIO_ENDPOINT, etc.) are picked up automatically
# by HierarchyConfig from the environment.
#
# Usage: /app/run-entrypoint.sh [jar_path]
#   jar_path defaults to /app/joshsim-fat.jar

set -e

JAR="${1:-/app/joshsim-fat.jar}"
WORK_DIR="/tmp/work"

# cd into the staged work dir so external resources resolve relative to it.
# JvmWorkingDirInputGetter uses cwd-relative FileInputStream, and the staged
# files (script + any external .jshd/.jshdz inputs) all live in $WORK_DIR.
mkdir -p "$WORK_DIR"
cd "$WORK_DIR"

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

# Retry stageFromMinio for transient network hiccups post-cold-start.
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

# JOB_COMPLETION_INDEX is set by K8s for indexed Jobs (0, 1, 2, ...).
# Two ways to map a pod's completion index to an absolute replicate index:
#   - JOSH_REPLICATE_INDICES (comma-separated, e.g. "3,7,8"): pick the
#     JOB_COMPLETION_INDEX-th entry. Used to backfill a specific, possibly
#     non-contiguous set of replicates.
#   - JOSH_REPLICATE_OFFSET (default 0): shift the completion index by a fixed
#     offset for contiguous pool/resume ranges.
# JOSH_REPLICATE_INDICES takes precedence when set.
if [ -n "${JOSH_REPLICATE_INDICES:-}" ]; then
  target_pos=${JOB_COMPLETION_INDEX:-0}
  pos=0
  REPLICATE_INDEX=""
  OLD_IFS=$IFS
  IFS=,
  for idx in $JOSH_REPLICATE_INDICES; do
    if [ "$pos" -eq "$target_pos" ]; then
      REPLICATE_INDEX=$idx
      break
    fi
    pos=$((pos + 1))
  done
  IFS=$OLD_IFS
  if [ -z "$REPLICATE_INDEX" ]; then
    echo "ERROR: JOB_COMPLETION_INDEX $target_pos out of range for" \
      "JOSH_REPLICATE_INDICES=$JOSH_REPLICATE_INDICES" >&2
    exit 1
  fi
else
  REPLICATE_INDEX=$(( ${JOB_COMPLETION_INDEX:-0} + ${JOSH_REPLICATE_OFFSET:-0} ))
fi

# JOSH_CUSTOM_TAGS holds newline-delimited key=value entries. One
# --custom-tag flag per non-empty line. Newline-delimited (vs JSON)
# avoids needing jq in the JRE-only batch image.
TAGS=""
if [ -n "$JOSH_CUSTOM_TAGS" ]; then
  while IFS= read -r line; do
    [ -n "$line" ] && TAGS="$TAGS --custom-tag=$line"
  done <<EOF
$JOSH_CUSTOM_TAGS
EOF
fi

# shellcheck disable=SC2086
java -XX:+ExitOnOutOfMemoryError -jar "$JAR" run "$SCRIPT" "$JOSH_SIMULATION" \
  --replicate-index="$REPLICATE_INDEX" $TAGS
