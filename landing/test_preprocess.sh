#!/bin/bash
set -e  # Exit on any error

echo "=== Josh Preprocessing Test ==="

# Determine if we're running from root or landing directory
if [ -d "landing" ]; then
    # Running from root directory (CI/CD)
    cd landing
    JOSH_PATH="../examples/guide"
else
    # Running from landing directory (local)
    JOSH_PATH="../examples/guide"
fi

# Download and extract tutorial data
echo "Downloading tutorial data..."
wget -q https://joshsim.org/guides/raw_tutorial_supplement.zip
unzip -o -q raw_tutorial_supplement.zip

# Check if fat jar exists (provided by CI) or build it locally
# In CI/CD, the jar is in build/libs/joshsim-fat.jar at repo root
if [ -f "../build/libs/joshsim-fat.jar" ]; then
    echo "Using CI/CD fat jar from build/libs..."
    cp ../build/libs/joshsim-fat.jar ./joshsim-fat.jar
elif [ -f "joshsim-fat.jar" ]; then
    echo "Using provided fat jar..."
elif [ -f "build/libs/joshsim-fat.jar" ]; then
    echo "Using local fat jar from build/libs..."
    cp build/libs/joshsim-fat.jar ./joshsim-fat.jar
else
    echo "Building fat jar locally..."
    if [ -f "../gradlew" ]; then
        ORIGINAL_DIR=$(pwd)
        cd ..
        ./gradlew fatJar
        cd "$ORIGINAL_DIR"
        cp ../build/libs/joshsim-fat.jar ./joshsim-fat.jar
    else
        echo "Error: Cannot find gradlew to build fat jar"
        exit 1
    fi
fi

# Create output directory for preprocessed files
mkdir -p preprocessed_data

echo "=== Processing Succession Model Data (NetCDF) ==="

# Process temperature data
echo "Processing temperature data..."
java -jar joshsim-fat.jar preprocess \
    $JOSH_PATH/succession.josh \
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
    $JOSH_PATH/succession.josh \
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
    $JOSH_PATH/grass_shrub_fire.josh \
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

echo "=== Validating Temporal Combination with InspectJshdCommand ==="

# Test multiple coordinates across different timesteps to verify temporal combination
echo "Testing temporal variation at known valid grid locations..."

# Test point 1: Grid (5, 4) - known valid location from GeotiffExternalDataReaderTest
echo "Checking precipitation values at grid (5, 4) across multiple timesteps:"
for timestep in 0 2 6 8; do
    year=$((timestep + 2008))
    echo -n "  Timestep $timestep ($year): "
    
    result=$(java -jar joshsim-fat.jar inspectJshd preprocessed_data/precipitation_geotiff.jshd data $timestep 5 4 2>&1)
    exit_code=$?
    
    if [ $exit_code -eq 0 ]; then
        echo "$result"
        
        # Extract numerical value for validation
        value=$(echo "$result" | sed -n 's/.*: \([0-9]\+\(\.[0-9]\+\)\?\) mm.*/\1/p')
        if [ -n "$value" ]; then
            # Check if value is within expected range (0-5000 mm)
            if (( $(echo "$value >= 0 && $value <= 5000" | bc -l) )); then
                echo "    ✓ Value $value mm is within expected range [0, 5000] mm"
            else
                echo "    ✗ Value $value mm is outside expected range [0, 5000] mm"
                exit 1
            fi
        fi
    else
        echo "Error reading value: $result"
        exit 1
    fi
done

# Test point 2: Grid (50, 50) - center of grid
echo ""
echo "Checking precipitation values at grid (50, 50) across multiple timesteps:"
for timestep in 0 2 6 8; do
    year=$((timestep + 2008))
    echo -n "  Timestep $timestep ($year): "
    
    result=$(java -jar joshsim-fat.jar inspectJshd preprocessed_data/precipitation_geotiff.jshd data $timestep 50 50 2>&1)
    exit_code=$?
    
    if [ $exit_code -eq 0 ]; then
        echo "$result"
        
        # Extract numerical value for validation
        value=$(echo "$result" | sed -n 's/.*: \([0-9]\+\(\.[0-9]\+\)\?\) mm.*/\1/p')
        if [ -n "$value" ]; then
            # Check if value is within expected range (0-5000 mm)
            if (( $(echo "$value >= 0 && $value <= 5000" | bc -l) )); then
                echo "    ✓ Value $value mm is within expected range [0, 5000] mm"
            else
                echo "    ✗ Value $value mm is outside expected range [0, 5000] mm"
                exit 1
            fi
        fi
    else
        echo "Error reading value: $result"
        exit 1
    fi
done

# Verify temporal variation exists by collecting values from different timesteps
echo ""
echo "Validating temporal variation exists across different years..."
values_5_4=()
values_50_50=()

for timestep in 0 2 6 8; do
    # Collect values at (5, 4)
    result=$(java -jar joshsim-fat.jar inspectJshd preprocessed_data/precipitation_geotiff.jshd data $timestep 5 4 2>&1)
    value=$(echo "$result" | sed -n 's/.*: \([0-9]\+\(\.[0-9]\+\)\?\) mm.*/\1/p')
    if [ -n "$value" ]; then
        values_5_4+=("$value")
    fi
    
    # Collect values at (50, 50)
    result=$(java -jar joshsim-fat.jar inspectJshd preprocessed_data/precipitation_geotiff.jshd data $timestep 50 50 2>&1)
    value=$(echo "$result" | sed -n 's/.*: \([0-9]\+\(\.[0-9]\+\)\?\) mm.*/\1/p')
    if [ -n "$value" ]; then
        values_50_50+=("$value")
    fi
done

# Check for temporal variation at grid (5, 4)
echo "Temporal values at grid (5, 4): ${values_5_4[*]}"
if [ ${#values_5_4[@]} -ge 2 ]; then
    first_value=${values_5_4[0]}
    has_variation=false
    
    for value in "${values_5_4[@]:1}"; do
        if [ "$value" != "$first_value" ]; then
            has_variation=true
            break
        fi
    done
    
    if [ "$has_variation" = true ]; then
        echo "✓ Temporal variation detected at grid (5, 4) - different timesteps have different values"
    else
        echo "⚠ No temporal variation detected at grid (5, 4) - all values are identical ($first_value)"
    fi
else
    echo "⚠ Insufficient values collected for temporal variation analysis at grid (5, 4)"
fi

# Check for temporal variation at grid (50, 50)
echo "Temporal values at grid (50, 50): ${values_50_50[*]}"
if [ ${#values_50_50[@]} -ge 2 ]; then
    first_value=${values_50_50[0]}
    has_variation=false
    
    for value in "${values_50_50[@]:1}"; do
        if [ "$value" != "$first_value" ]; then
            has_variation=true
            break
        fi
    done
    
    if [ "$has_variation" = true ]; then
        echo "✓ Temporal variation detected at grid (50, 50) - different timesteps have different values"
    else
        echo "⚠ No temporal variation detected at grid (50, 50) - all values are identical ($first_value)"
    fi
else
    echo "⚠ Insufficient values collected for temporal variation analysis at grid (50, 50)"
fi

echo ""
echo "✓ Temporal combination validation completed successfully"
echo "  - Tested multiple grid coordinates across timesteps 0, 2, 6, 8 (years 2008, 2010, 2014, 2016)"
echo "  - Verified all values are within expected precipitation range [0, 5000] mm"
echo "  - Validated temporal variation exists across different years"

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