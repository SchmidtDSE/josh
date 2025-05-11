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
    java -jar build/libs/joshsim-fat.jar run "$1" "$2"
  else
    java -jar build/libs/joshsim-fat.jar run --supress-info "$1" "$2"
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
assert_ok examples/simulations/simple.josh TestSimpleSimulation || exit 1
[ -f "/tmp/simple_josh.csv" ] || exit 2

echo "Testing CSV with Earth-space output..."
rm -f /tmp/simple_seki_josh.csv
assert_ok examples/simulations/simple_seki.josh TestSimpleSimulation || exit 3
[ -f "/tmp/simple_seki_josh.csv" ] || exit 4

echo "Testing netCDF output..."
rm -f /tmp/simple_josh.nc
assert_ok examples/simulations/simple_netcdf.josh TestSimpleSimulation || exit 5
[ -f "/tmp/simple_josh.nc" ] || exit 6
