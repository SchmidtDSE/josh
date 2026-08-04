#!/bin/bash

# Runs the guide models as a CLI smoke test. The models live in docs/src/guides and export to
# memory://editor/patches for the browser, which the JVM rejects; what runs here is the emitted
# copy under build/docs/runnable, where the harvester has retargeted the export to a file:// path
# and applied any overlay the unit declares. That is why there are no longer `_cli` twins to run.

RUNNABLE=build/docs/runnable

if [ ! -f build/libs/joshsim-fat.jar ]; then
   gradle fatJar
fi

if [ ! -d "$RUNNABLE" ]; then
  echo "No emitted models at $RUNNABLE. Run './gradlew harvestDocs' first." >&2
  exit 16
fi

# The export target is an absolute path baked into the emitted model, and the jar does not create
# its parents. upload-artifact drops empty directories, so the directory may not have survived the
# trip from the harvest job.
mkdir -p build/docs/exports

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

echo "Testing guide examples..."
assert_ok $RUNNABLE/hello.josh Main 1 || exit 17

# The guides that read external data are staged from the compressed copies committed beside their
# models, which is the same data the site publishes decompressed. A missing one is an error rather
# than a warning: these files are in the repository, so absence means the checkout is wrong, not
# that an upstream job failed to hand something over.
echo "Staging tutorial data from the authored tree..."
for source in docs/src/guides/*/*.jshdz; do
    if [ ! -f "$source" ]; then
        echo "Error: no committed tutorial data found under docs/src/guides"
        exit 20
    fi
    cp "$source" .
    echo "  $(basename "$source")"
done

assert_ok $RUNNABLE/grass_shrub_fire.josh Main 1 || exit 18
assert_ok $RUNNABLE/two_trees.josh Main 1 || exit 19

echo "✓ All guide example tests passed!"