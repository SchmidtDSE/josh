# Spin-up & Spin-down

Let a simulation warm up to a stable state before the observed period, and keep
running afterward to watch recovery — without changing any data or model code.

## The idea

Most ecological runs want more than the years for which we have data:

- **Spin-up** — run for a long stretch *before* the observed period so the system
  settles into equilibrium, drawing forcing (precipitation, temperature, …) by
  resampling historical years.
- **Observed** — the real period, driven 1:1 by the actual data.
- **Spin-down** — keep running *after* the observed period to see how long the
  system takes to recover from a disturbance, again resampling historical years.

## What it looks like

```josh
start simulation Fire

  steps.low = 0 years        # observed data spans years 0–86
  steps.high = 86 years

  start spinup
    duration = 500 years
    year = sample discrete uniform from 0 years to 86 years
  end spinup

  start spindown
    duration = 500 years
    year = sample discrete uniform from 76 years to 86 years
  end spindown

end simulation
```

Read aloud: *"warm up for 500 years drawing each year's forcing uniformly from
0–86; run the observed period 0–86; then run 500 more years drawing from 76–86."*

The phase body is a set of named properties (`name = expression`), reusing the
same assignment form as the rest of the language: `duration` (the phase length)
and `year` (resampled each step to pick which data year's forcing is felt).
Naming `year` explicitly is the point — it is what `meta.year` *becomes* during
the phase. The year expression is not limited to `sample discrete uniform`: a
constant (`year = 85 years`), `normal`, or a function of `prior` all work. Making
the body a property set also leaves room for future properties (e.g. a
convergence `until`, below) with no grammar change.

The year is drawn with the new **`discrete uniform`** form, which reuses the
existing uniform distribution but draws an actual integer uniformly over the
inclusive range — `sample discrete uniform from 0 years to 86 years` yields a
whole year index in `[0..86]`, no coercion. It is an **opt-in qualifier**
(implemented; `DISCRETE_` token + a `randUniformDiscrete()` machine op): a plain
`uniform from …` stays continuous, so the random sequence of every existing
seeded model is byte-for-byte unchanged. *Discrete uniform* is also standard
statistical terminology, so it reads naturally for an ecologist.

> Why a qualifier and not type-directed inference (integer bounds ⇒ discrete)?
> The latter is more elegant but reinterprets the ~180 existing integer-bound
> `uniform from …` usages across the guide tutorials, the paper example, and the
> conformance suite. Because a discrete draw consumes the RNG differently than a
> continuous one, that silently shifts every seeded model's sequence — verified to
> break seed-tuned conformance tests. The explicit qualifier confines the new
> behavior to code that asks for it.

### Time units

The simulation is written in one **time unit** — `years` for an annual model
(the common case), or `count` for abstract/unitless models. The window bounds
(`from 76 years to 86 years`) are *positions* on that axis and share the unit of
`steps.low`/`steps.high`; the `duration = 500 years` is a *length* on the same
axis. Both reading as `years` is intentional, not an overload — like "from mile 76
to mile 86, for 500 miles." The engine already accepts `years` on `steps.*`
(verified), so this is a convention plus light validation (window unit matches
`steps.*`; window falls inside `[steps.low, steps.high]`).

The rule is **1 step = 1 unit of the declared time unit**. Sub-annual models
(monthly steps where `duration = 500 years` ≠ 500 steps) would need an explicit
time-per-step declaration; that is deliberately out of scope here.

### Multiple scenarios in one file

Because simulations are named and selected at run time, spin-up lives with the
*scenario*, not with the shared data resources. Keep a plain run and a warmed-up
run side by side:

```josh
start simulation Main           # no spin-up
  steps.low = 0 years
  steps.high = 86 years
end simulation

start simulation MainWithSpinup
  steps.low = 0 years
  steps.high = 86 years
  start spinup
    duration = 500 years
    year = sample discrete uniform from 0 years to 86 years
  end spinup
end simulation
```

Because the phase properties are ordinary expressions, they are also
config-tunable for free (`year = sample discrete uniform from 0 years to config
s.start years`, `duration = config s.len years`) — see
[COMPOSITION.md](COMPOSITION.md). And designing the
`spinup`/`spindown` stanza to attach *by simulation name* lets a scenario later
live in its own imported file, the same merge question as separating exports.

```
$ java -jar joshsim.jar run fire.josh MainWithSpinup
```

## How time behaves

There are two clocks. Today they are identical; this feature decouples them. The
step clock is **anchored at the observed period**: step 0 is always the first
observed step, spin-up counts backward into the negatives, spin-down continues
past the end. So `step 0` means the same thing whether or not spin-up exists.

| | meaning | during spin-up | first observed step | during spin-down |
|---|---|---|---|---|
| `meta.stepCount` | anchored step (state, `prior`, ordering) | −500 … −1 | 0 | 87 … 586 |
| `meta.year` | which data year is felt this step (drives every `external` read) | random 0–86 | 0 | random 76–86 |
| `meta.phase` *(new)* | `"spinup"` / `"observed"` / `"spindown"` | `"spinup"` | `"observed"` | `"spindown"` |

(`steps.low`/`steps.high` define the observed window; the blocks extend the clock
around it. `meta.stepCount` moves from the old 0-based absolute counter to this
anchored value — backward-compatible for every model without spin-up. `.init`
still fires at the true first step, off an internal counter, so it runs at the
start of spin-up.)

Key consequences:

- **The draw is shared per step.** `external Precipitation` and
  `external Temperature` in the same spin-up step read the *same* random year, so
  forcing stays physically consistent.
- **State carries across boundaries.** `prior` at the first observed step points
  at the last spin-up step — the spun-up ecosystem *is* the observed run's
  initial condition. Spin-up is not a separate throwaway run.
- **No new data.** Resampling only re-reads year indices that already exist in
  the preprocessed grid (0–86, 76–86). Nothing new to preprocess.
- **`steps.low` / `steps.high` keep their meaning** — the observed window. The
  blocks extend the run around it.

### Output

Which phases are written is configurable (e.g. a `--phases` flag driven by
`meta.phase`), defaulting to all. Spin-up is often throwaway, so suppressing it
while keeping observed + spin-down is a common choice.

## Scope: build narrow, structure for later

Ship the narrow feature — fixed `spinup` / `spindown` blocks with a `duration`
only — but choose two internal representations now so the deferred features below
are *additions*, not rewrites:

- **Phases are an ordered list internally**, even though the surface only exposes
  `spinup` and `spindown`. The bridge holds `[before, observed, after]` with the
  order hardcoded; nothing else assumes exactly two phases.
- **A phase's termination is an abstraction.** Today only a fixed `duration`
  exists; the type leaves room for a *condition* — a boolean
  evaluated against grid state after each step. Both `until` (end a phase when
  stable) and `earlyStop` (end the run when collapsed) are this same condition
  variant, differing only in what the transition does, so the abstraction must
  cover them even though only `DurationTermination` is built now.

### Reserved for later (accounted for, not built)

- **Convergence spin-up (`until`).** "Warm up *until* the system stabilizes,
  capped for safety" is the honest form of *find a stable state*. With the
  named-property body it is simply another property alongside `duration` (which
  becomes the safety cap), no grammar change:

  ```josh
  start spinup
    duration = 2000 years    # safety cap
    year = sample discrete uniform from 0 years to 86 years
    until = mean(ForeverTree.count) > 100 count
  end spinup
  ```

  It needs a windowed stop condition (history access) and variable-length runs,
  but the negative-step anchoring already absorbs variable lengths — every
  replicate realigns at step 0 — so it slots into the termination abstraction
  without reworking the clock. The `until` *property* is reserved; don't
  implement (and see `earlyStop` below for the shared missing capability).
- **Early stop (`earlyStop`).** For expensive models, end the *whole run* early
  when state collapses (or saturates) — e.g. `start earlyStop` with a condition
  like `mean(ForeverTree.count) < 1`. The step-loop hook is trivial and already
  located: the driver loop is `while (!bridge.isComplete())` in
  [JoshSimFacadeUtil.java:178-199](src/main/java/org/joshsim/JoshSimFacadeUtil.java#L178-L199),
  with the completed `TimeStep` (frozen `meta` + all patches) in hand right after
  `callback.onStep(...)`. This is the **same condition-termination variant** as
  `until`; only the transition differs (end the run vs. advance the phase).

  **What it costs — confirmed by tracing the aggregation path.** The blocker is
  that a stop condition wants a *grid-wide* value like `mean(ForeverTree.count)`,
  and there is no simulation-scope grid aggregate today. But the trace shows the
  cost is small and mostly precedented:

  - *The aggregation ladder is fully reusable.* A patch already computes
    `mean(ForeverTree.age)` over its organisms via
    [DistributionScope](src/main/java/org/joshsim/engine/func/DistributionScope.java)
    (projects `.attr` across members) + `mean`/`count` in
    [SingleThreadEventHandlerMachine](src/main/java/org/joshsim/lang/interpret/machine/SingleThreadEventHandlerMachine.java).
    Both reduce a `Distribution` and need **zero** new logic at simulation scope.
  - *One precedented rung is missing — the binding.* Organisms are *attributes*
    on the patch entity, so `EntityScope` resolves the bare name; patches are
    **not** attributes on the simulation — they live in `Replicate`. But
    [MinimalEngineBridge.getCurrentPatches()](src/main/java/org/joshsim/lang/bridge/MinimalEngineBridge.java#L252)
    already returns them **wrapped in `ShadowingEntity`** as an `Iterable`.
    Injecting a synthetic grid/patches key whose value is a `Distribution` built
    from that iterable is the *same pattern* `SyntheticScope` already uses for
    `meta`/`here`/`current`/`prior`.
  - *One genuinely new semantic.* A patch is itself a container, so
    `ForeverTree.count` across patches is a *distribution of distributions* —
    `mean(ForeverTree.count)` could mean mean-over-patches-of-within-patch-count
    or mean over all organisms grid-wide. That nested flattening is the only new
    design choice.

  **The de-risking constraint (recommended):** require the condition to reference
  only a value the simulation **already declares** (a `meta.*` / export scalar
  the user computed), not an inline grid aggregate. Then the condition contains
  no grid query at all — the grid was already collapsed to a scalar by the user's
  own declaration, on a reading *they* chose — and the nested-distribution
  question moves out of the termination feature into "can a simulation attribute
  aggregate the grid," which is that same single binding rung. Verdict:
  low-to-medium risk, low end reachable; the future build is the patches-as-scope
  binding, **not** new aggregation primitives. Reserve `start earlyStop`; don't
  implement.
- **User-defined / ordered phases.** A `start state "Seedling"`-style named-phase
  surface plus an order declaration is *not* low-cost: parsing is cheap (reuse
  `STR_`), but assembling the order, placing the observed window, computing per-
  phase ranges, and validating it is the expensive part — exactly what the fixed
  two-block form gets for free. Because phases are already an ordered list
  internally, this stays an additive change if a real need (staged warm-up,
  repeated disturbance–recovery cycles) appears.

## Effort — high level

Six localized touch points, none large; the engine already has the two-clock
structure to build on.

| Area | Change | Size |
|---|---|---|
| Grammar | `spinup`/`spindown` tokens; a phase stanza whose body is `name = expression` properties (`year`, `duration`), reusing the event-handler form | S |
| Entity build | capture each block's duration and year expression onto the simulation as an ordered phase list (attach by simulation name) | S |
| Bridge | read phase lengths, anchor the clock at the observed period (negative spin-up steps), add `getDataTimestep()` + `getPhase()` | **M** |
| External read | resolve at `getDataTimestep()` instead of the raw step (one line) | S |
| Meta attrs | `meta.stepCount` → anchored step; `meta.year` → data year; add `meta.phase` | S |
| Per-step year eval | evaluate the dynamic year expression through the normal step/RNG pipeline and cache per step (gets per-replicate reproducibility for free) | **M** |

Plus: configurable export filter, spec/docs updates, and a runnable example
wired into CI with `assert_run` (not just `validate`).

### Implementation notes (verified against the code)

- **Negative anchored steps are safe in the core.** Timesteps are stored in a
  `HashMap<Long,TimeStep>` (`Replicate`), and `TimeStep`/`Query`/exporters treat
  the step as an opaque `long` — negatives just work. **Critical invariant:**
  external/grid reads resolve at the *data year* (`getDataTimestep()`, always in
  `[steps.low, steps.high]`), never the anchored step, because the precomputed
  grid (`DoublePrecomputedGrid`) indexes by `timestep − minTimestep` and would go
  out of bounds on a negative. Internal patch-state queries (`getPriorTimestep`)
  use the anchored step against the HashMap, which is negative-safe.
- **Two cleanups in the run loop / replicate:** (1) the memory-retention guard
  `deleteTimeStep(completedStep − 2)` gated by `completedStep > 2`
  (`JoshSimFacadeUtil`) frees nothing during spin-up — re-gate on
  `completedStep − 2 >= firstAnchoredStep`; (2) confirm `Replicate.stepNumber`
  tracks the anchored `currentStep` so the live-step query guard doesn't misfire.
- **Phase carrier = existing event-handler machinery.** The simulation entity
  already runs per-step handlers (`SimulationStepper.performStream(simulation,
  "step")`), and handlers are `CompiledCallable`s in a
  `Map<EventKey, EventHandlerGroup>`. Durations (`for <expr>`) ride as
  constant-substep attributes the bridge reads like `steps.low`; year expressions
  ride as synthetic handlers reusing `evaluate(Scope)`. No new invocation path —
  this drops "entity build" and "per-step year eval" toward S.
- **Gate the year draw to its phase.** A simulation step handler fires every
  step, so the year expression must be evaluated *only* during its own phase
  (on-demand from the bridge inside `getDataTimestep()` is preferred), or it
  consumes RNG draws during observed/spin-down and shifts every other `sample`,
  breaking reproducibility.

### Open decisions

- ~~Discretization convention for a sampled year used as a grid index.~~
  **Resolved & implemented:** an opt-in `discrete uniform from X to Y` qualifier
  draws an integer over the inclusive range (no coercion); a plain `uniform`
  stays continuous. Chosen over type-directed inference specifically to avoid
  perturbing the RNG sequence of the ~180 existing integer-bound `uniform`
  usages (tutorials, paper, conformance) — that perturbation was verified to
  break seed-tuned tests. Backed by a `DISCRETE_` grammar token (also kept as a
  valid identifier) + `randUniformDiscrete()`; full distribution/stochastic
  conformance stays green.
- `meta.stepCount` moving to the anchored value (vs keeping the old absolute
  counter and exposing the anchor under a new name) — recommended as above.
- Merge/attach semantics so a `spinup`/`spindown` block can live in a separate
  imported file (shared with the exports-separation question in COMPOSITION.md).
- Whether to also generalize `external Name at <expression>` (currently only
  `at <integer>`) as a power-user escape hatch alongside the declarative blocks.
