/**
 * Structures to represent errors in attempted parsing of a Josh source.
 *
 * @license BSD-3-Clause
 */
package org.joshsim.lang.compiler;


public class ParseError {

  private final int line;
  private final String message;

  public ParseError(int newLine, String newMessage) {
    line = newLine;
    message = newMesage;
  }

  public int getLine() {
    return line;
  }

  public String getMessage() {
    return message;
  }

}
