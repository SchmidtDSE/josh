#!/bin/bash
# Run assertion-based test

cd "$(dirname "$0")"

echo "Running assertion-based test..."
echo "================================"

# Clean up any previous outputs
rm -f simulation.log 2>/dev/null

# Run the simulation - assertions will be checked automatically
if java -jar ../../build/libs/joshsim-fat.jar run test_assertion_based.josh TestSimulation; then
  echo ""
  echo "✅ PASS - All assertions succeeded"
  exit 0
else
  echo ""
  echo "❌ FAIL - One or more assertions failed"
  exit 1
fi
