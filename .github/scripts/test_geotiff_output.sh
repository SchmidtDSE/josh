#!/bin/bash
# License: BSD-3-Clause
# Test GeoTIFF output format from the simple_geotiff simulation.
#
# Usage: test_geotiff_output.sh

set -e

echo "Testing GeoTIFF output..."
rm -f /tmp/simple_josh_*.tiff
java -Xmx6g -jar build/libs/joshsim-fat.jar run --replicates=2 \
  examples/simulations/simple_geotiff.josh TestSimpleSimulation || exit 12
[ -f "/tmp/simple_josh_averageAge_0_0.tiff" ] || exit 13
[ -s "/tmp/simple_josh_averageAge_0_0.tiff" ] || exit 14
[ -f "/tmp/simple_josh_averageHeight_1_1.tiff" ] || exit 15
[ -s "/tmp/simple_josh_averageHeight_1_1.tiff" ] || exit 16

echo "GeoTIFF output format tests passed!"
