"""Check that remote simulation step labels are correct.

Reads the CSV output of a 5-replicate remote run of StepCheck and
verifies that step == year == height for every row, and that all
expected (step, replicate) pairs are present.
"""

import csv
import sys

EXPECTED_REPLICATES = 5
EXPECTED_STEPS = 10  # steps.low=0, steps.high=9 inclusive

found_pairs = set()
errors = []

try:
    with open('/tmp/remote_step_check.csv', newline='') as f:
        reader = csv.DictReader(f)
        for row in reader:
            step = int(row['step'])
            replicate = int(row['replicate'])
            year = int(float(row['year']))
            height = int(float(row['height']))

            if year != step:
                errors.append(
                    f'step={step} replicate={replicate}: year={year} != step'
                )
            if height != step:
                errors.append(
                    f'step={step} replicate={replicate}: height={height} != step'
                )

            found_pairs.add((step, replicate))

except FileNotFoundError:
    print('FAIL: output CSV not found at /tmp/remote_step_check.csv')
    sys.exit(1)

expected_pairs = {
    (s, r)
    for s in range(EXPECTED_STEPS)
    for r in range(EXPECTED_REPLICATES)
}
missing_pairs = expected_pairs - found_pairs

if errors:
    print(f'FAIL: {len(errors)} rows with incorrect values')
    for e in errors[:10]:
        print(f'  {e}')
    sys.exit(1)

if missing_pairs:
    print(f'FAIL: {len(missing_pairs)} (step, replicate) pairs missing')
    for m in sorted(missing_pairs)[:10]:
        print(f'  step={m[0]}, replicate={m[1]}')
    sys.exit(1)

print(
    f'PASS: {len(found_pairs)} unique (step, replicate) pairs found, '
    'height == year == step for all rows'
)
sys.exit(0)
