#!/usr/bin/env python3
import json
import re
import sys
import os
from collections import defaultdict
from datetime import datetime

if len(sys.argv) != 3:
    print("Usage: extract_results.py <test_dir> <test_id>")
    sys.exit(1)

test_dir = sys.argv[1]
test_id = sys.argv[2]

organism_log = os.path.join(test_dir, "debug_organism_0.txt")
sim_log = os.path.join(test_dir, "simulation.log")
result_json = os.path.join(test_dir, "result.json")

# Check if simulation completed
sim_completed = False
try:
    with open(sim_log, 'r') as f:
        sim_content = f.read()
        if "completed successfully" in sim_content:
            sim_completed = True
except FileNotFoundError:
    pass

if not sim_completed:
    with open(result_json, 'w') as f:
        json.dump({"status": "failed", "error": "simulation did not complete"}, f, indent=2)
    sys.exit(1)

# Count events by step
events_by_step = defaultdict(int)
total_events = 0
event_types = defaultdict(int)

try:
    with open(organism_log, 'r') as f:
        for line in f:
            match = re.match(r'\[Step (\d+),', line)
            if match:
                step = int(match.group(1))
                events_by_step[step] += 1
                total_events += 1

                if 'ORG_STEP' in line:
                    event_types['ORG_STEP'] += 1
                elif 'TREE_INIT' in line or 'ORG_INIT' in line:
                    event_types['TREE_INIT'] += 1
except FileNotFoundError:
    pass

# Determine first and last execution steps
steps = sorted(events_by_step.keys())
first_step = steps[0] if steps else -1
last_step = steps[-1] if steps else -1

# Bug detection logic
bug_present = False
bug_type = "none"
bug_desc = "Organisms execute at all steps after creation"

if total_events == 0:
    bug_present = True
    bug_type = "never_executes"
    bug_desc = "Organisms created but never execute .step logic"
elif first_step == last_step:
    bug_present = True
    bug_type = "creation_only"
    bug_desc = f"Organisms only execute at creation step (Step {first_step})"
elif len(steps) > 1:
    # Check for gaps - expected is consecutive steps
    expected_steps = set(range(first_step, last_step + 1))
    actual_steps = set(steps)
    if expected_steps != actual_steps:
        bug_present = True
        bug_type = "execution_gap"
        bug_desc = "Organisms do not execute at all steps (gap detected)"

# Build result JSON
result = {
    "test_id": test_id,
    "timestamp": datetime.now().isoformat(),
    "status": "completed",
    "results": {
        "simulation_completed": True,
        "total_organism_events": total_events,
        "events_by_step": dict(events_by_step),
        "event_types": dict(event_types),
        "bug_analysis": {
            "bug_present": bug_present,
            "bug_type": bug_type,
            "description": bug_desc,
            "first_execution_step": first_step,
            "last_execution_step": last_step
        }
    },
    "performance": {
        "runtime_seconds": 0,
        "organism_log_size_mb": 0
    }
}

with open(result_json, 'w') as f:
    json.dump(result, f, indent=2)

print(f"Extracted results for {test_id}: bug_present={bug_present}, bug_type={bug_type}, steps={first_step}-{last_step}")
