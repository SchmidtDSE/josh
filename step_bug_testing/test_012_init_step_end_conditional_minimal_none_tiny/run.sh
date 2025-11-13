#!/bin/bash
cd "$(dirname "$0")"
java -jar ../../build/libs/joshsim-fat.jar run \
  --data="editor.jshc=editor.jshc" \
  test.josh TestSimulation \
  2>&1 | tee simulation.log
