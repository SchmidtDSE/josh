#!/bin/bash
# Usage: generate_data.sh <grid_size> <output_dir>
# Generates synthetic Presence, Temperature, Precipitation jshd files

GRID_SIZE=$1
OUTPUT_DIR=$2

# Generate Presence.jshd (simple grid with presence values)
java -jar build/libs/joshsim-fat.jar run \
  examples/test/test_basic_preprocess.josh GeneratePresence \
  --custom-tag="gridSize=$GRID_SIZE" \
  --custom-tag="output=$OUTPUT_DIR/Presence.jshd"

# Generate Temperature.jshd
java -jar build/libs/joshsim-fat.jar run \
  examples/test/test_basic_preprocess.josh GenerateTemperature \
  --custom-tag="gridSize=$GRID_SIZE" \
  --custom-tag="output=$OUTPUT_DIR/Temperature.jshd"

# Generate Precipitation.jshd
java -jar build/libs/joshsim-fat.jar run \
  examples/test/test_basic_preprocess.josh GeneratePrecipitation \
  --custom-tag="gridSize=$GRID_SIZE" \
  --custom-tag="output=$OUTPUT_DIR/Precipitation.jshd"
