#!/bin/bash

if [ ! -f build/libs/joshsim-fat.jar ]; then
   gradle fatJar
fi

verbose=true
if [ "$1" = "quiet" ]; then
  verbose=false
  shift
fi

assert_ok() {
  if [ "$verbose" = true ]; then
    java -Xmx6g -jar build/libs/joshsim-fat.jar run --replicate-number $3 "$1" "$2"
  else
    java -Xmx6g -jar build/libs/joshsim-fat.jar run --replicate-number $3 --suppress-info "$1" "$2"
  fi
  local status=$?
  if [ $status -eq 0 ]; then
    return 0
  else
    return $status
  fi
}

echo "Testing CSV output..."
rm -f /tmp/simple_josh.csv
assert_ok examples/simulations/simple.josh TestSimpleSimulation 1 || exit 1
[ -f "/tmp/simple_josh_1.csv" ] || exit 2
[ -s "/tmp/simple_josh_1.csv" ] || exit 3

echo "Testing CSV with Earth-space output..."
rm -f /tmp/simple_seki_josh.csv
assert_ok examples/simulations/simple_seki.josh TestSimpleSimulation 2 || exit 4
[ -f "/tmp/simple_seki_josh_2.csv" ] || exit 5
[ -s "/tmp/simple_seki_josh_2.csv" ] || exit 6

echo "Testing netCDF output..."
rm -f /tmp/simple_josh.nc
assert_ok examples/simulations/simple_netcdf.josh TestSimpleSimulation 3 || exit 7
[ -f "/tmp/simple_josh_3.nc" ] || exit 8
[ -s "/tmp/simple_josh_3.nc" ] || exit 9

echo "Testing geotiff output..."
rm -f /tmp/simple_josh.nc
assert_ok examples/simulations/simple_geotiff.josh TestSimpleSimulation 4 || exit 10
[ -f "/tmp/simple_josh_averageAge_0_4.tiff" ] || exit 11
[ -s "/tmp/simple_josh_averageAge_0_4.tiff" ] || exit 12
[ -f "/tmp/simple_josh_averageHeight_1_4.tiff" ] || exit 13
[ -s "/tmp/simple_josh_averageHeight_1_4.tiff" ] || exit 14

echo "Testing guide examples..."
assert_ok examples/guide/hello_cli.josh Main 1 || exit 15

# Copy required jshd files for CLI tests that now use external data
echo "Checking for preprocessed data files..."
ls -la landing/preprocessed_data/ 2>/dev/null || echo "No landing/preprocessed_data directory found"

if [ -f "landing/preprocessed_data/precipitation_geotiff.jshd" ]; then
    echo "Found precipitation_geotiff.jshd, copying to precipitation.jshd"
    cp landing/preprocessed_data/precipitation_geotiff.jshd precipitation.jshd
elif [ -f "landing/preprocessed_data/precipitationGrassfire2008.jshd" ]; then
    echo "Found precipitationGrassfire2008.jshd, copying to precipitation.jshd"
    cp landing/preprocessed_data/precipitationGrassfire2008.jshd precipitation.jshd
else
    echo "Error: No GeoTIFF precipitation data found for CLI tests"
    exit 18
fi

if [ -f "landing/preprocessed_data/temperatureTulare.jshd" ]; then
    echo "Found temperatureTulare.jshd, copying to working directory"
    cp landing/preprocessed_data/temperatureTulare.jshd .
else
    echo "Warning: temperatureTulare.jshd not found"
fi

if [ -f "landing/preprocessed_data/precipitationTulare.jshd" ]; then
    echo "Found precipitationTulare.jshd, copying to working directory"
    cp landing/preprocessed_data/precipitationTulare.jshd .
else
    echo "Warning: precipitationTulare.jshd not found"
fi

echo "Files available for CLI tests:"
ls -la *.jshd 2>/dev/null || echo "No .jshd files in working directory"

assert_ok examples/guide/grass_shrub_fire_cli.josh Main 1 || exit 16
assert_ok examples/guide/two_trees_cli.josh Main 1 || exit 17

echo "Testing config example with external config file..."
# Copy the config file to working directory as expected by the config system
cp examples/features/config_example.jshc example.jshc || exit 18
rm -f /tmp/config_example_josh.csv
assert_ok examples/features/config_example.josh ConfigExample 1 || exit 19
[ -f "/tmp/config_example_josh_1.csv" ] || exit 20
[ -s "/tmp/config_example_josh_1.csv" ] || exit 21
echo "Config example test passed successfully!"
