/**
 * Tests for KnownEventSet.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for the compile-time event-name record.
 */
class KnownEventSetTest {

  @Test
  void recognizesBaseInitAndStandardSubsteps() {
    KnownEventSet events = new KnownEventSet();
    assertTrue(events.isInitEvent("init"));
    assertTrue(events.isSubstepEvent("start"));
    assertTrue(events.isSubstepEvent("step"));
    assertTrue(events.isSubstepEvent("end"));
  }

  @Test
  void recognizesStructuralEventsAsEventNames() {
    KnownEventSet events = new KnownEventSet();
    assertTrue(events.isEventName("constant"));
    assertTrue(events.isEventName("remove"));
    assertTrue(events.isEventName("init"));
    assertTrue(events.isEventName("step"));
  }

  @Test
  void nonEventNameIsNotAnEvent() {
    KnownEventSet events = new KnownEventSet();
    assertFalse(events.isEventName("height"));
    assertFalse(events.isInitEvent("founding"));
  }

  @Test
  void initEventForNamespacesTheOrigin() {
    assertEquals("init:founding", KnownEventSet.initEventFor("founding"));
  }

  @Test
  void addedOriginBecomesAnInitEvent() {
    KnownEventSet events = new KnownEventSet();
    events.addInitOrigin("founding");
    assertTrue(events.isInitEvent("init:founding"));
    assertTrue(events.isEventName("init:founding"));
    assertFalse(events.isInitEvent("init:outplant"));
  }

  @Test
  void combineUnionsBothSets() {
    KnownEventSet first = new KnownEventSet();
    first.addInitOrigin("founding");
    KnownEventSet second = new KnownEventSet();
    second.addInitOrigin("outplant");

    KnownEventSet combined = first.combine(second);
    assertTrue(combined.isInitEvent("init:founding"));
    assertTrue(combined.isInitEvent("init:outplant"));
    assertTrue(combined.isInitEvent("init"));
  }

  @Test
  void combineDoesNotMutateOperands() {
    KnownEventSet first = new KnownEventSet();
    first.addInitOrigin("founding");
    KnownEventSet second = new KnownEventSet();
    second.addInitOrigin("outplant");

    first.combine(second);
    assertFalse(first.isInitEvent("init:outplant"));
    assertFalse(second.isInitEvent("init:founding"));
  }

}
