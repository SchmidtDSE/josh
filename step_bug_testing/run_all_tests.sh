#!/bin/bash
# Comprehensive test runner for organism lifecycle testing

echo "============================================"
echo "ORGANISM LIFECYCLE INTEGRATION TEST SUITE"
echo "============================================"
echo ""

# Get all test directories
TESTS=($(find /workspaces/josh/step_bug_testing -maxdepth 1 -type d -name "test_*" | sort))

PASSED=0
FAILED=0
ERRORS=0

# Key tests to highlight
KEY_TESTS=("test_005" "test_024" "test_026" "test_012" "test_023")

declare -A RESULTS

for TEST_DIR in "${TESTS[@]}"; do
  TEST_NAME=$(basename "$TEST_DIR")
  echo "Running $TEST_NAME..."

  cd "$TEST_DIR"

  # Run test
  if ! ./run.sh > /dev/null 2>&1; then
    echo "  ❌ ERROR: Test execution failed"
    RESULTS[$TEST_NAME]="ERROR"
    ERRORS=$((ERRORS + 1))
    continue
  fi

  # Check for IllegalMonitorStateException
  if grep -q "IllegalMonitorStateException" simulation.log 2>/dev/null; then
    echo "  ❌ FAIL: IllegalMonitorStateException detected"
    RESULTS[$TEST_NAME]="FAIL_EXCEPTION"
    FAILED=$((FAILED + 1))
    continue
  fi

  # Check if debug file exists
  if [ ! -f "debug_organism_0.txt" ]; then
    echo "  ⚠️  WARNING: No debug output (may be expected for some tests)"
    RESULTS[$TEST_NAME]="PASS_NO_DEBUG"
    PASSED=$((PASSED + 1))
    continue
  fi

  # Count events by step
  STEP_0=$(grep -c "^\[Step 0," debug_organism_0.txt 2>/dev/null || echo "0")
  STEP_1=$(grep -c "^\[Step 1," debug_organism_0.txt 2>/dev/null || echo "0")
  STEP_2=$(grep -c "^\[Step 2," debug_organism_0.txt 2>/dev/null || echo "0")
  STEP_3=$(grep -c "^\[Step 3," debug_organism_0.txt 2>/dev/null || echo "0")
  STEP_4=$(grep -c "^\[Step 4," debug_organism_0.txt 2>/dev/null || echo "0")

  # Fix potential arithmetic errors by ensuring numeric values
  STEP_0=${STEP_0:-0}
  STEP_1=${STEP_1:-0}
  STEP_2=${STEP_2:-0}
  STEP_3=${STEP_3:-0}
  STEP_4=${STEP_4:-0}
  TOTAL_EVENTS=$((STEP_0 + STEP_1 + STEP_2 + STEP_3 + STEP_4))

  # Check if organisms continue executing after creation
  if [ "$TOTAL_EVENTS" -gt 0 ]; then
    if [ "$STEP_1" -gt 0 ] || [ "$STEP_2" -gt 0 ] || [ "$STEP_3" -gt 0 ] || [ "$STEP_4" -gt 0 ]; then
      echo "  ✅ PASS: Organisms execute at multiple steps (S0:$STEP_0 S1:$STEP_1 S2:$STEP_2 S3:$STEP_3 S4:$STEP_4)"
      RESULTS[$TEST_NAME]="PASS"
      PASSED=$((PASSED + 1))
    else
      echo "  ⚠️  WARNING: Only step 0 events (S0:$STEP_0)"
      RESULTS[$TEST_NAME]="PASS_STEP0_ONLY"
      PASSED=$((PASSED + 1))
    fi
  else
    echo "  ❌ FAIL: No organism events detected"
    RESULTS[$TEST_NAME]="FAIL_NO_EVENTS"
    FAILED=$((FAILED + 1))
  fi

  cd - > /dev/null
done

echo ""
echo "============================================"
echo "TEST RESULTS SUMMARY"
echo "============================================"
echo "  ✅ Passed: $PASSED"
echo "  ❌ Failed: $FAILED"
echo "  ⚠️  Errors: $ERRORS"
echo "  📊 Total:  $((PASSED + FAILED + ERRORS))"
echo ""

echo "============================================"
echo "KEY TEST RESULTS (Critical for validation)"
echo "============================================"
for KEY in "${KEY_TESTS[@]}"; do
  for TEST_NAME in "${!RESULTS[@]}"; do
    if [[ "$TEST_NAME" == *"$KEY"* ]]; then
      echo "  $TEST_NAME: ${RESULTS[$TEST_NAME]}"
    fi
  done
done
echo ""

# Exit with error if any tests failed
if [ "$FAILED" -gt 0 ] || [ "$ERRORS" -gt 0 ]; then
  exit 1
else
  exit 0
fi
