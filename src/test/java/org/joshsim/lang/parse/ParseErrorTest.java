/**
 * Tests for the ParseError structure.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.parse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;


/**
 * Test for the ParseError structure.
 */
public class ParseErrorTest {

  @Test
  public void testParseErrorGetters() {
    int line = 42;
    String message = "Unexpected token";
    ParseError error = new ParseError(line, message);

    assertEquals(line, error.getLine(), "Line number should match constructor value");
    assertEquals(message, error.getMessage(), "Message should match constructor value");
    assertTrue(error.getSourceName().isEmpty(), "Two-arg constructor has no source name");
  }

  @Test
  public void testParseErrorWithSourceName() {
    ParseError error = new ParseError(7, "Bad token", Optional.of("imported.josh"));

    assertEquals("imported.josh", error.getSourceName().orElseThrow());
  }
}
