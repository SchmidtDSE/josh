# Deep Runtime Investigation: Organism Discovery & Conditional Handler Bug

## Executive Summary
The bug where organisms created via conditional collection handlers (`:if`) only execute their `.step` logic once stems from a **caching issue in the attribute handler lookup system**. The `attributesWithoutHandlersBySubstep` map is computed once at entity type initialization and never updated when conditional handlers dynamically affect which attributes have handlers.

---

## 1. DISCOVERY MECHANISM FLOW

### 1.1 How InnerEntityGetter.getInnerEntities() Works

**Location**: `InnerEntityGetter.java:39-78`

The discovery mechanism iterates through all attributes of an entity and extracts any that contain child entities:

```java
public static Iterable<MutableEntity> getInnerEntities(MutableEntity target) {
    List<MutableEntity> result = new ArrayList<>();
    
    Map<String, Integer> indexMap = target.getAttributeNameToIndex();
    int numAttributes = indexMap.size();
    
    for (int i = 0; i < numAttributes; i++) {
        Optional<EngineValue> valueMaybe = target.getAttributeValue(i);
        // Skip if not set
        if (valueMaybe.isEmpty()) continue;
        
        EngineValue value = valueMaybe.get();
        // Skip if value doesn't contain entities
        if (!value.getLanguageType().containsAttributes()) continue;
        
        // Extract single or collection of entities
        Optional<Integer> sizeMaybe = value.getSize();
        if (sizeMaybe.isEmpty()) continue;
        
        int size = sizeMaybe.get();
        if (size == 1) {
            result.add(value.getAsMutableEntity());
        } else {
            // Handle collections
            Iterable<EngineValue> innerValues = value.getAsDistribution().getContents(size, false);
            for (EngineValue innerValue : innerValues) {
                result.add(innerValue.getAsMutableEntity());
            }
        }
    }
    return result;
}
```

**Key Points:**
1. Discovery reads **all attributes** via `target.getAttributeValue(i)` 
2. It checks if the returned EngineValue is set (not Optional.empty())
3. It calls `getLanguageType().containsAttributes()` to determine if it contains entities
4. It extracts entities either as single or from distributions (collections)

### 1.2 When Discovery is Called

**Location**: `ShadowingEntity.java:747-753`

Discovery is invoked during `startSubstep()`:

```java
@Override
public void startSubstep(String name) {
    InnerEntityGetter.getInnerEntities(this).forEach((x) -> x.startSubstep(name));
    inner.startSubstep(name);
    
    // Clear array-based cache
    Arrays.fill(resolvedCacheByIndex, null);
}
```

This means **discovery is called at the START of each substep**, before any handlers execute.

### 1.3 Discovery Timing in Phase Loop

**Location**: `SimulationStepper.java:73-105`

The phase execution order is:
1. `target.startStep()` - Initialize timestep
2. If first step: `performStream(simulation, "init")` then `performStream(patches, "init", ...)`
3. If "start" event exists: `performStream(simulation, "start")` then patches
4. If "step" event exists: `performStream(simulation, "step")` then patches  
5. If "end" event exists: `performStream(simulation, "end")` then patches
6. `target.endStep()` - Cleanup

For each substep, `performStream()` does:
```java
private MutableEntity updateEntity(MutableEntity target, String subStep) {
    target.startSubstep(subStep);      // ← DISCOVERY HAPPENS HERE
    updateEntityUnsafe(target);         // ← ATTRIBUTE RESOLUTION + RECURSIVE DISCOVERY
    target.endSubstep();
    return target;
}
```

**CRITICAL**: `updateEntityUnsafe()` (lines 190-233) recursively discovers entities AGAIN, but from the already-resolved attributes.

---

## 2. CONDITIONAL HANDLER PROCESSING

### 2.1 Handler Parsing

**Location**: `JoshFunctionVisitor.java:168-290`

Conditional handlers are parsed into EventHandler objects with optional CompiledSelectors:

```java
public JoshFragment visitConditionalIfEventHandlerGroupMember(...) {
    EventHandlerAction innerAction = ctx.inner.accept(parent).getCurrentAction();
    EventHandlerAction conditionAction = ctx.target.accept(parent).getCurrentAction();
    
    CompiledCallable decoratedInterpreterAction = makeCallableMachine(innerAction);
    CompiledCallable decoratedConditionAction = makeCallableMachine(conditionAction);
    CompiledSelector decoratedConditionSelector = new CompiledSelectorFromCallable(
        decoratedConditionAction
    );
    
    return new CompiledCallableFragment(decoratedInterpreterAction, decoratedConditionSelector);
}
```

Multiple handlers (if/elif/else) are collected into an EventHandlerGroup:

```java
public JoshFragment visitEventHandlerGroupMultiple(...) {
    EventHandlerGroupBuilder groupBuilder = new EventHandlerGroupBuilder();
    
    for (int branchIndex = 0; branchIndex < numBranches; branchIndex++) {
        JoshFragment childFragment = ctx.getChild(childIndex).accept(parent);
        
        if (childFragment.getCompiledSelector().isPresent()) {
            groupBuilder.addEventHandler(new EventHandler(
                childFragment.getCompiledCallable(),
                eventKey.getAttribute(),
                eventKey.getEvent(),
                childFragment.getCompiledSelector().get()  // ← CONDITIONAL
            ));
        } else {
            groupBuilder.addEventHandler(new EventHandler(
                childFragment.getCompiledCallable(),
                eventKey.getAttribute(),
                eventKey.getEvent()
            ));
        }
    }
    return new EventHandlerGroupFragment(groupBuilder);
}
```

### 2.2 Handler Execution

**Location**: `ShadowingEntity.java:673-694`

When an attribute is resolved during substep:

```java
private boolean executeHandlers(EventHandlerGroup handlers) {
    Scope decoratedScope = new SyntheticScope(this);
    
    for (EventHandler handler : handlers.getEventHandlers()) {
        Optional<CompiledSelector> conditionalMaybe = handler.getConditional();
        
        boolean matches;
        if (conditionalMaybe.isPresent()) {
            CompiledSelector conditional = conditionalMaybe.get();
            matches = conditional.evaluate(decoratedScope);  // ← EVALUATE CONDITION
        } else {
            matches = true;
        }
        
        if (matches) {
            EngineValue value = handler.getCallable().evaluate(decoratedScope);
            setAttributeValue(handler.getAttributeName(), value);
            return true;  // ← STOP AFTER FIRST MATCH
        }
    }
    return false;
}
```

**Key**: Only the FIRST handler whose condition evaluates to TRUE is executed.

### 2.3 Conditional vs Unconditional Handler Differences

**Unconditional Handler** (e.g., `SpeciesA.start = prior.SpeciesA[...]`):
- Always executes
- EventKey maps directly to attribute
- Handler is in `eventHandlerGroups` from compilation time
- Attribute is always marked as "having handlers" in `attributesWithoutHandlersBySubstep`

**Conditional Handler** (e.g., `JoshuaTrees.step:if(meta.year == 2025) = create count`):
- Only executes if condition is TRUE
- Still has EventKey that maps to attribute
- Handler is in `eventHandlerGroups` from compilation time
- BUT: Attribute is marked as "having handlers" even though condition might be FALSE
- When condition is FALSE → **no handler executes** → attribute resolves from prior
- When condition is TRUE → **handler executes** → attribute is set to new value

---

## 3. TIMING & SEQUENCING

### 3.1 Phase Execution Order (SimulationStepper.java:73-105)

Per-timestep flow:
1. `startStep()` - Increment timestep, setup
2. For each event type (init, start, step, end):
   - Call `startSubstep(eventName)` on patch/entity
   - Call `updateEntityUnsafe()` to resolve all attributes
   - Call `endSubstep()`
3. `endStep()` - Cleanup, prepare for next timestep

### 3.2 Discovery and Resolution Sequence

For each entity during a substep:

```
startSubstep(name)
├─ InnerEntityGetter.getInnerEntities(this)  ← DISCOVERY #1
│  └─ For each inner entity: innerEntity.startSubstep(name)
├─ inner.startSubstep(name)
└─ Clear resolvedCacheByIndex

updateEntityUnsafe(target)
├─ For each attribute (int-based iteration):
│  └─ target.getAttributeValue(i)  ← Triggers resolution if needed
│     └─ ShadowingEntity.getAttributeValue(index)
│        └─ Check cache first
│        └─ If not cached, resolveAttributeByIndex()
│           └─ Check hasNoHandlers() - POTENTIAL BUG LOCATION!
│           └─ Get handlers via getHandlersForAttribute()
│           └─ Execute handlers if found
│           └─ If no handlers match, resolveAttributeFromPrior()
├─ Extract inner entities from resolved values  ← DISCOVERY #2
└─ For each discovered inner entity: updateEntityUnsafe(innerEntity)
```

**Key Issue**: 
- Discovery #1 (in startSubstep) doesn't resolve attributes—it just iterates what's currently set
- Attributes created by conditional handlers in PREVIOUS substep are discovered in this phase
- BUT: Those attributes are marked as having no handlers in the CURRENT substep
- So when resolution tries to resolve them, it skips handler lookup!

---

## 4. THE BUG: ATTRIBUTE CACHING AND CONDITIONAL HANDLERS

### 4.1 attributesWithoutHandlersBySubstep Computation

**Location**: `EntityBuilder.java:179-228`

This map is computed ONCE at entity type initialization:

```java
public Map<String, boolean[]> getAttributesWithoutHandlersBySubstep() {
    if (attributesWithoutHandlersBySubstep != null) {
        return attributesWithoutHandlersBySubstep;  // ← CACHED!
    }
    
    Map<String, Integer> indexMap = getAttributeNameToIndex();
    int arraySize = indexMap.size();
    Map<String, boolean[]> result = new HashMap<>();
    
    for (String substep : SUBSTEPS) {
        boolean[] attrsWithoutHandlers = new boolean[arraySize];
        
        // Step 1: Mark all initial attributes as having no handlers
        for (String attrName : attributes.keySet()) {
            Integer index = indexMap.get(attrName);
            if (index != null) {
                attrsWithoutHandlers[index] = true;
            }
        }
        
        // Step 2: Unmark attributes that have handlers for this substep
        for (EventHandlerGroup group : eventHandlerGroups.values()) {
            EventKey key = group.getEventKey();
            if (key.getEvent().equals(substep)) {
                for (EventHandler handler : group.getEventHandlers()) {
                    Integer index = indexMap.get(handler.getAttributeName());
                    if (index != null) {
                        attrsWithoutHandlers[index] = false;  // ← HAS HANDLERS
                    }
                }
            }
        }
        
        result.put(substep, attrsWithoutHandlers);
    }
    
    attributesWithoutHandlersBySubstep = Collections.unmodifiableMap(result);
    return attributesWithoutHandlersBySubstep;
}
```

**The computation is correct at entity type definition time.**

### 4.2 How hasNoHandlers() is Used

**Location**: `DirectLockMutableEntity.java:279-286` and `ShadowingEntity.java:626-629`

In `resolveAttributeUnsafeByIndex()`:

```java
private void resolveAttributeUnsafeByIndex(int index, String name) {
    Optional<String> substep = getSubstep();
    
    if (substep.isEmpty()) {
        resolveAttributeFromPriorByIndex(index);
        return;
    }
    
    if (inner.hasNoHandlers(name, substep.get())) {  // ← FAST-PATH CHECK
        resolveAttributeFromPriorByIndex(index);      // ← SKIP HANDLER LOOKUP!
        return;
    }
    
    // Otherwise: check for handlers, execute if found
    Iterator<EventHandlerGroup> handlersMaybe = getHandlersForAttribute(name).iterator();
    // ...
}
```

### 4.3 The Bug Scenario

For `JoshuaTrees.step:if(meta.year == 2025) = create count`:

**Timestep 0 (year != 2025):**
1. `JoshuaTrees` entity is created (statically in patch)
2. During "step" substep:
   - `startSubstep("step")` → discovery finds no children (condition false → no creation)
   - `updateEntityUnsafe()` → tries to resolve `Child` attribute
   - `hasNoHandlers("Child", "step")` → **Returns TRUE** because `Child.step` has no handler!
   - Attribute resolves from prior (empty)

**Timestep 1 (year == 2025):**
1. Same `JoshuaTrees` entity (persists)
2. During "step" substep:
   - `startSubstep("step")` → discovery finds no children (yet—they're being created this step)
   - `updateEntityUnsafe()` → tries to resolve `Child` attribute
   - **BUG**: `hasNoHandlers("Child", "step")` → **Still returns TRUE!**
     - The cache was computed at TYPE initialization time
     - No `Child` handler was defined for "step" on the TYPE definition
     - The cache never gets updated even though children ARE created
   - Attribute resolves from prior (empty) instead of being resolved via handler
   - The newly created children are NOT added to `Child` attribute!

**Timestep 2+:**
1. Children were never captured in `Child` attribute
2. Discovery mechanism can't find them (they're not in any attribute)
3. Children never execute `.step`

### 4.4 Why Unconditional Handlers Work

For `SpeciesA.start = prior.SpeciesA[...]`:
- The handler is ALWAYS registered on the entity type
- The condition (if any) is part of the handler execution, not the caching logic
- `hasNoHandlers()` correctly returns FALSE because the handler is registered
- Handler always executes, even if its internal condition is false

---

## 5. ROOT CAUSE ANALYSIS

### The Three-Layer Problem

**Layer 1: Handler Registration (Type-Level)**
- All handlers (conditional or not) are registered on entity types at compilation
- `EventHandlerGroup` objects are created with multiple `EventHandler` objects
- Each handler may have a `CompiledSelector` (the condition)

**Layer 2: Handler Caching (Type-Level)**
- `attributesWithoutHandlersBySubstep` is a **shared, immutable map** computed once per entity type
- It only checks if ANY handler exists for (attribute, substep)
- It DOES NOT consider the handler's condition
- Once cached, it is never invalidated or updated

**Layer 3: Attribute Resolution (Instance-Level)**
- During each substep, when an attribute is resolved:
  1. Fast-path check: `hasNoHandlers()` returns true → resolve from prior only
  2. Otherwise: Look up handlers and execute if conditions match

**The Bug**: Layer 2 cache doesn't understand Layer 1 conditions.
- For `Child.step:if(condition) = ...`:
  - Layer 2 sees: "No unconditional handler for Child.step exists"
  - Layer 2 marks: "Child has no handlers for step"
  - Layer 3: Resolves from prior instead of checking if conditional handler should execute

---

## 6. SPECIFIC BUG LOCATIONS

### Location 1: attributesWithoutHandlersBySubstep Computation
**File**: `EntityBuilder.java:203-220`

The code that checks if a handler exists:
```java
if (key.getEvent().equals(substep)) {
    for (EventHandler handler : group.getEventHandlers()) {
        Integer index = indexMap.get(handler.getAttributeName());
        if (index != null) {
            attrsWithoutHandlers[index] = false;  // ← Always marks as "has handler"
        }
    }
}
```

**Issue**: This marks an attribute as "having handlers" regardless of whether the handler's condition is true. For conditional handlers that rarely match, this is conservative but incorrect in practice.

### Location 2: hasNoHandlers Fast-Path Check
**File**: `DirectLockMutableEntity.java:279-286`

```java
public boolean hasNoHandlers(String attributeName, String substep) {
    int index = getAttributeIndexInternal(attributeName);
    if (index < 0) {
        return false;
    }
    boolean[] attrsForSubstep = attributesWithoutHandlersBySubstep.get(substep);
    return attrsForSubstep != null && attrsForSubstep[index];
}
```

**Issue**: This checks a static cache that doesn't account for conditional handler execution. If the condition is false, the handler doesn't execute, but the cache still says "has handlers".

### Location 3: Conditional Handler Execution
**File**: `ShadowingEntity.java:673-694`

```java
private boolean executeHandlers(EventHandlerGroup handlers) {
    // ...
    for (EventHandler handler : handlers.getEventHandlers()) {
        Optional<CompiledSelector> conditionalMaybe = handler.getConditional();
        
        boolean matches;
        if (conditionalMaybe.isPresent()) {
            matches = conditional.evaluate(decoratedScope);
        } else {
            matches = true;
        }
        
        if (matches) {
            EngineValue value = handler.getCallable().evaluate(decoratedScope);
            setAttributeValue(handler.getAttributeName(), value);
            return true;
        }
    }
    return false;  // ← No handler executed
}
```

**Issue**: When no conditional handler matches, the method returns `false`. The caller then resolves from prior. But `hasNoHandlers()` was never called to begin with because the handler WAS registered!

---

## 7. WHY DISCOVERY ONLY HAPPENS ONCE

When a conditional handler creates children:

**First occurrence (creation):**
1. Handler executes: `Child = create count`
2. Attribute is set in the current substep
3. `updateEntityUnsafe()` discovers the children from this attribute
4. Children's `startSubstep()` is called
5. All subsequent phases will find the children

**But the bug**: If discovery happens in `startSubstep()` BEFORE the handler that creates them:
- `startSubstep()` calls `InnerEntityGetter.getInnerEntities()` 
- At this point, `Child` attribute is empty (not yet created)
- No children are discovered
- Discovery won't happen again until next timestep's `startSubstep()`

**Why**: `startSubstep()` discovery happens BEFORE `updateEntityUnsafe()` resolution. If the handler hasn't executed yet, the entities don't exist.

---

## 8. SEQUENCE DIAGRAM: THE BUG IN ACTION

```
Timestep 0 (meta.year == 2024):
  patch.startSubstep("step")
    └─ discover inner entities from patch attributes (finds Tree)
  patch.updateEntityUnsafe()
    └─ resolve patch.Tree attribute
      └─ Tree handler exists, but condition is FALSE
      └─ No handler matches, resolve from prior
      └─ patch.Child attribute = empty
  tree.startSubstep("step")
    └─ discover inner entities from tree (Child attribute is empty!)
  tree.updateEntityUnsep()
    └─ resolve tree.Child attribute
      └─ hasNoHandlers("Child", "step") = TRUE ← BUG!
      └─ skip handler lookup entirely
      └─ resolve from prior = empty
    └─ no inner entities discovered from Child
    └─ NO RECURSION INTO CHILDREN

Timestep 1 (meta.year == 2025):
  patch.startSubstep("step")
    └─ discover inner entities from patch (finds Tree)
  patch.updateEntityUnsafe()
    └─ resolve patch.Tree attribute
      └─ Tree handler exists, condition is TRUE
      └─ Child = create 2 ChildTrees
      └─ patch.Child attribute is now set
  tree.startSubstep("step")
    └─ discover inner entities from tree
      └─ tree.Child attribute is STILL EMPTY! ← BUG!
      └─ NO CHILDREN DISCOVERED
  tree.updateEntityUnsafe()
    └─ resolve tree.Child attribute
      └─ hasNoHandlers("Child", "step") = TRUE ← STILL BUG!
      └─ skip handler lookup entirely
      └─ resolve from prior = empty
    └─ NO CHILDREN FOUND

Timestep 2+:
  Children from Timestep 1 creation are lost
  Tree entity has no Child attribute set
  Children never execute .step
```

---

## 9. SUMMARY TABLE

| Aspect | Unconditional Handler | Conditional Handler |
|--------|----------------------|---------------------|
| **Parsing** | Single EventHandler | Multiple EventHandlers (if/elif/else) |
| **Registration** | Registered at type definition | Registered at type definition |
| **Handler Lookup** | Always found via getHandlersForAttribute() | Found, but condition may be false |
| **Cache Marking** | attribute → hasNoHandlers=false | attribute → hasNoHandlers=false |
| **Execution Path** | Handler resolves attribute → found | Handler would resolve, but never checked |
| **Issue** | Works correctly | hasNoHandlers() returns true incorrectly |
| **Root Cause** | N/A | Static cache doesn't account for dynamic condition evaluation |

---

## 10. CONCLUSION

The bug occurs because:

1. **Conditional handlers are registered at type-definition time**, just like unconditional handlers
2. **The `attributesWithoutHandlersBySubstep` cache marks attributes as "having handlers"** if ANY handler exists for that (attribute, substep) pair
3. **But conditional handlers may not execute if their condition is false**
4. **The fast-path check `hasNoHandlers()` skips handler lookup entirely**, resolving only from prior
5. **When a conditional handler's condition is false in earlier timesteps but true later, the attribute has already been marked as "having no handlers" in the cache**
6. **Discovery mechanism relies on attributes being properly resolved**, but the caching bug prevents resolution
7. **Organisms created by conditional handlers are never discovered because they're never stored in the parent attribute**

The fix requires either:
- Invalidating/updating the `attributesWithoutHandlersBySubstep` cache when conditional handlers execute
- Changing the fast-path check to always verify conditional handlers
- Storing dynamically-created entities in a separate tracking mechanism
