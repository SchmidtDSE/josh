#!/bin/bash

# CLI behavior assertions: what the `validate`, `discoverConfig`, `run --enable-profiler`, and
# `run --output-steps` commands do, checked against models under examples/simulations and
# docs/src/reference that serve as fixtures.
#
# The per-file `assert_ok` list for examples/features is gone. Those models moved to
# docs/src/reference, where `./gradlew harvestDocs` discovers and validates every one of them by
# walking the tree -- including the eight that had never been added to the list here, and the two
# that were commented out with no recorded reason.

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

assert_run() {
  local file="$1"
  local sim="$2"
  shift 2
  if [ "$verbose" = true ]; then
    java -jar build/libs/joshsim-fat.jar run "$@" "$file" "$sim"
  else
    java -jar build/libs/joshsim-fat.jar run --suppress-info "$@" "$file" "$sim"
  fi
  local status=$?
  if [ $status -eq 0 ]; then
    return 0
  else
    return $status
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

assert_ok examples/simulations/external.josh || exit 25
assert_ok examples/simulations/interaction.josh || exit 26
assert_ok examples/simulations/query.josh || exit 27
assert_ok examples/simulations/simple.josh || exit 28
assert_ok examples/simulations/simple_seki.josh || exit 29
assert_ok examples/simulations/simple_geotiff.josh || exit 30
assert_ok examples/simulations/simple_netcdf.josh || exit 31
assert_ok examples/simulations/state.josh || exit 32
assert_ok examples/simulations/variables.josh || exit 33

# Test discoverConfig command functionality. The model is a documentation unit, which harvestDocs
# validates; what is asserted here is that discoverConfig reports the variables it reads.
test_discover_config docs/src/reference/config/config_example.josh "example.testVar1 example.testVar2" || exit 35

# Test discoverConfig on file with no config variables
test_discover_config examples/simulations/simple.josh "" || exit 36

# Test discoverConfig error handling with nonexistent file
if [ "$verbose" = true ]; then
  java -jar build/libs/joshsim-fat.jar discoverConfig nonexistent.josh >/dev/null 2>&1
else
  java -jar build/libs/joshsim-fat.jar discoverConfig nonexistent.josh >/dev/null 2>&1
fi
[ $? -eq 1 ] || exit 38

# Test profiler example runs both without and with --enable-profiler
rm -f /tmp/profiler_josh.csv
assert_run examples/simulations/profiler.josh ProfilerExample || exit 40
[ -f "/tmp/profiler_josh.csv" ] || exit 41
[ -s "/tmp/profiler_josh.csv" ] || exit 42

rm -f /tmp/profiler_josh.csv
assert_run examples/simulations/profiler.josh ProfilerExample --enable-profiler || exit 43
[ -f "/tmp/profiler_josh.csv" ] || exit 44
[ -s "/tmp/profiler_josh.csv" ] || exit 45

# Test profiler_multi example runs both without and with --enable-profiler
rm -f /tmp/profiler_multi_josh.csv
assert_run examples/simulations/profiler_multi.josh ProfilerMultiExample || exit 46
[ -f "/tmp/profiler_multi_josh.csv" ] || exit 47
[ -s "/tmp/profiler_multi_josh.csv" ] || exit 48

rm -f /tmp/profiler_multi_josh.csv
assert_run examples/simulations/profiler_multi.josh ProfilerMultiExample --enable-profiler || exit 49
[ -f "/tmp/profiler_multi_josh.csv" ] || exit 50
[ -s "/tmp/profiler_multi_josh.csv" ] || exit 51

# The spin-up recipe itself (widened steps.low/steps.high, phase marked by an ordinary "state"
# attribute) asserts its own phase boundaries in-model, so it now runs in the conformance suite as
# the "spinup" unit rather than as an exit code here.
#
# What remains is CLI behavior: --output-steps filters the incremental patch export down to just
# the observed rows. The exported "state" column is an ordinary attribute, not a dedicated CLI
# concept, and is what a caller would filter on to select spinup/observed/spindown after the fact.
# Uses the inclusive range syntax (0-2) rather than listing each index, since individually listing
# steps doesn't scale to large step counts.
rm -f /tmp/spinup_export_josh.csv
assert_run docs/src/reference/time/spinup_export.josh SpinupExport --seed 1 || exit 54
grep -q "spinup" /tmp/spinup_export_josh.csv || exit 55
rm -f /tmp/spinup_export_josh.csv
assert_run docs/src/reference/time/spinup_export.josh SpinupExport --seed 1 --output-steps 0-2 || exit 56
grep -q "observed" /tmp/spinup_export_josh.csv || exit 57
! grep -qE "spinup|spindown" /tmp/spinup_export_josh.csv || exit 58
