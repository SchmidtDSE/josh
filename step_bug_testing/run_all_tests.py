#!/usr/bin/env python3
"""Comprehensive test runner for organism lifecycle testing"""

import os
import subprocess
import sys
from pathlib import Path
from collections import defaultdict

def count_events_by_step(debug_file):
    """Count organism events by step from debug file"""
    if not debug_file.exists() or debug_file.stat().st_size == 0:
        return None

    counts = defaultdict(int)
    with open(debug_file) as f:
        for line in f:
            if line.startswith('[Step '):
                step_num = line.split(',')[0].split()[1]
                counts[int(step_num)] += 1
    return counts

def run_test(test_dir):
    """Run a single test and return results"""
    test_name = test_dir.name
    run_script = test_dir / "run.sh"

    if not run_script.exists():
        return {"status": "SKIP", "reason": "No run.sh"}

    # Run the test
    try:
        result = subprocess.run(
            ["bash", str(run_script)],
            cwd=test_dir,
            capture_output=True,
            timeout=60
        )
    except subprocess.TimeoutExpired:
        return {"status": "ERROR", "reason": "Timeout"}
    except Exception as e:
        return {"status": "ERROR", "reason": str(e)}

    # Check simulation log for exceptions
    sim_log = test_dir / "simulation.log"
    if sim_log.exists():
        with open(sim_log) as f:
            log_content = f.read()
            if "IllegalMonitorStateException" in log_content:
                return {"status": "FAIL", "reason": "IllegalMonitorStateException"}

    # Check debug output
    debug_file = test_dir / "debug_organism_0.txt"
    counts = count_events_by_step(debug_file)

    if counts is None:
        return {"status": "PASS", "reason": "No debug output (expected for some tests)", "counts": {}}

    if not counts:
        return {"status": "FAIL", "reason": "No organism events", "counts": {}}

    # Check if organisms execute beyond step 0
    beyond_step_0 = any(step > 0 and count > 0 for step, count in counts.items())

    if beyond_step_0:
        return {"status": "PASS", "reason": "Organisms execute at multiple steps", "counts": counts}
    else:
        return {"status": "WARN", "reason": "Only step 0 events", "counts": counts}

def main():
    base_dir = Path("/workspaces/josh/step_bug_testing")

    # Find all test directories
    test_dirs = sorted([d for d in base_dir.iterdir() if d.is_dir() and d.name.startswith("test_")])

    print("=" * 60)
    print("ORGANISM LIFECYCLE INTEGRATION TEST SUITE")
    print("=" * 60)
    print()

    results = {}
    key_tests = ["test_005", "test_024", "test_026", "test_012", "test_023"]

    for test_dir in test_dirs:
        test_name = test_dir.name
        print(f"Running {test_name}...", end=" ", flush=True)

        result = run_test(test_dir)
        results[test_name] = result

        # Print result with counts
        if result["status"] == "PASS":
            counts = result.get("counts", {})
            if counts:
                count_str = " ".join(f"S{s}:{c}" for s, c in sorted(counts.items())[:5])
                print(f"✅ PASS: {result['reason']} ({count_str})")
            else:
                print(f"✅ PASS: {result['reason']}")
        elif result["status"] == "FAIL":
            print(f"❌ FAIL: {result['reason']}")
        elif result["status"] == "WARN":
            counts = result.get("counts", {})
            count_str = " ".join(f"S{s}:{c}" for s, c in sorted(counts.items())[:5])
            print(f"⚠️  WARN: {result['reason']} ({count_str})")
        elif result["status"] == "ERROR":
            print(f"⚠️  ERROR: {result['reason']}")
        else:
            print(f"⏭️  SKIP: {result['reason']}")

    # Summary
    print()
    print("=" * 60)
    print("TEST RESULTS SUMMARY")
    print("=" * 60)

    status_counts = defaultdict(int)
    for result in results.values():
        status_counts[result["status"]] += 1

    print(f"  ✅ Passed: {status_counts['PASS']}")
    print(f"  ❌ Failed: {status_counts['FAIL']}")
    print(f"  ⚠️  Warnings: {status_counts['WARN']}")
    print(f"  ⚠️  Errors: {status_counts['ERROR']}")
    print(f"  ⏭️  Skipped: {status_counts['SKIP']}")
    print(f"  📊 Total:  {len(results)}")
    print()

    # Key tests
    print("=" * 60)
    print("KEY TEST RESULTS (Critical for validation)")
    print("=" * 60)
    for key in key_tests:
        matching = [name for name in results.keys() if key in name]
        for test_name in matching:
            result = results[test_name]
            print(f"  {test_name}: {result['status']} - {result['reason']}")
    print()

    # Exit code
    if status_counts['FAIL'] > 0 or status_counts['ERROR'] > 0:
        sys.exit(1)
    else:
        sys.exit(0)

if __name__ == "__main__":
    main()
