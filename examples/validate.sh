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
    java -jar build/libs/joshsim-fat.jar validate "$1"
  else
    java -jar build/libs/joshsim-fat.jar validate --suppress-info "$1"
  fi
  local status=$?
  if [ $status -eq 0 ]; then
    return 0
  else
    return $status
  fi
}

assert_not_ok() {
  if [ "$verbose" = true ]; then
    java -jar build/libs/joshsim-fat.jar validate "$1"
  else
    java -jar build/libs/joshsim-fat.jar validate --suppress-info "$1"
  fi
  local status=$?
  if [ $status -ne 0 ]; then
    return 0
  else
    return 1
  fi
}

# Test discoverConfig command
test_discover_config() {
  local file="$1"
  local expected_vars="$2"
  local exit_code="$3"
  
  if [ "$verbose" = true ]; then
    echo "Testing discoverConfig on $file..."
  fi
  
  # Run discoverConfig and capture output
  local output
  if [ "$verbose" = true ]; then
    output=$(java -jar build/libs/joshsim-fat.jar discoverConfig "$file" 2>&1)
  else
    output=$(java -jar build/libs/joshsim-fat.jar discoverConfig "$file" 2>/dev/null)
  fi
  local status=$?
  
  if [ $status -ne 0 ]; then
    if [ "$verbose" = true ]; then
      echo "discoverConfig failed with exit code $status for $file"
    fi
    return $status
  fi
  
  # Check expected variables are found
  for var in $expected_vars; do
    if ! echo "$output" | grep -q "^$var$"; then
      if [ "$verbose" = true ]; then
        echo "Expected variable '$var' not found in output: $output"
      fi
      return 1
    fi
  done
  
  return 0
}

assert_not_ok examples/features/error.josh || exit 1

assert_ok examples/features/autobox_distribution.josh || exit 2
assert_ok examples/features/autobox_scalar.josh || exit 3
assert_ok examples/features/comments.josh || exit 4
assert_ok examples/features/conditional_full.josh || exit 5
assert_ok examples/features/conditional_lambda.josh || exit 6
# assert_ok examples/features/config.josh || exit 7
assert_ok examples/features/cyclic.josh || exit 8
assert_ok examples/features/disturbance.josh || exit 9
assert_ok examples/features/external.josh || exit 10
assert_ok examples/features/here.josh || exit 11
# assert_ok examples/features/import.josh || exit 12
assert_ok examples/features/limit.josh || exit 13
assert_ok examples/features/management.josh || exit 14
assert_ok examples/features/map.josh || exit 15
assert_ok examples/features/patch.josh || exit 16
assert_ok examples/features/query_spatial.josh || exit 17
assert_ok examples/features/query_temporal.josh || exit 18
assert_ok examples/features/sample.josh || exit 19
assert_ok examples/features/selector.josh || exit 20
assert_ok examples/features/simulation.josh || exit 21
assert_ok examples/features/slice.josh || exit 22
assert_ok examples/features/units_custom.josh || exit 23
assert_ok examples/features/units_default.josh || exit 24

assert_ok examples/simulations/external.josh || exit 25
assert_ok examples/simulations/interaction.josh || exit 26
assert_ok examples/simulations/query.josh || exit 27
assert_ok examples/simulations/simple.josh || exit 28
assert_ok examples/simulations/simple_seki.josh || exit 29
assert_ok examples/simulations/simple_geotiff.josh || exit 30
assert_ok examples/simulations/simple_netcdf.josh || exit 31
assert_ok examples/simulations/state.josh || exit 32
assert_ok examples/simulations/variables.josh || exit 33

# Test config example validation
assert_ok examples/features/config_example.josh || exit 34

# Test discoverConfig command functionality
test_discover_config examples/features/config_example.josh "example.testVar1 example.testVar2" || exit 35

# Test discoverConfig on file with no config variables
test_discover_config examples/simulations/simple.josh "" || exit 36

# Test discoverConfig error handling with nonexistent file
if [ "$verbose" = true ]; then
  java -jar build/libs/joshsim-fat.jar discoverConfig nonexistent.josh >/dev/null 2>&1
else
  java -jar build/libs/joshsim-fat.jar discoverConfig nonexistent.josh >/dev/null 2>&1
fi
[ $? -eq 1 ] || exit 37
