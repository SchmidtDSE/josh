#!/bin/bash
# Usage: extract_results.sh <test_dir> <test_id>

TEST_DIR=$1
TEST_ID=$2
ORGANISM_LOG="$TEST_DIR/debug_organism_0.txt"
PATCH_LOG="$TEST_DIR/debug_patch_0.txt"
SIM_LOG="$TEST_DIR/simulation.log"
RESULT_JSON="$TEST_DIR/result.json"

# Check if simulation completed
COMPLETED=$(grep -c "completed successfully" "$SIM_LOG" || echo "0")
if [ "$COMPLETED" -eq 0 ]; then
  echo '{"status": "failed", "error": "simulation did not complete"}' > "$RESULT_JSON"
  exit 1
fi

# Extract events by step
declare -A EVENTS_BY_STEP
TOTAL_EVENTS=0

if [ -f "$ORGANISM_LOG" ]; then
  for STEP in {0..10}; do
    COUNT=$(grep -c "^\[Step $STEP" "$ORGANISM_LOG" || echo "0")
    EVENTS_BY_STEP[$STEP]=$COUNT
    TOTAL_EVENTS=$((TOTAL_EVENTS + COUNT))
  done
fi

# Determine first and last execution steps
FIRST_STEP=-1
LAST_STEP=-1
for STEP in {0..10}; do
  if [ "${EVENTS_BY_STEP[$STEP]}" -gt 0 ]; then
    if [ $FIRST_STEP -eq -1 ]; then
      FIRST_STEP=$STEP
    fi
    LAST_STEP=$STEP
  fi
done

# Bug detection logic
BUG_PRESENT=false
BUG_TYPE="none"
BUG_DESC="Organisms execute at all steps after creation"

if [ $TOTAL_EVENTS -eq 0 ]; then
  BUG_PRESENT=true
  BUG_TYPE="never_executes"
  BUG_DESC="Organisms created but never execute .step logic"
elif [ $FIRST_STEP -eq $LAST_STEP ]; then
  BUG_PRESENT=true
  BUG_TYPE="creation_only"
  BUG_DESC="Organisms only execute at creation step (Step $FIRST_STEP)"
elif [ $((LAST_STEP - FIRST_STEP)) -lt 3 ]; then
  # If simulation ran for 5+ steps but organisms only executed for <3 steps
  MAX_STEP=$(grep "Progress.*step" "$SIM_LOG" | tail -1 | grep -oP 'step \d+' | grep -oP '\d+')
  if [ "$MAX_STEP" -ge 5 ] && [ $((LAST_STEP - FIRST_STEP)) -lt 3 ]; then
    BUG_PRESENT=true
    BUG_TYPE="execution_gap"
    BUG_DESC="Organisms do not execute at all steps (gap detected)"
  fi
fi

# Get runtime
RUNTIME=$(grep "completed" "$SIM_LOG" | grep -oP '\d+\.\d+' | tail -1 || echo "0")

# Count event types
TREE_INIT=$(grep -c "TREE_INIT\|ORG_INIT" "$ORGANISM_LOG" || echo "0")
ORG_STEP=$(grep -c "ORG_STEP" "$ORGANISM_LOG" || echo "0")

# Build events_by_step JSON
EVENTS_JSON="{"
for STEP in {0..10}; do
  if [ $STEP -gt 0 ]; then EVENTS_JSON+=","; fi
  EVENTS_JSON+="\"$STEP\":${EVENTS_BY_STEP[$STEP]}"
done
EVENTS_JSON+="}"

# Generate JSON
cat > "$RESULT_JSON" << JSON_EOF
{
  "test_id": "$TEST_ID",
  "timestamp": "$(date -Iseconds)",
  "status": "completed",

  "results": {
    "simulation_completed": true,
    "total_organism_events": $TOTAL_EVENTS,
    "events_by_step": $EVENTS_JSON,
    "event_types": {
      "TREE_INIT": $TREE_INIT,
      "ORG_STEP": $ORG_STEP
    },
    "bug_analysis": {
      "bug_present": $BUG_PRESENT,
      "bug_type": "$BUG_TYPE",
      "description": "$BUG_DESC",
      "first_execution_step": $FIRST_STEP,
      "last_execution_step": $LAST_STEP
    }
  },

  "performance": {
    "runtime_seconds": $RUNTIME,
    "organism_log_size_mb": $(du -m "$ORGANISM_LOG" 2>/dev/null | cut -f1 || echo "0")
  }
}
JSON_EOF

echo "Extracted results for $TEST_ID"
