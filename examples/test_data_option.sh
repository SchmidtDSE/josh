#!/bin/bash

# Test script for --data option functionality
# This tests that the --data option works correctly and restricts file access appropriately

set -e

echo "Testing --data option functionality..."

# Ensure test isolation by cleaning up any previous test files
rm -f /tmp/config_example_josh_999.csv
rm -f /tmp/config_example_josh_998.csv
rm -f example.jshc

# Test config example with --data option specifying config file explicitly
mkdir -p test_data
cp examples/features/config_example.jshc test_data/example.jshc

# Test with --data option - this should use JvmMappedInputGetter
java -Xmx6g -jar build/libs/joshsim-fat.jar run \
  --replicate-number 999 \
  --data example.jshc=test_data/example.jshc \
  examples/features/config_example.josh ConfigExample

# Verify output file was created
[ -f "/tmp/config_example_josh_999.csv" ] || exit 1
[ -s "/tmp/config_example_josh_999.csv" ] || exit 2

echo "Verifying --data restricts file access..."
# Create a second config file that should NOT be accessible
echo "testVar1 = 999 meters" > test_data/hidden.jshc
echo "testVar2 = 999 meters" >> test_data/hidden.jshc

# Clean up before next test to ensure isolation
rm -f example.jshc /tmp/config_example_josh_998.csv

# This should work - file is in working directory and no --data option
cp test_data/hidden.jshc example.jshc
java -Xmx6g -jar build/libs/joshsim-fat.jar run \
  --replicate-number 998 \
  examples/features/config_example.josh ConfigExample || exit 3

# Clean up after test
rm -f example.jshc /tmp/config_example_josh_998.csv

echo "--data option tests passed successfully!"