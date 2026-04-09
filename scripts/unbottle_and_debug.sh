#!/bin/bash
# unbottle_and_debug.sh — Extract a joshpy bottle and launch Java with JDWP debugger.
#
# Usage:
#   bash scripts/unbottle_and_debug.sh <bottle.tar.gz>
#
# The JVM starts suspended on port 5005. Attach VSCode debugger "Attach to Bottle" to continue.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
BOTTLES_DIR="$REPO_ROOT/joshpy-bottles/extracted"
COMPRESSED_DIR="$REPO_ROOT/joshpy-bottles/compressed"
FAT_JAR="$REPO_ROOT/build/libs/joshsim-fat.jar"
DEBUG_PORT=5005

# --- Validate input ---

if [[ $# -lt 1 ]]; then
  echo "Usage: bash scripts/unbottle_and_debug.sh <bottle.tar.gz>" >&2
  exit 1
fi

# Resolve bottle path: check as given, then try compressed/ directory
if [[ -f "$1" ]]; then
  BOTTLE_PATH="$(realpath "$1")"
elif [[ -f "$COMPRESSED_DIR/$1" ]]; then
  BOTTLE_PATH="$(realpath "$COMPRESSED_DIR/$1")"
elif [[ -f "$COMPRESSED_DIR/$(basename "$1")" ]]; then
  BOTTLE_PATH="$(realpath "$COMPRESSED_DIR/$(basename "$1")")"
else
  echo "Error: File not found: $1 (also checked $COMPRESSED_DIR/)" >&2
  exit 1
fi

if [[ "$BOTTLE_PATH" != *.tar.gz ]]; then
  echo "Error: Expected a .tar.gz file, got: $1" >&2
  exit 1
fi

# --- Check fat jar ---

if [[ ! -f "$FAT_JAR" ]]; then
  echo "Error: $FAT_JAR not found. Run ./gradlew fatJar first." >&2
  exit 1
fi

# --- Extract ---

mkdir -p "$BOTTLES_DIR"

# Peek at top-level directory name inside the archive (use awk to avoid SIGPIPE from head)
BOTTLE_DIR_NAME="$(tar -tzf "$BOTTLE_PATH" | awk -F/ '{print $1; exit}')"
EXTRACT_DIR="$BOTTLES_DIR/$BOTTLE_DIR_NAME"

if [[ -d "$EXTRACT_DIR" ]]; then
  echo "Already extracted: $EXTRACT_DIR"
else
  tar -xzf "$BOTTLE_PATH" -C "$BOTTLES_DIR"
  echo "Extracted: $EXTRACT_DIR"
fi

# --- Parse manifest ---

MANIFEST="$EXTRACT_DIR/manifest.json"

if [[ ! -f "$MANIFEST" ]]; then
  echo "Error: No manifest.json in bottle" >&2
  exit 1
fi

SIM_NAME="$(jq -r '.simulation' "$MANIFEST")"
RUN_HASH="$(jq -r '.run_hash' "$MANIFEST")"
PARAM_COUNT="$(jq '.parameters | length' "$MANIFEST")"
DATA_COUNT="$(jq '.original_data_paths | length' "$MANIFEST")"
BOTTLED_AT="$(jq -r '.bottled_at' "$MANIFEST")"

# --- Parse run.sh for --data and --custom-tag args ---

RUN_SH="$EXTRACT_DIR/run.sh"

if [[ ! -f "$RUN_SH" ]]; then
  echo "Error: No run.sh in bottle" >&2
  exit 1
fi

DATA_ARGS=()
while IFS= read -r line; do
  value="$(echo "$line" | sed 's/.*--data //' | sed 's/ *\\$//')"
  DATA_ARGS+=("--data" "$value")
done < <(grep -- '--data ' "$RUN_SH")

CUSTOM_TAG_ARGS=()
while IFS= read -r line; do
  value="$(echo "$line" | sed 's/.*--custom-tag //' | sed 's/ *\\$//')"
  CUSTOM_TAG_ARGS+=("--custom-tag" "$value")
done < <(grep -- '--custom-tag ' "$RUN_SH")

# --- Workaround: inject tags that joshpy SweepManager provides at runtime but
# --- doesn't include in run.sh. Remove once fixed in joshpy bottling. ---

# CUSTOM_TAG_ARGS+=("--custom-tag" "run_hash=$RUN_HASH")

# --- Print summary ---

echo ""
echo "=== Bottle Debug Session ==="
echo "Simulation:  $SIM_NAME"
echo "Run hash:    $RUN_HASH"
echo "Bottled at:  $BOTTLED_AT"
echo "Data files:  $DATA_COUNT"
echo "Parameters:  $PARAM_COUNT"
echo "Debug port:  $DEBUG_PORT (suspend=y)"
echo ""
echo "Waiting for debugger on port $DEBUG_PORT..."
echo "-> Attach VSCode debugger \"Attach to Bottle\" to continue"
echo ""

# --- Launch with JDWP ---

cd "$EXTRACT_DIR"

exec java \
  -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address="*:$DEBUG_PORT" \
  -ea \
  -Xmx4g \
  -jar "$FAT_JAR" \
  run \
  simulation.josh \
  "$SIM_NAME" \
  "${DATA_ARGS[@]}" \
  "${CUSTOM_TAG_ARGS[@]}"
