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

}
