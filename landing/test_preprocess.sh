#!/bin/bash
set -e  # Exit on any error

echo "=== Josh Preprocessing Test ==="

# Download and extract tutorial data
echo "Downloading tutorial data..."
wget -q https://joshsim.org/guides/raw_tutorial_supplement.zip
unzip -o -q raw_tutorial_supplement.zip

# Check if fat jar exists (provided by CI) or build it locally
if [ ! -f "joshsim-fat.jar" ]; then
    echo "Building fat jar locally..."
    cd ..
    ./gradlew fatJar
    cd landing
    cp ../build/libs/joshsim-fat.jar ./
else
    echo "Using provided fat jar..."
fi

# Create output directory for preprocessed files
mkdir -p preprocessed_data

echo "=== Processing Succession Model Data (NetCDF) ==="

# Process temperature data
echo "Processing temperature data..."
java -jar joshsim-fat.jar preprocess \
    ../examples/guide/succession.josh \
    Main \
    raw_tutorial_supplement/06107_tasmax_mon_FGOALS-g3_ssp245_r1i1p1f1.nc \
    "tasmax" \
    K \
    preprocessed_data/temperatureTulare.jshd \
    --x-coord=lon \
    --y-coord=lat \
    --crs "EPSG:4326" \
    --time-dim "time"

# Check output
if [ ! -f "preprocessed_data/temperatureTulare.jshd" ] || [ ! -s "preprocessed_data/temperatureTulare.jshd" ]; then
    echo "Error: Temperature preprocessing failed!"
    exit 1
fi
echo "✓ Temperature data preprocessed successfully ($(wc -c < preprocessed_data/temperatureTulare.jshd) bytes)"

# Process precipitation data
echo "Processing precipitation data..."
java -jar joshsim-fat.jar preprocess \
    ../examples/guide/succession.josh \
    Main \
    raw_tutorial_supplement/06107_pr_mon_FGOALS-g3_ssp245_r1i1p1f1.nc \
    "pr" \
    "kgm2s" \
    preprocessed_data/precipitationTulare.jshd \
    --x-coord=lon \
    --y-coord=lat \
    --crs "EPSG:4326" \
    --time-dim "time"

# Check output
if [ ! -f "preprocessed_data/precipitationTulare.jshd" ] || [ ! -s "preprocessed_data/precipitationTulare.jshd" ]; then
    echo "Error: Precipitation preprocessing failed!"
    exit 1
fi
echo "✓ Precipitation data preprocessed successfully ($(wc -c < preprocessed_data/precipitationTulare.jshd) bytes)"

echo "=== Processing Grass-Shrub-Fire Model Data (GeoTIFF) ==="

# Process multiple years of GeoTIFF data
# The grass_shrub_fire model runs from 2025-2035 (timesteps 0-10)
# We map historical data 2008-2016 to timesteps 0-8

echo "Processing 2008 precipitation (timestep 0)..."
java -jar joshsim-fat.jar preprocess \
    ../examples/guide/grass_shrub_fire.josh \
    Main \
    raw_tutorial_supplement/CHC-CMIP6_SSP245_CHIRPS_2008_annual.tif \
    0 \
    mm \
    preprocessed_data/precipitation_geotiff.jshd \
    --timestep 0

# Check initial output
if [ ! -f "preprocessed_data/precipitation_geotiff.jshd" ] || [ ! -s "preprocessed_data/precipitation_geotiff.jshd" ]; then
    echo "Error: Initial GeoTIFF preprocessing failed!"
    exit 1
fi
echo "✓ 2008 GeoTIFF data preprocessed successfully ($(wc -c < preprocessed_data/precipitation_geotiff.jshd) bytes)"

# Process remaining years with --amend
for year in {2009..2016}; do
    timestep=$((year - 2008))
    echo "Processing ${year} precipitation (timestep ${timestep})..."
    
    java -jar joshsim-fat.jar preprocess \
        ../examples/guide/grass_shrub_fire.josh \
        Main \
        raw_tutorial_supplement/CHC-CMIP6_SSP245_CHIRPS_${year}_annual.tif \
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
cp preprocessed_data/temperatureTulare.jshd ./
cp preprocessed_data/precipitationTulare.jshd ./
cp preprocessed_data/precipitation_geotiff.jshd ./

# Verify preprocessed files are valid and non-empty
echo "Verifying temperature data..."
if [ -f "temperatureTulare.jshd" ] && [ -s "temperatureTulare.jshd" ]; then
    SIZE=$(wc -c < temperatureTulare.jshd)
    echo "✓ Temperature data: $SIZE bytes (SUCCESS)"
    if [ $SIZE -lt 1000000 ]; then
        echo "⚠ Warning: Temperature file might be smaller than expected"
    fi
else
    echo "✗ Temperature data validation FAILED"
    exit 1
fi

echo "Verifying precipitation data..."
if [ -f "precipitationTulare.jshd" ] && [ -s "precipitationTulare.jshd" ]; then
    SIZE=$(wc -c < precipitationTulare.jshd)
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

echo "=== Running Data Validation Assertions ==="

echo "Validating temperature data with assertions..."
java -jar joshsim-fat.jar run --replicate 1 ../examples/test/assert_temperature_tulare.josh AssertTemperatureTulare
if [ $? -eq 0 ]; then
    echo "✓ Temperature data assertions passed"
else
    echo "✗ Temperature data assertions FAILED"
    exit 1
fi

echo "Validating precipitation data with assertions..."
java -jar joshsim-fat.jar run --replicate 1 ../examples/test/assert_precipitation_tulare.josh AssertPrecipitationTulare
if [ $? -eq 0 ]; then
    echo "✓ Precipitation data assertions passed"
else
    echo "✗ Precipitation data assertions FAILED"
    exit 1
fi

echo "Validating GeoTIFF precipitation data with assertions..."
# Copy GeoTIFF precipitation data to expected filename for assertion test
cp precipitation_geotiff.jshd precipitation.jshd
java -jar joshsim-fat.jar run --replicate 1 ../examples/test/assert_precipitation_geotiff.josh AssertPrecipitationGeotiff
if [ $? -eq 0 ]; then
    echo "✓ GeoTIFF precipitation data assertions passed"
else
    echo "✗ GeoTIFF precipitation data assertions FAILED"
    exit 1
fi

echo "=== All preprocessing tests passed! ==="
exit 0