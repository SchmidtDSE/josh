/**
 * Unit tests for OutputPhasesParser.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link OutputPhasesParser} parsing logic and validation.
 */
public class OutputPhasesParserTest {

  @Test
  public void parseForCliEmptyReturnsEmpty() {
    assertTrue(OutputPhasesParser.parseForCli("").isEmpty());
  }

  @Test
  public void parseForCliNullReturnsEmpty() {
    assertTrue(OutputPhasesParser.parseForCli(null).isEmpty());
  }

  @Test
  public void parseForCliWhitespaceReturnsEmpty() {
    assertTrue(OutputPhasesParser.parseForCli("   ").isEmpty());
  }

  @Test
  public void parseForCliValidCsvReturnsSet() {
    Optional<Set<String>> result = OutputPhasesParser.parseForCli("observed,spindown");
    assertTrue(result.isPresent());
    assertEquals(Set.of("observed", "spindown"), result.get());
  }

  @Test
  public void parseForCliSinglePhase() {
    Optional<Set<String>> result = OutputPhasesParser.parseForCli("observed");
    assertTrue(result.isPresent());
    assertEquals(Set.of("observed"), result.get());
  }

  @Test
  public void parseForCliHandlesWhitespaceAndEmpties() {
    Optional<Set<String>> result = OutputPhasesParser.parseForCli(" observed , , spindown ");
    assertTrue(result.isPresent());
    assertEquals(Set.of("observed", "spindown"), result.get());
  }

  @Test
  public void parseForCliLowercasesPhases() {
    Optional<Set<String>> result = OutputPhasesParser.parseForCli("OBSERVED,SpinUp");
    assertTrue(result.isPresent());
    assertEquals(Set.of("observed", "spinup"), result.get());
  }

  @Test
  public void parseForCliAllPhases() {
    Optional<Set<String>> result = OutputPhasesParser.parseForCli("spinup,observed,spindown");
    assertTrue(result.isPresent());
    assertEquals(Set.of("spinup", "observed", "spindown"), result.get());
  }

  @Test
  public void parseForCliInvalidPhaseThrows() {
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> OutputPhasesParser.parseForCli("observed,bogus"));
    assertTrue(ex.getMessage().contains("spinup"));
    assertTrue(ex.getMessage().contains("observed"));
    assertTrue(ex.getMessage().contains("spindown"));
  }

  @Test
  public void parseForWasmEmptyReturnsEmpty() {
    assertTrue(OutputPhasesParser.parseForWasmOrRemote("").isEmpty());
  }

  @Test
  public void parseForWasmValidCsvReturnsSet() {
    Optional<Set<String>> result = OutputPhasesParser.parseForWasmOrRemote("observed,spindown");
    assertTrue(result.isPresent());
    assertEquals(Set.of("observed", "spindown"), result.get());
  }

  @Test
  public void parseForWasmInvalidPhaseThrows() {
    RuntimeException ex = assertThrows(RuntimeException.class,
        () -> OutputPhasesParser.parseForWasmOrRemote("nope"));
    assertTrue(ex.getMessage().contains("spinup"));
  }

  @Test
  public void parseForWasmOnlyEmptyTokensReturnsEmpty() {
    Optional<Set<String>> result = OutputPhasesParser.parseForWasmOrRemote(",,");
    assertFalse(result.isPresent());
  }
}
