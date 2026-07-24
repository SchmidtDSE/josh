/**
 * Tests for the date-only ISO simulation clock.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.bridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;


/** Verifies ISO calendar progression without non-JDK calendar dependencies. */
class IsoSimulationClockTest {

  @Test
  void advancesByCalendarMonths() {
    IsoSimulationClock clock = new IsoSimulationClock("2026-01-01", "2026-03-01", "P1M");

    assertEquals(3, clock.getCount());
    assertEquals(LocalDate.of(2026, 1, 1), clock.getAt(0));
    assertEquals(LocalDate.of(2026, 2, 1), clock.getAt(1));
    assertEquals(LocalDate.of(2026, 3, 1), clock.getAt(2));
  }

  @Test
  void rejectsEndpointsThatDoNotFallOnTheDeclaredSequence() {
    assertThrows(IllegalArgumentException.class,
        () -> new IsoSimulationClock("2026-01-01", "2026-02-15", "P1M"));
  }

  @Test
  void rejectsZeroIntervals() {
    assertThrows(IllegalArgumentException.class,
        () -> new IsoSimulationClock("2026-01-01", "2026-01-01", "P0D"));
  }

  @Test
  void singleStepClockHasCountOne() {
    IsoSimulationClock clock = new IsoSimulationClock("2026-01-01", "2026-01-01", "P1Y");
    assertEquals(1, clock.getCount());
    assertEquals(LocalDate.of(2026, 1, 1), clock.getAt(0));
  }

  @Test
  void rejectsIndexOutOfBounds() {
    IsoSimulationClock clock = new IsoSimulationClock("2026-01-01", "2026-03-01", "P1M");
    assertThrows(IllegalArgumentException.class, () -> clock.getAt(-1));
    assertThrows(IllegalArgumentException.class, () -> clock.getAt(3));
  }

  @Test
  void monthlyIntervalHandlesMonthEndTruncation() {
    // Jan 31 + P1M = Feb 28 (java.time truncates), then Feb 28 + P1M = Mar 28, not Mar 31.
    // The clock must reject this because time.high doesn't fall on the declared sequence.
    assertThrows(IllegalArgumentException.class,
        () -> new IsoSimulationClock("2026-01-31", "2026-03-31", "P1M"));

    // A valid monthly sequence starting on the first of each month.
    IsoSimulationClock valid = new IsoSimulationClock("2026-01-01", "2026-03-01", "P1M");
    assertEquals(LocalDate.of(2026, 2, 1), valid.getAt(1));
  }
}
