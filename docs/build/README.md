# The Josh docs builder

Authored Josh models are the only source of truth for the code that ships on joshsim.org. This
builder pairs each model with its prose, checks both, and writes a manifest that the conformance
runner and the page renderer consume.

The engine build does not depend on this. Python is installed on demand, and nothing here is baked
into the container image.

## Setup

```bash
python3 -m venv docs/build/.venv
docs/build/.venv/bin/pip install -r docs/build/requirements.txt
```

## Harvesting

From the repo root, with the jar built (`./gradlew fatJar`):

```bash
PYTHONPATH=docs/build/src docs/build/.venv/bin/python -m joshdocs harvest
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
docs/build/.venv/bin/ruff check docs/build
docs/build/.venv/bin/python -m pytest docs/build
```

One boundary is worth defending in review: **the jar owns Josh semantics.** Anything the builder
needs to know about a model — does it parse, what externals does it read — goes through
`joshdocs/joshjar.py` and, if the jar cannot answer it yet, through a new `inspect-*` command on the
jar. A regular expression over `.josh` here would be wrong: a scan for `external` in
`test_external_netcdf_temperature.josh` finds two data sets that do not exist, because that file's
external block is entirely commented out.
