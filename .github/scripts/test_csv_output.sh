#!/bin/bash
# License: BSD-3-Clause
# Test CSV output format from the simple and simple_seki simulations.
#
# Usage: test_csv_output.sh

set -e

echo "Testing CSV output..."
rm -f /tmp/simple_josh.csv
java -Xmx6g -jar build/libs/joshsim-fat.jar run --replicates=1 \
  examples/simulations/simple.josh TestSimpleSimulation || exit 1
[ -f "/tmp/simple_josh.csv" ] || exit 2
[ -s "/tmp/simple_josh.csv" ] || exit 3

echo "Testing CSV with Earth-space output..."
rm -f /tmp/simple_seki_josh.csv
java -Xmx6g -jar build/libs/joshsim-fat.jar run --replicates=2 \
  examples/simulations/simple_seki.josh TestSimpleSimulation || exit 4
[ -f "/tmp/simple_seki_josh.csv" ] || exit 5
[ -s "/tmp/simple_seki_josh.csv" ] || exit 6

echo "CSV output format tests passed!"
