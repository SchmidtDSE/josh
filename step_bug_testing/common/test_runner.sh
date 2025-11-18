#!/bin/bash

TESTS_DIR="/workspaces/josh/step_bug_testing"
LOG_FILE="$TESTS_DIR/runner.log"

echo "Starting exhaustive test run: $(date)" | tee -a "$LOG_FILE"

TOTAL_TESTS=0
COMPLETED=0
FAILED=0
TIMEOUT=0

# Find all test directories (test_NNN_*)
for TEST_DIR in "$TESTS_DIR"/test_*; do
  if [ ! -d "$TEST_DIR" ]; then continue; fi

  TEST_ID=$(basename "$TEST_DIR")
  TOTAL_TESTS=$((TOTAL_TESTS + 1))

  echo "" | tee -a "$LOG_FILE"
  echo "=== Running $TEST_ID ===" | tee -a "$LOG_FILE"
  echo "Progress: $COMPLETED/$TOTAL_TESTS completed" | tee -a "$LOG_FILE"

  # Run test with timeout (10 minutes = 600 seconds)
  cd "$TEST_DIR"
  timeout 600 ./run.sh > simulation.log 2>&1
  EXIT_CODE=$?

  if [ $EXIT_CODE -eq 124 ]; then
    echo "TIMEOUT: $TEST_ID" | tee -a "$LOG_FILE"
    TIMEOUT=$((TIMEOUT + 1))
    echo '{"status": "timeout"}' > result.json
  elif [ $EXIT_CODE -ne 0 ]; then
    echo "FAILED: $TEST_ID (exit code: $EXIT_CODE)" | tee -a "$LOG_FILE"
    FAILED=$((FAILED + 1))
    echo '{"status": "failed"}' > result.json
  else
    echo "COMPLETED: $TEST_ID" | tee -a "$LOG_FILE"
    COMPLETED=$((COMPLETED + 1))

    # Extract results to JSON
    ../../common/extract_results.sh "$TEST_DIR" "$TEST_ID"
  fi

  cd "$TESTS_DIR"
done

echo "" | tee -a "$LOG_FILE"
echo "=== Test Run Complete: $(date) ===" | tee -a "$LOG_FILE"
echo "Total: $TOTAL_TESTS | Completed: $COMPLETED | Failed: $FAILED | Timeout: $TIMEOUT" | tee -a "$LOG_FILE"
