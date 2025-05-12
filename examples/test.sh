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
    java -jar build/libs/joshsim-fat.jar run --replicate $3 "$1" "$2"
  else
    java -jar build/libs/joshsim-fat.jar run --replicate $3 --supress-info "$1" "$2"
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
