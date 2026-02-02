#!/bin/bash
# License: BSD-3-Clause
# Parse JUnit XML test results and output counts to GITHUB_OUTPUT.
#
# Usage: parse_conformance_results.sh <results_dir>

set -e

RESULTS_DIR="${1:-build/test-results/conformanceTest}"

if [ ! -d "$RESULTS_DIR" ]; then
  echo "No test results found"
  echo "summary=No test results found" >> "$GITHUB_OUTPUT"
  exit 0
fi

# Count test results from XML files
PASSED=0
FAILED=0
SKIPPED=0
FAILURES=""

for xml in "$RESULTS_DIR"/*.xml; do
  if [ -f "$xml" ]; then
    # Extract counts from XML
    FILE_TESTS=$(grep -o 'tests="[0-9]*"' "$xml" | head -1 | grep -o '[0-9]*' || echo "0")
    FILE_FAILURES=$(grep -o 'failures="[0-9]*"' "$xml" | head -1 | grep -o '[0-9]*' || echo "0")
    FILE_ERRORS=$(grep -o 'errors="[0-9]*"' "$xml" | head -1 | grep -o '[0-9]*' || echo "0")
    FILE_SKIPPED=$(grep -o 'skipped="[0-9]*"' "$xml" | head -1 | grep -o '[0-9]*' || echo "0")

    FILE_FAILED=$((FILE_FAILURES + FILE_ERRORS))
    FILE_PASSED=$((FILE_TESTS - FILE_FAILED - FILE_SKIPPED))

    PASSED=$((PASSED + FILE_PASSED))
    FAILED=$((FAILED + FILE_FAILED))
    SKIPPED=$((SKIPPED + FILE_SKIPPED))

    # Extract failure details
    if [ "$FILE_FAILED" -gt 0 ]; then
      # Get test names that failed
      FAILED_TESTS=$(grep -oP 'testcase name="\K[^"]+' "$xml" | while read test; do
        if grep -q "testcase name=\"$test\"" "$xml" && grep -A1 "testcase name=\"$test\"" "$xml" | grep -q "<failure\|<error"; then
          echo "$test"
        fi
      done)
      if [ -n "$FAILED_TESTS" ]; then
        FAILURES="$FAILURES$FAILED_TESTS"$'\n'
      fi
    fi
  fi
done

TOTAL=$((PASSED + FAILED + SKIPPED))

if [ "$TOTAL" -eq 0 ]; then
  echo "summary=No tests executed" >> "$GITHUB_OUTPUT"
  exit 0
fi

PASS_RATE=$(echo "scale=1; $PASSED * 100 / $TOTAL" | bc)

# Create summary
SUMMARY="**Conformance Tests:** $PASSED/$TOTAL passing ($PASS_RATE%)"
if [ "$FAILED" -gt 0 ]; then
  SUMMARY="$SUMMARY | $FAILED failing"
fi
if [ "$SKIPPED" -gt 0 ]; then
  SUMMARY="$SUMMARY | $SKIPPED skipped"
fi

echo "summary=$SUMMARY" >> "$GITHUB_OUTPUT"
echo "passed=$PASSED" >> "$GITHUB_OUTPUT"
echo "failed=$FAILED" >> "$GITHUB_OUTPUT"
echo "total=$TOTAL" >> "$GITHUB_OUTPUT"
echo "pass_rate=$PASS_RATE" >> "$GITHUB_OUTPUT"
