# Josh Conformance Test Analysis

**Status:** 111 passing (94.9%) / 6 failing (5.1%) / 117 total tests

*Last updated: January 21, 2026*

---

## Test Results Summary

The conformance test suite validates Josh language behavior across all major functionality areas. After implementing the dev-restore features and fixing critical bugs, **111 of 117 tests pass**. The 6 remaining failures all require **design decisions** - the behavior is ambiguous in the language specification.

| Category | Passing | Failing | Total |
|----------|---------|---------|-------|
| control | 5 | 0 | 5 |
| core | 5 | 2 | 7 |
| entities | 4 | 0 | 4 |
| io | 18 | 0 | 18 |
| spatial | 12 | 0 | 12 |
| stochastic | 10 | 0 | 10 |
| temporal | 9 | 1 | 10 |
| types | 48 | 3 | 51 |
| **Total** | **111** | **6** | **117** |

---

## Test Subsection Descriptions

### control/ (5 tests)
Tests for Josh's control flow and state management mechanics.

#### conditionals/ (1 test)
Validates `:if` modifiers on attributes and conditional execution.

##### Example ([test_control_conditionals.josh](../josh-tests/conformance/control/conditionals/test_control_conditionals.josh#L34-L38))
```josh
# Conditional growth - grow by 1m normally, but 2m on even steps
height.init = 1 m
height.step = prior.height + 1 m
height.step:if(meta.stepCount == 2 count) = prior.height + 2 m
height.step:if(meta.stepCount == 4 count) = prior.height + 2 m
```

#### states/ (4 tests)
Tests entity state transitions, state-specific handlers, and timing of state evaluation.

##### Example ([test_states_basic.josh](../josh-tests/conformance/control/states/test_states_basic.josh#L34-L51))
```josh
# Initialize state to Seedling
state.init = "Seedling"

# State transition from Seedling to Sapling at age 10
state.step:if(current.age >= 10 years and current.state == "Seedling") = "Sapling"

# State transition from Sapling to Mature at age 30
state.step:if(current.age >= 30 years and current.state == "Sapling") = "Mature"

# State transition from Mature to Dead at age 60
state.step:if(current.age >= 60 years and current.state == "Mature") = "Dead"

# Height grows differently based on state
height.init = 0.1 m
height.step:if(current.state == "Seedling") = prior.height + 0.1 m
height.step:if(current.state == "Sapling") = prior.height + 0.5 m
height.step:if(current.state == "Mature") = prior.height + 0.2 m
height.step:if(current.state == "Dead") = prior.height  # No growth when dead
```

---

### core/ (7 tests, 2 failing)
Tests for fundamental Josh language constructs.

#### collections/ (5 tests, 2 failing)
Tests collection creation, filtering, chaining operations, and combining collections.

##### Creation Example ([test_collections_create.josh](../josh-tests/conformance/core/collections/test_collections_create.josh#L25))
```josh
Trees.step:if(meta.stepCount == 1 count) = create 10 count of Tree
```

##### Filtering Example ([test_collections_filter_basic.josh](../josh-tests/conformance/core/collections/test_collections_filter_basic.josh#L24-L37))
```josh
# Filter by age > 3 years
oldTrees.step:if(meta.stepCount >= 2 count) = Trees[Tree.age > 3 years]

# Filter by height >= 2.5 m
tallTrees.step:if(meta.stepCount >= 2 count) = Trees[Tree.height >= 2.5 m]

# Filter by status != 0 count
matureTrees.step:if(meta.stepCount >= 2 count) = Trees[Tree.status != 0 count]
```

##### Chaining Example (FAILING - heterogeneous collections) ([test_collections_chained.josh](../josh-tests/conformance/core/collections/test_collections_chained.josh#L29))
```josh
# This test fails with "No conversion exists between Pine and Fir"
allConifers.step:if(meta.stepCount >= 1 count) = Pines | Firs
```

#### keywords/ (1 test)
Validates the `prior` keyword for accessing previous timestep values.

##### Example ([test_keywords_prior.josh](../josh-tests/conformance/core/keywords/test_keywords_prior.josh))
```josh
age.step = prior.age + 1 year
height.step = prior.height + 0.5 m
```

#### lifecycle/ (1 test)
Tests basic organism lifecycle: creation, attribute initialization, and step execution.

##### Example ([test_simple_organism.josh](../josh-tests/conformance/core/lifecycle/test_simple_organism.josh#L28-L35))
```josh
start organism Tree

  age.init = 0 year
  age.step = prior.age + 1 year
  assert.ageAtStep3.step:if(meta.stepCount == 3 count) = current.age == 3 years
  assert.ageAtStep5.step:if(meta.stepCount == 5 count) = current.age == 5 years

end organism
```

---

### entities/ (4 tests)
Tests for different Josh entity types.

#### disturbance/ (2 tests)
Tests disturbance entity behavior, including activation timing and organism responses.

##### Example ([test_disturbance_basic.josh](../josh-tests/conformance/entities/disturbance/test_disturbance_basic.josh#L30-L48))
```josh
start disturbance Fire

  # Spatial extent attributes
  centerX.init = 50 m
  centerY.init = 50 m
  radius.init = 20 m

  # Timing attribute - disturbance occurs at step 3
  occurTime.init = 3 count

  # Active status based on timing
  active.init = false
  active.step:if(meta.stepCount == current.occurTime) = true
  active.step:if(meta.stepCount > current.occurTime) = false

  # Intensity that increases when active
  intensity.init = 0 count
  intensity.step:if(current.active == true) = prior.intensity + 10 count
  intensity.step:if(current.active == false) = prior.intensity

end disturbance
```

#### management/ (2 tests)
Tests management entity creation and interaction with organisms.

##### Example ([test_management_basic.josh](../josh-tests/conformance/entities/management/test_management_basic.josh#L30-L53))
```josh
start management Thinning

  # Spatial extent - rectangular area
  centerX.init = 30 m
  centerY.init = 30 m
  width.init = 40 m
  height.init = 40 m

  # Timing - management occurs at step 3
  scheduledTime.init = 3 count

  # Active status based on timing
  active.init = false
  active.step:if(meta.stepCount == current.scheduledTime) = true
  active.step:if(meta.stepCount != current.scheduledTime) = false

  # Management intensity
  intensity.init = 50 count
  intensity.step = prior.intensity

  # Operation counter - increments when active
  operationCount.init = 0 count
  operationCount.step:if(current.active == true) = prior.operationCount + 1 count
  operationCount.step:if(current.active == false) = prior.operationCount

end management
```

---

### io/ (18 tests)
Tests for input/output operations: assertions, exports, and external data.

#### assertions/ (4 tests)
Tests `assert.*` attributes with conditional evaluation, scope access, and complex boolean expressions.

##### Example ([test_assertions_pass.josh](../josh-tests/conformance/io/assertions/test_assertions_pass.josh#L28-L54))
```josh
# Test 1: Equality assertions on counts
treeCount.step = count(Tree)
assert.treeCountEquals.step = treeCount == 10 count

# Test 3: Range assertions (greater than)
assert.treeCountGreaterThan.step = treeCount > 5 count

# Test 5: Combined range assertions (within bounds)
assert.treeCountInRange.step = (treeCount >= 10 count) and (treeCount <= 10 count)

# Test 6: Aggregation assertions on collections
avgAge.step = mean(Tree.age)
assert.avgAgePositive.step = avgAge >= 0 years

# Test 8: Assertions on simulation metadata
assert.stepCountValid.step = meta.stepCount >= 0 count
```

#### exports/ (8 tests)
Tests export functionality: CSV, GeoTIFF, NetCDF, and memory exports.

##### Example ([test_export_csv_basic.josh](../josh-tests/conformance/io/exports/test_export_csv_basic.josh#L18-L32))
```josh
# Export to file:///tmp/ for temporary CSV file
exportFiles.patch = "file:///tmp/test_export_csv_basic.csv"

# Export organism attribute aggregations
export.avgAge.step = mean(Tree.age)
export.avgHeight.step = mean(Tree.height)
export.avgBiomass.step = mean(Tree.biomass)
export.treeCount.step = count(Tree)
```

#### external/ (6 tests)
Tests reading external data sources: GeoTIFF, JSHD, and NetCDF files.

##### Example ([test_external_geotiff_sequential.josh](../josh-tests/conformance/io/external/test_external_geotiff_sequential.josh#L1-L37))
```josh
start external SequentialData
  source.location = "file://test-data/spatial/grid_10x10_sequential.tiff"
  source.format = "geotiff"
  source.units = "count"
  source.band = 0
end external

start patch Default
  # Test that external data can be accessed
  # Sequential data has values from 0 to 99
  elevation.init = sample external SequentialData

  # Verify data loaded successfully and is in expected range
  assert.inRange.init = (current.elevation >= 0 count) and (current.elevation <= 99 count)
end patch
```

---

### spatial/ (12 tests)
Tests for spatial operations and patch mechanics.

#### neighbors/ (4 tests)
Tests neighbor queries: accessing attributes across patches, counting neighbors, and distance calculations.

##### Example ([test_spatial_neighbors_count.josh](../josh-tests/conformance/spatial/neighbors/test_spatial_neighbors_count.josh#L36-L46))
```josh
# Count all neighbors within 50m (should capture all in 3x3 grid)
allNeighbors.step = count(Tree within 50 m radial at prior)

# Count close neighbors within 15m
closeNeighbors.step = count(Tree within 15 m radial at prior)

# Assert that we find all 9 organisms with large radius
assert.findAll.step:if(meta.stepCount == 1 count) = current.allNeighbors == 9 count

# Assert that close neighbors is less than or equal to all neighbors
assert.closeVsAll.step:if(meta.stepCount == 1 count) = current.closeNeighbors <= current.allNeighbors
```

#### patches/ (4 tests)
Tests patch mechanics: attribute access, heterogeneous patch types, and location handling.

##### Example ([test_spatial_patches_attributes.josh](../josh-tests/conformance/spatial/patches/test_spatial_patches_attributes.josh#L26-L60))
```josh
# Patch-level attribute: temperature varies by location
temperature.init = sample uniform from 15 celsius to 25 celsius
temperature.step = prior.temperature + 0.5 celsius

# Patch-level attribute: soil moisture
soilMoisture.init = sample uniform from 10 percent to 90 percent
soilMoisture.step = prior.soilMoisture - 1 percent

# Organisms can access patch attributes using 'here' keyword
start organism Tree
  localTemperature.step = here.temperature
  localMoisture.step = here.soilMoisture

  # Growth affected by temperature
  biomass.step = prior.biomass + (current.localTemperature / 1 celsius) * 0.1 kg
end organism
```

#### queries/ (4 tests)
Tests `within` spatial queries: radial searches, multiple radii, empty results, and self-exclusion.

##### Example ([test_spatial_within_radius.josh](../josh-tests/conformance/spatial/queries/test_spatial_within_radius.josh#L39-L53))
```josh
# Count neighbors within 10m radius
neighborsWithin10m.step = count(Tree within 10 m radial at prior)

# Count neighbors within 20m radius
neighborsWithin20m.step = count(Tree within 20 m radial at prior)

# Test that within 20m finds more or equal organisms than within 10m
assert.moreNeighbors20m.step:if(meta.stepCount == 1 count) = current.neighborsWithin20m >= current.neighborsWithin10m

# Test that not all organisms are within 10m (assuming spread across grid)
assert.notAllClose.step:if(meta.stepCount == 1 count) = current.neighborsWithin10m <= 9 count
```

---

### stochastic/ (10 tests)
Tests for random/probabilistic behavior and distribution operations.

#### arithmetic/ (3 tests)
Tests arithmetic with distributions: scalar operations and distribution-distribution operations.

##### Example ([test_stochastic_arithmetic_distribution.josh](../josh-tests/conformance/stochastic/arithmetic/test_stochastic_arithmetic_distribution.josh#L89-L97))
```josh
start organism SumOrg
  # Sum of two distributions
  value1.init = sample uniform from 20 m to 40 m
  value2.init = sample uniform from 50 m to 90 m
  value.init = value1 + value2
  value.step = prior.value
end organism
```

#### distributions/ (4 tests)
Tests distribution creation: normal and uniform distributions with various parameters.

##### Example ([test_stochastic_normal.josh](../josh-tests/conformance/stochastic/distributions/test_stochastic_normal.josh#L46-L52))
```josh
start organism TestOrg
  # Sample from normal distribution with mean=100, std=15
  value.init = sample normal with mean of 100 m std of 15 m
  value.step = prior.value
end organism
```

#### sampling/ (3 tests)
Tests `sample` operation: from collections, with replacement, without replacement.

##### Example ([test_stochastic_sampling_with_replacement.josh](../josh-tests/conformance/stochastic/sampling/test_stochastic_sampling_with_replacement.josh#L23-L61))
```josh
# Sample 200 times from a small range [1, 10] - expect duplicates
TestOrg.init = create 200 count of TestOrg

# Count occurrences of each value to verify duplicates exist
count1.step = count(TestOrg[TestOrg.discreteValue >= 0.5 count and TestOrg.discreteValue < 1.5 count])
count2.step = count(TestOrg[TestOrg.discreteValue >= 1.5 count and TestOrg.discreteValue < 2.5 count])
# ... counts 3-10 ...

# Verify the sampling produced expected variance
maxCount.step = max(count1 | count2 | count3 | count4 | count5 | count6 | count7 | count8 | count9 | count10)
minCount.step = min(count1 | count2 | count3 | count4 | count5 | count6 | count7 | count8 | count9 | count10)
assert.hasVariance.step = (maxCount - minCount) > 0 count
assert.hasDuplicates.step = maxCount > 15 count
```

---

### temporal/ (10 tests, 1 failing)
Tests for temporal mechanics and meta-variable access.

#### prior/ (3 tests)
Tests `prior.*` access: basic values, chained access, and collections from previous timestep.

##### Example ([test_temporal_prior_basic.josh](../josh-tests/conformance/temporal/prior/test_temporal_prior_basic.josh#L31-L45))
```josh
# Age uses prior to reference previous timestep
age.init = 0 year
age.step = prior.age + 1 year

# Height grows based on prior height
height.init = 1 m
height.step = prior.height + 0.5 m

# Biomass depends on both height and age
biomass.init = 0 kg
biomass.step = prior.biomass + (current.height * 0.1 kg/m)

# Growth rate calculated from height change
growthRate.init = 0 m
growthRate.step = current.height - prior.height
```

#### queries/ (7 tests, 1 failing)
Tests `meta.*` variables: stepCount access, complex conditions, cross-attribute references.

##### Example ([test_temporal_queries_meta.josh](../josh-tests/conformance/temporal/queries/test_temporal_queries_meta.josh#L27-L46))
```josh
# Track metadata at patch level
recordedYear.init = 0 year
recordedYear.step = meta.year

recordedStepCount.init = 0 count
recordedStepCount.step = meta.stepCount

# Assert meta.year matches expected values
assert.yearStep0.step:if(meta.stepCount == 0 count) = meta.year == 2020 years
assert.yearStep1.step:if(meta.stepCount == 1 count) = meta.year == 2021 years
assert.yearStep2.step:if(meta.stepCount == 2 count) = meta.year == 2022 years

# Assert meta.stepCount increases from 0
assert.stepCountStep0.step:if(meta.year == 2020 years) = meta.stepCount == 0 count
assert.stepCountStep1.step:if(meta.year == 2021 years) = meta.stepCount == 1 count
```

##### FAILING - unit simplification ([test_temporal_queries_meta_cross_attribute.josh](../josh-tests/conformance/temporal/queries/test_temporal_queries_meta_cross_attribute.josh#L124-L125))
```josh
# This test fails with "No conversion between mm * proportion and mm"
# Water requirements based on simulation environment
waterNeeded.init = meta.rainfall * 0.01 proportion
```

---

### types/ (51 tests, 3 failing)
Tests for Josh's type system: scalars, distributions, functions, and conversions.

#### conversions/ (8 tests)
Tests type conversions: autoboxing, chained conversions, dimensionless units, explicit casts.

##### Example ([test_conversions_implicit.josh](../josh-tests/conformance/types/conversions/test_conversions_implicit.josh#L22-L47))
```josh
# Test implicit conversion in arithmetic operations
distance1.init = 1000 m
distance2.init = 2 km

# Addition with implicit conversion
totalDistanceM.init = (distance1 + (distance2 as m))
assert.implicitAdd.init = totalDistanceM == 3000 m

# Test with custom units - implicit conversion in expressions
length1.init = 24 in
length2.init = 2 ft

# Subtraction with implicit conversion
diffInches.init = (length1 - (length2 as in))
assert.implicitSubtract.init = diffInches == 0 in

# Test comparison with implicit conversion
dist1.init = 5000 m
dist2.init = 5 km
assert.implicitCompare.init = (dist1 as km) == dist2
```

#### distributions/ (15 tests)
Tests distribution operations: arithmetic, reduction functions, realized vs virtual distributions.

##### Example ([test_distributions_realized.josh](../josh-tests/conformance/types/distributions/test_distributions_realized.josh#L24-L47))
```josh
# Create organisms with realized distribution - each gets unique value
Trees.init = create 100 count of Tree

# Test that values are different across organisms (not all the same)
assert.hasVariation.init = (max(Trees.height) - min(Trees.height)) > 50 m

# Assert all values are within expected range [0, 100]
assert.minInRange.init = min(Trees.height) >= 0 m
assert.maxInRange.init = max(Trees.height) <= 100 m

# Test mean is roughly in the middle
assert.meanReasonable.init = mean(Trees.height) > 40 m
assert.meanReasonable2.init = mean(Trees.height) < 60 m

start organism Tree
  # Each organism gets a unique sampled value at creation (realized distribution)
  height.init = sample uniform from 0 m to 100 m
end organism
```

#### functions/ (8 tests, 2 failing)
Tests built-in functions: abs, difference, log/ln, power, rounding, trigonometric.

##### Example ([test_functions_abs.josh](../josh-tests/conformance/types/functions/test_functions_abs.josh#L22-L40))
```josh
# Test abs with positive scalar
positiveValue.init = 5 m
absPositive.init = abs(positiveValue)
assert.absPositive.init = absPositive == 5 m

# Test abs with negative scalar
negativeValue.init = -10 m
absNegative.init = abs(negativeValue)
assert.absNegative.init = absNegative == 10 m

# Test abs with zero
zeroValue.init = 0 m
absZero.init = abs(zeroValue)
assert.absZero.init = absZero == 0 m

# Test abs with negative fractional value
negativeFraction.init = -3.7 m
absFraction.init = abs(negativeFraction)
assert.absFraction.init = absFraction == 3.7 m
```

##### FAILING - non-integer exponents with units ([test_functions_power.josh](../josh-tests/conformance/types/functions/test_functions_power.josh#L45-L48))
```josh
# Test square root (exponent 0.5)
# This test fails with "Non-integer exponents with units are not supported"
base4.init = 4 m
sqrt4.init = base4 ^ 0.5
assert.sqrt4.init = sqrt4 == 2 m
```

#### limit/ (4 tests)
Tests `limit` operation: min-only, max-only, range, and chained limits.

##### Example ([test_limit_range.josh](../josh-tests/conformance/types/limit/test_limit_range.josh#L22-L45))
```josh
# Test value below range - should be clamped to min
belowMin.init = 5 m
belowLimited.init = limit belowMin to [10 m, 50 m]
assert.belowRangeMin.init = belowLimited == 10 m

# Test value above range - should be clamped to max
aboveMax.init = 75 m
aboveLimited.init = limit aboveMax to [10 m, 50 m]
assert.aboveRangeMax.init = aboveLimited == 50 m

# Test value within range - should be unchanged
withinRange.init = 30 m
withinLimited.init = limit withinRange to [10 m, 50 m]
assert.withinRange.init = withinLimited == 30 m

# Test value at lower boundary
atMin.init = 10 m
atMinLimited.init = limit atMin to [10 m, 50 m]
assert.atMinBoundary.init = atMinLimited == 10 m

# Test value at upper boundary
atMax.init = 50 m
atMaxLimited.init = limit atMax to [10 m, 50 m]
assert.atMaxBoundary.init = atMaxLimited == 50 m
```

#### map/ (5 tests)
Tests `map` operation: linear, quadratic, sigmoid interpolation.

##### Example ([test_map_linear.josh](../josh-tests/conformance/types/map/test_map_linear.josh#L22-L69))
```josh
# Test basic linear map from [0, 100] to [0, 1]
input1.init = 0 m
mapped1.init = map input1 from [0 m, 100 m] to [0 count, 1 count]
assert.mapAtMin.init = mapped1 == 0 count

input2.init = 100 m
mapped2.init = map input2 from [0 m, 100 m] to [0 count, 1 count]
assert.mapAtMax.init = mapped2 == 1 count

input3.init = 50 m
mapped3.init = map input3 from [0 m, 100 m] to [0 count, 1 count]
assert.mapAtMid.init = mapped3 == 0.5 count

# Test linear map with inverted output range
invertInput1.init = 0 m
invertMapped1.init = map invertInput1 from [0 m, 100 m] to [100 count, 0 count]
assert.invertedAtMin.init = invertMapped1 == 100 count
```

#### scalars/ (11 tests, 1 failing)
Tests scalar arithmetic and units: basic ops, mixed units, multiplication, conversions.

##### Example ([test_scalars_arithmetic_basic.josh](../josh-tests/conformance/types/scalars/test_scalars_arithmetic_basic.josh#L22-L44))
```josh
# Test addition with same units
length1.init = 10 m
length2.init = 5 m
sumLength.init = length1 + length2
assert.addition.init = sumLength == 15 m

# Test subtraction with same units
distance1.init = 20 m
distance2.init = 8 m
diffDistance.init = distance1 - distance2
assert.subtraction.init = diffDistance == 12 m

# Test multiplication by scalar
baseLength.init = 3 m
multiplier.init = 4 count
productLength.init = baseLength * multiplier
assert.multiplication.init = productLength == 12 m

# Test division by scalar
totalLength.init = 10 m
divisor.init = 2 count
quotientLength.init = totalLength / divisor
assert.division.init = quotientLength == 5 m
```

##### FAILING - dimensionless unit simplification ([test_scalars_units_percentage.josh](../josh-tests/conformance/types/scalars/test_scalars_units_percentage.josh#L40-L45))
```josh
# Test 10 percent of 1 km = 100 m
# This test fails with "No conversion between km * ratio and m"
total4.init = 1 km
percent4.init = 0.10 ratio
result4.init = percent4 * total4
result4M.init = result4 as m
assert.tenPercentKm.init = result4M == 100 m
```

---

## Failing Tests Detail

All 6 failing tests require **design decisions** - the current behavior is intentional but the tests assume different behavior.

### 1. Heterogeneous Collection Concatenation (2 tests)

| Test | Error |
|------|-------|
| [`test_collections_chained`](../josh-tests/conformance/core/collections/test_collections_chained.josh) | `No conversion exists between "Pine" and "Fir"` |
| [`test_collections_combine`](../josh-tests/conformance/core/collections/test_collections_combine.josh) | `No conversion exists between "Pine" and "Fir"` |

**What happens:** The `|` operator attempts type conversion when combining collections of different organism types.

**Test code ([line 29](../josh-tests/conformance/core/collections/test_collections_chained.josh#L29)):**
```josh
allConifers.step:if(meta.stepCount >= 1 count) = Pines | Firs
```

**Design question:** Should Josh support heterogeneous collections?
- **Option A:** Support mixed collections (ecologically useful - "all trees")
- **Option B:** Require homogeneous collections (type-safe, current behavior)
- **Option C:** Support if common base type/attributes exist

### 2. Unit Simplification (2 tests)

| Test | Error |
|------|-------|
| [`test_scalars_units_percentage`](../josh-tests/conformance/types/scalars/test_scalars_units_percentage.josh) | `No conversion exists between "km * ratio" and "m"` |
| [`test_temporal_queries_meta_cross_attribute`](../josh-tests/conformance/temporal/queries/test_temporal_queries_meta_cross_attribute.josh) | `No conversion exists between "mm * proportion" and "mm"` |

**What happens:** When multiplying a value by a dimensionless ratio/proportion, the compound unit isn't auto-simplified.

**Test code ([lines 40-45](../josh-tests/conformance/types/scalars/test_scalars_units_percentage.josh#L40-L45)):**
```josh
total4.init = 1 km
percent4.init = 0.10 ratio
result4.init = percent4 * total4
result4M.init = result4 as m
assert.tenPercentKm.init = result4M == 100 m
```

**Design question:** Should dimensionless multipliers cancel out?
- **Option A:** Auto-simplify (`km * ratio` -> `km`)
- **Option B:** Require explicit: `force (percent4 * total4) as km`
- **Option C:** Only simplify for specific units (count, ratio, proportion, percent)

### 3. Non-Integer Exponents with Units (2 tests)

| Test | Error |
|------|-------|
| [`test_functions_power`](../josh-tests/conformance/types/functions/test_functions_power.josh) | `Non-integer exponents with units are not supported` |
| [`test_functions_chained`](../josh-tests/conformance/types/functions/test_functions_chained.josh) | Same |

**What happens:** Square root and fractional exponents fail when the base has units.

**Test code ([lines 45-48](../josh-tests/conformance/types/functions/test_functions_power.josh#L45-L48)):**
```josh
# Test square root (exponent 0.5)
base4.init = 4 m
sqrt4.init = base4 ^ 0.5
assert.sqrt4.init = sqrt4 == 2 m
```

**Design question:** What should `4 m ^ 0.5` return?
- The mathematical result is `2 m^0.5` - but `m^0.5` isn't a meaningful unit
- **Option A:** Allow only for unitless values (current behavior)
- **Option B:** Force strip units first: `(force base4 as count) ^ 0.5`
- **Option C:** Implement half-units (m^0.5) for intermediate calculations

---

## Running the Tests

```bash
# Run all conformance tests
./gradlew conformanceTest

# Run specific test manually
java -jar build/libs/joshsim-fat.jar run ../josh-tests/conformance/<path>.josh <SimName> --seed 42

# Generate test data for external data tests
./gradlew generateTestData
```

---

## Related Documentation

- `LANGUAGE_DESIGN_CHOICES.md` - Design decisions needed for remaining failures
- `DEV_RESTORE_SUMMARY.md` - Overview of dev-restore branch and PRs
- `LanguageSpecification.md` - Full Josh language specification
