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
  void baseInitEventIsRecognizedForAnyEntity() {
    KnownEventSet events = new KnownEventSet();
    assertTrue(events.isInitEvent("Tree", "init"));
    assertTrue(events.isInitEvent("Shrub", "init"));
  }

  @Test
  void addedOriginBecomesAnInitEventOnlyForItsOwnEntity() {
    KnownEventSet events = new KnownEventSet();
    events.addInitOrigin("Tree", "founding");

    assertTrue(events.isInitEvent("Tree", "init:founding"));
    assertFalse(events.isInitEvent("Tree", "init:outplant"));
  }

  @Test
  void originDeclaredByOneEntityDoesNotLeakToAnother() {
    KnownEventSet events = new KnownEventSet();
    events.addInitOrigin("Tree", "founding");

    // Shrub never declared "founding"; it must not inherit Tree's variant.
    assertFalse(events.isInitEvent("Shrub", "init:founding"));
    // Shrub still gets the always-available base init.
    assertTrue(events.isInitEvent("Shrub", "init"));
  }

  @Test
  void getInitEventsIsScopedToTheRequestedEntity() {
    KnownEventSet events = new KnownEventSet();
    events.addInitOrigin("Tree", "founding");
    events.addInitOrigin("Tree", "outplant");
    events.addInitOrigin("Shrub", "cutting");

    boolean treeHasCutting = false;
    for (String event : events.getInitEvents("Tree")) {
      if (event.equals("init:cutting")) {
        treeHasCutting = true;
      }
    }
    assertFalse(treeHasCutting, "Tree must not see Shrub's declared origin");
  }

  @Test
  void combineUnionsBothSetsKeepingEntitiesSeparate() {
    KnownEventSet first = new KnownEventSet();
    first.addInitOrigin("Tree", "founding");
    KnownEventSet second = new KnownEventSet();
    second.addInitOrigin("Shrub", "outplant");

    KnownEventSet combined = first.combine(second);
    assertTrue(combined.isInitEvent("Tree", "init:founding"));
    assertTrue(combined.isInitEvent("Shrub", "init:outplant"));
    assertTrue(combined.isInitEvent("Tree", "init"));

    // Combining must not cross-pollinate entities either.
    assertFalse(combined.isInitEvent("Tree", "init:outplant"));
    assertFalse(combined.isInitEvent("Shrub", "init:founding"));
  }

  @Test
  void combineDoesNotMutateOperands() {
    KnownEventSet first = new KnownEventSet();
    first.addInitOrigin("Tree", "founding");
    KnownEventSet second = new KnownEventSet();
    second.addInitOrigin("Tree", "outplant");

    first.combine(second);
    assertFalse(first.isInitEvent("Tree", "init:outplant"));
    assertFalse(second.isInitEvent("Tree", "init:founding"));
  }

}
