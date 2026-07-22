
/**
 * Structures to represent errors in attempted parsing of a Josh source.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.parse;

import java.util.Optional;


/**
 * Description of an error encountered in parsing a Josh source file or string.
 *
 * <p>Structure representing a parse error that occurs during the compilation process of a Josh
 * source file which encapsulates the line number where the error was encountered and a message
 * providing details about the error. When the error originates from a file brought in via
 * {@code import} (see {@code JoshImportPreprocessor}), {@link #getSourceName()} identifies which
 * file the line number refers to; it is empty for the top-level entry source, matching prior
 * behavior.</p>
 */
public class ParseError {

  private final int line;
  private final String message;
  private final Optional<String> sourceName;

  /**
   * Constructs a new ParseError with the specified line number and message.
   *
   * @param line the line number where the parsing error occurred
   * @param message the error message describing the parsing failure
   */
  public ParseError(int line, String message) {
    this(line, message, Optional.empty());
  }

  /**
   * Constructs a new ParseError attributed to a specific imported source file.
   *
   * @param line the line number where the parsing error occurred, relative to sourceName
   * @param message the error message describing the parsing failure
   * @param sourceName the imported file the error occurred in, or empty for the entry source
   */
  public ParseError(int line, String message, Optional<String> sourceName) {
    this.line = line;
    this.message = message;
    this.sourceName = sourceName;
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

  /**
   * Returns the imported file this error is attributed to, if any.
   *
   * @return the imported source file name, or empty if the error is against the entry source
   */
  public Optional<String> getSourceName() {
    return sourceName;
  }

}
