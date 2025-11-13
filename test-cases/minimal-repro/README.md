# Minimal Reproduction Test Case

**Bug**: Conditional handlers on organism collections prevent organism discovery after first execution

**Status**: Ready for testing

---

## Quick Start

```bash
# From repository root
cd /workspaces/josh

# Build Josh (if not already built)
./gradlew fatJar

# Run broken version (demonstrates bug)
java -jar build/libs/joshsim-fat.jar run test-cases/minimal-repro/broken.josh

# Run working version (demonstrates correct behavior)
java -jar build/libs/joshsim-fat.jar run test-cases/minimal-repro/working.josh

# Check results
grep "ORG_STEP" test-cases/minimal-repro/debug_organism_broken_0.txt | wc -l
# Expected: 10 (should be 40 - BUG)

grep "ORG_STEP" test-cases/minimal-repro/debug_organism_working_0.txt | wc -l
# Expected: 50 (correct)
```

---

## Files in This Directory

- **PLAN.md** - Comprehensive test plan and design rationale
- **EXPECTED.md** - Detailed expected behavior specification with verification criteria
- **broken.josh** - Simulation demonstrating the bug (conditional handler)
- **working.josh** - Simulation demonstrating correct behavior (unconditional handler)
- **README.md** - This file (quick reference)

---

## What This Test Demonstrates

### The Bug

Organisms created via conditional handlers (e.g., `Trees.step:if(year==1) = create 10 of SimpleTree`) execute their `.step` logic ONLY at the timestep they're created, never again in subsequent timesteps.

### Evidence

**Broken Version (broken.josh)**:
- Creates 10 trees at Step 1 using `Trees.step:if(meta.year == 1)`
- Trees execute `.step` logic once (10 ORG_STEP events at Step 1)
- Trees persist but don't execute at Steps 2-4 (0 ORG_STEP events)
- Total: 10 ORG_STEP events (should be 40)

**Working Version (working.josh)**:
- Creates 10 trees at init using `Trees.init`
- Trees execute `.step` logic at ALL steps (10 ORG_STEP events per step)
- Total: 50 ORG_STEP events (correct)

### Root Cause

Three interacting runtime mechanisms:

1. **Static Handler Cache** (`EntityBuilder.java:203-220`)
   - Marks `Trees.step` as "has handler" at definition time
   - Doesn't distinguish conditional from unconditional handlers

2. **Fast-Path Optimization** (`ShadowingEntity.java:532`)
   - Skips handler lookup when cache says "no handlers"
   - Never taken for `Trees.step` (cache says it has handler)

3. **Discovery Mechanism** (`InnerEntityGetter.java:39-78`)
   - Discovers organisms by reading attribute values
   - Step 1: Handler executes, attribute SET, organisms found ✅
   - Step 2+: Handler doesn't execute (condition false), attribute UNSET, organisms NOT found ❌

---

## Expected Output

### Broken Version

```
Step 0: 0 ORG_STEP events (no trees yet)
Step 1: 10 ORG_STEP events (trees created and execute ONCE)
Step 2: 0 ORG_STEP events (BUG - trees persist but don't execute)
Step 3: 0 ORG_STEP events (BUG)
Step 4: 0 ORG_STEP events (BUG)

Total: 10 ORG_STEP events (should be 40)
```

### Working Version

```
Step 0: 10 ORG_STEP events (trees created in init, execute first step)
Step 1: 10 ORG_STEP events (trees execute, age=1)
Step 2: 10 ORG_STEP events (trees execute, age=2)
Step 3: 10 ORG_STEP events (trees execute, age=3)
Step 4: 10 ORG_STEP events (trees execute, age=4)

Total: 50 ORG_STEP events (correct)
```

---

## Verification

### Manual Verification

```bash
# Check broken version event counts
echo "Broken version ORG_STEP events:"
grep "ORG_STEP" test-cases/minimal-repro/debug_organism_broken_0.txt | wc -l

echo "Broken version TREE_COUNT at Step 2 (shows persistence):"
grep "year:2" test-cases/minimal-repro/debug_patch_broken_0.txt | grep "TREE_COUNT"

# Check working version event counts
echo "Working version ORG_STEP events:"
grep "ORG_STEP" test-cases/minimal-repro/debug_organism_working_0.txt | wc -l

echo "Working version age progression:"
grep "ORG_STEP.*age:0" test-cases/minimal-repro/debug_organism_working_0.txt | wc -l
grep "ORG_STEP.*age:4" test-cases/minimal-repro/debug_organism_working_0.txt | wc -l
```

### Automated Verification Script

```bash
#!/bin/bash
# verify_test.sh - Automated test verification

echo "Running minimal reproduction tests..."

# Run both versions
java -jar build/libs/joshsim-fat.jar run test-cases/minimal-repro/broken.josh
java -jar build/libs/joshsim-fat.jar run test-cases/minimal-repro/working.josh

# Check broken version
BROKEN_EVENTS=$(grep "ORG_STEP" test-cases/minimal-repro/debug_organism_broken_0.txt | wc -l)
BROKEN_PERSIST=$(grep "TREE_COUNT.*count:10" test-cases/minimal-repro/debug_patch_broken_0.txt | wc -l)

# Check working version
WORKING_EVENTS=$(grep "ORG_STEP" test-cases/minimal-repro/debug_organism_working_0.txt | wc -l)

# Verify results
echo ""
echo "RESULTS:"
echo "--------"
echo "Broken version:"
echo "  ORG_STEP events: $BROKEN_EVENTS (expected: 10)"
echo "  Steps showing 10 trees: $BROKEN_PERSIST (expected: 4, Steps 1-4)"
echo ""
echo "Working version:"
echo "  ORG_STEP events: $WORKING_EVENTS (expected: 50)"
echo ""

# Check pass/fail
if [ "$BROKEN_EVENTS" -eq 10 ] && [ "$WORKING_EVENTS" -eq 50 ]; then
    echo "✅ TEST PASSED: Bug successfully reproduced"
    exit 0
else
    echo "❌ TEST FAILED: Unexpected behavior"
    exit 1
fi
```

---

## Next Steps

After confirming this minimal reproduction works:

1. **Add Runtime Logging**:
   - Log `EntityBuilder.getAttributesWithoutHandlersBySubstep()`
   - Log `ShadowingEntity.hasNoHandlers()` calls
   - Log `InnerEntityGetter.getInnerEntities()` discoveries

2. **Propose Fix Options**:
   - Option A: Auto-copy prior value when conditional is false
   - Option B: Mark conditional handlers differently in cache
   - Option C: Check conditional during discovery
   - Option D: Deprecate conditional syntax on collections

3. **Implement Fix**:
   - Choose best approach
   - Add regression tests
   - Update documentation

---

## Related Files

**Task File**: `/workspaces/josh/.claude/tasks/organism-step-debug-investigation.md`

**Runtime Code**:
- `/workspaces/josh/src/main/java/org/joshsim/engine/entity/base/EntityBuilder.java:203-220`
- `/workspaces/josh/src/main/java/org/joshsim/lang/bridge/ShadowingEntity.java:532`
- `/workspaces/josh/src/main/java/org/joshsim/lang/bridge/InnerEntityGetter.java:39-78`
- `/workspaces/josh/src/main/java/org/joshsim/engine/entity/handler/EventHandler.java`

**Example Simulations**:
- `/workspaces/josh/jotr/historic/two_trees.josh` (working pattern)
- `/workspaces/josh/jotr/historic/stochastic_flowering_updated.josh` (broken pattern)

---

## Contact

For questions about this test case or the bug, see the investigation task file:
`/workspaces/josh/.claude/tasks/organism-step-debug-investigation.md`
