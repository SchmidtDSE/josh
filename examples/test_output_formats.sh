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
    java -Xmx6g -jar build/libs/joshsim-fat.jar run --replicates=$3 "$1" "$2"
  else
    java -Xmx6g -jar build/libs/joshsim-fat.jar run --replicates=$3 --suppress-info "$1" "$2"
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
[ -f "/tmp/simple_josh.csv" ] || exit 2
[ -s "/tmp/simple_josh.csv" ] || exit 3

echo "Testing CSV with Earth-space output..."
rm -f /tmp/simple_seki_josh.csv
assert_ok examples/simulations/simple_seki.josh TestSimpleSimulation 2 || exit 4
[ -f "/tmp/simple_seki_josh.csv" ] || exit 5
[ -s "/tmp/simple_seki_josh.csv" ] || exit 6

echo "Testing netCDF output..."
rm -f /tmp/simple_josh_*.nc
assert_ok examples/simulations/simple_netcdf.josh TestSimpleSimulation 2 || exit 7
[ -f "/tmp/simple_josh_0.nc" ] || exit 8
[ -f "/tmp/simple_josh_1.nc" ] || exit 9
[ -s "/tmp/simple_josh_0.nc" ] || exit 10
[ -s "/tmp/simple_josh_1.nc" ] || exit 11

echo "Testing geotiff output..."
rm -f /tmp/simple_josh_*.tiff
assert_ok examples/simulations/simple_geotiff.josh TestSimpleSimulation 2 || exit 12
[ -f "/tmp/simple_josh_averageAge_0_2.tiff" ] || exit 13
[ -s "/tmp/simple_josh_averageAge_0_2.tiff" ] || exit 14
[ -f "/tmp/simple_josh_averageHeight_1_2.tiff" ] || exit 15
[ -s "/tmp/simple_josh_averageHeight_1_2.tiff" ] || exit 16

echo "✓ All output format tests passed!"