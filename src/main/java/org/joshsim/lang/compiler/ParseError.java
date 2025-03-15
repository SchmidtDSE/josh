
/**
 * Structures to represent errors in attempted parsing of a Josh source.
 *
 * <p>This class encapsulates parsing errors that occur during the compilation
 * of Josh source code, storing both the line number where the error occurred
 * and the associated error message.</p>
 *
 * @license BSD-3-Clause
 */
package org.joshsim.lang.compiler;


public class ParseError {

  private final int line;
  private final String message;

  /**
   * Constructs a new ParseError with the specified line number and message.
   *
   * @param newLine the line number where the parsing error occurred
   * @param newMessage the error message describing the parsing failure
   */
  public ParseError(int newLine, String newMessage) {
    line = newLine;
    message = newMessage;
  }

  /**
   * Returns the line number where the parsing error occurred.
   *
   * @return the line number of the error
   */
  public int getLine() {
    return line;
  }

  /**
   * Returns the error message describing the parsing failure.
   *
   * @return the error message
   */
  public String getMessage() {
    return message;
  }

}
