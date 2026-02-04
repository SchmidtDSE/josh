#!/bin/bash
# License: BSD-3-Clause
# Generate a markdown report from JUnit XML test results.
#
# Usage: generate_conformance_report.sh <results_dir> <branch> <commit>

set -e

RESULTS_DIR="${1:-build/test-results/conformanceTest}"
BRANCH="${2:-unknown}"
COMMIT="${3:-unknown}"
REPORT_FILE="conformance-report.md"

echo "# Conformance Test Report" > "$REPORT_FILE"
echo "" >> "$REPORT_FILE"
echo "**Run:** $(date -u '+%Y-%m-%d %H:%M:%S UTC')" >> "$REPORT_FILE"
echo "**Branch:** $BRANCH" >> "$REPORT_FILE"
echo "**Commit:** $COMMIT" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"

if [ ! -d "$RESULTS_DIR" ]; then
  echo "No test results directory found." >> "$REPORT_FILE"
  cat "$REPORT_FILE"
  exit 0
fi

# Parse results
echo "## Summary" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"

PASSED=0
FAILED=0
TOTAL=0

for xml in "$RESULTS_DIR"/*.xml; do
  if [ -f "$xml" ]; then
    FILE_TESTS=$(grep -o 'tests="[0-9]*"' "$xml" | head -1 | grep -o '[0-9]*' || echo "0")
    FILE_FAILURES=$(grep -o 'failures="[0-9]*"' "$xml" | head -1 | grep -o '[0-9]*' || echo "0")
    FILE_ERRORS=$(grep -o 'errors="[0-9]*"' "$xml" | head -1 | grep -o '[0-9]*' || echo "0")

    FILE_FAILED=$((FILE_FAILURES + FILE_ERRORS))
    FILE_PASSED=$((FILE_TESTS - FILE_FAILED))

    PASSED=$((PASSED + FILE_PASSED))
    FAILED=$((FAILED + FILE_FAILED))
    TOTAL=$((TOTAL + FILE_TESTS))
  fi
done

if [ "$TOTAL" -gt 0 ]; then
  PASS_RATE=$(echo "scale=1; $PASSED * 100 / $TOTAL" | bc)
  echo "| Metric | Value |" >> "$REPORT_FILE"
  echo "|--------|-------|" >> "$REPORT_FILE"
  echo "| Total Tests | $TOTAL |" >> "$REPORT_FILE"
  echo "| Passing | $PASSED ($PASS_RATE%) |" >> "$REPORT_FILE"
  echo "| Failing | $FAILED |" >> "$REPORT_FILE"
  echo "" >> "$REPORT_FILE"
fi

# List failures if any
if [ "$FAILED" -gt 0 ]; then
  echo "## Failing Tests" >> "$REPORT_FILE"
  echo "" >> "$REPORT_FILE"

  for xml in "$RESULTS_DIR"/*.xml; do
    if [ -f "$xml" ]; then
      # Extract failed test names and their output
      python3 .github/scripts/parse_test_failures.py "$xml" >> "$REPORT_FILE"
    fi
  done
fi

# Output report
cat "$REPORT_FILE"
