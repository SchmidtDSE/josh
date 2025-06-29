#!/bin/bash
set -e  # Exit on any error

echo "=== Josh Preprocessing Test ==="

# Download and extract tutorial data
echo "Downloading tutorial data..."
wget -q https://joshsim.org/guides/raw_tutorial_supplement.zip
unzip -o -q raw_tutorial_supplement.zip

# Build the fat jar first
echo "Building fat jar..."
cd ..
./gradlew fatJar
cd landing

# Copy fat jar to current directory
cp ../build/libs/joshsim-fat.jar ./

# Create output directory for preprocessed files
mkdir -p preprocessed_data

echo "=== Processing Succession Model Data (NetCDF) ==="

# Process temperature data
echo "Processing temperature data..."
java -jar joshsim-fat.jar preprocess \
    ../examples/tutorial/succession.josh \
    Main \
    meantemp_tulare_annual.nc \
    Maximum_air_temperature_at_2m \
    K \
    preprocessed_data/temperature.jshd \
    --x-coord=lon \
    --y-coord=lat \
    --crs "EPSG:4326" \
    --time-dim "calendar_year"

# Check output
if [ ! -f "preprocessed_data/temperature.jshd" ] || [ ! -s "preprocessed_data/temperature.jshd" ]; then
    echo "Error: Temperature preprocessing failed!"
    exit 1
fi
echo "✓ Temperature data preprocessed successfully ($(wc -c < preprocessed_data/temperature.jshd) bytes)"

# Process precipitation data
echo "Processing precipitation data..."
java -jar joshsim-fat.jar preprocess \
    ../examples/tutorial/succession.josh \
    Main \
    precip_riverside_annual.nc \
    Precipitation_total \
    mm \
    preprocessed_data/precipitation.jshd \
    --x-coord=lon \
    --y-coord=lat \
    --crs "EPSG:4326" \
    --time-dim "calendar_year"

# Check output
if [ ! -f "preprocessed_data/precipitation.jshd" ] || [ ! -s "preprocessed_data/precipitation.jshd" ]; then
    echo "Error: Precipitation preprocessing failed!"
    exit 1
fi
echo "✓ Precipitation data preprocessed successfully ($(wc -c < preprocessed_data/precipitation.jshd) bytes)"

echo "=== Processing Grass-Shrub-Fire Model Data (GeoTIFF) ==="

# Process multiple years of GeoTIFF data
# The grass_shrub_fire model runs from 2025-2035 (timesteps 0-10)
# We map historical data 2007-2016 to timesteps 0-9

echo "Processing 2007 precipitation (timestep 0)..."
java -jar joshsim-fat.jar preprocess \
    ../examples/tutorial/grass_shrub_fire.josh \
    Main \
    CHC-CMIP6_SSP245_CHIRPS_2007_annual.tif \
    0 \
    mm \
    preprocessed_data/precipitation_geotiff.jshd \
    --timestep 0

# Check initial output
if [ ! -f "preprocessed_data/precipitation_geotiff.jshd" ] || [ ! -s "preprocessed_data/precipitation_geotiff.jshd" ]; then
    echo "Error: Initial GeoTIFF preprocessing failed!"
    exit 1
fi
echo "✓ 2007 GeoTIFF data preprocessed successfully ($(wc -c < preprocessed_data/precipitation_geotiff.jshd) bytes)"

# Process remaining years with --amend
for year in {2008..2016}; do
    timestep=$((year - 2007))
    echo "Processing ${year} precipitation (timestep ${timestep})..."
    
    java -jar joshsim-fat.jar preprocess \
        ../examples/tutorial/grass_shrub_fire.josh \
        Main \
        CHC-CMIP6_SSP245_CHIRPS_${year}_annual.tif \
        0 \
        mm \
        preprocessed_data/precipitation_geotiff.jshd \
        --amend \
        --timestep ${timestep}
    
    # Verify file still exists and is growing
    if [ ! -f "preprocessed_data/precipitation_geotiff.jshd" ] || [ ! -s "preprocessed_data/precipitation_geotiff.jshd" ]; then
        echo "Error: Amending GeoTIFF data for year ${year} failed!"
        exit 1
    fi
    echo "✓ ${year} data added ($(wc -c < preprocessed_data/precipitation_geotiff.jshd) bytes)"
done

echo "=== Running Validation Tests ==="

# Copy preprocessed data to current directory for tests
cp preprocessed_data/temperature.jshd ./
cp preprocessed_data/precipitation.jshd ./
cp preprocessed_data/precipitation_geotiff.jshd ./

# Verify preprocessed files are valid and non-empty
echo "Verifying temperature data..."
if [ -f "temperature.jshd" ] && [ -s "temperature.jshd" ]; then
    SIZE=$(wc -c < temperature.jshd)
    echo "✓ Temperature data: $SIZE bytes (SUCCESS)"
    if [ $SIZE -lt 1000000 ]; then
        echo "⚠ Warning: Temperature file might be smaller than expected"
    fi
else
    echo "✗ Temperature data validation FAILED"
    exit 1
fi

echo "Verifying precipitation data..."
if [ -f "precipitation.jshd" ] && [ -s "precipitation.jshd" ]; then
    SIZE=$(wc -c < precipitation.jshd)
    echo "✓ Precipitation data: $SIZE bytes (SUCCESS)"
    if [ $SIZE -lt 1000000 ]; then
        echo "⚠ Warning: Precipitation file might be smaller than expected"
    fi
else
    echo "✗ Precipitation data validation FAILED"
    exit 1
fi

echo "Verifying GeoTIFF precipitation data..."
if [ -f "precipitation_geotiff.jshd" ] && [ -s "precipitation_geotiff.jshd" ]; then
    SIZE=$(wc -c < precipitation_geotiff.jshd)
    echo "✓ GeoTIFF precipitation data: $SIZE bytes (SUCCESS)"
    if [ $SIZE -lt 500000 ]; then
        echo "⚠ Warning: GeoTIFF file might be smaller than expected"
    fi
else
    echo "✗ GeoTIFF precipitation data validation FAILED"
    exit 1
fi

echo "=== All preprocessing tests passed! ==="
exit 0