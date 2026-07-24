/**
 * ISO-8601 simulation clock used by simulations that explicitly select {@code time.type = "ISO"}.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.bridge;

import java.time.LocalDate;
import java.time.Period;


/** Immutable date-only simulation clock backed exclusively by {@code java.time}. */
final class IsoSimulationClock {

  private final LocalDate low;
  private final LocalDate high;
  private final Period interval;
  private final long count;

  IsoSimulationClock(String lowValue, String highValue, String intervalValue) {
    low = LocalDate.parse(lowValue);
    high = LocalDate.parse(highValue);
    interval = Period.parse(intervalValue);
    if (interval.isZero() || interval.isNegative()) {
      throw new IllegalArgumentException("time.interval must be a positive ISO-8601 period");
    }
    if (high.isBefore(low)) {
      throw new IllegalArgumentException("time.high must not precede time.low");
    }

    LocalDate cursor = low;
    long resolvedCount = 1;
    while (cursor.isBefore(high)) {
      cursor = cursor.plus(interval);
      resolvedCount++;
      if (cursor.isAfter(high)) {
        throw new IllegalArgumentException(
            "time.high must fall exactly on the sequence defined by time.low and time.interval");
      }
    }
    count = resolvedCount;
  }

  long getCount() {
    return count;
  }

  LocalDate getAt(long zeroBasedIndex) {
    if (zeroBasedIndex < 0 || zeroBasedIndex >= count) {
      throw new IllegalArgumentException(
          "ISO simulation time index is out of range: " + zeroBasedIndex);
    }
    return low.plus(interval.multipliedBy(Math.toIntExact(zeroBasedIndex)));
  }
}
