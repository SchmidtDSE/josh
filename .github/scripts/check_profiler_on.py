"""Check that profiler timings are non-trivial (> 0.0 ms, profiler enabled).

Reads the CSV output of a run of ProfilerMultiExample and verifies that
at least one row's avgHeightEvalDuration value is > 0.0 ms, confirming
that the profiler was active and capturing real timing data during the
simulation run. When the profiler is disabled, all values are exactly 0.0;
when enabled, values are consistently in the range 0.05-2.0 ms on typical
hardware. A threshold of > 0.0 reliably distinguishes the two cases.

This script is shared with the remote profiler test (component 3).
"""

import csv
import sys

THRESHOLD_MS = 0.0

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
