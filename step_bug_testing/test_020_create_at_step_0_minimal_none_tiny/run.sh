#!/bin/bash
cd "$(dirname "$0")"
java -jar ../../build/libs/joshsim-fat.jar run test.josh TestSimulation 2>&1 | tee simulation.log
