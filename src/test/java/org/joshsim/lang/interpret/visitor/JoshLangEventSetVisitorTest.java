/**
 * Tests for JoshLangEventSetVisitor and the origin-init compile-time guard.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret.visitor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    assertTrue(events.isInitEvent("init"));
    assertTrue(events.isInitEvent("init:founding"));
    assertTrue(events.isInitEvent("init:outplant"));
    assertFalse(events.isInitEvent("init:recruitment"));
  }

  @Test
  void programWithoutOriginInitHasOnlyBaseInit() {
    KnownEventSet events = discover("start organism Tree age.init = 0 years end organism");
    assertTrue(events.isInitEvent("init"));
    assertFalse(events.isInitEvent("init:founding"));
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

}
