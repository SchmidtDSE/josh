#!/bin/bash

assert_ok() {
  java -jar build/libs/joshsim-fat.jar validate "$1"
  local status=$?
  if [ $status -eq 0 ]; then
    return 0
  else
    return $status
  fi
}

assert_not_ok() {
  java -jar build/libs/joshsim-fat.jar validate "$1"
  local status=$?
  if [ $status -ne 0 ]; then
    return 0
  else
    return 1
  fi
}

assert_not_ok examples/error.josh || exit 1

assert_ok examples/age.josh || exit 1
