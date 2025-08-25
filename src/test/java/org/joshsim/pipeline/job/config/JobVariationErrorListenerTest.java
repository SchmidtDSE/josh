/**
 * Unit tests for JobVariationErrorListener class.
 *
 * <p>Tests error handling and user-friendly error message generation
 * for job variation parsing syntax errors.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.pipeline.job.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.antlr.v4.runtime.RecognitionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for JobVariationErrorListener error handling functionality.
 *
 * <p>Validates error message formatting and guidance generation
 * for common syntax error scenarios.</p>
 */
public class JobVariationErrorListenerTest {

  private JobVariationErrorListener errorListener;

  /**
   * Set up test instances before each test.
   */
  @BeforeEach
  public void setUp() {
    errorListener = new JobVariationErrorListener();
  }

  @Test
  public void testSyntaxErrorBasicFormatting() {
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> errorListener.syntaxError(null, null, 1, 5, "test error", null)
    );
    
    String message = exception.getMessage();
    assertTrue(message.contains("Syntax error at line 1, position 5"));
    assertTrue(message.contains("test error"));
  }

  @Test
  public void testSyntaxErrorWithRecognitionException() {
    RecognitionException recognitionException = new RecognitionException(null, null, null);
    
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> errorListener.syntaxError(null, null, 2, 10, "parsing failed", recognitionException)
    );
    
    String message = exception.getMessage();
    assertTrue(message.contains("Syntax error at line 2, position 10"));
    assertTrue(message.contains("parsing failed"));
    assertEquals(recognitionException, exception.getCause());
  }

  @Test
  public void testMissingEqualsGuidance() {
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> errorListener.syntaxError(null, null, 1, 0, "missing '='", null)
    );
    
    String message = exception.getMessage();
    assertTrue(message.contains("Each file specification must have exactly one equals sign"));
    assertTrue(message.contains("Expected format: filename=path"));
  }

  @Test
  public void testMissingSemicolonGuidance() {
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> errorListener.syntaxError(null, null, 1, 0, "missing ';'", null)
    );
    
    String message = exception.getMessage();
    assertTrue(message.contains("Use semicolon (;) to separate"));
    assertTrue(message.contains("file1=path1;file2=path2"));
  }

  @Test
  public void testGenericErrorWithoutGuidance() {
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> errorListener.syntaxError(null, null, 3, 15, "unknown token", null)
    );
    
    String message = exception.getMessage();
    assertTrue(message.contains("Syntax error at line 3, position 15"));
    assertTrue(message.contains("unknown token"));
    // Should not contain guidance for generic errors
    assertTrue(!message.contains("Each file specification"));
    assertTrue(!message.contains("Use semicolon"));
  }

  @Test
  public void testMultipleLinePositions() {
    IllegalArgumentException exception1 = assertThrows(
        IllegalArgumentException.class,
        () -> errorListener.syntaxError(null, null, 0, 0, "error at start", null)
    );
    assertTrue(exception1.getMessage().contains("line 0, position 0"));

    IllegalArgumentException exception2 = assertThrows(
        IllegalArgumentException.class,
        () -> errorListener.syntaxError(null, null, 100, 200, "error at end", null)
    );
    assertTrue(exception2.getMessage().contains("line 100, position 200"));
  }

  @Test
  public void testNullErrorMessage() {
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> errorListener.syntaxError(null, null, 1, 5, null, null)
    );
    
    String message = exception.getMessage();
    assertTrue(message.contains("Syntax error at line 1, position 5"));
    assertTrue(message.contains("null"));
  }
}