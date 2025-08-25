/**
 * ANTLR error listener for job variation parsing.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.pipeline.job.config;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

/**
 * ANTLR error listener that converts syntax errors to IllegalArgumentException.
 *
 * <p>This error listener follows the pattern established by ConfigInterpreter
 * to provide meaningful error messages when job variation parsing fails.</p>
 */
public class JobVariationErrorListener extends BaseErrorListener {

  /**
   * Handles syntax errors by throwing IllegalArgumentException with detailed message.
   *
   * @param recognizer The ANTLR recognizer that encountered the error
   * @param offendingSymbol The offending symbol (unused)
   * @param line The line number where the error occurred
   * @param charPositionInLine The character position where the error occurred
   * @param msg The error message from ANTLR
   * @param e The recognition exception (may be null)
   * @throws IllegalArgumentException Always thrown with detailed error information
   */
  @Override
  public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
      int line, int charPositionInLine, String msg, RecognitionException e) {
    
    String errorMessage = String.format(
        "Syntax error at line %d, position %d: %s", 
        line, charPositionInLine, msg);
    
    // Add helpful guidance for common syntax errors
    String guidance = getErrorGuidance(msg);
    if (!guidance.isEmpty()) {
      errorMessage += ". " + guidance;
    }
    
    throw new IllegalArgumentException(errorMessage, e);
  }

  /**
   * Provides user-friendly guidance based on the error message.
   *
   * @param errorMessage The ANTLR error message
   * @return User-friendly guidance string
   */
  private String getErrorGuidance(String errorMessage) {
    if (errorMessage == null) {
      return "";
    }
    if (errorMessage.contains("missing '='")) {
      return "Each file specification must have exactly one equals sign (=). "
          + "Expected format: filename=path";
    }
    if (errorMessage.contains("missing ';'")) {
      return "Use semicolon (;) to separate multiple file specifications. "
          + "Example: file1=path1;file2=path2";
    }
    return "";
  }
}