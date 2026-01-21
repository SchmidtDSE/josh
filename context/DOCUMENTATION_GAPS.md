# Documentation Gaps and Improvement Recommendations

**Last Updated:** January 21, 2026
**Analyzed Documents:** `LanguageSpecification.md`, `llms-full.txt`

---

## Executive Summary

After analyzing the language specification documents, I found that while the **spec is internally consistent**, there are **documentation gaps** that could lead to user confusion. The spec shows correct patterns but doesn't explicitly warn against incorrect ones. These are recommendations for documentation improvements that would help future users avoid common pitfalls.

---

## Finding 1: Collection `.step` Handlers - Add vs Replace

### What the Spec Shows (Correct)

**LanguageSpecification.md lines 234-242:**
```josh
JoshuaTrees.step = {
  const deadTrees = current.JoshuaTrees[current.JoshuaTrees.state == "dead"]
  return current.JoshuaTrees - deadTrees
}
JoshuaTrees.step = {
  const newCount = 1 count if count(current.JoshuaTrees) < 10 count else 0
  const new = create newCount JoshuaTree
  return current.JoshuaTrees + new  # <-- EXPLICIT + to add
}
```

**All examples use explicit `+` operator:**
- Line 241: `return current.JoshuaTrees + new`
- Line 1407: `return new + prior.JoshuaTrees`

### What's Missing

The spec **never states** that:
- `.step = X` REPLACES the attribute value, not adds to it
- To add organisms, you MUST use `current.Collection + create X`
- `Collection.step = create X` will replace the entire collection

### Recommendation

Add to Section "Lifecycle > Creation":
```markdown
**Important:** The `.step` handler returns the NEW value for an attribute, replacing the previous value.
To add organisms to an existing collection:

✅ Correct: `Trees.step = current.Trees + create 5 count of Tree`
❌ Incorrect: `Trees.step = create 5 count of Tree` (replaces entire collection!)
```

---

## Finding 2: Self-Referential Attributes and Bare Identifiers

### What the Spec Shows (Correct)

**All `.step` handlers use `prior.attr` to carry forward values:**
- Line 313: `seedBank.step = prior.seedBank + 5%`
- Line 328: `cover.step = prior.cover + 5%`
- Line 405: `height.step = prior.height + 1 m`
- (and 15+ more examples)

**No examples of `attr.step = attr` pattern exist.**

### Potentially Confusing Example

**Line 1252:**
```josh
height.step = prior.height + growth
```

Here `growth` is used without a qualifier. This works because:
1. `growth` is a DIFFERENT attribute (not `height`)
2. The computational graph resolves `growth.step` first

But someone might incorrectly infer: "I can use bare identifiers in `.step` handlers"

### What's Missing

The spec doesn't clarify:
1. **Bare identifiers resolve to `current.*` context** (only implied)
2. **Self-referencing creates cycles:**
   - `attr.step = attr` → resolves to `current.attr` → triggers `.step` → LOOP

### Recommendation

Add to Section "Keywords > Keyword for current":
```markdown
**Self-Reference Warning:** Using a bare attribute name in its own `.step` handler creates a circular dependency:

❌ `elevation.step = elevation`        # Circular! (resolves to current.elevation)
✅ `elevation.step = prior.elevation`  # Correct: uses previous timestep
```

---

## Finding 3: Multiple Handlers and Circular Dependencies

### What the Spec Shows

**Lines 308-316 - Multiple handlers can exist:**
```josh
start organism Deciduous
  seedBank.step = prior.seedBank + 5%
  seedBank.step:if(max(here.Fire.cover) > 0%) = "seed"
end organism
```

The spec says: "Note that handlers can be executed in parallel"

**Lines 573-592 - Circular dependency warning (cross-entity):**
```josh
# This is shown as an ERROR example:
start organism TreeA
  isShaded.step: max(here.TreeBs.height) > current.height
  height.step: prior.height + (2 in if current.isShaded else 4 in)
end organism
# ...similar TreeB...
```

### What's Missing

1. **No warning about intra-entity circular dependencies via conditional handlers:**
   ```josh
   # Not shown as problematic, but IS:
   diameter.step = prior.diameter + current.growthRate
   growthRate.step:if(current.thinned == true) = 1.0 cm
   thinned.step:if(current.diameter < 15 cm) = true
   # Creates: diameter → growthRate → thinned → diameter (CYCLE!)
   ```

2. **"Handlers can be executed in parallel" is ambiguous:**
   - Does this mean all handlers run and values merge?
   - Or conditional handlers take precedence?
   - What if multiple handlers return different values?

### Recommendation

Add to Section "Computational flow > Limitations":
```markdown
**Conditional Handler Dependencies:** Conditions in handler modifiers (`:if(...)`)
are part of the dependency graph. Avoid referencing attributes that depend
on the attribute being defined:

❌ Problematic pattern:
  diameter.step = prior.diameter + current.growthRate
  growthRate.step:if(current.thinned == true) = 1.0 cm
  thinned.step:if(current.diameter < 15 cm) = true  # diameter depends on thinned!

✅ Fixed with prior:
  thinned.step:if(prior.diameter < 15 cm) = true  # Uses prior, breaks cycle
```

---

## Finding 4: Inconsistent Examples with Multiple `.step` Handlers

### Potentially Confusing

**Lines 234-242 show TWO `.step` handlers on JoshuaTrees:**
```josh
JoshuaTrees.step = { ... return current.JoshuaTrees - deadTrees }
JoshuaTrees.step = { ... return current.JoshuaTrees + new }
```

**Questions not answered:**
- Do both handlers execute?
- What's the final value if both return different collections?
- Is one a removal, one an addition, and they compose?

### Recommendation

Clarify how multiple handlers work, or use a single handler:
```josh
JoshuaTrees.step = {
  const deadTrees = current.JoshuaTrees[current.JoshuaTrees.state == "dead"]
  const surviving = current.JoshuaTrees - deadTrees
  const newCount = 1 count if count(surviving) < 10 count else 0
  const new = create newCount JoshuaTree
  return surviving + new
}
```

---

## Summary Table

| Issue | Spec Pattern | Missing Warning | Priority |
|-------|--------------|-----------------|----------|
| Collection add vs replace | Shows `current.X + new` | Never says `.step = create` replaces | High |
| Self-reference | Always uses `prior.attr` | Never warns `attr.step = attr` is cyclic | High |
| Conditional dependencies | Shows cross-entity cycles | Doesn't show intra-entity via conditions | Medium |
| Multiple handlers | Shows multiple `.step` | Doesn't explain composition semantics | Medium |

---

## Conclusion

The spec:
- **Shows correct patterns** consistently
- **Doesn't warn** against incorrect patterns
- **Has some ambiguity** around multiple handlers

Recommended action: Update documentation with explicit warnings and clarifications to help future users avoid common pitfalls.
