# Phase 1: Deep Runtime Investigation - Final Summary

## Investigation Goal
Thoroughly analyze the Josh runtime code to understand how organism discovery works and why conditional handlers (`:if`) break it, causing organisms created dynamically to only execute their `.step` logic once.

## Deliverable Location
**Main Report**: `/workspaces/josh/INVESTIGATION_REPORT.md`

This document contains:
- Complete flow analysis of discovery mechanism
- Detailed conditional handler processing walkthrough
- Timing & sequencing of phase execution
- The bug mechanism with sequence diagrams
- Three-layer root cause analysis
- Specific bug locations with code references
- Summary tables comparing conditional vs unconditional handlers

## Key Findings At a Glance

### Discovery Mechanism (InnerEntityGetter.java:39-78)
The discovery system works by:
1. Iterating through all attributes of an entity
2. Calling `getAttributeValue(i)` which triggers resolution if needed
3. Extracting any attributes containing child entities
4. Returning them as an Iterable<MutableEntity>

**Timing**: Called at two points per substep:
- **startSubstep()**: Before attribute resolution
- **updateEntityUnsafe()**: After resolution, during recursion

### Conditional Handler Processing (JoshFunctionVisitor.java:168-290)
Handlers with conditions (`:if`) are parsed differently:
- Created as EventHandler objects with optional CompiledSelector
- Multiple handlers (if/elif/else) stored in EventHandlerGroup
- **Registered at entity TYPE definition time** (same as unconditional)
- During execution: Only handlers with TRUE conditions run

### Phase Execution Order (SimulationStepper.java:73-105)
Per timestep:
1. `startStep()` - Initialize
2. For each event (init, start, step, end):
   - `startSubstep(event)` - Discovery #1
   - `updateEntityUnsafe()` - Resolution + Discovery #2 + Recursion
   - `endSubstep()` - Cleanup
3. `endStep()` - Finalize

### The Bug: Handler Caching System

**Location**: `EntityBuilder.java:179-228` (computation) + `DirectLockMutableEntity.java:279-286` (usage)

The `attributesWithoutHandlersBySubstep` cache:
- Computed ONCE per entity type at compilation
- Marks which attributes have NO handlers for each substep
- Used for fast-path optimization in `hasNoHandlers()`
- **PROBLEM**: Only checks if handler EXISTS, not if condition is TRUE

**The Issue**: 
- For `Child.step:if(condition) = create`:
  - Handler IS registered
  - Cache marks attribute as "having handlers"
  - But handler only executes if condition is true
  - Fast-path still skips handler lookup if condition might be false
  - Attribute never gets set by the handler
  - Discovery can't find children because they don't exist

### Why Unconditional Handlers Work
- No CompiledSelector = no condition to fail
- `hasNoHandlers()` returns FALSE (handler IS registered)
- Handler lookup ALWAYS performed
- Handler always executes and sets attribute
- Children persist and are discovered next timestep

## Three-Factor Root Cause

The bug isn't in any single component. It's the interaction of three factors:

1. **Static Handler Caching**: `attributesWithoutHandlersBySubstep` is type-level, computed once, never invalidated, doesn't account for condition evaluation

2. **Fast-Path Optimization**: `hasNoHandlers()` is used to skip expensive handler lookups, but assumes handlers that exist will always execute

3. **Discovery Dependency**: Discovery mechanism relies on attributes being properly resolved. If handlers don't execute, entities aren't stored in attributes and can't be discovered

## Specific Bug Locations

| Location | File | Issue |
|----------|------|-------|
| Bug #1 | EntityBuilder.java:203-220 | Cache marks "has handlers" without checking condition truth |
| Bug #2 | ShadowingEntity.java:626-629<br>DirectLockMutableEntity.java:279-286 | `hasNoHandlers()` uses static cache that doesn't account for conditional execution |
| Bug #3 | DirectLockMutableEntity.java:183-200 | Possible: `onlyOnPrior` tracking may not preserve attributes set by conditional handlers |

## What Needs Further Investigation

Phase 2 should verify:
1. Where exactly handler-set attributes are stored in the attributes array
2. How the `onlyOnPrior` tracking affects attributes set by conditional handlers
3. Whether attributes persist correctly through freeze/thaw cycles
4. Why newly created children from conditional handlers aren't discovered in subsequent timesteps

## Files Examined

- ✓ InnerEntityGetter.java - Discovery mechanism
- ✓ ShadowingEntity.java - Discovery invocation & attribute resolution
- ✓ SimulationStepper.java - Phase execution & entity updates
- ✓ EntityBuilder.java - Handler cache computation
- ✓ DirectLockMutableEntity.java - Attribute state & handler lookup
- ✓ JoshFunctionVisitor.java - Conditional handler parsing
- ✓ EventHandler.java - Handler representation
- ✓ EventHandlerGroup.java - Handler grouping
- ✓ EventKey.java - Handler caching keys

## Conclusion

The organism discovery mechanism works correctly in principle. The bug occurs because:

1. **Conditional handlers are registered at type definition** just like unconditional handlers
2. **The handler cache marks attributes as "having handlers"** if any handler exists, but doesn't distinguish between conditional and unconditional
3. **But conditional handlers may not execute** if their condition is false
4. **When a handler doesn't execute, the attribute doesn't get set** and organisms created by the handler can't be discovered
5. **In subsequent timesteps, organisms created in previous timesteps aren't discovered** because they were never added to parent attributes in the first place

The fix requires either:
- Invalidating/updating cache when conditional handlers execute
- Removing fast-path optimization for attributes with conditional handlers
- Tracking dynamically-created entities separately from statically-defined attributes
- Better handling of attribute state persistence across freeze/thaw cycles
