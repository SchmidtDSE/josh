/**
 * Tests for JoshLangEventSetVisitor and the origin-init compile-time guard.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret.visitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.joshsim.JoshSimFacade;
import org.joshsim.engine.geometry.grid.GridGeometryFactory;
import org.joshsim.lang.interpret.KnownEventSet;
import org.joshsim.lang.io.JvmInputOutputLayerBuilder;
import org.joshsim.lang.parse.JoshParser;
import org.joshsim.lang.parse.ParseResult;
import org.junit.jupiter.api.Test;

/**
 * Verifies the pre-pass discovers declared origin init events and rejects them on patch/simulation.
 */
class JoshLangEventSetVisitorTest {

  private KnownEventSet discover(String code) {
    ParseResult parsed = new JoshParser().parse(code);
    assertFalse(parsed.hasErrors(), "fixture should parse: " + parsed.getErrors());
    return new JoshLangEventSetVisitor().visit(parsed.getProgram().orElseThrow());
  }

  @Test
  void discoversDeclaredOriginInitEvents() {
    KnownEventSet events = discover(
        "start organism Tree "
        + "start init through \"founding\" age = 40 years end init "
        + "start init through \"outplant\" age = 0 years end init "
        + "end organism");

    assertTrue(events.isInitEvent("Tree", "init"));
    assertTrue(events.isInitEvent("Tree", "init:founding"));
    assertTrue(events.isInitEvent("Tree", "init:outplant"));
    assertFalse(events.isInitEvent("Tree", "init:recruitment"));
  }

  @Test
  void programWithoutOriginInitHasOnlyBaseInit() {
    KnownEventSet events = discover("start organism Tree age.init = 0 years end organism");
    assertTrue(events.isInitEvent("Tree", "init"));
    assertFalse(events.isInitEvent("Tree", "init:founding"));
  }

  @Test
  void originDeclaredByOneEntityDoesNotLeakToAnotherEntity() {
    KnownEventSet events = discover(
        "start organism Tree "
        + "start init through \"founding\" age = 40 years end init "
        + "end organism "
        + "start organism Shrub "
        + "age.init = 0 years "
        + "end organism");

    assertTrue(events.isInitEvent("Tree", "init:founding"));
    // Shrub never declared "founding"; it must fall back to base init, not inherit Tree's variant.
    assertFalse(events.isInitEvent("Shrub", "init:founding"));
    assertTrue(events.isInitEvent("Shrub", "init"));
  }

  @Test
  void initThroughOnPatchIsRejected() {
    ParseResult parsed = JoshSimFacade.parse(
        "start simulation Main "
        + "grid.size = 10 m "
        + "grid.low = 0 degrees latitude, 0 degrees longitude "
        + "grid.high = 1 degrees latitude, 1 degrees longitude "
        + "steps.low = 0 count steps.high = 1 count "
        + "end simulation "
        + "start patch Default "
        + "start init through \"founding\" foo = 1 count end init "
        + "end patch");
    assertFalse(parsed.hasErrors(), "fixture should parse: " + parsed.getErrors());

    assertThrows(RuntimeException.class, () -> JoshSimFacade.interpret(
        new GridGeometryFactory(),
        parsed,
        new JvmInputOutputLayerBuilder().withReplicate(1).build()));
  }

  @Test
  void discoversDeclaredPhaseOrder() {
    KnownEventSet events = discover(
        "start simulation Main "
        + "start phases "
        + "with phase base "
        + "then phase disturb "
        + "then phase manage "
        + "end phases "
        + "end simulation");

    assertEquals(List.of("base", "disturb", "manage"), events.getSubstepOrder());
    assertTrue(events.isEventName("disturb"));
  }

  @Test
  void phasesOutsideSimulationIsRejected() {
    assertThrows(RuntimeException.class, () -> discover(
        "start patch Default "
        + "start phases with phase base end phases "
        + "end patch"));
  }

  @Test
  void multiplePhasesStanzasInOneSimulationAreRejected() {
    assertThrows(RuntimeException.class, () -> discover(
        "start simulation Main "
        + "start phases with phase base end phases "
        + "start phases with phase other end phases "
        + "end simulation"));
  }

  @Test
  void phasesMustStartWithWith() {
    assertThrows(RuntimeException.class, () -> discover(
        "start simulation Main "
        + "start phases then phase base end phases "
        + "end simulation"));
  }

  @Test
  void onlyTheFirstPhaseMayUseWith() {
    assertThrows(RuntimeException.class, () -> discover(
        "start simulation Main "
        + "start phases with phase base with phase disturb end phases "
        + "end simulation"));
  }

  @Test
  void differentPhasesDeclaredInDifferentSimulationsAreRejected() {
    assertThrows(RuntimeException.class, () -> discover(
        "start simulation Main "
        + "start phases with phase base end phases "
        + "end simulation "
        + "start simulation Other "
        + "start phases with phase alternate end phases "
        + "end simulation"));
  }

}
