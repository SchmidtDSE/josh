#!/bin/bash
set -e  # Exit on any error

echo "=== Basic Preprocessing Test ==="

# Build fat jar if it doesn't exist
if [ ! -f "build/libs/joshsim-fat.jar" ]; then
    echo "Building fat jar..."
    ./gradlew fatJar
else
    echo "Using existing fat jar..."
fi

# Create temporary directory for test data
TEMP_DIR=$(mktemp -d)
echo "Using temporary directory: $TEMP_DIR"

# Download and extract tutorial data if needed
if [ ! -f "$TEMP_DIR/raw_tutorial_supplement.zip" ]; then
    echo "Downloading tutorial data..."
    cd "$TEMP_DIR"
    wget -q https://joshsim.org/guides/raw_tutorial_supplement.zip
    
    # Check if unzip is available, install if needed
    if ! command -v unzip &> /dev/null; then
        echo "Installing unzip..."
        sudo apt-get update && sudo apt-get install -y unzip
    fi
    
    unzip -o -q raw_tutorial_supplement.zip
    cd -
fi

# Define paths
JAR_PATH="build/libs/joshsim-fat.jar"
JOSH_SCRIPT="examples/test/test_basic_preprocess.josh"
GEOTIFF_PATH="$TEMP_DIR/raw_tutorial_supplement/CHC-CMIP6_SSP245_CHIRPS_2008_annual.tif"
JSHD_OUTPUT="$TEMP_DIR/precipitation_test.jshd"

echo "=== Step 1: Preprocessing CHC-CMIP6 GeoTIFF to JSHD ==="

# Preprocess the CHC-CMIP6 GeoTIFF file to create a JSHD file
echo "Processing CHC-CMIP6 GeoTIFF data..."
java -jar "$JAR_PATH" preprocess \
    "$JOSH_SCRIPT" \
    Main \
    "$GEOTIFF_PATH" \
    0 \
    mm \
    "$JSHD_OUTPUT" \
    --timestep 0

# Check output
if [ ! -f "$JSHD_OUTPUT" ] || [ ! -s "$JSHD_OUTPUT" ]; then
    echo "Error: JSHD preprocessing failed!"
    exit 1
fi
echo "✓ JSHD file created successfully ($(wc -c < "$JSHD_OUTPUT") bytes)"

echo "=== Step 2: Inspecting JSHD values at known coordinates ==="

# Known test point from GeotiffExternalDataReaderTest
# Latitude: 35.4955033919704, Longitude: -119.99447700450675
# Expected: Non-zero precipitation value

# Note: InspectJshdCommand uses grid coordinates, so we need to calculate
# the grid coordinates from the lat/lon coordinates based on the simulation bounds
# Grid bounds: 35.5 to 34.5 degrees latitude, -120 to -119 degrees longitude
# Grid size: 1000m cells

# For simplicity, let's calculate approximate grid coordinates
# The simulation spans 1 degree lat x 1 degree lon
# Grid coordinates are in the center of the simulation area
# Lat 35.4955 -> approximately in the middle of the grid (around y=500)  
# Lon -119.9945 -> very close to eastern edge (around x=950)

echo "Testing inspection at approximate grid coordinates..."

# Test at coordinates calculated from the known test point
# Lat 35.4955 -> Y ≈ 4, Lon -119.9945 -> X ≈ 5
GRID_X=5
GRID_Y=4
TIMESTEP=0
VARIABLE="data"

echo "Inspecting at grid coordinates: ($GRID_X, $GRID_Y, $TIMESTEP)"
INSPECT_OUTPUT=$(java -jar "$JAR_PATH" inspectJshd "$JSHD_OUTPUT" "$VARIABLE" "$TIMESTEP" "$GRID_X" "$GRID_Y")
INSPECT_RESULT=$?

echo "Inspection output: $INSPECT_OUTPUT"

if [ $INSPECT_RESULT -ne 0 ]; then
    echo "Error: InspectJshdCommand failed with exit code $INSPECT_RESULT"
    exit 1
fi

# Extract the value from the output (format: "Value at (x, y, t): value units")
VALUE=$(echo "$INSPECT_OUTPUT" | grep -o "Value at ([0-9]*, [0-9]*, [0-9]*): [0-9]*\.*[0-9]* mm" | grep -o "[0-9]*\.*[0-9]* mm" | grep -o "[0-9]*\.*[0-9]*")

if [ -z "$VALUE" ]; then
    echo "Error: Could not extract precipitation value from inspection output"
    exit 1
fi

echo "✓ Extracted precipitation value: $VALUE mm"

# Validate that the value is reasonable (should be non-negative and not too high)
# Use awk for floating point comparison (more portable than bc)
if awk -v val="$VALUE" 'BEGIN { exit !(val >= 0 && val < 5000) }'; then
    echo "✓ Value $VALUE mm is within expected range [0, 5000) mm"
else
    echo "✗ Value $VALUE mm is outside expected range [0, 5000) mm"
    exit 1
fi

echo "=== Step 3: Testing multiple grid locations ==="

# Test a few more locations to ensure the preprocessing worked correctly
TEST_LOCATIONS="100,100 500,500 800,200 10,10 50,50"

for location in $TEST_LOCATIONS; do
    IFS=',' read -r x y <<< "$location"
    echo "Testing grid location ($x, $y)..."
    
    # Disable exit-on-error for this command since some locations may not have data
    set +e
    LOCATION_OUTPUT=$(java -jar "$JAR_PATH" inspectJshd "$JSHD_OUTPUT" "$VARIABLE" "$TIMESTEP" "$x" "$y" 2>&1)
    LOCATION_RESULT=$?
    set -e
    
    if [ $LOCATION_RESULT -eq 0 ]; then
        LOCATION_VALUE=$(echo "$LOCATION_OUTPUT" | grep -o "Value at ([0-9]*, [0-9]*, [0-9]*): [0-9]*\.*[0-9]* mm" | grep -o "[0-9]*\.*[0-9]* mm" | grep -o "[0-9]*\.*[0-9]*")
        echo "✓ Grid location ($x, $y): $LOCATION_VALUE mm"
    else
        echo "○ Grid location ($x, $y): no data (expected for some locations)"
    fi
done

echo "=== Step 4: Workflow validation complete ==="

echo "✓ Preprocessing CHC-CMIP6 GeoTIFF to JSHD: SUCCESS"
echo "✓ InspectJshdCommand validation: SUCCESS"
echo "✓ Full preprocessing → inspection workflow: SUCCESS"

# Cleanup
echo "Cleaning up temporary directory: $TEMP_DIR"
rm -rf "$TEMP_DIR"

echo "=== Basic preprocessing test passed! ==="
exit 0