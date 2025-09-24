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

echo "Testing config example with external config file..."
# Copy the config file to working directory as expected by the config system
cp examples/features/config_example.jshc example.jshc || exit 21
rm -f /tmp/config_example_josh.csv
assert_ok examples/features/config_example.josh ConfigExample 1 || exit 22
[ -f "/tmp/config_example_josh.csv" ] || exit 23
[ -s "/tmp/config_example_josh.csv" ] || exit 24
echo "✓ Config example test passed successfully!"