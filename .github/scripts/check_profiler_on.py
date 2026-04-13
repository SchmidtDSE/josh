"""Check that profiler timings are non-trivial (> 0.1 ms, profiler enabled).

Reads the CSV output of a run of ProfilerMultiExample and verifies that
at least one row's avgHeightEvalDuration value is > 0.1 ms, confirming
that the profiler was active and capturing real timing data during the
simulation run. When the profiler is disabled, all values are exactly 0.0;
when enabled, values are consistently above 0.1 ms because the simulation
uses 50 TimedTree agents per patch, giving each mean() call enough
evaluations to produce a meaningful timing even on fast hardware.

This script is shared with the remote profiler test (component 3).
"""

import csv
import sys

THRESHOLD_MS = 0.1

found = False
row_count = 0

try:
    with open('/tmp/profiler_multi_josh.csv', newline='') as f:
        reader = csv.DictReader(f)
        for row in reader:
            row_count += 1
            duration = float(row['avgHeightEvalDuration'])
            if duration > THRESHOLD_MS:
                found = True

except FileNotFoundError:
    print('FAIL: output CSV not found at /tmp/profiler_multi_josh.csv')
    sys.exit(1)

if not found:
    print(
        f'FAIL: no row has avgHeightEvalDuration > {THRESHOLD_MS} ms '
        f'({row_count} rows checked)'
    )
    sys.exit(1)

print(
    f'PASS: at least one row has avgHeightEvalDuration > {THRESHOLD_MS} ms '
    f'({row_count} rows checked)'
)
sys.exit(0)
