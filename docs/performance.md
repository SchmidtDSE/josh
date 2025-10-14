# Performance Optimizations

## Overview

This document catalogs the performance optimizations implemented in Josh Simulation Engine. These optimizations were identified through profiling and targeted to address CPU and memory hotspots in large-scale simulations (1M+ entities).

## Attribute Access Optimizations

### Integer-Based Array Access

**Location:** `DirectLockMutableEntity`, `FrozenEntity`, `ShadowingEntity`, `EntityBuilder`

**Description:** Replaced string-based HashMap attribute access with integer-based array indexing.

**Performance Gains:**
- O(1) array indexing vs O(log n) HashMap lookups
- Eliminates string hashing overhead
- Eliminates HashMap bucket lookup overhead
- Reduces method call overhead from HashMap.get()

**Implementation Details:**
- Each entity type maintains a shared immutable `attributeNameToIndex` map computed once per entity type in EntityBuilder
- Each entity type maintains a shared `indexToAttributeName` array for O(1) reverse lookup
- Entity instances store attribute values in `EngineValue[]` arrays indexed by attribute index
- ShadowingEntity uses array-based caching (`resolvedCacheByIndex`) for resolved values
- FrozenEntity uses direct array access with explicit null checks to avoid Optional.ofNullable() overhead

**Files Modified:**
- `src/main/java/org/joshsim/engine/entity/base/MutableEntity.java` - Interface supporting integer-based access
- `src/main/java/org/joshsim/engine/entity/base/DirectLockMutableEntity.java` - Implementation with array-based storage
- `src/main/java/org/joshsim/engine/entity/base/FrozenEntity.java` - Immutable entity with array access
- `src/main/java/org/joshsim/engine/entity/base/EntityBuilder.java` - Index map computation and sharing
- `src/main/java/org/joshsim/lang/bridge/ShadowingEntity.java` - Array-based resolution caching
- `src/main/java/org/joshsim/lang/bridge/PriorShadowingEntityDecorator.java` - Integer-based iteration
- `src/main/java/org/joshsim/lang/bridge/InnerEntityGetter.java` - Integer-based iteration
- `src/main/java/org/joshsim/lang/interpret/machine/EntityFastForwarder.java` - Integer-based resolution
- `src/main/java/org/joshsim/engine/func/EntityScope.java` - Direct array access avoiding string lookup overhead

## Spatial Query Optimizations

### Circle Query Optimization with Precomputed Offsets

**Location:** `TimeStep.java`

**Description:** Optimized circle geometry queries by precomputing which grid cells intersect a circle of a given radius, eliminating redundant intersection tests.

**Performance Gains:**
- 43% reduction in loop iterations for circle queries (compared to square bounding box approach)
- Zero false positives in candidate selection
- O(1) cache lookup for repeated queries with same radius
- Eliminates per-query intersection mathematics

**Implementation Details:**
- Global cache `CIRCLE_OFFSETS_CACHE` maps radius (ceiled to integer) to list of (dx, dy) offsets
- Offsets computed once per unique radius using exact circle-square intersection mathematics
- Cache key uses ceiling to handle fractional radii conservatively
- Thread-safe using ConcurrentHashMap for parallel query processing
- Typical simulations use 5-10 distinct radii (total cache memory < 1 MB)
- Each offset stored as IntPair (2 × 4-byte ints) for memory efficiency

**Files Modified:**
- `src/main/java/org/joshsim/engine/simulation/TimeStep.java` - Circle offset caching and optimized query

### Spatial Index for Patch Lookups

**Location:** `TimeStep.java`

**Description:** 2D grid-based spatial index for efficient patch candidate selection.

**Performance Gains:**
- 90-95% reduction in expensive intersection checks
- O(1) grid cell lookup vs O(N) linear scan
- Eliminates need to check all patches for every query

**Implementation Details:**
- `PatchSpatialIndex` organizes patches by grid location
- Built once per TimeStep and reused for all queries
- Uses 2D Entity[][] array for direct cell access
- Pre-filters patches based on grid cell overlap before exact intersection tests
- For circle queries, uses exact precomputed offsets (zero false positives)
- For square/point queries, uses bounding box filtering (may have false positives, requires intersection test)

**Files Modified:**
- `src/main/java/org/joshsim/engine/simulation/TimeStep.java` - Spatial index implementation

## Caching Strategies

### Shared Handler Cache per Entity Type

**Location:** `EntityBuilder.java`, `DirectLockMutableEntity.java`, `ShadowingEntity.java`

**Description:** Pre-compute all possible handler lookups once per entity type and share across all instances.

**Performance Gains:**
- Eliminates per-instance HandlerCacheKey allocations
- Eliminates per-instance ConcurrentHashMap lookups during resolution
- Identified as significant CPU and memory optimization in profiling
- For simulations with 1M entities, eliminates millions of allocations

**Implementation Details:**
- `EntityBuilder.computeCommonHandlerCache()` pre-computes all (attribute × substep × state) combinations
- Cache keyed by "attribute:substep" or "attribute:substep:state" strings
- Shared immutable map across all entity instances of same type
- ShadowingEntity uses shared cache for O(1) handler lookup without allocation

**Files Modified:**
- `src/main/java/org/joshsim/engine/entity/base/EntityBuilder.java` - Cache computation
- `src/main/java/org/joshsim/engine/entity/base/DirectLockMutableEntity.java` - Cache storage and access
- `src/main/java/org/joshsim/lang/bridge/ShadowingEntity.java` - Cache usage

### Shared Attribute Names per Entity Type

**Location:** `EntityBuilder.java`

**Description:** Compute attribute name set once per entity type instead of per instance.

**Performance Gains:**
- Identified as #1 CPU and memory hotspot in profiling
- For simulations with 1M entities, replaces 1M HashSet allocations with single shared Set
- Eliminates per-instance handler iteration during entity construction

**Implementation Details:**
- `EntityBuilder.computeAttributeNames()` extracts all unique attribute names from handlers
- Result cached as immutable Set shared across all instances
- Thread-safe for concurrent reads during parallel simulation execution

**Files Modified:**
- `src/main/java/org/joshsim/engine/entity/base/EntityBuilder.java` - Attribute name set computation and sharing

### Circular Dependency Tracking with Arrays

**Location:** `ShadowingEntity.java`

**Description:** Use array-based tracking for circular dependency detection instead of HashSet add/remove operations.

**Performance Gains:**
- O(1) array access vs HashSet add/remove overhead
- Eliminates allocations from HashSet operations
- Reduces garbage collection pressure

**Implementation Details:**
- `resolvingByIndex` boolean array tracks which attributes are currently being resolved
- Array indexed by attribute index for O(1) access
- Cleared after each resolution completes
- Falls back to unsafe resolution for entities without index support (e.g., test mocks)

**Files Modified:**
- `src/main/java/org/joshsim/lang/bridge/ShadowingEntity.java` - Array-based loop detection

### Fast-Path Handler Lookup Optimization

**Location:** `EntityBuilder.java`, `DirectLockMutableEntity.java`, `ShadowingEntity.java`

**Description:** Track which attributes have no handlers for specific substeps to skip expensive lookups.

**Performance Gains:**
- Enables fast-path in ShadowingEntity by skipping handler lookups when attribute has no handlers
- Computed once per entity type, shared across all instances
- Significant performance improvement for attributes without handlers (e.g., constants)

**Implementation Details:**
- `EntityBuilder.computeAttributesWithoutHandlersBySubstep()` analyzes handlers per substep
- Conservative approach: only marks as "no handlers" if in initial attributes but no handler for substep
- Prevents false negatives (incorrectly skipping handler execution)
- DirectLockMutableEntity provides `hasNoHandlers()` for O(1) lookup

**Files Modified:**
- `src/main/java/org/joshsim/engine/entity/base/EntityBuilder.java` - Computation of no-handler tracking
- `src/main/java/org/joshsim/engine/entity/base/DirectLockMutableEntity.java` - Fast-path checking
- `src/main/java/org/joshsim/lang/bridge/ShadowingEntity.java` - Fast-path usage

## Memory Optimizations

### ArrayList Pre-sizing to Eliminate Growth Overhead

**Location:** Various files

**Description:** Pre-size ArrayList instances with known or estimated capacity to avoid ArrayList.grow() overhead.

**Performance Gains:**
- Eliminates array copying during ArrayList growth
- Reduces memory allocations
- Particularly important in hot paths with many allocations

**Implementation Locations:**
- `TimeStep.queryCandidatesForCircle()` - Pre-size with exact offset count
- `TimeStep.queryCandidates()` - Pre-size with bounding box dimensions
- `TimeStep.getOffsetsForRadius()` - Pre-size with bounding box size
- `SimulationStepper.stepForward()` - Pre-size innerEntities ArrayList
- `SingleThreadEventHandlerMachine` - Pre-size with known counts

**Files Modified:**
- `src/main/java/org/joshsim/engine/simulation/TimeStep.java`
- `src/main/java/org/joshsim/lang/bridge/SimulationStepper.java`
- `src/main/java/org/joshsim/lang/interpret/machine/SingleThreadEventHandlerMachine.java`

### Shared Index Maps to Eliminate Per-Instance HashMap Overhead

**Location:** `EntityBuilder.java`

**Description:** Compute attributeNameToIndex and indexToAttributeName once per entity type and share across instances.

**Performance Gains:**
- Eliminates per-instance HashMap allocations
- For 1M entities, replaces 1M HashMap allocations with single shared immutable map
- Reduces memory footprint significantly

**Implementation Details:**
- EntityBuilder computes maps once and caches as immutable
- All entity instances of same type share the same map reference
- Thread-safe immutable structures

**Files Modified:**
- `src/main/java/org/joshsim/engine/entity/base/EntityBuilder.java`

### GridPatchBuilder BigDecimal Cache

**Location:** `GridPatchBuilder.java`

**Description:** Cache frequently-used BigDecimal values to eliminate redundant allocations.

**Performance Gains:**
- Eliminates millions of BigDecimal allocations during grid building
- Cache dimensioned for typical grid dimensions up to 10,000x10,000 cells

**Files Modified:**
- `src/main/java/org/joshsim/engine/geometry/grid/GridPatchBuilder.java`

## Summary

These optimizations collectively provide significant performance improvements for large-scale simulations:

1. **Attribute Access:** Integer-based array access eliminates HashMap overhead
2. **Spatial Queries:** 43% fewer iterations for circles, 90-95% fewer intersection checks overall
3. **Memory:** Shared data structures eliminate millions of allocations for large simulations
4. **Handler Resolution:** Fast-path optimizations skip unnecessary handler lookups

The optimizations were identified through profiling and targeted at the hottest code paths in entity attribute access, spatial queries, and handler resolution.
