#!/bin/bash
# Test Generator for Step Bug Testing
# Usage: ./generate_test.sh <test_id> <test_name> <pattern_type> [organism_type]

set -e

if [ $# -lt 3 ]; then
  echo "Usage: $0 <test_id> <test_name> <pattern_type> [organism_type]"
  echo "Example: $0 013 separate_collection minimal Tree"
  exit 1
fi

TEST_ID=$1
TEST_NAME=$2
PATTERN_TYPE=$3
ORGANISM_TYPE=${4:-Tree}

TEST_DIR="/workspaces/josh/step_bug_testing/test_${TEST_ID}_${TEST_NAME}"
mkdir -p "$TEST_DIR"

echo "Generating test ${TEST_ID}: ${TEST_NAME}"

# Generate organism section based on type
generate_organism() {
  local org_name=$1
  local complexity=${2:-minimal}

  cat << EOF
start organism ${org_name}
  # COMPLEXITY: ${complexity}
  age.init = 0 years
  age.step = prior.age + 1 year

  log.step = debug(geoKey, "ORG_STEP", "age:", age, "year:", meta.year)
end organism
EOF
}

# Generate patch section based on pattern
generate_patch_pattern() {
  case "$1" in
    separate_collection)
      cat << 'EOF'
start patch Default
  # PATTERN: separate_collection - new items in separate attribute
  newTrees.step:if(meta.year == 1) = create 10 of Tree
  Trees.end = prior.Trees | newTrees

  log.step = debug(geoKey, "PATCH_STEP", "year:", meta.year, "treeCount:", count(Trees))
end patch
EOF
      ;;

    filter_in_end)
      cat << 'EOF'
start patch Default
  # PATTERN: filter_in_end - filter instead of combine
  Trees.init = create 0 of Tree
  Trees.step:if(meta.year == 1) = create 10 of Tree
  Trees.end = Trees[age >= 0 years]

  log.step = debug(geoKey, "PATCH_STEP", "year:", meta.year, "treeCount:", count(Trees))
end patch
EOF
      ;;

    count_in_end)
      cat << 'EOF'
start patch Default
  # PATTERN: count_in_end - read-only access in .end
  Trees.step:if(meta.year == 1) = create 10 of Tree
  treeCount.end = count(Trees)

  log.step = debug(geoKey, "PATCH_STEP", "year:", meta.year, "treeCount:", count(Trees))
end patch
EOF
      ;;

    end_different_collection)
      cat << 'EOF'
start patch Default
  # PATTERN: end_different_collection - .end on OTHER collection
  Trees.step:if(meta.year == 1) = create 10 of Tree
  OtherStuff.end = Trees

  log.step = debug(geoKey, "PATCH_STEP", "year:", meta.year, "treeCount:", count(Trees))
end patch
EOF
      ;;

    one_with_end_one_without)
      cat << 'EOF'
start patch Default
  # PATTERN: one_with_end_one_without - mixed collections
  Trees.step:if(meta.year == 1) = create 10 of Tree
  Trees.end = prior.Trees | Trees

  Shrubs.step:if(meta.year == 1) = create 5 of Shrub
  # No Shrubs.end

  log.step = debug(geoKey, "PATCH_STEP", "year:", meta.year, "treeCount:", count(Trees), "shrubCount:", count(Shrubs))
end patch
EOF
      ;;

    create_at_step_0)
      cat << 'EOF'
start patch Default
  # PATTERN: create_at_step_0 - create at year 0 instead of year 1
  Trees.init = create 0 of Tree
  Trees.step:if(meta.year == 0) = create 10 of Tree
  Trees.end = prior.Trees | Trees

  log.step = debug(geoKey, "PATCH_STEP", "year:", meta.year, "treeCount:", count(Trees))
end patch
EOF
      ;;

    unconditional_step)
      cat << 'EOF'
start patch Default
  # PATTERN: unconditional_step - create every step
  Trees.init = create 0 of Tree
  Trees.step = create 1 of Tree
  Trees.end = prior.Trees | Trees

  log.step = debug(geoKey, "PATCH_STEP", "year:", meta.year, "treeCount:", count(Trees))
end patch
EOF
      ;;

    two_collections_both_end)
      cat << 'EOF'
start patch Default
  # PATTERN: two_collections_both_end - both have .end handlers
  Trees.step:if(meta.year == 1) = create 10 of Tree
  Trees.end = prior.Trees | Trees

  Shrubs.step:if(meta.year == 1) = create 5 of Shrub
  Shrubs.end = prior.Shrubs | Shrubs

  log.step = debug(geoKey, "PATCH_STEP", "year:", meta.year, "treeCount:", count(Trees), "shrubCount:", count(Shrubs))
end patch
EOF
      ;;

    prior_only_end)
      cat << 'EOF'
start patch Default
  # PATTERN: prior_only_end - .end references only prior
  Trees.step:if(meta.year == 1) = create 10 of Tree
  Trees.end = prior.Trees

  log.step = debug(geoKey, "PATCH_STEP", "year:", meta.year, "treeCount:", count(Trees))
end patch
EOF
      ;;

    *)
      echo "Unknown pattern: $1"
      exit 1
      ;;
  esac
}

# Generate full test.josh file
cat > "$TEST_DIR/test.josh" << EOF
start simulation TestSimulation
  steps.low = 0
  steps.high = 4

  grid.size = 30 m
  grid.low = 34.0 degrees latitude, -116.0 degrees longitude
  grid.high = 34.01 degrees latitude, -116.01 degrees longitude
  grid.patch = "Default"

  year.init = 0 count
  year.step = prior.year + 1 count

  debugFiles.organism = "file:////${TEST_DIR}/debug_organism_0.txt"
  debugFiles.patch = "file:////${TEST_DIR}/debug_patch_0.txt"
end simulation

$(generate_patch_pattern "$PATTERN_TYPE")

$(generate_organism "Tree" "minimal")

EOF

# Add Shrub organism if needed for multi-collection tests
if [[ "$PATTERN_TYPE" == "one_with_end_one_without" || "$PATTERN_TYPE" == "two_collections_both_end" ]]; then
  cat >> "$TEST_DIR/test.josh" << 'EOF'

start organism Shrub
  # COMPLEXITY: minimal
  age.init = 0 years
  age.step = prior.age + 1 year

  log.step = debug(geoKey, "ORG_STEP", "age:", age, "year:", meta.year)
end organism

EOF
fi

# Add units
cat >> "$TEST_DIR/test.josh" << 'EOF'
# Units
start unit year
  alias years
end unit

start unit m
  alias meters
end unit

start unit count
end unit

start unit degrees
end unit
EOF

# Create run script
cat > "$TEST_DIR/run.sh" << 'RUNSCRIPT'
#!/bin/bash
cd "$(dirname "$0")"
java -jar ../../build/libs/joshsim-fat.jar run test.josh TestSimulation 2>&1 | tee simulation.log
RUNSCRIPT
chmod +x "$TEST_DIR/run.sh"

# Create symlink to editor.jshc
ln -sf ../common/editor.jshc "$TEST_DIR/editor.jshc"

echo "✓ Generated test directory: $TEST_DIR"
echo "  - test.josh"
echo "  - run.sh"
echo "  - editor.jshc (symlink)"
echo ""
echo "To run: cd $TEST_DIR && ./run.sh"
