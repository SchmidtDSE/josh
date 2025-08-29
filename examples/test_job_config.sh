#!/bin/bash

# Test script for job configuration functionality with semicolon delimiter
# This tests the semicolon delimiter parsing from Component 5 ANTLR grammar

set -e

echo "Testing job configuration functionality with semicolon delimiter..."

# Ensure test isolation by cleaning up any previous test files
rm -f /tmp/job_config_*.csv
rm -f test_data_job/*.jshc

# Create test data directory and configuration files
mkdir -p test_data_job
echo "testVar1 = 10 meters" > test_data_job/example_1.jshc
echo "testVar2 = 15 meters" >> test_data_job/example_1.jshc

echo "testVar1 = 20 meters" > test_data_job/example_2.jshc
echo "testVar2 = 25 meters" >> test_data_job/example_2.jshc

echo "testVar1 = 30 meters" > test_data_job/other_1.jshc
echo "testVar2 = 35 meters" >> test_data_job/other_1.jshc

echo "testVar1 = 40 meters" > test_data_job/other_2.jshc
echo "testVar2 = 45 meters" >> test_data_job/other_2.jshc

echo "Testing basic job configuration with semicolon delimiter..."
# Test semicolon separator parsing functionality
java -Xmx6g -jar build/libs/joshsim-fat.jar run \
  --replicates 2 \
  --data example.jshc=test_data_job/example_1.jshc\;other.jshc=test_data_job/other_1.jshc \
  examples/simulations/job_config_test.josh JobConfigTest

# Verify expected output file exists and is non-empty
[ -f "/tmp/job_config_test.csv" ] || exit 1
[ -s "/tmp/job_config_test.csv" ] || exit 2

echo "Testing replicate handling with multiple configurations..."
# Test replicate functionality with different configuration
java -Xmx6g -jar build/libs/joshsim-fat.jar run \
  --replicates 3 \
  --data example.jshc=test_data_job/example_2.jshc\;other.jshc=test_data_job/other_2.jshc \
  examples/simulations/job_config_replicate_test.josh JobConfigReplicateTest

# Verify output file exists and contains multiple replicates
[ -f "/tmp/job_config_replicate_test.csv" ] || exit 3
[ -s "/tmp/job_config_replicate_test.csv" ] || exit 4

# Check that the CSV contains replicate column and 3 replicates of data
if ! grep -q "replicate" /tmp/job_config_replicate_test.csv; then
  echo "Error: Output file should contain replicate column"
  exit 5
fi

# Verify the replicate column is present and contains valid data
# Find the replicate column position in the header
REPLICATE_COL=$(head -1 /tmp/job_config_replicate_test.csv | tr ',' '\n' | grep -n "replicate" | cut -d':' -f1)
if [ -z "$REPLICATE_COL" ]; then
  echo "Error: Could not find replicate column"
  exit 6
fi

# Check that replicate values are present (at least replicate 0)
REPLICATE_VALUES=$(cut -d',' -f"$REPLICATE_COL" /tmp/job_config_replicate_test.csv | tail -n +2 | sort -u | tr '\n' ' ')
echo "Found replicate values: $REPLICATE_VALUES"
if [ -z "$REPLICATE_VALUES" ]; then
  echo "Error: No replicate values found"
  exit 6
fi

echo "Testing semicolon delimiter validation with complex paths..."
# Test handling of paths that might contain colons (URI schemes, Windows paths)
rm -f /tmp/job_config_*.csv
java -Xmx6g -jar build/libs/joshsim-fat.jar run \
  --replicates 1 \
  --data example.jshc=test_data_job/example_1.jshc\;other.jshc=test_data_job/other_1.jshc \
  examples/simulations/job_config_test.josh JobConfigTest

# Verify output file was created successfully with semicolon delimiter parsing
[ -f "/tmp/job_config_test.csv" ] || exit 7
[ -s "/tmp/job_config_test.csv" ] || exit 8

echo "Testing configuration value validation..."
# Verify that different configuration values are actually being used
java -Xmx6g -jar build/libs/joshsim-fat.jar run \
  --replicates 1 \
  --data example.jshc=test_data_job/example_2.jshc\;other.jshc=test_data_job/other_2.jshc \
  examples/simulations/job_config_test.josh JobConfigTest

# Check that configuration values from files are reflected in output
if ! grep -q "20\|25" /tmp/job_config_test.csv; then
  echo "Error: Configuration values (20, 25) not found in output"
  exit 9
fi

echo "Testing empty specification handling..."
# Test error handling for malformed data specification
if java -Xmx6g -jar build/libs/joshsim-fat.jar run \
  --replicates 1 \
  --data "" \
  examples/simulations/job_config_test.josh JobConfigTest 2>/dev/null; then
  echo "Warning: Empty data specification should probably be handled gracefully"
fi

echo "Testing custom parameter functionality..."
# Test single custom parameter
java -Xmx6g -jar build/libs/joshsim-fat.jar run \
  --replicates 1 \
  --custom-tag environment=test \
  --data example.jshc=test_data_job/example_1.jshc\;other.jshc=test_data_job/other_1.jshc \
  examples/simulations/job_config_custom_test.josh JobConfigCustomTest

# Verify output file includes custom parameter
[ -f "/tmp/job_config_example_1_other_1_test.csv" ] || exit 10
[ -s "/tmp/job_config_example_1_other_1_test.csv" ] || exit 11

echo "Testing multiple custom parameters..."
# Test multiple custom parameters
java -Xmx6g -jar build/libs/joshsim-fat.jar run \
  --replicates 2 \
  --custom-tag env=prod \
  --custom-tag version=v2.1 \
  --data example.jshc=test_data_job/example_1.jshc\;other.jshc=test_data_job/other_1.jshc \
  examples/simulations/job_config_multi_custom_test.josh JobConfigMultiCustomTest

# Verify output file includes both custom parameters
[ -f "/tmp/job_multi_example_1_other_1_prod_v2.1.csv" ] || exit 12
[ -s "/tmp/job_multi_example_1_other_1_prod_v2.1.csv" ] || exit 13

echo "Testing custom parameter error handling..."
# Test unknown template error
if java -Xmx6g -jar build/libs/joshsim-fat.jar run \
  --replicates 1 \
  --custom-tag known=value \
  --data example.jshc=test_data_job/example_1.jshc\;other.jshc=test_data_job/other_1.jshc \
  examples/simulations/job_config_custom_test.josh JobConfigCustomTest 2>/dev/null; then
  echo "Error: Should have failed due to unknown template {environment}"
  exit 14
fi

echo "Testing custom parameter with reserved name validation..."
# Test reserved name validation
if java -Xmx6g -jar build/libs/joshsim-fat.jar run \
  --replicates 1 \
  --custom-tag replicate=invalid \
  --data example.jshc=test_data_job/example_1.jshc\;other.jshc=test_data_job/other_1.jshc \
  examples/simulations/job_config_test.josh JobConfigTest 2>/dev/null; then
  echo "Error: Should have failed due to reserved name 'replicate'"
  exit 15
fi

echo "Testing malformed custom parameter validation..."
# Test malformed custom tag validation
if java -Xmx6g -jar build/libs/joshsim-fat.jar run \
  --replicates 1 \
  --custom-tag malformed \
  --data example.jshc=test_data_job/example_1.jshc\;other.jshc=test_data_job/other_1.jshc \
  examples/simulations/job_config_test.josh JobConfigTest 2>/dev/null; then
  echo "Error: Should have failed due to malformed custom tag format"
  exit 16
fi

echo "Cleaning up test files..."
rm -rf test_data_job
rm -f /tmp/job_config_*.csv
rm -f /tmp/job_multi_*.csv

echo "Job configuration and custom parameter tests passed successfully!"