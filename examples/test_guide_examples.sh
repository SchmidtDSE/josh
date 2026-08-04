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

# Copy required jshd files for CLI tests that now use external data
echo "Checking for preprocessed data files..."
ls -la landing/preprocessed_data/ 2>/dev/null || echo "No landing/preprocessed_data directory found"

if [ -f "landing/preprocessed_data/precipitation_geotiff.jshd" ]; then
    echo "Found precipitation_geotiff.jshd, copying to precipitation.jshd"
    cp landing/preprocessed_data/precipitation_geotiff.jshd precipitation.jshd
elif [ -f "landing/preprocessed_data/precipitationGrassfire2008.jshd" ]; then
    echo "Found precipitationGrassfire2008.jshd, copying to precipitation.jshd"
    cp landing/preprocessed_data/precipitationGrassfire2008.jshd precipitation.jshd
else
    echo "Error: No GeoTIFF precipitation data found for CLI tests"
    exit 20
fi

if [ -f "landing/preprocessed_data/temperatureTulare.jshd" ]; then
    echo "Found temperatureTulare.jshd, copying to working directory"
    cp landing/preprocessed_data/temperatureTulare.jshd .
else
    echo "Warning: temperatureTulare.jshd not found"
fi

if [ -f "landing/preprocessed_data/precipitationTulare.jshd" ]; then
    echo "Found precipitationTulare.jshd, copying to working directory"
    cp landing/preprocessed_data/precipitationTulare.jshd .
else
    echo "Warning: precipitationTulare.jshd not found"
fi

echo "Files available for CLI tests:"
ls -la *.jshd 2>/dev/null || echo "No .jshd files in working directory"

assert_ok $RUNNABLE/grass_shrub_fire.josh Main 1 || exit 18
assert_ok $RUNNABLE/two_trees.josh Main 1 || exit 19

echo "✓ All guide example tests passed!"