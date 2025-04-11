#!/bin/bash

# Simple CLI interface for validating JOSH model configuration files
# Our docker build adds this to PATH, so it can be run from anywhere!

# Display usage information
function usage {
  echo "Usage: $0 [--supress-info] <path_to_model_file>"
  echo "  --supress-info    Run validation with minimal output"
  exit 1
}

# Parse arguments
supress-info=false
if [[ "$1" == "--supress-info" ]]; then
  supress-info=true
  shift
fi

if [[ "$1" == "--help" ]]; then
  usage
fi

# Check if file is provided
if [[ -z "$1" ]]; then
  echo "Error: No model file specified"
  usage
fi

model_file="$1"

# Check if file exists
if [[ ! -f "$model_file" ]]; then
  echo "Error: File '$model_file' not found"
  exit 1
fi

# Ensure JAR exists
if [[ ! -f build/libs/joshsim-fat.jar ]]; then
  echo "Building fat JAR..."
  gradle fatJar || { echo "Failed to build JAR"; exit 1; }
fi

# Run validation
echo "Validating: $model_file"
if [[ "$supress-info" == true ]]; then
  java -jar build/libs/joshsim-fat.jar validate --supress-info "$model_file"
else
  java -jar build/libs/joshsim-fat.jar validate "$model_file"
fi

exit_code=$?

# Print result
if [[ $exit_code -eq 0 ]]; then
  echo "Validation successful"
else
  echo "Validation failed (exit code: $exit_code)"
fi

exit $exit_code