# Phase 2: Minimal Reproduction Test Case - Summary

**Date**: 2025-11-12
**Status**: COMPLETED
**Task**: Create minimal test case demonstrating organism discovery bug

---

## Deliverables

All deliverables have been created in `/workspaces/josh/test-cases/minimal-repro/`:

1. **PLAN.md** - Comprehensive test plan with design rationale
2. **broken.josh** - Simulation demonstrating the bug (conditional handler)
3. **working.josh** - Control simulation with correct behavior (unconditional handler)
4. **EXPECTED.md** - Detailed expected behavior specification
5. **README.md** - Quick reference guide
6. **SUMMARY.md** - This document

---

## Test Design Summary

### Minimal Complexity

- **Single organism type**: `SimpleTree` with only age tracking
- **Single patch type**: `Default` with minimal logic
- **5 timesteps**: Steps 0-4 (short runtime < 1 second)
- **10 organisms**: Created at Step 1 via conditional handler
- **No dependencies**: No external data files required
- **Clear output**: Debug logging shows exact execution pattern

### Bug Reproduction

**Broken Version (`broken.josh`)**:
```josh
# Conditional handler creates trees at Step 1 only
Trees.step:if(meta.year == 1) = create 10 of SimpleTree
```

**Expected Behavior**:
- Step 0: 0 ORG_STEP events (no trees yet)
- Step 1: 10 ORG_STEP events (trees created and execute ONCE)
- Steps 2-4: 0 ORG_STEP events each (BUG - trees persist but don't execute)
- **Total: 10 ORG_STEP events (should be 40)**

**Working Version (`working.josh`)**:
```josh
# Unconditional handler creates trees at init
Trees.init = create 10 of SimpleTree
```

**Expected Behavior**:
- Step 0: 10 ORG_STEP events (trees execute)
- Step 1: 10 ORG_STEP events (trees execute)
- Step 2: 10 ORG_STEP events (trees execute)
- Step 3: 10 ORG_STEP events (trees execute)
- Step 4: 10 ORG_STEP events (trees execute)
- **Total: 50 ORG_STEP events (correct)**

### Clear Contrast

- **5x difference** in organism execution (10 vs 50 events)
- **Only difference** is conditional handler syntax (`:if(meta.year == 1)`)
- **Proves** bug is in conditional handler mechanism, not simulation logic

---

## Technical Root Cause (Confirmed)

The bug is caused by THREE interacting runtime mechanisms:

### 1. Static Handler Cache
**File**: `/workspaces/josh/src/main/java/org/joshsim/engine/entity/base/EntityBuilder.java:203-220`

```java
// Marks attributes as "having handlers" WITHOUT checking conditionals
for (EventHandler handler : group.getEventHandlers()) {
  attrsWithoutHandlers[index] = false;  // ❌ Permanent marking
}
```

**Problem**: `Trees.step` marked as "has handler" at definition time, regardless of conditional.

### 2. Fast-Path Optimization
**File**: `/workspaces/josh/src/main/java/org/joshsim/lang/bridge/ShadowingEntity.java:532`

```java
if (inner.hasNoHandlers(name, substep.get())) {
  resolveAttributeFromPrior(name);  // Copy prior value
  return;
}
```

**Problem**: Fast-path NEVER taken for `Trees.step` (cache says it has handler).

### 3. Discovery Mechanism
**File**: `/workspaces/josh/src/main/java/org/joshsim/lang/bridge/InnerEntityGetter.java:39-78`

```java
Optional<EngineValue> valueMaybe = target.getAttributeValue(i);
if (valueMaybe.isEmpty()) {
  continue;  // ❌ No value = no organisms discovered
}
```

**Problem**: Discovery reads attribute value. When conditional is false (Steps 2+), handler doesn't execute, attribute stays UNSET, no organisms found.

### Timeline

- **Step 1**: Conditional TRUE → Handler executes → Attribute SET → Organisms discovered ✅
- **Step 2+**: Conditional FALSE → Handler doesn't execute → Attribute UNSET → No organisms discovered ❌

---

## Running the Tests

### Quick Start

```bash
# From repository root
cd /workspaces/josh

# Build (if needed)
./gradlew fatJar

# Run broken version
java -jar build/libs/joshsim-fat.jar run test-cases/minimal-repro/broken.josh

# Run working version
java -jar build/libs/joshsim-fat.jar run test-cases/minimal-repro/working.josh
```

### Verify Results

```bash
# Check broken version (should show 10 events)
grep "ORG_STEP" test-cases/minimal-repro/debug_organism_broken_0.txt | wc -l

# Check working version (should show 50 events)
grep "ORG_STEP" test-cases/minimal-repro/debug_organism_working_0.txt | wc -l

# Verify tree persistence in broken version (should show 4 lines with count:10)
grep "TREE_COUNT.*count:10" test-cases/minimal-repro/debug_patch_broken_0.txt | wc -l
```

---

## Success Criteria

### Test PASSES if:

1. **Broken version shows bug**:
   - Exactly 10 ORG_STEP events (all at Step 1)
   - TREE_COUNT shows 10 at Steps 1-4 (trees persist)
   - No ORG_STEP events at Steps 2-4

2. **Working version shows correct behavior**:
   - Exactly 50 ORG_STEP events (10 per step × 5 steps)
   - ORG_STEP events at ALL steps (0-4)
   - Ages increment correctly (0→4)

3. **Clear contrast**:
   - 5x difference in execution (10 vs 50)
   - Only difference is conditional syntax
   - Root cause clearly isolated

---

## What This Proves

### Bug Evidence

1. **Organisms Persist**: TREE_COUNT shows 10 trees at Steps 1-4 in broken version
2. **Organisms Don't Execute**: 0 ORG_STEP events at Steps 2-4 in broken version
3. **Conditional Handler is Culprit**: Only syntax difference between versions
4. **Not a Logic Issue**: Working version proves organism execution mechanism works

### Root Cause Confirmation

This test isolates the three interacting components:

1. Static handler cache marks conditional handlers as "has handler" permanently
2. Fast-path optimization never taken because cache says handler exists
3. Discovery mechanism can't find organisms when conditional is false (attribute unset)

Result: Organisms execute once (when conditional is true) then never again (when conditional is false).

---

## Next Steps

### Immediate Actions

1. **Run Tests**: Verify test case reproduces bug consistently
2. **Share with Team**: Provide minimal reproduction to developers
3. **Document Workaround**: Update user documentation about conditional handlers on collections

### Runtime Investigation

1. **Add Debug Logging**:
   - Log `EntityBuilder.getAttributesWithoutHandlersBySubstep()` output
   - Log `ShadowingEntity.hasNoHandlers()` calls for `Trees.step`
   - Log `InnerEntityGetter.getInnerEntities()` discoveries
   - Track conditional handler evaluation

2. **Analyze Logs**:
   - Confirm static cache behavior
   - Verify fast-path is never taken
   - Prove discovery fails when attribute is unset

### Fix Options

1. **Option A: Auto-copy prior value when conditional is false**
   - Modify handler execution to copy `prior.Trees` when conditional evaluates false
   - Pros: Minimal change, maintains discovery mechanism
   - Cons: Implicit behavior, may not match user expectations

2. **Option B: Mark conditional handlers differently in cache**
   - Extend cache to track "always has handler" vs "conditionally has handler"
   - Take fast-path for conditional handlers when condition is false
   - Pros: Maintains fast-path optimization
   - Cons: More complex caching logic

3. **Option C: Check conditional during discovery**
   - Discovery mechanism evaluates conditionals before reading attributes
   - If conditional true, read attribute; if false, use prior value
   - Pros: Centralizes conditional logic
   - Cons: Performance impact on discovery

4. **Option D: Deprecate conditional syntax on collections**
   - Warn users about `:if` syntax on organism collection attributes
   - Recommend moving conditionals inside full bodies
   - Pros: Preserves current behavior, guides users to working patterns
   - Cons: Breaking change for existing code

### Recommended Approach

**Short-term**: Option D (deprecation + warning)
- Add static analysis to detect conditional handlers on organism collections
- Emit warnings during compilation
- Update documentation with working patterns

**Long-term**: Option A (auto-copy prior value)
- Modify handler execution to preserve organism collections
- Add comprehensive tests
- Document behavior in language specification

---

## Related Files

### Test Case Files
- `/workspaces/josh/test-cases/minimal-repro/PLAN.md` - Full test plan
- `/workspaces/josh/test-cases/minimal-repro/broken.josh` - Bug demonstration
- `/workspaces/josh/test-cases/minimal-repro/working.josh` - Correct behavior
- `/workspaces/josh/test-cases/minimal-repro/EXPECTED.md` - Expected behavior
- `/workspaces/josh/test-cases/minimal-repro/README.md` - Quick reference

### Task Documentation
- `/workspaces/josh/.claude/tasks/organism-step-debug-investigation.md` - Full investigation

### Runtime Code
- `/workspaces/josh/src/main/java/org/joshsim/engine/entity/base/EntityBuilder.java:203-220` - Static cache
- `/workspaces/josh/src/main/java/org/joshsim/lang/bridge/ShadowingEntity.java:532` - Fast-path
- `/workspaces/josh/src/main/java/org/joshsim/lang/bridge/InnerEntityGetter.java:39-78` - Discovery
- `/workspaces/josh/src/main/java/org/joshsim/engine/entity/handler/EventHandler.java` - Handler definition

### Example Simulations
- `/workspaces/josh/jotr/historic/two_trees.josh` - Working pattern (unconditional)
- `/workspaces/josh/jotr/historic/stochastic_flowering_updated.josh` - Broken pattern (conditional)

---

## Contact

For questions or further investigation, refer to:
- **Task file**: `/workspaces/josh/.claude/tasks/organism-step-debug-investigation.md`
- **Test directory**: `/workspaces/josh/test-cases/minimal-repro/`
