# Josh Debugging & Provenance Tracking Quickstart

This guide shows you how to use the new debugging and provenance tracking features in Josh simulations.

## Features Overview

### 1. **GeoKey with Sequence IDs**
Each entity gets a unique GeoKey that includes:
- Location (latitude, longitude)
- Sequence number (distinguishes entities at the same location)

Format: `(lat, lon, seq)`
Example: `(34.25, -116.25, 5)`

### 2. **Parent Provenance Tracking**
Access parent entity information using `parent.geoKey` to track which entities created which offspring.

### 3. **Debug Output**
Write debug messages during simulation execution to:
- Standard output (`stdout`)
- Local files (`file:///path/to/file.txt`)
- MinIO/S3 storage (`minio://bucket/path`)
- Memory (for web editor)

## Quick Start Examples

### Example 1: Basic Debug Output

```josh
start simulation MyDebugDemo
  grid.size = 1000 m
  grid.low = 34.0 degrees latitude, -116.5 degrees longitude
  grid.high = 34.5 degrees latitude, -116.0 degrees longitude

  steps.low = 0 count
  steps.high = 5 count

  exportFiles.agent = "memory://editor/organisms"

  # Configure debug output (choose one):
  debugFiles.organism = "stdout"                              # To console
  # debugFiles.organism = "file:///tmp/my_debug.txt"         # To file
  # debugFiles.patch = "file:///tmp/patch_debug.txt"         # For patches
end simulation

start patch Default
  Tree.init = create 5 count of Tree
end patch

start organism Tree
  age.init = 0 years
  age.step = prior.age + 1 years
  height.init = 1.0 meters
  height.step = prior.height + 0.5 meters

  # Debug during initialization
  debugInit.init = debug("Tree created at", geoKey)

  # Debug during step with multiple values
  debugStep.step = debug("Step", meta.stepCount, "age:", age, "height:", height)

  export.age.step = age
  export.geoKey.step = geoKey
end organism

start unit year
  alias years
end unit
```

**Run it:**
```bash
josh run my_simulation.josh MyDebugDemo
```

**Output format:**
```
[Step 0, organism] "Tree created at" "(34.25, -116.25, 0)"
[Step 1, organism] "Step" "1" "age:" "1 years" "height:" "1.5 meters"
```

### Example 2: Parent Provenance Tracking

```josh
start simulation ProvenanceDemo
  grid.size = 1000 m
  grid.low = 34.0 degrees latitude, -116.5 degrees longitude
  grid.high = 34.5 degrees latitude, -116.0 degrees longitude

  steps.low = 0 count
  steps.high = 10 count

  exportFiles.agent = "file:///tmp/trees.csv"
  debugFiles.organism = "file:///tmp/tree_provenance.txt"
end simulation

start patch Default
  ParentTree.init = create 3 count of ParentTree
end patch

start organism ParentTree
  age.init = 0 years
  age.step = prior.age + 1 years

  # Store parent info (for initial trees, this will be empty)
  parentGeoKey.init = ""
  generation.init = 1 count

  # Reproduce after aging
  canReproduce.step = age >= 2 years

  # Debug reproduction
  debugRepro.step = if(canReproduce,
    debug("REPRODUCE", geoKey, "gen", generation, "creating offspring"),
    0 count)

  # Create offspring with parent tracking
  Seed.step = if(canReproduce, create 1 count of Seed at nearby 100 meters,
                 create 0 count of Seed)

  export.geoKey.step = geoKey
  export.age.step = age
  export.generation.step = generation
  export.parentGeoKey.step = parentGeoKey
end organism

start organism Seed
  # ACCESS PARENT INFORMATION - This is the key feature!
  parentGeoKey.init = parent.geoKey        # Get parent's GeoKey
  parentGen.init = parent.generation       # Get parent's generation

  germinationTime.init = 0 count
  germinationTime.step = prior.germinationTime + 1 count

  # Debug seed with provenance info
  debugSeed.init = debug("SEED created from parent", parentGeoKey, "gen", parentGen)

  shouldGerminate.step = germinationTime >= 2 count

  # Germinate into new tree
  debugGerm.step = if(shouldGerminate,
    debug("GERMINATE", geoKey, "from parent", parentGeoKey, "->ParentTree"),
    0 count)

  ParentTree.step = if(shouldGerminate, create 1 count of ParentTree,
                       create 0 count of ParentTree)

  # Pass provenance to offspring
  ParentTree.step.parentGeoKey = parentGeoKey
  ParentTree.step.generation = parentGen + 1 count

  shouldDie.step = shouldGerminate

  export.geoKey.step = geoKey
  export.parentGeoKey.step = parentGeoKey
  export.parentGen.step = parentGen
end organism

start unit year
  alias years
end unit
```

**Run it:**
```bash
josh run provenance.josh ProvenanceDemo
```

**Debug output shows:**
```
[Step 0, organism] "SEED created from parent" "(34.1, -116.3, 0)" "gen" "1"
[Step 2, organism] "GERMINATE" "(34.15, -116.32, 0)" "from parent" "(34.1, -116.3, 0)" "->ParentTree"
[Step 3, organism] "REPRODUCE" "(34.15, -116.32, 0)" "gen" "2" "creating offspring"
```

**Export CSV shows:**
```csv
type,geoKey,age,generation,parentGeoKey
ParentTree,"(34.1, -116.3, 0)",5,1,""
ParentTree,"(34.15, -116.32, 0)",3,2,"(34.1, -116.3, 0)"
Seed,"(34.2, -116.35, 0)",1,0,"(34.15, -116.32, 0)"
```

## Debug Output Targets

### Standard Output (stdout)
```josh
debugFiles.organism = "stdout"
```
- Writes to console
- Good for development and quick debugging
- Output appears in terminal

### File Output
```josh
debugFiles.organism = "file:///tmp/my_debug.txt"
debugFiles.patch = "file:///tmp/patch_debug.txt"
```
- Writes to local file
- File is created if it doesn't exist
- Useful for logging and post-analysis

### MinIO/S3 Output
```josh
debugFiles.organism = "minio://my-bucket/simulations/debug_{replicate}.txt"
```
- Writes to cloud storage
- Requires MinIO configuration via command-line args
- Good for remote simulations

#### Template Variables in Paths
Debug file paths support template variables for organizing output:

```josh
debugFiles.organism = "file:///tmp/{user}/{editor}/debug_{replicate}.txt"
```

Available template variables:
- `{replicate}` - Replicate number (0, 1, 2, ...)
- `{user}` - Custom tag for user identification
- `{editor}` - Custom tag for editor/configuration name
- Any custom tags passed via `--custom-tag` argument

Example with custom tags:
```bash
josh run simulation.josh MyConfig \
  --custom-tag user=alice \
  --custom-tag editor=optimistic \
  --replicates 3
```

This creates files:
- `/tmp/alice/optimistic/debug_0.txt`
- `/tmp/alice/optimistic/debug_1.txt`
- `/tmp/alice/optimistic/debug_2.txt`

### Memory Output (Web Editor)
```josh
debugFiles.organism = "memory://editor/debug"
```
- Stores in memory for web interface
- Used internally by Josh web editor

## Debug Function Syntax

### Single Argument
```josh
debug("Simple message")
```

### Multiple Arguments
```josh
debug("Label:", value1, "another:", value2)
```

### With Expressions
```josh
debug("Age check:", age > 5 years, "count:", 2 * seedCount)
```

### In Conditionals
```josh
shouldDebug.step = if(age > 10 years,
  debug("Old tree:", geoKey, "age:", age),
  0 count)
```

## Accessing GeoKey

### Current Entity
```josh
myGeoKey.step = geoKey
```

### Parent Entity
```josh
parentKey.init = parent.geoKey
```

### In Debug Messages
```josh
debug("My location:", geoKey, "parent was at:", parent.geoKey)
```

### In Exports
```josh
export.geoKey.step = geoKey
export.parentGeoKey.step = parentGeoKey
```

## Best Practices

### 1. **Use Meaningful Debug Labels**
```josh
# Good
debug("REPRODUCE", "parent:", geoKey, "offspring:", childCount)

# Less helpful
debug(geoKey, childCount)
```

### 2. **Debug Key Events**
- Entity creation (`.init`)
- State changes (reproduction, death)
- Important decisions
- Error conditions

### 3. **Include Context**
```josh
debug("REPRODUCE", "step:", meta.stepCount, "parent:", geoKey, "gen:", generation)
```

### 4. **Use Separate Debug Files**
```josh
debugFiles.organism = "file:///tmp/organisms.txt"   # Entity behavior
debugFiles.patch = "file:///tmp/patches.txt"        # Patch updates
```

### 5. **Track Provenance Chains**
Store parent IDs and generations to reconstruct family trees:
```josh
parentGeoKey.init = parent.geoKey
parentGeneration.init = parent.generation
generation.init = parentGeneration + 1 count
```

## Tips & Tricks

### Filtering Debug Output
Debug messages include step number and entity type, so you can filter:
```bash
# Only show Step 5 messages
cat debug.txt | grep "\[Step 5,"

# Only show organism events
cat debug.txt | grep ", organism\]"

# Only show REPRODUCE events
cat debug.txt | grep "REPRODUCE"
```

### Unique Entity Identification
GeoKeys with sequences ensure uniqueness:
```josh
# Two trees at same location get different sequence numbers
Tree {} @ location(5, 5)    # Gets (5, 5, 0)
Tree {} @ location(5, 5)    # Gets (5, 5, 1)
```

### Zero Overhead When Not Used
- If you don't configure `debugFiles.*`, there's zero performance cost
- Debug messages are only evaluated when debug output is configured
- GeoKeys are cached for efficiency

## Common Issues

### Issue: Debug output is empty
**Solution**: Make sure you're using the correct entity type:
```josh
debugFiles.organism = "stdout"    # For organisms
debugFiles.patch = "stdout"       # For patches
debugFiles.agent = "stdout"       # For agents
```

### Issue: `parent.geoKey` doesn't work
**Solution**: Use it during entity creation (`.init`), not later:
```josh
parentKey.init = parent.geoKey    # ✓ Works
parentKey.step = parent.geoKey    # ✗ Won't work - parent not available
```

### Issue: Too many debug messages
**Solution**: Use conditionals to debug only when needed:
```josh
debugImportant.step = if(age > 100 years and health < 0.1,
  debug("CRISIS", geoKey, "age:", age, "health:", health),
  0 count)
```

## Complete Working Example

See `examples/test/debug_file_simple.josh` for a complete, runnable example that demonstrates:
- GeoKey access
- Parent provenance tracking
- Debug output to file
- Multi-parameter debug calls

Run it with:
```bash
./gradlew run --args="run examples/test/debug_file_simple.josh FileDebug"
cat /tmp/josh_debug_file.txt
```

## Additional Resources

- **Integration Tests**: `examples/test/test_parent_geokey.josh` - Tests parent.geoKey access
- **Debug Tests**: `examples/test/test_debug_output.josh` - Tests debug() function
- **Task Documentation**: `.claude/tasks/entity-debugging-and-provenance-tracking.md` - Full specification

---

For questions or issues, see https://github.com/joshsim/josh
