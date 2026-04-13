"""Check that profiler timings are all zero (profiler disabled).

Reads the CSV output of a run of ProfilerMultiExample and verifies that
every row's avgHeightEvalDuration value is less than 1.0 ms, confirming
that the profiler was not enabled during the simulation run.
"""

import csv
import sys

errors = []
row_count = 0

try:
    with open('/tmp/profiler_multi_josh.csv', newline='') as f:
        reader = csv.DictReader(f)
        for row in reader:
            row_count += 1
            step = row['step']
            replicate = row['replicate']
            duration = float(row['avgHeightEvalDuration'])

            if duration >= 1.0:
                errors.append(
                    f'step={step} replicate={replicate}: '
                    f'avgHeightEvalDuration={duration} >= 1.0 ms'
                )

except FileNotFoundError:
    print('FAIL: output CSV not found at /tmp/profiler_multi_josh.csv')
    sys.exit(1)

if errors:
    print(f'FAIL: {len(errors)} rows have avgHeightEvalDuration >= 1.0 ms')
    for e in errors[:10]:
        print(f'  {e}')
    sys.exit(1)

print(f'PASS: all {row_count} rows have avgHeightEvalDuration < 1.0 ms')
sys.exit(0)
