# joshpy-bottles

Reproducible snapshots of joshpy sweep runs, used for debugging simulation failures locally.

A "bottle" is a self-contained `.tar.gz` archive produced by joshpy's `SweepManager` that captures everything needed to re-run a single simulation job: the `.josh` script, `.jshc` config, `.jshd` data files, and a `manifest.json` with parameters and provenance.

## Directory structure

```
joshpy-bottles/
  compressed/    # .tar.gz bottle archives (gitignored)
  extracted/     # unpacked bottles, created automatically (gitignored)
```

Both directories are gitignored. Bottles contain large binary data files and should be shared out-of-band (e.g. MinIO, shared drives).

## Bottle contents

Each archive extracts to a directory named `bottle_<label>_<run_hash>/`:

```
bottle_test-minio-fail_a412e32bbcb9/
  manifest.json       # provenance: simulation name, parameters, jar SHA, git hash, timestamps
  simulation.josh     # the Josh script
  sweep_config.jshc   # parameter configuration
  run.sh              # the exact command joshpy used to launch this job
  data/               # .jshd preprocessed data files
```

## Debugging a bottle

### With Gradle (recommended)

```bash
./gradlew debugBottle -Pbottle=<bottle.tar.gz>
```

This will:
1. Build the fat JAR if needed
2. Resolve the bottle from `joshpy-bottles/compressed/` (accepts a filename or full path)
3. Extract it if not already extracted
4. Load MinIO credentials from `.devcontainer/.env`
5. Launch the JVM with JDWP suspended on port 5005

Then attach the VSCode debugger using the **"Attach to Bottle"** launch configuration.

### With the shell script

```bash
source .devcontainer/.env  # load MinIO credentials if needed
bash scripts/unbottle_and_debug.sh <bottle.tar.gz>
```

The shell script does not source `.env` automatically -- you must do it yourself if the simulation targets a MinIO export.

## Naming convention

Bottles are named by joshpy as:

```
bottle_<label>_<run_hash>_<timestamp>.tar.gz
```

- **label**: user-provided sweep label (e.g. `test-minio-fail`)
- **run_hash**: 12-char hex hash identifying the sweep run
- **timestamp**: `YYYYMMDD_HHMMSS` in UTC
