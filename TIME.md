# Calendar / Clock (and Spin-up) Options

## The Problem

`meta.stepCount` does three jobs: engine loop counter, calendar year, data-file slice index.
`external X at N` conflates the last two, with no validation. Spin-up is a hand-rolled recipe with magic numbers (`steps.low/high` and a  step-triggered `state` transition.

**How much of this should the engine own vs. the model and author own?**

## Some solutions

### 1 — Manual (foundation only; today's recipe, unchanged)

`meta.year` stays the raw step counter relabeled. Era, offset, and forcing are 100% manual.

```
steps.low = -200 count
steps.high = 86 count

state.init = "spinup"
state.step:if(meta.stepCount >= 200 count) = "observed"

forcingYear.step
  :if(current.state == "spinup") = sample discrete uniform from 2015 count to 2100 count
  :else = meta.stepCount - 200 count + 2015 count      # user hand-rolls the offset too
```
**Pros:** no new engine machinery; unlimited forcing flexibility; safest vs. #479 relapse.
**Cons:** `meta.year` still fake; highest boilerplate; magic numbers, largest footgun surface.

### 2 — Calendar anchor (no era, no forcing)

Declare a calendar anchor parallel to `steps.low/high`; `meta.year` becomes real and auto-offset (same mechanism as today's `meta.stepCount` anchoring, smarter formula). Era and forcing stay entirely user-authored.

```
steps.low = -200 count
steps.high = 86 count
steps.calendarStart = 2015 year
steps.calendarUnit  = 1 year
# meta.year: 1815 at step -200, 2015 at step 0, 2100 at step 85 — engine-computed, honest
state.init = "spinup"
state.step:if(meta.stepCount >= 200 count) = "observed"
forcingYear.step
  :if(current.state == "spinup") = sample discrete uniform from 2015 year to 2100 year
  :else = meta.year
```
**Pros:** `meta.year` honest; smallest possible engine change (one field); era/forcing still unlimited.
**Cons:** era transitions still hand-rolled (medium footgun); user must know `>=` not `==`.

### 3 — Balanced (engine owns clock + era; model owns forcing)

`calendar` stanza replaces `steps.low/high` math; engine derives step count, `meta.era`, calendar-aware `meta.year`. Model still supplies `forcingYear` — any policy.

```
start calendar
  start era "spinup" from 1815 year to 2014 year
  start era "observed" from 2015 year to 2100 year
  start era "spindown" from 2101 year to 2500 year
end calendar

forcingYear.step
  :if(meta.era == "observed") = meta.year
  :elif(meta.era == "spinup") = sample discrete uniform from 2015 year to 2100 year
  :elif(meta.era == "spindown") = sample discrete uniform from 2080 year to 2100 year
```
**Pros:** engine owns clock + era bookkeeping (less boilerplate, no magic-number drift); forcing still fully model-owned, can very easily avoid thinking in `step` at all
**Cons:** observed window will change with external data forcing, still requires either config discipline OR diff simulation definitions OR external data metadata access (see below). Duplicative logic to `state` (though much more narrow, basically just aligning calendar year with step and assigning a first order 'era' attribute)

### 4 — Opinionated (engine owns clock, era, and forcing policy)

Forcing chosen from an engine menu; bare `external X` resolves automatically.

```
start calendar
  spinup 200 year
  from 2015 year to 2100 year by 1 year
  spindown 400 year
end calendar

septTempC.step = external FutureTempSep - 273.15   # engine substitutes the forcing year
```
**Pros:** lowest boilerplate; correct-by-default (no user-authored forcing bugs possible).
**Cons:** we own spinup and spindown logic (would need to implement our own logic and maintain those implementations) for things that the model author probably wants contorol over.

## Other considerations (mostly ergonomics and user protection from mis-indexing):

#### Convenience methods for external
Some desire to allow our simulation to be driven by external data (climate forcing length). For eg, we have 86 years of future data and 68 years (I think?) of historical data. Current strategy is to just manually toggle, in the `.jshc` the `steps.high` value to suit, but this is a magic number and easy to get wrong (and will waste compute and be quite frustrating, leading to an index-out-of-bounds error after 69 sim years in the case of historical, and just silently wrong results with wrong climate forcing in the case of future data).

A simple (minimal) helper would be to add convenience methods to josh to query some metadata from the `jshd` file such as:

```
steps.low = 0
steps.high = length of external precipitation

...

fireImpact.step
  :if(year == year of external fireRbr) = external fireRbr
  :else = 0 count
```

Such a change would make it easier to swap external data without needing to be so careful about config values or different simualtions. However, it also might encourage people to jam in all kinds of conditionals that would otherwise be more readable as different simulations.


#### Year-based indexing of external

1. `.jshd` carries its own time axis (baked in at preprocess time).
2. Reads disambiguated: `at step N` or `at index N`/ `at year Y` or `at time Y` / `at first`.
3. Out-of-range read is a hard error (file, requested coord, available span), misindex not possible when using a time index (at year) and fails loudly

```bash
java -jar joshsim.jar preprocess sim.josh Sim futureTempSep.nc temp celsius futureTempSep.jshdz \
  --time-start 2015 --time-unit year --time-count 86

java -jar joshsim.jar preprocess sim.josh Sim geologyFireRbr.tiff rbr rbr geologyFireRbr.jshdz \
  --time-instant 2020 --time-unit year
```

```
fireImpact.step = external geologyFireRbr at year 2020
fireImpact.step = external geologyFireRbr at year 2100 # fails due to preprocess mismatch
fireImpact.step = external geolofyFireRbr # works bare but more dangerous

precip.step = external precipitation at year 2050 # works with ssp245, fails with historical
precip.step = external precipitation at index 10 # works with either, but indexes different years (matches current behavior)
precip.step = external precipitation # maybe works bare and defaults to simulation step?
```

#### Spin-up toggle

Probably prefer having different simulation definitions because these seem like appropriate use case for different Simulation scenarios:

```
start Simulation Main
...
end Simulation

start Simulation Spinup
...
end Simulation
```

Alternative is to control spinup behavior with some sort of config var like:
```
start Simulation

  spinupYears.constant = config editor.spinupYears
  steps.low = 0 - spinupYears
  steps.high = 86
  ...

end simulation

(actual implementation depends on what we go with above, but probably would push us toward more magic number style )
```


DECISION -
design around external data metadata read


```

start Simulation Main

    spinupDuration.constant = config.spinupDuration
    spindownDuration.constant = config.spindownDuration

    scenarioStartYear.constant = first year of external precipitation  ## Language for this TBD
    scenarioEndYear.constant = last year of external precipitation  ## Language for this TBD

    steps.low = 0 count
    steps.high = (spinupDuration + length of external precipitation + spindownDuration) count

    # steps.calendarStart = scenarioStartYear
    steps.calendarStart = 2026
    steps.calendarUnit  = 1 year

    state.init = "spinup"
    state.step:if(meta.stepCount >= spinupDuration) = "observed"

    forcingYear.step
      :if(current.state == "spinup") = sample discrete uniform from (scenarioStartYear) to (scenarioStartYear + 10) year
      :if(current.state == "spindown") = sample discrete uniform from (scenarioEndYear - 10) to (scenarioEndYear) year
      :else = meta.year

end Simulation

external precip at year meta.forcingYear

```

Basically, we need:

- ability to get metadata from jshds (first year of ..., last year of ...)
- encode time dimensions into time dimension, index by calendar year (or whatever time dim is)


Need to validate:
- Initialize from mid simulation with a series of 'init through' calls

Also: do real dev / staging / main split