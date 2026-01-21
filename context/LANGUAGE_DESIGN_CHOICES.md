# Josh Language Design Choices

This document tracks language design decisions that require discussion and consensus. These are not bugs, but rather places where the language specification is ambiguous or where implementation choices affect user expectations.

**Last Updated:** January 21, 2026

---

## Summary

| # | Issue | Status | Failing Tests |
|---|-------|--------|---------------|
| 1 | Heterogeneous Collection Concatenation | **Needs Decision** | 2 |
| 2 | Dimensionless Unit Simplification | **Needs Decision** | 2 |
| 3 | Non-Integer Exponents with Units | **Needs Decision** | 2 |
| 4 | Functions on Distributions | Resolved (PR #346) | 0 |
| 5 | Compound Unit Parsing in Literals | Resolved (PR #344) | 0 |
| 6 | Quadratic Mapping Behavior | Resolved (PR #346) | 0 |
| 7 | Sigmoid Mapping Steepness | Resolved (PR #346) | 0 |
| 8 | Entity Removal Syntax | Resolved (Design Doc) | 0 |

**Current Test Status:** 111 passing / 6 failing (all 6 failures are design decisions listed above)

---

## Open Design Decisions (3)

### 1. Heterogeneous Collection Concatenation

**Status:** Needs Decision
**Affected Tests:** `test_collections_chained`, `test_collections_combine`

#### The Problem

The `|` operator fails when concatenating collections of different organism types:

```josh
# From test_collections_chained.josh:23-29
Oaks.init = create 10 count of Oak
Pines.init = create 10 count of Pine
Firs.init = create 10 count of Fir

# This fails with "No conversion exists between Pine and Fir"
allConifers.step:if(meta.stepCount >= 1 count) = Pines | Firs
```

**Error:** `java.lang.IllegalArgumentException: No conversion exists between "Pine" and "Fir"`

#### Why This Matters for Ecology

Ecologists frequently need to operate on mixed-species collections:
- Calculate total forest biomass across all tree species
- Count all organisms in a patch regardless of type
- Apply disturbance effects to "all vegetation"

The current workaround requires filtering each type separately and combining results manually.

#### Design Options

| Option | Behavior | Example |
|--------|----------|---------|
| **A: Support heterogeneous** | `Pine \| Fir` creates mixed collection | `allTrees = Pines \| Firs \| Oaks` |
| **B: Homogeneous only** | Require same type (current) | Must use separate aggregations |
| **C: Common base type** | Support if types share declared interface | Requires new `implements` keyword |

#### Implementation Considerations

**If Option A (heterogeneous support):**
- How does filtering work? `allConifers[Pine.age > 5]` vs `allConifers[age > 5]`?
- Type of result: new "mixed" type or union type?
- Attribute access: only common attributes, or error on type-specific?

**If Option B (current behavior):**
- Document the limitation clearly
- Provide recommended patterns for cross-type aggregation:
  ```josh
  totalBiomass.step = sum(Pines.biomass) + sum(Firs.biomass) + sum(Oaks.biomass)
  ```

**If Option C (interfaces):**
- Requires grammar extension for interface declaration
- Organisms would declare `implements Conifer` or similar
- Most complex but most type-safe

#### Spec Reference

Line 623 discusses distribution concatenation but doesn't address cross-type entity collections. The spec is silent on this case.

---

### 2. Dimensionless Unit Simplification

**Status:** Needs Decision
**Affected Tests:** `test_scalars_units_percentage`, `test_temporal_queries_meta_cross_attribute`

#### The Problem

When multiplying a value by a dimensionless quantity (ratio, proportion, percent, count), the compound unit is preserved rather than simplified:

```josh
# From test_scalars_units_percentage.josh:41-45
total4.init = 1 km
percent4.init = 0.10 ratio
result4.init = percent4 * total4      # Type becomes: km * ratio
result4M.init = result4 as m          # FAILS - can't convert "km * ratio" to "m"
```

**Error:** `java.lang.IllegalArgumentException: No conversion exists between "km * ratio" and "m"`

Similarly:
```josh
# From test_temporal_queries_meta_cross_attribute.josh
basePrecip.init = 100 mm
modifier.init = 0.5 proportion
adjustedPrecip.init = basePrecip * modifier  # Type: mm * proportion
# Cannot compare to or convert to plain "mm"
```

#### Why This Matters

Dimensionless quantities like ratios and proportions are mathematically equivalent to multiplying by 1 (with a scale factor). Users expect:
- `10 km * 0.5 ratio` = `5 km` (not `5 km * ratio`)
- `100 mm * 0.8 proportion` = `80 mm` (not `80 mm * proportion`)

The current behavior forces awkward workarounds or prevents natural expressions.

#### Design Options

| Option | `10 km * 0.5 ratio` becomes | Pros | Cons |
|--------|---------------------------|------|------|
| **A: Auto-simplify** | `5 km` | Intuitive, matches math | May hide errors |
| **B: Explicit cast** | `5 km * ratio` (need `force as km`) | Explicit intent | Verbose |
| **C: Specific units** | Simplify only for `count`, `ratio`, `proportion`, `percent` | Controlled | Hardcoded list |

#### Implementation Considerations

**If Option A (auto-simplify):**
- Define which units are "dimensionless": `count`, `ratio`, `proportion`, `percent`, `fraction`
- In `CompoundUnits.java`, detect and cancel dimensionless components
- Risk: `meters * count` becoming `meters` might mask legitimate errors

**If Option B (explicit):**
- Current behavior is essentially this, but error message is unhelpful
- Add `simplify` keyword or improve `force ... as` to handle this case
- Example: `result4M.init = simplify result4 as m`

**If Option C (specific units):**
- Maintain a whitelist of "dimensionless" unit names
- Auto-simplify only when multiplying by these specific units
- Middle ground between A and B

#### Spec Reference

The spec (Section 4.2 - Units) doesn't address dimensionless unit handling explicitly. Line 357 shows units like `"in / month"` but doesn't discuss ratio/proportion semantics.

---

### 3. Non-Integer Exponents with Units

**Status:** Needs Decision
**Affected Tests:** `test_functions_power`, `test_functions_chained`

#### The Problem

Raising a value with units to a non-integer power fails:

```josh
# From test_functions_power.josh:46-48
base4.init = 4 m
sqrt4.init = base4 ^ 0.5  # FAILS
assert.sqrt4.init = sqrt4 == 2 m
```

**Error:** `java.lang.UnsupportedOperationException: Non-integer exponents with units are not supported`

#### Why This Is Mathematically Tricky

| Expression | Mathematical Result | Unit Problem |
|------------|---------------------|--------------|
| `(4 m) ^ 2` | `16 m²` | Valid - area |
| `(4 m²) ^ 0.5` | `2 m` | Valid - square root of area is length |
| `(4 m) ^ 0.5` | `2 m^0.5` | Invalid - what is √meter? |
| `(4 m) ^ 1.5` | `8 m^1.5` | Invalid - what is m^1.5? |

The unit `m^0.5` has no physical meaning. However, in physics, expressions like `√(2*g*h)` work because the units cancel:
- `g = 10 m/s²`, `h = 5 m`
- `2*g*h = 100 m²/s²`
- `√(100 m²/s²) = 10 m/s` (velocity)

The problem is that Josh evaluates eagerly, so `√(4 m)` fails before it could combine with other terms.

#### Design Options

| Option | `(4 m) ^ 0.5` | Pros | Cons |
|--------|---------------|------|------|
| **A: Unitless only** | Error (current) | Mathematically correct | May need workarounds |
| **B: Strip units** | `2` (unitless) | Convenient | Loses information, surprising |
| **C: Require explicit** | `(force base as count) ^ 0.5` | Explicit | Verbose |
| **D: Intermediate units** | `2 m^0.5` | Complete | Complex, rarely useful |

#### Implementation Considerations

**If Option A (current - unitless only):**
- Improve error message to suggest workaround
- Document that users should strip units first for non-integer powers
- Pattern: `sqrt.init = (force value as count) ^ 0.5`

**If Option B (auto-strip):**
- Silently convert to unitless before applying power
- Very surprising behavior - not recommended

**If Option C (explicit strip):**
- Same as current but with better guidance
- Add helper like `unitless(value)` as syntactic sugar for `force value as count`

**If Option D (intermediate units):**
- Implement fractional unit exponents in `CompoundUnits`
- Allow `m^0.5 * m^0.5 = m` through unit algebra
- Most mathematically complete but adds significant complexity

#### Practical Use Cases in Ecology

Most ecological formulas using square roots involve relationships where units cancel naturally. The main use case would be:

```josh
# Pythagorean distance (but Josh has built-in distance functions)
distance = sqrt(dx^2 + dy^2)  # Would need both dx and dy in same units
```

**Recommendation:** Option A (unitless only) with improved error message and documentation. The mathematical complexity of fractional unit exponents isn't justified by ecological modeling needs.

---

## Resolved Design Decisions (5)

### 4. Functions on Distributions: Element-wise vs Sample-first

**Status:** Resolved - Option A (Element-wise)
**Resolved in:** PR #346

#### Decision Made

Mathematical functions (`abs`, `sin`, `cos`, `pow`, `ceil`, `floor`, `round`, `log10`, `ln`) apply **element-wise** to distributions.

```josh
# Each tree gets abs() applied to its individual value
absAges.init = abs(Trees.age)  # Distribution of absolute values
```

#### Implementation

In `SingleThreadEventHandlerMachine.java`, the `applyToDistribution()` helper method iterates over distribution elements and applies the function to each.

#### Why Element-wise?

| Approach | `abs(Trees.age)` where ages are [-5, 3, -2] | Result Type |
|----------|---------------------------------------------|-------------|
| Element-wise (chosen) | `[5, 3, 2]` | Distribution |
| Sample-first | `abs(sample(...))` = single value | Scalar |
| Explicit only | Error - must use `mean()` first | Error |

Element-wise preserves distribution shape and is deterministic. Users who want a single value can explicitly use `mean(abs(Trees.age))`.

---

### 5. Compound Unit Parsing in Literals

**Status:** Resolved - Use String Units
**Resolved in:** PR #344

#### Decision Made

Compound units in literals must use string syntax for the `source.units` attribute in external data definitions:

```josh
start external RainfallData
  source.location = "file://data/rainfall.nc"
  source.format = "netcdf"
  source.units = "mm / month"  # String-based compound unit
end external
```

For arithmetic expressions, compound units arise naturally from operations:

```josh
velocity.init = 100 m / 1 year  # Results in compound unit m/year
```

#### Why Not Grammar Extension?

Extending the grammar to parse `2.5 m/yr` as a unit literal would create ambiguity with division:
- Is `x m/yr` the unit "meters per year" or "x meters divided by variable yr"?

The string approach (`"m / yr"`) is unambiguous and leverages the existing `Units.of()` parser.

---

### 6. Quadratic Mapping: Parabola Behavior

**Status:** Resolved - Parabola (inverted-U curve)
**Resolved in:** PR #346

#### Decision Made

`map x from [a, b] to [c, d] quadratically` produces a **parabola** where:
- Domain endpoints (a, b) map to range minimum (c or d)
- Domain center maps to range maximum

```josh
# Input 0 → 0, Input 50 → 100, Input 100 → 0
mapped.init = map input from [0 m, 100 m] to [0 count, 100 count] quadratically
```

This produces an inverted-U (∩) shape, not a monotonic x² curve.

#### Why Parabola?

The parabola interpretation matches common ecological modeling needs:
- **Optimal temperature curves**: Both too cold and too hot reduce growth
- **Intermediate disturbance hypothesis**: Moderate disturbance maximizes diversity
- **Habitat suitability**: Optimal conditions at intermediate values

For monotonic power relationships, users should use explicit expressions:
```josh
accelerating.init = (input / 100 m) ^ 2 * 100 count
```

---

### 7. Sigmoid Mapping: Steepness Parameter

**Status:** Resolved - Fixed Steepness (6.0)
**Resolved in:** PR #346

#### Decision Made

Sigmoid mapping uses a fixed steepness of 6.0, which maps domain boundaries to approximately 0.25% and 99.75% of the range.

```josh
# At input 0: output ≈ 0.25% of range
# At input 50: output = 50% of range
# At input 100: output ≈ 99.75% of range
mapped.init = map input from [0 m, 100 m] to [0 count, 100 count] sigmoidally
```

#### Why Fixed Steepness?

| Steepness | Domain edge → Range coverage |
|-----------|------------------------------|
| 4.0 | 1.8% - 98.2% |
| 5.0 | 0.67% - 99.3% |
| **6.0 (chosen)** | 0.25% - 99.75% |
| 8.0 | 0.03% - 99.97% |

Steepness 6.0 provides near-boundary values (99.5% coverage) without requiring clamping. If exact boundaries are needed, users can post-process with `limit`:

```josh
exactSigmoid.init = limit (map x sigmoidally) to [0 count, 100 count]
```

---

### 8. Entity Removal: Subtraction Pattern

**Status:** Resolved - Use Subtraction Pattern
**Resolved in:** Design Documentation (no PR - clarification only)

#### Decision Made

Entity removal uses the **subtraction pattern**, not a dedicated `remove` keyword:

```josh
# Mark entities for removal
Tree.state.step:if(current.age > 100 years) = "dead"

# Remove dead entities via subtraction
Trees.step = current.Trees - current.Trees[Tree.state == "dead"]
```

There is no `remove` keyword in the grammar.

#### Why Subtraction?

The subtraction pattern follows Josh's core philosophy:
- Event handlers **replace** attribute values (no side effects)
- Explicit over implicit behavior
- The collection attribute is reassigned to a new filtered collection

A test (`test_collections_remove`) incorrectly assumed a `remove` statement existed and was deleted.

#### Future Consideration

A dedicated `remove` operation could be valuable for performance in high-entity-count simulations (bacteria, particles) where immediate cleanup would prevent memory bloat. This would be a future enhancement, not a current design choice.

---

## Related Documentation

- `CONFORMANCE_ANALYSIS.md` - Current test status and code examples
- `DEV_RESTORE_SUMMARY.md` - PR history and implementation details
- `LanguageSpecification.md` - Full Josh language specification
