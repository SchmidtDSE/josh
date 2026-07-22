/**
 * Tests for the Parser facade.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.parse;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


/**
 * Test for the Parser facade.
 */
public class JoshParserTest {

  private JoshParser parser;

  /**
   * Create a new parser before each test.
   */
  @BeforeEach
  public void setUp() {
    parser = new JoshParser();
  }

  @Test
  public void testEmpty() {
    ParseResult result = parser.parse("");
    assertFalse(result.hasErrors());
  }

  @Test
  public void testError() {
    ParseResult result = parser.parse("start organism test");
    assertTrue(result.hasErrors());
  }

  @Test
  public void testComplete() {
    ParseResult result = parser.parse("start organism test end organism");
    assertFalse(result.hasErrors());
  }

  @Test
  public void testCreateThroughParses() {
    ParseResult result = parser.parse(
        "start patch Default "
        + "Trees.init = create 3 count of Tree through \"founding\" "
        + "end patch");
    assertFalse(result.hasErrors());
  }

  @Test
  public void testCreateSingleThroughParses() {
    ParseResult result = parser.parse(
        "start patch Default "
        + "Sprouts.init = create Sprout through \"outplant\" "
        + "end patch");
    assertFalse(result.hasErrors());
  }

  @Test
  public void testInitThroughStanzaParses() {
    ParseResult result = parser.parse(
        "start organism Tree "
        + "start init through \"founding\" "
        + "age = 40 years "
        + "state = \"Adult\" "
        + "end init "
        + "end organism");
    assertFalse(result.hasErrors());
  }

  @Test
  public void testCreateThroughRequiresString() {
    ParseResult result = parser.parse(
        "start patch Default "
        + "Trees.init = create 3 count of Tree through founding "
        + "end patch");
    assertTrue(result.hasErrors());
  }

  @Test
  public void testPhasesStanzaParses() {
    ParseResult result = parser.parse(
        "start simulation Main "
        + "start phases "
        + "with phase base "
        + "then phase disturb "
        + "then phase manage "
        + "end phases "
        + "end simulation");
    assertFalse(result.hasErrors());
  }

  @Test
  public void testImportStatementParses() {
    ParseResult result = parser.parse("import \"other.josh\"");
    assertFalse(result.hasErrors());
  }

  @Test
  public void testImportStatementRequiresStringLiteral() {
    ParseResult result = parser.parse("import other.josh");
    assertTrue(result.hasErrors());
  }

  @Test
  public void testReplaceStanzaParses() {
    ParseResult result = parser.parse("replace organism Tree end organism");
    assertFalse(result.hasErrors());
  }

  @Test
  public void testUpdateStanzaParses() {
    ParseResult result = parser.parse("update organism Tree end organism");
    assertFalse(result.hasErrors());
  }

  @Test
  public void testPhasesStanzaRequiresPhaseKeyword() {
    ParseResult result = parser.parse(
        "start simulation Main "
        + "start phases "
        + "with base "
        + "end phases "
        + "end simulation");
    assertTrue(result.hasErrors());
  }

}
