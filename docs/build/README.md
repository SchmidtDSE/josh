# The Josh docs builder

Authored Josh models are the only source of truth for the code that ships on joshsim.org. This
builder pairs each model with its prose, checks both, and writes a manifest that the conformance
runner and the page renderer consume.

The engine build does not depend on this. Neither uv nor Python is baked into the container image;
both are installed on demand, and CI installs them per job.

## Setup

[uv](https://docs.astral.sh/uv/) manages the environment. It is a single static binary and brings
its own Python, so this is the whole setup on a machine with neither:

```bash
curl -LsSf https://astral.sh/uv/install.sh | sh
uv sync --project docs/build
```

`uv.lock` pins every dependency to an exact version, with hashes. Add `--frozen` to fail rather than
re-resolve when the lock and `pyproject.toml` disagree, which is what CI does.

## Harvesting

From the repo root, with the jar built (`./gradlew fatJar`):

```bash
uv run --project docs/build joshdocs harvest
```

That writes `build/docs/docs-manifest.json`. Useful flags:

| Flag | Why |
|---|---|
| `--skip-jar` | Preview prose without building the 116MB jar; skips validation |
| `--validate-tests` | Also run `validate` over the conformance suite, which the conformance runner already executes |
| `--no-tests` | Harvest only authored content |
| `--jobs N` | How many jar invocations to run at once |

The exit code is 0 when the harvest is clean, 1 when any unit has a problem, and 2 when the builder
could not run at all. A failing run reports **every** problem it found, each with a file and, where
the source makes it knowable, a line:

```
docs/src/recipes/dispersal/wind-dispersal.md:4
  order: expected an integer, got 'thirty'
```

## What reads the manifest

`JoshConformanceTest` reads `build/docs/docs-manifest.json` before it walks `josh-tests`, so a model
authored under `docs/src` with `assert: true` runs in the conformance suite, with the simulation and
seed it declares rather than a first-`start simulation`-wins grep and a hardcoded seed. The walk
stays as a fallback and fills in anything the manifest does not name, so the suite is unchanged on a
checkout that has never run this builder.

Gradle wires the two together:

```bash
./gradlew harvestDocs      # writes the manifest, building the jar first
./gradlew conformanceTest  # depends on harvestDocs
```

`harvestDocs` warns and skips when `uv` is not on `PATH`, leaving the runner on its filename walk, so
working on the engine never requires a Python toolchain. CI installs uv and then checks that the
manifest exists, so the manifest-driven path cannot be skipped quietly where it is the thing being
tested.

## A unit

A unit is one `.josh` model plus one same-stem `.md` sidecar in the same directory:

```
docs/src/recipes/dispersal/wind-dispersal.josh
docs/src/recipes/dispersal/wind-dispersal.md
```

Both halves are required. A model without a sidecar and a sidecar without a model are both build
failures — an example that no build step looks at is the problem this pipeline exists to solve. Name
a markdown file `index.md`, `README.md`, or `_something.md` when it documents a directory rather than
a unit.

Conformance tests under `josh-tests/` need no sidecar: their `# @category:` header already carries
the metadata, and their id is the filename stem, which is what already lands in the JUnit results.

`docs/src/reference/` holds the worked examples of every field below, including the three cases that
are not simply "this should validate": an asserting unit (`time/spinup`), a unit that must fail to
parse (`testing/error`), and units for syntax the engine does not implement yet
(`config/config`, `imports/import`, `testing/test`).

## Sidecar fields

Only `title` is required; `kind` is inferred from the directory (`guides/`, `recipes/`,
`reference/`). Unknown fields are rejected rather than ignored, so a typo fails the build instead of
silently doing nothing.

```yaml
---
title: "Wind-driven seed dispersal"
order: 30
runnable: true
assert: true
simulation: Main
exports: [patch]
tags: [dispersal, spatial]
---
```

| Field | Default | Meaning |
|---|---|---|
| `title` | — | Required. Heading for the rendered page |
| `kind` | from the directory | `guide`, `recipe`, `reference`, or `test` |
| `id` | filename stem | URL segment and join key to CI results. Set only to break a collision |
| `destination` | directory under `docs/src` | Output directory for the rendered page |
| `order` | `100` | Sort order within a destination |
| `runnable` | `true`, or `false` for `reference` | Whether the model is a complete, runnable simulation |
| `assert` | `false` | Run this model in CI as a conformance test |
| `simulation` | — | Name of the simulation stanza. Required with `exports` |
| `exports` | none | Export slots (`patch`, `meta`, `entity`) to retarget for CLI runs |
| `data` | none | External data files the model needs, when the name does not imply the file |
| `seed` | `42` | Seed for runs, matching the conformance runner |
| `expect` | `valid` | `parse-error` for a model that documents a deliberate mistake |
| `status` | `active` | `reserved` for syntax not implemented yet; requires `reason` |
| `reason` | — | Why a reserved unit is reserved |
| `tags` | none | Free-form tags for indexes |

Contradictions are rejected: `assert: true` needs `runnable: true`, `expect: parse-error` cannot be
runnable, and `exports` needs `simulation` so the overlay knows which stanza to update.

## Exports without editing the source

A model that writes to `memory://editor/patches` runs in the browser but not on the JVM, and the
reverse for `file://`. Rather than keep two copies of a model, the harvester appends an
`update simulation <Name>` stanza that redeclares only the `exportFiles` handlers:

```
update simulation Main

  exportFiles.patch = "file:///.../wind-dispersal_patch.csv"

end simulation
```

`update` merges onto the simulation already declared, so it replaces those handlers and leaves the
rest alone. The authored file is never rewritten, and the code shown on a rendered page is the
authored file byte for byte. Emitted models land in `build/docs/runnable/`.

## Working on the builder

```bash
uv run --project docs/build ruff check docs/build
uv run --project docs/build pytest docs/build
```

One boundary is worth defending in review: **the jar owns Josh semantics.** Anything the builder
needs to know about a model — does it parse, what externals does it read — goes through
`joshdocs/joshjar.py` and, if the jar cannot answer it yet, through a new `inspect-*` command on the
jar. A regular expression over `.josh` here would be wrong: a scan for `external` in
`test_external_netcdf_temperature.josh` finds two data sets that do not exist, because that file's
external block is entirely commented out.

The command lines behind that boundary are not written here either. They come from
[joshpy](https://github.com/SchmidtDSE/joshpy), this engine's Python interface, pinned by commit in
`pyproject.toml`; `joshjar.py` is the adapter that gives it one jar, one timeout, and error messages
aimed at a documentation author. Two repos spelling the same command line is how a flag bug gets
fixed twice — `--json` on the `inspect-*` commands was one, and it is why this borrows rather than
reimplements. Bumping the pin is a deliberate commit, so an engine CLI change cannot break the docs
build on its own.
