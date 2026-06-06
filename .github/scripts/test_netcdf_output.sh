#!/bin/bash
# License: BSD-3-Clause
# Test netCDF output format from the simple_netcdf simulation.
#
# Usage: test_netcdf_output.sh

set -e

echo "Testing netCDF output..."
rm -f /tmp/simple_josh_*.nc
java -Xmx6g -jar build/libs/joshsim-fat.jar run --replicates=2 \
  examples/simulations/simple_netcdf.josh TestSimpleSimulation || exit 7
[ -f "/tmp/simple_josh_0.nc" ] || exit 8
[ -f "/tmp/simple_josh_1.nc" ] || exit 9
[ -s "/tmp/simple_josh_0.nc" ] || exit 10
[ -s "/tmp/simple_josh_1.nc" ] || exit 11

echo "NetCDF output format tests passed!"
