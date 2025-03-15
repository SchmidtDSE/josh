
/**
 * Structures to represent errors in attempted parsing of a Josh source.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.parse;


/**
 * Description of an error encountered in parsing a Josh source file or string.
 *
 * <p>Structure representing a parse error that occurs during the compilation process of a Josh
 * source file which encapsulates the line number where the error was encountered and a message
 * providing details about the error.</p>
 */
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
