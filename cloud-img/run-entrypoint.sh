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

# cd into the staged work dir so external resources resolve relative to it.
# JvmWorkingDirInputGetter uses cwd-relative FileInputStream, and the staged
# files (script + any external .jshd/.jshdz inputs) all live in $WORK_DIR.
mkdir -p "$WORK_DIR"
cd "$WORK_DIR"

# GKE Autopilot pods sometimes have a brief window after start where DNS
# isn't usable yet. Probe a stable host so a flaky resolver doesn't kill
# stageFromMinio after we've paid full JAR startup cost.
for attempt in 1 2 3 4 5 6 7 8 9 10; do
  if getent hosts storage.googleapis.com >/dev/null 2>&1; then
    break
  fi
  sleep 2
done

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

SCRIPT=$(find "$WORK_DIR" -name '*.josh' -type f | head -1)

if [ -z "$SCRIPT" ]; then
  echo "ERROR: No .josh file found in $WORK_DIR" >&2
  exit 1
fi

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
