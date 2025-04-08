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

assert_ok examples/simulations/simple.josh TestSimpleSimulation || exit 1
