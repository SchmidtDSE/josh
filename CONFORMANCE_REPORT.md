# Conformance Test Results

**Date:** 2026-01-13
**Branch:** `feat/test_external_data_generator`
**Commit:** Based on 4e4b6cc6 (Random seed logic) + test data generator

## Summary

| Status | Count | Percentage |
|--------|-------|------------|
| **Passed** | 83 | 69.2% |
| **Failed** | 37 | 30.8% |
| **Total** | 120 | 100% |

---

## Test Results by Category

### Control Flow (5/5 - 100%)
| Test | Status |
|------|--------|
| test_control_conditionals | PASS |
| test_states_basic | PASS |
| test_states_conditional | PASS |
| test_states_handlers | PASS |
| test_states_timing | PASS |

### Core - Collections (1/6 - 17%)
| Test | Status | Error |
|------|--------|-------|
| test_collections_create | PASS | |
| test_collections_chained | FAIL | No conversion between "Pine" and "Fir" |
| test_collections_combine | FAIL | Unable to get value for Tree.age |
| test_collections_filter_basic | FAIL | Unable to get value for Tree.age |
| test_collections_filter_complex | FAIL | Unable to get value for Tree.age |
| test_collections_remove | FAIL | Entity attribute access error |

### Core - Keywords & Lifecycle (2/2 - 100%)
| Test | Status |
|------|--------|
| test_keywords_prior | PASS |
| test_simple_organism | PASS |

### Entities - Disturbance (2/2 - 100%)
| Test | Status |
|------|--------|
| test_disturbance_basic | PASS |
| test_disturbance_organisms | PASS |

### Entities - Management (1/2 - 50%)
| Test | Status | Error |
|------|--------|-------|
| test_management_basic | PASS | |
| test_management_organisms | FAIL | Assertion failed |

### IO - Assertions (4/5 - 80%)
| Test | Status | Notes |
|------|--------|-------|
| test_assertions_conditional_complex | PASS | |
| test_assertions_conditional_if | PASS | |
| test_assertions_pass | PASS | |
| test_assertions_scope | PASS | |
| test_assertions_fail | FAIL | Expected - tests assertion mechanism |

### IO - Exports (7/8 - 88%)
| Test | Status | Error |
|------|--------|-------|
| test_export_csv_basic | PASS | |
| test_export_csv_timesteps | PASS | |
| test_export_geotiff_multi | PASS | |
| test_export_memory_aggregation | PASS | |
| test_export_memory_basic | PASS | |
| test_export_netcdf_spatial | PASS | |
| test_export_netcdf_temporal | PASS | |
| test_export_geotiff_single | FAIL | Loop resolving elevation |

### IO - External Data (6/6 - 100%)
| Test | Status |
|------|--------|
| test_external_geotiff_checkerboard | PASS |
| test_external_geotiff_sequential | PASS |
| test_external_jshd_basic | PASS |
| test_external_jshd_large | PASS |
| test_external_netcdf_precipitation | PASS |
| test_external_netcdf_temperature | PASS |

### Spatial - Neighbors (4/4 - 100%)
| Test | Status |
|------|--------|
| test_spatial_neighbors_attributes | PASS |
| test_spatial_neighbors_count | PASS |
| test_spatial_neighbors_distance | PASS |
| test_spatial_neighbors_interactions | PASS |

### Spatial - Patches (2/4 - 50%)
| Test | Status | Error |
|------|--------|-------|
| test_spatial_patches_attributes | PASS | |
| test_spatial_patches_location | PASS | |
| test_spatial_patches_heterogeneous | FAIL | Unable to get value for meta.x |
| test_spatial_patches_types | FAIL | Unable to get value for meta.x |

### Spatial - Queries (4/4 - 100%)
| Test | Status |
|------|--------|
| test_spatial_within_empty | PASS |
| test_spatial_within_multiple_radii | PASS |
| test_spatial_within_radius | PASS |
| test_spatial_within_self | PASS |

### Stochastic - Arithmetic (2/3 - 67%)
| Test | Status | Error |
|------|--------|-------|
| test_stochastic_arithmetic_complex | PASS | |
| test_stochastic_arithmetic_distribution | PASS | |
| test_stochastic_arithmetic_scalar | FAIL | Assertion failed |

### Stochastic - Distributions (2/4 - 50%)
| Test | Status | Error |
|------|--------|-------|
| test_stochastic_uniform | PASS | |
| test_stochastic_uniform_range | PASS | |
| test_stochastic_normal | FAIL | Assertion failed (stdDevCheck) |
| test_stochastic_normal_parameters | FAIL | Assertion failed (stdDev1) |

### Stochastic - Sampling (0/3 - 0%)
| Test | Status | Error |
|------|--------|-------|
| test_stochastic_sampling_collection | FAIL | Unable to get value for SourceOrg |
| test_stochastic_sampling_with_replacement | FAIL | Cannot add boolean |
| test_stochastic_sampling_without_replacement | FAIL | Assertion failed |

### Temporal - Prior (2/3 - 67%)
| Test | Status | Error |
|------|--------|-------|
| test_temporal_prior_basic | PASS | |
| test_temporal_prior_chained | PASS | |
| test_temporal_prior_collections | FAIL | Assertion failed |

### Temporal - Queries (1/8 - 13%)
| Test | Status | Error |
|------|--------|-------|
| test_temporal_queries_meta_nested_basic | PASS | |
| test_temporal_queries_history | FAIL | Assertion failed |
| test_temporal_queries_meta | FAIL | Unable to get value |
| test_temporal_queries_meta_complex | FAIL | Assertion failed |
| test_temporal_queries_meta_cross_attribute | FAIL | Unable to get value for yr |
| test_temporal_queries_meta_deep_nesting | FAIL | Unable to get value for yr |
| test_temporal_queries_meta_entity_types | FAIL | Unable to get value for yr |
| test_temporal_queries_meta_temporal | FAIL | Unable to get value |

### Types - Conversions (8/8 - 100%)
| Test | Status |
|------|--------|
| test_conversions_autoboxing_mixed | PASS |
| test_conversions_chained | PASS |
| test_conversions_dimensionless | PASS |
| test_conversions_distribution_to_scalar | PASS |
| test_conversions_explicit_cast | PASS |
| test_conversions_implicit | PASS |
| test_conversions_incompatible_units | PASS |
| test_conversions_scalar_to_distribution | PASS |

### Types - Distributions (13/14 - 93%)
| Test | Status | Error |
|------|--------|-------|
| test_distributions_arithmetic_addition | PASS | |
| test_distributions_arithmetic_complex | PASS | |
| test_distributions_arithmetic_multiplication | PASS | |
| test_distributions_mean | PASS | |
| test_distributions_min_max | PASS | |
| test_distributions_multiple_reductions | PASS | |
| test_distributions_realized | PASS | |
| test_distributions_realized_vs_virtual | PASS | |
| test_distributions_sampling_normal | PASS | |
| test_distributions_sampling_replacement | PASS | |
| test_distributions_sampling_uniform | PASS | |
| test_distributions_std | PASS | |
| test_distributions_sum | PASS | |
| test_distributions_virtual | PASS | |
| test_distributions_count | FAIL | Assertion failed |

### Types - Functions (2/8 - 25%)
| Test | Status | Error |
|------|--------|-------|
| test_functions_log | PASS | |
| test_functions_rounding | PASS | |
| test_functions_abs | FAIL | Cannot apply abs to distribution |
| test_functions_chained | FAIL | Unable to get meta.stepCount |
| test_functions_difference | FAIL | Unable to get meta.stepCount |
| test_functions_distributions | FAIL | Cannot apply abs to distribution |
| test_functions_power | FAIL | Non-integer exponents with units |
| test_functions_trigonometric | FAIL | Assertion failed |

### Types - Limits (4/4 - 100%)
| Test | Status |
|------|--------|
| test_limit_chained | PASS |
| test_limit_max | PASS |
| test_limit_min | PASS |
| test_limit_range | PASS |

### Types - Map (0/5 - 0%)
| Test | Status | Error |
|------|--------|-------|
| test_map_chained | FAIL | Unknown mapping: linearly |
| test_map_limit_combined | FAIL | Unknown mapping |
| test_map_linear | FAIL | Unknown mapping: linearly |
| test_map_quadratic | FAIL | Unknown mapping: sigmoidally |
| test_map_sigmoid | FAIL | Unknown mapping: sigmoidally |

### Types - Scalars (9/10 - 90%)
| Test | Status | Error |
|------|--------|-------|
| test_scalars_arithmetic_basic | PASS | |
| test_scalars_arithmetic_mixed | PASS | |
| test_scalars_arithmetic_multiplication | PASS | |
| test_scalars_arithmetic_precedence | PASS | |
| test_scalars_conversion_custom | PASS | |
| test_scalars_conversion_metric | PASS | |
| test_scalars_conversion_temperature | PASS | |
| test_scalars_units_degrees | PASS | |
| test_scalars_units_distance | PASS | |
| test_types_unit_conversion | PASS | |
| test_scalars_units_percentage | FAIL | Assertion failed |

---

## Failure Categories

| Category | Count | Tests |
|----------|-------|-------|
| Collection filtering scope issues | 5 | Tree.age not resolving in filter lambdas |
| Meta attribute access | 9 | meta.x, meta.stepCount, yr not in scope |
| Unimplemented map functions | 5 | linearly, sigmoidally not implemented |
| Function limitations | 3 | abs/pow don't support distributions |
| Assertion failures (logic/tolerance) | 9 | Various assertion mismatches |
| Sampling issues | 3 | Entity references, boolean operations |
| Other | 3 | Loop resolution, expected failures |

---

## Recommendations

### High Priority
1. **Collection filtering** - Fix scope resolution for `Entity.attribute` in filter lambdas
2. **Meta attribute access** - Ensure `meta.*` available in all contexts

### Medium Priority
3. **Implement map functions** - Add `linearly`, `quadratically`, `sigmoidally`
4. **Distribution support** - Extend `abs()`, `pow()` for distributions

### Low Priority
5. **Review assertion tolerances** - Some stochastic tests may need wider bounds
6. **Test corrections** - `test_assertions_fail` is expected to fail

---

## Running Tests

```bash
# Generate test data and preprocess (automatic with conformanceTest)
./gradlew generateTestData

# Run all conformance tests
./gradlew conformanceTest

# Run a single test manually
java -jar build/libs/joshsim-fat.jar run josh-tests/conformance/<path>/test_<name>.josh <SimName> --seed 42
```
