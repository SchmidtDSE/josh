/**
 * Tests for the ParseResult structure.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.parse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.joshsim.lang.antlr.JoshLangParser;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;


/**
 * Tests for the ParseResult structure.
 */
public class ParseResultTest {

  @Test
  public void testProgramConstructor() {
    JoshLangParser.ProgramContext mockProgram = Mockito.mock(JoshLangParser.ProgramContext.class);
    ParseResult result = new ParseResult(mockProgram);
    
    assertTrue(result.getProgram().isPresent(), "Program should be present");
    assertEquals(mockProgram, result.getProgram().get(), "Program should match constructor value");
    assertFalse(result.hasErrors(), "Should not have errors");
    assertTrue(result.getErrors().isEmpty(), "Error list should be empty");
  }

  @Test
  public void testErrorConstructor() {
    List<ParseError> errors = Arrays.asList(
        new ParseError(1, "Error 1"),
        new ParseError(2, "Error 2")
    );
    ParseResult result = new ParseResult(errors);
    
    assertFalse(result.getProgram().isPresent(), "Program should not be present");
    assertTrue(result.hasErrors(), "Should have errors");
    assertEquals(errors.size(), result.getErrors().size(), "Error list size should match");
  }
}
