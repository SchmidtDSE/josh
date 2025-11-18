#!/bin/bash
# Automated Test Runner for Organism Lifecycle Regression Suite
# Component 1.2 from Integration Testing Workflow Plan
#
# Usage: ./run_regression_suite.sh [--quick | --full]
#   --quick : Run critical tests only
#   --full  : Run all tests (default)

set -e

# Color codes for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Test categories
CRITICAL_TESTS=(
  "test_005_init_step_end_minimal_none_tiny"
  "test_012_init_step_end_conditional_minimal_none_tiny"
  "test_023_two_collections_both_end_minimal_none_tiny"
  "test_024_one_with_end_one_without_minimal_none_tiny"
  "test_026_prior_only_end_minimal_none_tiny"
)

NESTED_ORGANISM_TESTS=(
  # Placeholder for future nested organism tests
  # "test_parent_lineage"
  # "test_parent_geokey"
  # "test_parent_synthetic"
)

# Determine test mode
MODE="full"
if [ "$#" -gt 0 ]; then
  if [ "$1" == "--quick" ]; then
    MODE="quick"
  elif [ "$1" == "--full" ]; then
    MODE="full"
  else
    echo "Usage: $0 [--quick | --full]"
    exit 1
  fi
fi

# Build test list based on mode
if [ "$MODE" == "quick" ]; then
  echo "Running CRITICAL TESTS ONLY"
  TESTS_TO_RUN=("${CRITICAL_TESTS[@]}")
else
  echo "Running ALL TESTS"
  # Find all test directories matching test_[0-9]* pattern
  TESTS_TO_RUN=($(find /workspaces/josh/step_bug_testing -maxdepth 1 -type d -name "test_[0-9]*" | sort | xargs -n1 basename))
fi

# Initialize counters
PASSED=0
FAILED=0
SKIPPED=0
TOTAL=0

# Store results for summary
declare -A RESULTS

echo "============================================"
echo "ORGANISM LIFECYCLE REGRESSION TEST SUITE"
echo "============================================"
echo "Mode: ${MODE}"
echo "Tests to run: ${#TESTS_TO_RUN[@]}"
echo ""

# Run each test
for TEST_NAME in "${TESTS_TO_RUN[@]}"; do
  TOTAL=$((TOTAL + 1))
  TEST_DIR="/workspaces/josh/step_bug_testing/${TEST_NAME}"

  # Check if test directory exists
  if [ ! -d "$TEST_DIR" ]; then
    echo -e "${YELLOW}⚠️  SKIP${NC}: $TEST_NAME (directory not found)"
    RESULTS[$TEST_NAME]="SKIP"
    SKIPPED=$((SKIPPED + 1))
    continue
  fi

  echo -n "Testing $TEST_NAME ... "

  # Change to test directory
  cd "$TEST_DIR"

  # Clean up previous run outputs
  rm -f debug_organism_0.txt debug_patch_0.txt simulation.log 2>/dev/null || true

  # Run simulation
  if ! bash run.sh > /dev/null 2>&1; then
    echo -e "${RED}❌ FAIL${NC} (execution error)"
    RESULTS[$TEST_NAME]="FAIL"
    FAILED=$((FAILED + 1))
    cd - > /dev/null
    continue
  fi

  # Check for organism output file
  if [ ! -f "debug_organism_0.txt" ]; then
    echo -e "${YELLOW}⚠️  SKIP${NC} (no organism output)"
    RESULTS[$TEST_NAME]="SKIP"
    SKIPPED=$((SKIPPED + 1))
    cd - > /dev/null
    continue
  fi

  # Validate organism execution at multiple timesteps
  # Extract unique steps and count them
  STEP_COUNT=$(grep "^\[Step" debug_organism_0.txt | awk '{print $1, $2}' | sort -u | wc -l)

  # Test validation logic
  if [ "$STEP_COUNT" -ge 2 ]; then
    echo -e "${GREEN}✅ PASS${NC} (step_count=$STEP_COUNT)"
    RESULTS[$TEST_NAME]="PASS"
    PASSED=$((PASSED + 1))
  elif [ "$STEP_COUNT" -eq 1 ]; then
    echo -e "${RED}❌ FAIL${NC} (only 1 timestep detected)"
    RESULTS[$TEST_NAME]="FAIL"
    FAILED=$((FAILED + 1))
  else
    echo -e "${YELLOW}⚠️  SKIP${NC} (no timestep data)"
    RESULTS[$TEST_NAME]="SKIP"
    SKIPPED=$((SKIPPED + 1))
  fi

  cd - > /dev/null
done

# Print summary
echo ""
echo "============================================"
echo "TEST RESULTS SUMMARY"
echo "============================================"
echo -e "${GREEN}✅ Passed:${NC}  $PASSED"
echo -e "${RED}❌ Failed:${NC}  $FAILED"
echo -e "${YELLOW}⚠️  Skipped:${NC} $SKIPPED"
echo "📊 Total:   $TOTAL"
echo ""

# Show critical test results if running full suite
if [ "$MODE" == "full" ] && [ "${#CRITICAL_TESTS[@]}" -gt 0 ]; then
  echo "============================================"
  echo "CRITICAL TEST RESULTS"
  echo "============================================"
  for CRITICAL_TEST in "${CRITICAL_TESTS[@]}"; do
    if [ -n "${RESULTS[$CRITICAL_TEST]}" ]; then
      RESULT="${RESULTS[$CRITICAL_TEST]}"
      case "$RESULT" in
        "PASS")
          echo -e "${GREEN}✅ PASS${NC}: $CRITICAL_TEST"
          ;;
        "FAIL")
          echo -e "${RED}❌ FAIL${NC}: $CRITICAL_TEST"
          ;;
        "SKIP")
          echo -e "${YELLOW}⚠️  SKIP${NC}: $CRITICAL_TEST"
          ;;
      esac
    else
      echo -e "${YELLOW}⚠️  SKIP${NC}: $CRITICAL_TEST (not run)"
    fi
  done
  echo ""
fi

# Exit with appropriate code
if [ "$FAILED" -gt 0 ]; then
  echo "❌ Test suite FAILED"
  exit 1
else
  echo "✅ Test suite PASSED"
  exit 0
fi
