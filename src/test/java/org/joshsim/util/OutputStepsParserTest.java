/**
 * Tests for the OutputStepsParser utility.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;


/**
 * Test for the OutputStepsParser utility.
 */
public class OutputStepsParserTest {

  @Test
  public void testEmptyReturnsEmpty() {
    assertTrue(OutputStepsParser.parseForCli("").isEmpty());
    assertTrue(OutputStepsParser.parseForCli(null).isEmpty());
    assertTrue(OutputStepsParser.parseForCli("   ").isEmpty());
  }

  @Test
  public void testPlainCommaSeparatedList() {
    Optional<Set<Integer>> result = OutputStepsParser.parseForCli("5,7,8,9,20");
    assertEquals(Set.of(5, 7, 8, 9, 20), result.orElseThrow());
  }

  @Test
  public void testInclusiveRange() {
    Optional<Set<Integer>> result = OutputStepsParser.parseForCli("0-3");
    assertEquals(Set.of(0, 1, 2, 3), result.orElseThrow());
  }

  @Test
  public void testMixedRangesAndSingleSteps() {
    Optional<Set<Integer>> result = OutputStepsParser.parseForCli("5,7-9,20");
    assertEquals(Set.of(5, 7, 8, 9, 20), result.orElseThrow());
  }

  @Test
  public void testSingleValueRangeCollapsesToOneStep() {
    Optional<Set<Integer>> result = OutputStepsParser.parseForCli("4-4");
    assertEquals(Set.of(4), result.orElseThrow());
  }

  @Test
  public void testInvertedRangeThrows() {
    assertThrows(IllegalArgumentException.class, () -> OutputStepsParser.parseForCli("9-5"));
  }

  @Test
  public void testGarbageThrowsForCli() {
    assertThrows(IllegalArgumentException.class, () -> OutputStepsParser.parseForCli("abc"));
  }

  @Test
  public void testGarbageThrowsForWasmOrRemote() {
    assertThrows(RuntimeException.class, () -> OutputStepsParser.parseForWasmOrRemote("abc"));
  }

  @Test
  public void testWasmOrRemoteParsesRanges() {
    Optional<Set<Integer>> result = OutputStepsParser.parseForWasmOrRemote("0-2,10");
    assertEquals(Set.of(0, 1, 2, 10), result.orElseThrow());
  }
}
