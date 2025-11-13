# Deep Runtime Investigation - Complete Index

## Phase 1: Deep Runtime Investigation (COMPLETE)

### Objective
Thoroughly investigate the Josh runtime code to understand organism discovery mechanism and why conditional handlers break it.

### Deliverables

#### Primary Report
- **File**: `INVESTIGATION_REPORT.md` (22 KB, 557 lines)
- **Sections**:
  1. Executive Summary
  2. Discovery Mechanism Flow (InnerEntityGetter.java:39-78)
  3. Conditional Handler Processing (JoshFunctionVisitor.java:168-290)
  4. Timing & Sequencing (SimulationStepper.java:73-105)
  5. The Bug: Attribute Caching System (EntityBuilder.java:179-228)
  6. Root Cause Analysis (3-layer problem)
  7. Specific Bug Locations with code references
  8. Why Discovery Only Happens Once
  9. Sequence Diagram: Bug in Action
  10. Conclusion & Summary Tables

#### Executive Summary
- **File**: `INVESTIGATION_SUMMARY.md` (5.9 KB, 125 lines)
- **Contents**:
  - Investigation goal and scope
  - Key findings at a glance
  - Three-factor root cause
  - Specific bug locations table
  - What Phase 2 should investigate
  - Files examined checklist
  - Quick reference conclusion

### Key Findings

#### Discovery Mechanism
**Location**: `src/main/java/org/joshsim/lang/bridge/InnerEntityGetter.java:39-78`

The discovery system:
- Iterates all attributes via `getAttributeValue(index)`
- Extracts any attributes containing child entities
- Returns `Iterable<MutableEntity>` for processing
- Called at TWO points: `startSubstep()` and `updateEntityUnsafe()`

#### Conditional Handler Processing
**Location**: `src/main/java/org/joshsim/lang/interpret/visitor/delegates/JoshFunctionVisitor.java:168-290`

Conditional handlers (`:if(condition) = ...`):
- Parsed into `EventHandler` + `CompiledSelector`
- Multiple handlers in `EventHandlerGroup`
- Registered at ENTITY TYPE DEFINITION time
- Only handlers with TRUE conditions execute
- If no match: attribute resolves from prior

#### Phase Execution Order
**Location**: `src/main/java/org/joshsim/lang/bridge/SimulationStepper.java:73-105`

Per timestep:
1. `startStep()` - Initialize
2. For each event (init, start, step, end):
   - `startSubstep()` - Discovery #1 (BEFORE resolution)
   - `updateEntityUnsafe()` - Resolution + Discovery #2 (AFTER) + Recursion
   - `endSubstep()` - Cache cleanup
3. `endStep()` - Finalize

### Root Cause: Three Interacting Factors

#### Factor 1: Static Handler Caching
**File**: `EntityBuilder.java:179-228`

The `attributesWithoutHandlersBySubstep` cache:
- Computed ONCE per entity type at compilation
- Immutable map: `substep → boolean[]`
- Never invalidated during runtime
- Only checks if handler EXISTS, not if condition is TRUE

#### Factor 2: Fast-Path Optimization  
**Files**: 
- `ShadowingEntity.java:626-629`
- `DirectLockMutableEntity.java:279-286`

The `hasNoHandlers()` method:
- Fast-path to skip expensive handler lookups
- Assumption: If handler exists, it will execute
- Problem: Conditional handlers might not execute!

#### Factor 3: Discovery Depends on Resolution
**File**: `InnerEntityGetter.java:39-78`

Discovery mechanism:
- Calls `getAttributeValue()` on all attributes
- If attribute not set: `Optional.empty()`
- Discovery skips attributes with no entities
- Problem: Handler-created entities can't be discovered if handler doesn't execute

### Bug Scenario

**Given**: `ParentTree.step:if(year == 2025) = create count of Child`

**Timestep 0** (year = 2024):
- Handler condition FALSE → no execution
- Child attribute remains empty
- No children discovered

**Timestep 1** (year = 2025):
- Handler condition TRUE → executes!
- Children created and stored in Child attribute
- Discovery finds and processes children
- Children are now active

**Timestep 2+** (year > 2025):
- Should find children from previous timestep
- **QUESTION**: Why aren't they discoverable?
  - Lost in freeze/thaw?
  - `onlyOnPrior` tracking broken?
  - Wrong discovery timing?

### Files Investigated

#### Core Discovery & Resolution
- ✓ `src/main/java/org/joshsim/lang/bridge/InnerEntityGetter.java`
- ✓ `src/main/java/org/joshsim/lang/bridge/ShadowingEntity.java`
- ✓ `src/main/java/org/joshsim/lang/bridge/SimulationStepper.java`

#### Handler System
- ✓ `src/main/java/org/joshsim/lang/interpret/visitor/delegates/JoshFunctionVisitor.java`
- ✓ `src/main/java/org/joshsim/engine/entity/handler/EventHandler.java`
- ✓ `src/main/java/org/joshsim/engine/entity/handler/EventHandlerGroup.java`
- ✓ `src/main/java/org/joshsim/engine/entity/handler/EventHandlerGroupBuilder.java`
- ✓ `src/main/java/org/joshsim/engine/entity/handler/EventKey.java`

#### Attribute & Cache System
- ✓ `src/main/java/org/joshsim/engine/entity/base/EntityBuilder.java`
- ✓ `src/main/java/org/joshsim/engine/entity/base/DirectLockMutableEntity.java`

### Specific Bug Locations

| # | File | Lines | Issue |
|---|------|-------|-------|
| 1 | `EntityBuilder.java` | 203-220 | Cache marks "has handlers" without checking condition |
| 2 | `ShadowingEntity.java` | 626-629 | Fast-path uses static cache ignoring conditions |
| 2b | `DirectLockMutableEntity.java` | 279-286 | `hasNoHandlers()` implementation |
| 3 | `DirectLockMutableEntity.java` | 183-200 | Potential: `onlyOnPrior` tracking issue |

### Why Unconditional Handlers Work

For `SpeciesA.start = prior.SpeciesA[...]`:
- No `CompiledSelector` → no condition to fail
- `hasNoHandlers()` returns FALSE (handler IS registered)
- Handler lookup ALWAYS performed
- Handler ALWAYS executes
- Attribute ALWAYS gets set
- Attribute persists through timesteps
- Discovery finds entities every timestep

### Why Conditional Handlers Break

For `Child.step:if(year==2025) = create count`:
- Handler IS registered
- But handler only executes if condition is TRUE
- If condition is FALSE early: attribute stays empty
- Discovery can't find what doesn't exist
- Newly-created entities may not persist properly
- Even if they do persist, aren't discovered in later timesteps

### What Phase 2 Should Investigate

1. **Attribute Persistence**
   - Do attributes set by handlers persist through freeze/thaw?
   - Is `onlyOnPrior` tracking correct for handler-set attributes?
   - Does `getAttributeValue()` properly find prior values?

2. **Handler Execution State**
   - Do conditional handlers actually execute when condition is true?
   - Are attribute state changes properly recorded?
   - Are newly created entities properly added to attributes?

3. **Discovery Timing**
   - Does discovery find entities after resolution?
   - Are children discovered in first timestep when created?
   - Are children discovered in subsequent timesteps?

4. **Cache Behavior**
   - What does `hasNoHandlers()` return for conditional handlers?
   - Does cache affect handler lookup?
   - Should fast-path be removed for conditional handlers?

### Conclusion

The organism discovery mechanism is CORRECT algorithmically.

The bug occurs because:
1. Conditional handlers registered at TYPE DEFINITION time
2. Static cache marks "has handlers" if ANY handler exists
3. But conditional handlers may NOT EXECUTE if condition is false
4. When handler doesn't execute, attribute doesn't get set
5. Discovery can only find entities STORED IN ATTRIBUTES
6. Organisms created by conditional handlers are LOST if not properly stored

### Quick Reference

| Item | Location |
|------|----------|
| Discovery | `InnerEntityGetter.java:39-78` |
| Conditional Parsing | `JoshFunctionVisitor.java:168-290` |
| Handler Execution | `ShadowingEntity.java:673-694` |
| Phase Loop | `SimulationStepper.java:73-105` |
| Handler Cache | `EntityBuilder.java:179-228` |
| Fast-Path Check | `DirectLockMutableEntity.java:279-286` |
| Attribute Freeze | `DirectLockMutableEntity.java:183-200` |

---

**Investigation Status**: Complete  
**Next Phase**: Phase 2 - Verify attribute persistence and handler execution  
**Thoroughness Level**: Medium (focused on discovery and conditional handlers)
