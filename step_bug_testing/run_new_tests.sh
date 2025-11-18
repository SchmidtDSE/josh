#!/bin/bash
# Run all new tests (013-026)

echo "Running new test suite..."
echo "========================="
echo ""

TESTS=(
  "test_013_separate_collection_minimal_none_tiny"
  "test_017_filter_in_end_minimal_none_tiny"
  "test_018_count_in_end_minimal_none_tiny"
  "test_019_end_different_collection_minimal_none_tiny"
  "test_020_create_at_step_0_minimal_none_tiny"
  "test_021_unconditional_step_minimal_none_tiny"
  "test_023_two_collections_both_end_minimal_none_tiny"
  "test_024_one_with_end_one_without_minimal_none_tiny"
  "test_026_prior_only_end_minimal_none_tiny"
)

PASSED=0
FAILED=0
ERRORS=0

for TEST_NAME in "${TESTS[@]}"; do
  echo "Running $TEST_NAME..."
  cd "/workspaces/josh/step_bug_testing/$TEST_NAME"

  if ! ./run.sh > /dev/null 2>&1; then
    echo "  ❌ ERROR: Test execution failed"
    ERRORS=$((ERRORS + 1))
    continue
  fi

  # Check if debug file exists and has organism events
  if [ ! -f "debug_organism_0.txt" ]; then
    echo "  ❌ ERROR: No debug output generated"
    ERRORS=$((ERRORS + 1))
    continue
  fi

  # Count events by step
  STEP_0=$(grep -c "^\[Step 0," debug_organism_0.txt 2>/dev/null || echo "0")
  STEP_1=$(grep -c "^\[Step 1," debug_organism_0.txt 2>/dev/null || echo "0")
  STEP_2=$(grep -c "^\[Step 2," debug_organism_0.txt 2>/dev/null || echo "0")
  STEP_3=$(grep -c "^\[Step 3," debug_organism_0.txt 2>/dev/null || echo "0")
  STEP_4=$(grep -c "^\[Step 4," debug_organism_0.txt 2>/dev/null || echo "0")

  # Check if organisms continue executing after step 0
  if [ "$STEP_1" -gt 0 ] && [ "$STEP_2" -gt 0 ]; then
    echo "  ✅ PASS: Organisms execute at multiple steps (S0:$STEP_0 S1:$STEP_1 S2:$STEP_2)"
    PASSED=$((PASSED + 1))
  else
    echo "  ❌ FAIL: Organisms stop after creation (S0:$STEP_0 S1:$STEP_1 S2:$STEP_2)"
    FAILED=$((FAILED + 1))
  fi

  cd - > /dev/null
done

echo ""
echo "========================="
echo "Test Results Summary:"
echo "  Passed: $PASSED"
echo "  Failed: $FAILED"
echo "  Errors: $ERRORS"
echo "  Total:  $((PASSED + FAILED + ERRORS))"
