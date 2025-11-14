#!/bin/bash
# Validates organism execution in a test output file
# Usage: ./validate_organism_execution.sh <debug_organism_0.txt>

DEBUG_FILE=$1

if [ ! -f "$DEBUG_FILE" ]; then
    echo "Error: File not found: $DEBUG_FILE"
    exit 1
fi

if [ ! -s "$DEBUG_FILE" ]; then
    echo "⚠️  Empty debug file (no organisms or debug disabled)"
    exit 0
fi

echo "Analyzing organism execution in: $DEBUG_FILE"
echo ""

# Check execution across timesteps
echo "Event counts by timestep:"
grep "^\[Step" "$DEBUG_FILE" | awk '{print $1, $2}' | sort | uniq -c

echo ""

# Validate consistent execution
step_counts=$(grep "^\[Step" "$DEBUG_FILE" | awk '{print $1, $2}' | sort -u | wc -l)

if [ "$step_counts" -ge 2 ]; then
    echo "✅ Organisms execute at multiple timesteps"

    # Check for consistency
    event_counts=$(grep "^\[Step" "$DEBUG_FILE" | awk '{print $1, $2}' | sort | uniq -c | awk '{print $1}' | sort -u | wc -l)

    if [ "$event_counts" -eq 1 ]; then
        echo "✅ Event counts are consistent across timesteps"
    else
        echo "⚠️  Event counts vary across timesteps (may be intentional)"
    fi
else
    echo "❌ CRITICAL BUG: Organisms only execute at 1 timestep!"
    exit 1
fi

# Check for exceptions
if grep -q "IllegalMonitorStateException" "$DEBUG_FILE"; then
    echo "❌ CRITICAL BUG: IllegalMonitorStateException detected"
    exit 1
fi

if grep -q "CircularDependencyException" "$DEBUG_FILE"; then
    echo "❌ CRITICAL BUG: Circular dependency detected"
    exit 1
fi

echo ""
echo "✅ All validation checks passed"
exit 0
