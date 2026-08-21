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

## Rendering

```bash
uv run --project docs/build joshdocs render   # -> landing/library/
uv run --project docs/build joshdocs serve    # -> http://127.0.0.1:8123/library/
```

`serve` is rooted at the **site** (`landing/`), not at the tree `render` writes. A page reaches its
stylesheet, the nav's targets, and the engine by site-absolute path, because it sits at an arbitrary
depth under `/library/` while those are staged once at the root. Rooting the server at
`landing/library/` answers all of them with the 404 page, which a browser reports as
`blocked because of a disallowed MIME type ("text/html")` -- a message that names the symptom and
not the cause, so `serve` warns when the directory it is given holds no `library/`.

`render` reads only the manifest, so it needs no jar. Pair it with `harvest --skip-jar` to work on
prose without a 116MB build. The output tree is emptied first unless `--keep` is given, so a page
whose unit was deleted does not linger; `landing/library/` is gitignored and rebuilt on every push.

Each unit becomes `<destination>/<id>.html`, which is its prose file's own path with a new
extension whenever the author let both fields default. Two indexes are generated per kind plus one
for the library, so a new unit appears in the navigation by existing. Conformance tests are in the
manifest but get no page: they carry no prose, and 156 stubs would not be documentation.

The rule the whole pipeline exists to enforce is applied here. **The complete model on a page is
read from the authored `.josh` and escaped, never transcribed**, and the copy under
`landing/library/models/` that the download button serves is that same file. The fenced snippets
inside the prose are the author's own excerpts and are allowed to differ, because a tutorial builds
its model a piece at a time. `tests/test_integration.py` asserts the listing equals the source for
every unit in the real tree, with a control that fails when the template hardcodes a listing.

`serve` does not watch for changes: watching would need a dependency the build has no other use
for, and re-running `render` takes well under a second.

### Links in prose

Relative links are repointed at the page they mean, and a link with no target **fails the render**.
Three spellings resolve:

| Written | Resolves to |
|---|---|
| `../two_trees/two_trees.md` | that unit's page, by path |
| `two_trees.md` | that unit's page, by id — the spelling inherited from the old flat tree |
| `hello_debug_ci.josh` | the copy under `models/`, for an overlay fragment that cannot be a unit |

Site-absolute (`/use.html`), external, and in-page (`#anchor`) links are left alone. Rewriting
happens on the Markdown token stream rather than on rendered HTML, so a URL inside a fenced
`joshlang` block — `exportFiles.patch = "memory://editor/patches"` — is never touched.

A page that is not published cannot be linked to. That is what makes a broken cross-reference a
build failure rather than a 404 a reader finds first.

## What reads the manifest

`JoshConformanceTest` reads `build/docs/docs-manifest.json` before it walks `josh-tests`, so a model
authored under `docs/src` with `assert: true` runs in the conformance suite, with the simulation and
seed it declares rather than a first-`start simulation`-wins grep and a hardcoded seed. The walk
stays as a fallback and fills in anything the manifest does not name, so the suite is unchanged on a
checkout that has never run this builder.

Gradle wires the two together:

```bash
./gradlew harvestDocs      # writes the manifest, building the jar first
./gradlew renderDocs       # depends on harvestDocs; writes landing/library
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

The one other `.josh` file that is not a unit is an **overlay fragment** — see below. Naming it in a
sidecar's `overlay:` is what marks it as one; a `.josh` that no sidecar claims and has no prose is a
build failure, same as any other unpaired model.

`docs/src/reference/` holds the worked examples of every field below, including the three cases that
are not simply "this should validate": an asserting unit (`time/spinup`), a unit that must fail to
parse (`testing/error`), and units for syntax the engine does not implement yet
(`config/config`, `imports/import`, `testing/test`).

## Sidecar fields

`title` and `description` are required; `kind` is inferred from the directory (`guides/`,
`recipes/`, `reference/`). Unknown fields are rejected rather than ignored, so a typo fails the
build instead of silently doing nothing.

```yaml
---
title: "Wind-driven seed dispersal"
description: >-
  Moving seed off the parent patch with a wind kernel, and what the kernel does at the edge of
  the grid.
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
| `description` | — | Required. One sentence, at most 200 characters, shown under the title on the index and as the page's subtitle |
| `kind` | from the directory | `guide`, `recipe`, `reference`, or `test` |
| `id` | filename stem | URL segment and join key to CI results. Set only to break a collision |
| `destination` | directory under `docs/src` | Output directory for the rendered page |
| `order` | `100` | Sort order within a destination |
| `runnable` | `true`, or `false` for `reference` | Whether the model is a complete, runnable simulation |
| `assert` | `false` | Run this model in CI as a conformance test |
| `simulation` | — | Name of the simulation stanza. Required with `exports` |
| `exports` | none | Export slots (`patch`, `meta`, `entity`) to retarget for CLI runs |
| `overlay` | none | A `.josh` file beside the model holding `update` stanzas to append for runs |
| `data` | none | Data files beside the model. Required for every external a runnable model reads |
| `seed` | `42` | Seed for runs, matching the conformance runner |
| `expect` | `valid` | `parse-error` for a model that documents a deliberate mistake |
| `status` | `active` | `reserved` for syntax not implemented yet; requires `reason` |
| `reason` | — | Why a reserved unit is reserved |
| `tags` | none | Free-form tags for indexes |

Contradictions are rejected: `assert: true` needs `runnable: true`, `expect: parse-error` cannot be
runnable, and `exports` needs `simulation` so the overlay knows which stanza to update.

Omissions are too. A runnable model that reads an external must declare the file providing it, and
the harvest fails naming the ones it does not — then fails again if the declared file is not there.
Conformance tests are exempt from the first check, since their fixtures are staged by the test
harness rather than shipped by the author.

## Data beside the model

A guide's data is committed as `.jshdz` in the same directory as its model. That is XZ-compressed
`.jshd`, which the engine reads directly: `MultiFormatExternalGetter` resolves a bare `external`
name to `<name>.jshdz` before `<name>.jshd`, so the model needs no change and neither does a run.
Compression is what makes committing viable at all — the three tutorial files are 3.5 MB as `.jshd`
and 34 KB as `.jshdz`.

The site publishes them decompressed, at `data/<unit id>/<name>.jshd` beside the pages. `.jshdz` is
JVM-only — the XZ decoder is not compiled through TeaVM — so neither the browser editor a reader
uploads into nor the run button on the page itself can read the compressed form.

`joshdocs render` does the expanding, rather than a step in the deploy workflow. That keeps one rule
in one language and means a local preview serves the same bytes from the same paths as the deployed
site, which is what lets a run button work before anything has been deployed. Prose links a data
file by its **published** name — `[precipitation data](precipitation.jshd)`, not `.jshdz` — and a
link naming a file no unit declares fails the render like any other dead link.

Nesting under the unit id is deliberate: declared filenames are only unique within a directory, so
publishing them flat would quietly serve one guide's `precipitation.jshd` to another.

Committing them is what makes the build reproducible. The data used to be preprocessed on every push
from a zip fetched over the network at build time, which meant the input to the build was a mutable
URL and a change in it would alter the published guides with no commit and no diff.

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

## Overlay files, for differences `exports` cannot express

`update` is not limited to `exportFiles`, and a CI run sometimes has to differ from the documented
model in other ways — a shortened `steps.high`, a `debugFiles` target. Declaring an `overlay:` points
at a `.josh` file beside the model holding stanzas you wrote:

```
docs/src/guides/two_trees/two_trees.josh      # the model, shown to readers, never edited
docs/src/guides/two_trees/two_trees.md        # sidecar, declaring `overlay: two_trees_ci.josh`
docs/src/guides/two_trees/two_trees_ci.josh   # update simulation Main / steps.high = 5 count / end
```

The alternative is a second copy of the model differing in three lines, which nothing compares — the
drift this pipeline exists to remove.

Three things follow from the fragment being real Josh in a real file rather than a string in YAML:

- **Josh syntax stays in Josh files.** The builder concatenates; it never writes Josh expressions.
- **A fragment is not a unit.** A bare `update` stanza does not validate on its own — the engine
  rejects it with *"no prior definition exists"* — so it has no sidecar and is skipped by collection.
  It is the `overlay:` reference that says so, so a fragment nothing claims is a build failure.
- **The composed model is validated,** not just the authored half. A mistake in a fragment can only
  surface once it is applied, and it is reported against the fragment rather than the emitted copy.

An authored fragment is appended before the generated export block, so the export target the build
controls always wins over a path an author might hardcode.

## Running a model in the reader's browser

A page whose model can execute in a browser gets a **Run it here** button. The engine is the same
one the command line uses, compiled to WebAssembly and staged into the landing site by
`landing/war/get_from_jar.sh` — the same arrangement the editor and the demo already have, since
each is deployed to its own host and cannot reach across to another's copy.

`browserRunnable` in the manifest decides which pages get the button. It is **derived, not
declared**: the harvest asks the jar (`inspect-exports`) whether every target the simulation
declares uses the `memory` protocol. WebAssembly has no filesystem, so a `file://` target aborts
the run — and a model declaring no target at all qualifies, since it still executes, which is the
whole point for one whose `assert` handlers are the result. A unit naming no `simulation` is
excluded rather than guessed at, and a model the engine rejected is never offered.

The run box **reads the model out of the listing already on the page** rather than holding a copy
of it, so the code that runs is necessarily the code the reader is looking at. There is nothing to
keep in sync, because there is no second copy. The variables offered in the result plot are
discovered from the result itself — the records know their own attribute names — so a page gains a
new plot by the model exporting a new value, with no front-matter change.

The browser JavaScript in `landing/js/` is copied byte-for-byte from `demo.joshsim.org/js/`, with
`runner.js` the only file written for this. Nothing in CI lints or executes JavaScript today, so
prefer copying a proven file over writing a new one.

## Working on the builder

```bash
uv run --project docs/build ruff check docs/build
uv run --project docs/build pytest docs/build
```

The page templates are `docs/build/templates/`, Jinja files beside the package rather than inside
it, so changing markup never means opening Python. `base.html` carries the head and the site nav;
the nav is one copy here against the five hand-maintained copies in `landing/*.html`, which is the
duplication this tree exists to stop adding to. Undefined variables are an error, so a renamed
manifest field fails the build instead of rendering an empty page.

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
