
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
assert_ok examples/selector.josh || exit 3
assert_ok examples/limit.josh || exit 4
assert_ok examples/sample.josh || exit 5
