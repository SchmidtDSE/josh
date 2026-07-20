/**
 * Tests for StringLiteralUtil.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Test the STR_ quote-stripping helper.
 */
public class StringLiteralUtilTest {

  @Test
  public void testStripsSurroundingQuotes() {
    assertEquals("founding", StringLiteralUtil.stripQuotes("\"founding\""));
  }

  @Test
  public void testStripsEmptyQuotedString() {
    assertEquals("", StringLiteralUtil.stripQuotes("\"\""));
  }

  @Test
  public void testLeavesUnquotedUnchanged() {
    assertEquals("founding", StringLiteralUtil.stripQuotes("founding"));
  }

  @Test
  public void testLeavesSingleQuoteCharUnchanged() {
    assertEquals("\"", StringLiteralUtil.stripQuotes("\""));
  }
}
