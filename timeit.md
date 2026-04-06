# Josh Timing
Adding simple evaluation timing functionality to Josh.

## Objective
While we have engaged the [Java Flight Recorder](https://www.baeldung.com/java-flight-recorder-monitoring) to determine where Josh itself can be made more efficient, this view does not necessarily assist in profiling Josh simulations themselves. In other words, the current tools focus on places where, for example, String.compareTo is taking up CPU cycles but not that averageHeight is taking a long time to compute. Ultimately, this means that current tooling assists those building Josh but not necessarily those using it.

We may consider adding a `.evalDuration` suffix to determine the duration it look to resolve a value.

### User experience
This allows users to treat this as a standard variable in a new built in milliseconds unit. Therefore, one can keep track of total time spent in resolving an attribute across the simulation:

```
totalHeightEvalDuration.step = sum(Trees.height.evalDuration)
```

Additionally, one may consider reporitng the mean of a series of internal agents per-step:

```
export.avgHeightEvalDuration.step = mean(Trees.height.evalDuration)
```
  

### Constraints
Anything on ValueResolver is on the hot path so we have to be mindful that runs without the need for timing avoid that machinery. Even an if statement could become problematic. The best move is probably some kind of setting at the point of invoking Josh to enable timing (`--enable-profiler` is proposed). However, we also have to be mindful of not carrying too many shared instances around the codebase.

## Design
The cleanest option is probably a shift to a decorator. `ValueResolver` would become an interface and a new implementer called `RecursiveValueResolver` would take the current logic which recuses on different path components. Then, an additional `TimedValueResolver` would act as a decorator around a `ValueResolver` and capture the time spent in resolving its path. Then, a synthetic `{path}.evalDuration` attribute with milliseconds for last resolve would become available and be intercepted by `TimedValueResolver`. If `RecursiveValueResolver` is asked for `evalDuration`, it will return -1. 

To keep the hot path clear, it is recommended that `EngineValueFactory` hold onto to a `ValueResolverFactory` and expose a `buildValueResolver()` method. The `EngineValueFactory` is already transmitted throughout code and the two objects are related. Conceptually, the `ValueResolver` is another pathway for building new `EngineValues` so the name may remain appropriate but it could also change to `ValueSupportFactory` to signal that it has some additional logic. Nevertheless, this will avoid a conditional as the `EngineValueFactory` can be given a `ValueResolverFactory` at construction. This could also potentially serve to memoize ValueResolver instances if desired in the future.  

### Josh Editor
The Josh Editor will require an additional checkbox in the run dialog (see `RunPanelPresenter`) in order to specify if the profiler logic should run. This means that the WASM pathway will need to take another parameter (see `WasmEnigneBackend` and `JoshJsSimFacade`).

### Differential
This is largely exploring functionality similar to time it and other related libraries. There are other options:

- Full profiler with CPU sampling similar to the JFR.
- This focuses on CPU not memory and either sampling or an expanded inspection could offer that information.
- We could wait to further design or implement this until running larger scale realistic simulations to see where the priorities should lie.

We currently are saying no to these differential options.

### Limitations
For this initial implementation, we will not allow enabling the profiler from the WASM path.

### Examples
Both of these examples would be valid and likely very similar but not exactly the same:

```
totalAge.step = sum(here.SomeFilteredTrees.age)
export.totalAgeTimeOuter = totalAge.evalDuration
export.totalAgeTimeInner = sum(
    here.SomeFilteredTrees.age.evalDuration
)
```

## Procedure
We have a 4 subagent flow we will use to implement this time since most of the patterns needed are already present in the codebase so we can follow existing modeled conventions. For each component we will conduct the following flow:

 - We will pass the full path to task spec document (and the name / number of the component to work on) to `component-planner` which will write to the task document with additional details.
 - We will pass the full path to the task document (and the name / number of the component to work on) to `component-implementer` which will make the first implementation but will not concern itself with style violations.
 - We will pass the full path to the task document (and the name / number of the component to work on) to `component-validator` which will clean up and improve implementation with style violations resolved.
 - We will pass the full path to the task document (and the name / number of the component to work on) to `component-concluder` which will commit, summarize the component in the task file to just a few sentences to describe what was done, and reference the commit hash. This will keep the spec file from getting too long.
 - The coordinating agent will check after the 4 agents are done if it is safe to continue to the next component. If something uncertain came up that requires user input, it will pause. Otherwise, it will continue to the next component.

Note that all components except component 10 will go through this flow. All commits should list Claude as a co-author.

## Components
We will go one component at a time to implement.

### 1. Rearrange ValueResolver to interface
We will move the current implementation of `ValueResolver` to `RecursiveValueResolver` and then have it implement `ValueResolver`. Where possible across the codebase, we will use the `ValueResolver` type. At this point, `evalDuration` will not exist. Acceptance criteria will be that existing tests and style checks pass.

### 2. Add passive evalDuration
We should have `RecursiveValueResolver` return 0 if `evalDuration` is the final part of the attribute path resolved. We should be careful to maybe pre-compute this in constructor (saving to a bool) in order to avoid expensive string compare on the hot path. Acceptance criteria will be a new unit test that ensures 0 is indeed returned. This can be a test in an actual live Josh script (see `examples` directory) as well as a unit test on `RecursiveValueResolver` itself.

**Update from the original design:** From our conversation, we are doing 0 instead of -1.

### 3. Ensure evalDuration is reserved
Let’s look into use of `ValueResolver`. If the user tries to set the value for `evalDuration` via a Josh script (see `eventHandlerGeneral`), it should result in an error as that name is reserved. As with component 2, a unit test and a new example run and validated through `examples` may provide good acceptance criteria.

### 4. Add ValueResolverFactory
We should add a `ValueResolverFactory` interface with two implementers. The first should be a `RecursiveValueResolverFactory` that yields a `RecursiveValueResolver` without decoration. At this stage, we can just add a unit test for these new classes but not actually integrate them in code (at this stage, it is desirable to maintain direct calls to the `ValueResolver` constructor that do not go through the factory).

### 5. Move to ValueSupportFactory
We should next rename `EngineValueFactory` to `ValueSupportFactory` and add a `buildValueResolver()` method. At this stage, this can return a `RecursiveValueResolver` which was built at factory constructor but it should use `ValueResolverFactory` as the return type. Acceptance will be passing unit tests and other checks but limiting direct calls to the value resolver constructor to the factory and tests. Otherwise, the factory should be used where possible.

### 6. Add milliseconds
Add milliseconds as a built in type. Acceptance can be similar testing to the built in meters type. See `METER_UNITS`.

### 7. Add TimedValueResolver
The new `TimedValueResolver` should implement `ValueResolver` but its constructor should take an inner `ValueResolver`. This “decorator” pattern will then hold onto the number of milliseconds it took to run last time. This value should be returned with milliseconds as its units when `evalDuration` is requested. Therefore, this decorator will intercept requests for `evalDuration` and return without going to its inner `ValueResolver` in this case. Otherwise, it will pass through with that timing happening “in the background” (transparent to the user). At this stage, this object cannot be used in the actual Josh runtime so acceptance will be unit testing of the new class following existing unit testing patterns.

### 8. Integrate TimedValueResolver
Next, let’s add a `TimedRecursiveValueResolverFactory` which builds a `RecursiveValueResolver` but decorates it with a `TimedValueResolver` before returning. At this stage, `ValueSupportFactory` will not use this new object. However, for acceptance, we can add unit tests for this new factory to be integrated in later components.

### 9. Add enable profiler to CLI
We should next add an `—-enable-profiler` flag to the CLI execution path. See `org.joshsim.command`. This should control how the `ValueSupportFactory` is instantiated. If this flag is present, the `ValueSupportFactory` constructor should be passed a `TimedRecursiveValueResolverFactory`. Otherwise, it should be passed a `RecursiveValueResolverFactory`. The web editor path should temporarily force `RecursiveValueResolverFactory` until the next component. Acceptance should include adding an example which uses timing and including in CI / CD under validate examples a script which uses `evalDuration` where it is run both with and without the profiler command.

### 10. Profiler check
We should compare to the `dev` branch. We may consider running the stress test (see `examples/stress.sh`) when we clean and rebuild from `dev`. Then, we can clean and rebuild from the new feature branch before running both with and without `—-enable-profiler`. Using the Java Flight Recorder, let’s confirm that this experiment did not introduce any major inefficiency without `—-enable-profiler` and let’s understand the extent of the hit when `—-enable-profiler` is included. This can be summarized in the GitHub PR for review. Note, for this component only, we will not use the subagent flow. This can be completed by the coordinating agent.
