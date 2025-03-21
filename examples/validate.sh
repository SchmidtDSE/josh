
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
    java -jar build/libs/joshsim-fat.jar validate --quiet "$1"
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
    java -jar build/libs/joshsim-fat.jar validate --quiet "$1"
  fi
  local status=$?
  if [ $status -ne 0 ]; then
    return 0
  else
    return 1
  fi
}

assert_not_ok examples/error.josh || exit 1

assert_ok examples/age.josh || exit 2
assert_ok examples/autobox_distribution.josh || exit 3
assert_ok examples/autobox_scalar.josh || exit 4
assert_ok examples/comments.josh || exit 5
assert_ok examples/conditional_full.josh || exit 6
assert_ok examples/conditional_lambda.josh || exit 7
assert_ok examples/config.josh || exit 8
assert_ok examples/cyclic.josh || exit 9
assert_ok examples/disturbance.josh || exit 10
assert_ok examples/external.josh || exit 11
assert_ok examples/full_interaction.josh || exit 12
assert_ok examples/full_joshuatree.josh || exit 13
assert_ok examples/here.josh || exit 13
assert_ok examples/import.josh || exit 13
assert_ok examples/limit.josh || exit 14
assert_ok examples/management.josh || exit 15
assert_ok examples/map.josh || exit 15
assert_ok examples/patch.josh || exit 16
assert_ok examples/query_spatial.josh || exit 17
assert_ok examples/query_temporal.josh || exit 18
assert_ok examples/sample.josh || exit 19
assert_ok examples/selector.josh || exit 20
assert_ok examples/simulation.josh || exit 21
assert_ok examples/slice.josh || exit 22
assert_ok examples/state.josh || exit 23
assert_ok examples/units_custom.josh || exit 24
assert_ok examples/units_default.josh || exit 25
