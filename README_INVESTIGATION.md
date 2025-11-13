# Josh Organism Discovery Bug Investigation

## Quick Start

This directory contains the **Phase 1: Deep Runtime Investigation** of the Josh organism discovery bug where organisms created via conditional handlers only execute their `.step` logic once.

### Reading Order

1. **START HERE**: `INVESTIGATION_INDEX.md` (7.8 KB)
   - Quick overview of what was investigated
   - Key findings summary
   - Specific bug locations
   - What Phase 2 should do

2. **EXECUTIVE SUMMARY**: `INVESTIGATION_SUMMARY.md` (5.9 KB)
   - High-level findings
   - Root cause explanation
   - Files examined checklist
   - Next steps

3. **DETAILED ANALYSIS**: `INVESTIGATION_REPORT.md` (22 KB)
   - Complete technical deep-dive
   - Code flow analysis
   - Timing diagrams
   - Three-layer root cause
   - Sequence diagrams

## The Bug in 30 Seconds

**Symptom**: Organisms created by conditional handlers (e.g., `Child.step:if(year==2025) = create count`) only execute their `.step` logic once.

**Root Cause**: A static handler cache doesn't distinguish between conditional handlers (which might not execute) and unconditional handlers (which always execute). When a conditional handler's condition is false, the attribute doesn't get set, so newly-created organisms can't be discovered. In subsequent timesteps, they're lost.

**Three Factors**:
1. Static cache marks attributes as "having handlers" without checking conditions
2. Fast-path optimization skips handler lookup based on this cache
3. Discovery mechanism can only find organisms stored in attributes

## Key Code Locations

| Component | File | Lines | Purpose |
|-----------|------|-------|---------|
| Discovery | `src/main/java/org/joshsim/lang/bridge/InnerEntityGetter.java` | 39-78 | Extracts child entities from attributes |
| Condition Parsing | `src/main/java/org/joshsim/lang/interpret/visitor/delegates/JoshFunctionVisitor.java` | 168-290 | Parses `:if` handlers |
| Handler Execution | `src/main/java/org/joshsim/lang/bridge/ShadowingEntity.java` | 673-694 | Evaluates conditions and runs handlers |
| Phase Loop | `src/main/java/org/joshsim/lang/bridge/SimulationStepper.java` | 73-105 | Orchestrates discovery and resolution |
| **Handler Cache** | `src/main/java/org/joshsim/engine/entity/base/EntityBuilder.java` | 179-228 | **BUG #1: Computation** |
| **Fast-Path** | `src/main/java/org/joshsim/lang/bridge/ShadowingEntity.java` | 626-629 | **BUG #2: Skips handlers** |
| | `src/main/java/org/joshsim/engine/entity/base/DirectLockMutableEntity.java` | 279-286 | **BUG #2: Cache check** |
| Attribute State | `src/main/java/org/joshsim/engine/entity/base/DirectLockMutableEntity.java` | 183-200 | **BUG #3?: Persistence** |

## Investigation Findings

### What Works: Unconditional Handlers
```josh
SpeciesA.start = prior.SpeciesA[...]
```
- Handler always executes
- Attribute always gets set
- Discovery finds organisms every timestep
- ✓ Works correctly

### What Breaks: Conditional Handlers
```josh
Child.step:if(year == 2025) = create count
```
- Handler only executes if condition is TRUE
- If condition is FALSE: attribute not set
- Discovery can't find organisms that don't exist
- Even if persisted: not discovered in later timesteps
- ✗ Breaks discovery

## Phase 1 Deliverables

| File | Size | Content |
|------|------|---------|
| `INVESTIGATION_INDEX.md` | 7.8 KB | Quick reference index |
| `INVESTIGATION_SUMMARY.md` | 5.9 KB | Executive summary |
| `INVESTIGATION_REPORT.md` | 22 KB | Detailed technical analysis |
| **TOTAL** | **36 KB** | **899 lines of analysis** |

## What Phase 2 Should Verify

1. **Attribute Persistence**
   - Are attributes created by handlers persisted correctly?
   - Does the freeze/thaw cycle handle them properly?
   - Is `onlyOnPrior` tracking correct?

2. **Handler Execution**
   - Do conditional handlers execute when conditions are true?
   - Are newly created entities properly stored?
   - What happens to entities between timesteps?

3. **Discovery Timing**
   - When exactly are entities discovered?
   - Are children discovered in the same timestep they're created?
   - Are children discoverable in subsequent timesteps?

4. **Cache Effects**
   - What does `hasNoHandlers()` return?
   - Does the cache prevent handler lookup?
   - Should the fast-path optimization be removed?

## Investigation Methodology

- **Scope**: Code-only analysis (no runtime execution)
- **Approach**: Static code analysis of key files
- **Depth**: Medium (focused on discovery and conditional handlers)
- **Files Analyzed**: 12 core files
- **Total Lines Examined**: ~5000 lines of Java code

## Key Insights

### Discovery Mechanism
- Works by reading attributes and extracting entities
- Called at TWO points per substep (before and after resolution)
- Relies on attributes being properly set

### Conditional Handler System
- Handlers with conditions are registered at entity type definition
- Stored as `EventHandler` + `CompiledSelector`
- Multiple handlers grouped into `EventHandlerGroup`
- Only handlers with TRUE conditions execute

### Phase Execution
- Each timestep: init → start → step → end
- For each phase: startSubstep → updateEntityUnsafe → endSubstep
- Discovery #1: Before resolution (finds existing entities)
- Discovery #2: After resolution (finds newly created entities)
- Recursion: Processes all discovered entities

### The Bug
Three factors interact to break conditional handlers:
1. Static type-level cache doesn't consider runtime conditions
2. Fast-path optimization assumes all registered handlers execute
3. Discovery mechanism can't find organisms not stored in attributes

## Files in This Investigation

```
/workspaces/josh/
├── INVESTIGATION_INDEX.md          (Start here!)
├── INVESTIGATION_SUMMARY.md        (Executive summary)
├── INVESTIGATION_REPORT.md         (Detailed analysis)
└── README_INVESTIGATION.md         (This file)
```

## Next Steps

1. **Read `INVESTIGATION_INDEX.md`** for a quick overview
2. **Review `INVESTIGATION_REPORT.md`** for detailed analysis
3. **Use findings** to guide Phase 2 investigation
4. **Focus Phase 2 on**: Attribute persistence and handler execution state

---

**Investigation Status**: ✓ Complete  
**Thoroug Depth**: Medium  
**Files Examined**: 12  
**Code Lines Analyzed**: ~5000  
**Total Report**: 36 KB, 899 lines
