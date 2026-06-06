"""Check profiler timings from CSV output of a ProfilerMultiExample run.

Usage: python3 check_profiler.py <on|off>

  on  -- asserts that at least one row has avgHeightEvalDuration > 0.0 ms,
         confirming the profiler was active and capturing real timing data.
         When the profiler is disabled, all values are exactly 0.0; when
         enabled, values are non-zero. Using > 0.0 avoids false failures on
         fast CI runners where durations can be very small but still non-zero.

  off -- asserts that every row has avgHeightEvalDuration < 1.0 ms,
         confirming the profiler was not enabled during the simulation run.
"""

import csv
import sys

ON_THRESHOLD_MS = 0.0
OFF_THRESHOLD_MS = 1.0

if len(sys.argv) < 2 or sys.argv[1] not in ('on', 'off'):
    print('Usage: check_profiler.py <on|off>')
    sys.exit(1)

mode = sys.argv[1]

if mode == 'on':
    found = False
    row_count = 0

    try:
        with open('/tmp/profiler_multi_josh.csv', newline='') as f:
            reader = csv.DictReader(f)
            for row in reader:
                row_count += 1
                duration = float(row['avgHeightEvalDuration'])
                if duration > ON_THRESHOLD_MS:
                    found = True

    except FileNotFoundError:
        print('FAIL: output CSV not found at /tmp/profiler_multi_josh.csv')
        sys.exit(1)

    if not found:
        print(
            f'FAIL: no row has avgHeightEvalDuration > {ON_THRESHOLD_MS} '
            f'(profiler appears inactive; {row_count} rows checked)'
        )
        sys.exit(1)

    print(
        f'PASS: at least one row has avgHeightEvalDuration > {ON_THRESHOLD_MS} '
        f'({row_count} rows checked)'
    )
    sys.exit(0)

else:  # mode == 'off'
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

                if duration >= OFF_THRESHOLD_MS:
                    errors.append(
                        f'step={step} replicate={replicate}: '
                        f'avgHeightEvalDuration={duration} >= {OFF_THRESHOLD_MS} ms'
                    )

    except FileNotFoundError:
        print('FAIL: output CSV not found at /tmp/profiler_multi_josh.csv')
        sys.exit(1)

    if errors:
        print(f'FAIL: {len(errors)} rows have avgHeightEvalDuration >= {OFF_THRESHOLD_MS} ms')
        for e in errors[:10]:
            print(f'  {e}')
        sys.exit(1)

    print(f'PASS: all {row_count} rows have avgHeightEvalDuration < {OFF_THRESHOLD_MS} ms')
    sys.exit(0)
