# Josh language reference

One directory per topic. Each unit is a `.josh` model plus a same-stem `.md` sidecar: the model is
the only copy of the code, and the sidecar carries the prose and the front-matter that tells the
build what to do with it.

| Topic | What it covers |
| --- | --- |
| [syntax/](syntax/) | Comments, conditionals, selectors, and the value operators `limit`, `map`, and slicing |
| [entities/](entities/) | The stanza types: `simulation`, `patch`, `disturbance`, `management` |
| [units/](units/) | Built-in conversion with `as` and `force`, and declaring custom units |
| [distributions/](distributions/) | `sample`, scalars as distributions, and `binomial` |
| [queries/](queries/) | `here.`, `prior.`, spatial radii, and entities that reference each other |
| [external/](external/) | Declaring external data and querying its time axis |
| [config/](config/) | Supplying values from a `.jshc` namespace |
| [imports/](imports/) | Sharing declarations across models |
| [performance/](performance/) | `evalDuration`, and what to do with what it tells you |
| [time/](time/) | Spin-up and spin-down without dedicated engine machinery |
| [testing/](testing/) | Where assertions live, and what a rejected model looks like |
| [debugging/](debugging/) | `debug(...)` messages and where `debugFiles` sends them |

Most of these are snippets rather than complete simulations: they declare the stanzas a feature
needs and nothing more, so they are validated rather than run. The ones that are complete say so
with `runnable: true`.

These files were previously `examples/features/`, checked by a hand-maintained list of exit codes in
`examples/validate.sh` — a list that eight of them had never been added to. The harvest now
discovers every unit by walking this tree, so a model cannot be added here without being checked.

Every page under [joshsim.org/library/reference/](https://joshsim.org/library/reference/) is
generated from this directory: the prose comes from the `.md`, and the complete model on the page is
read from the `.josh` rather than copied into it.

## Adding a unit

Write the `.josh` and a `.md` beside it with the same stem. The sidecar needs a `title:`; `kind:` is
inferred from the top-level directory. `docs/build/README.md` documents every field.

Three declarations carry the cases that are not simply "this should validate":

- `assert: true` runs the model in the conformance suite. It needs `runnable: true` and a
  `simulation:`, and the model needs `assert.*` handlers for there to be anything to check.
- `expect: parse-error` records a model that must *fail* to parse. The harvest reports a problem if
  it ever starts validating cleanly.
- `status: reserved` with a `reason:` records syntax the docs describe but the engine does not
  implement yet. Reserved units are neither validated nor run.

Model files stay comment-free; explanation belongs here in the sidecar, and tunable parameters
belong in a `.jshc`.
