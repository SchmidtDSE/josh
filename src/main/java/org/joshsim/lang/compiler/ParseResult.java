/**
 * Structures to represent the outcome of attempted parsing of a Josh source.
 *
 * @license BSD-3-Clause
 */
package org.joshsim.lang.compiler;

import java.util.List;
import java.util.Optional;
import org.joshsim.lang.JoshLangParser;

/**
 * Structure representing the result of parsing a Josh source code file.
 */
public class ParseResult {

  private final Optional<JoshLangParser.Program> program;
  private final List<ParseError> errors;

  /**
   * Constructs a ParseResult with the specified program and no errors.
   *
   * @param program the parsed program.
   */
  public ParseResult(JoshLangParser.Program newProgram) {
    program = newProgram;
    errors = new ArrayList<>();
  }

  /**
   * Constructs a ParseResult with the specified errors and no program.
   *
   * @throws InvalidArgumentException if newErrors is empty.
   * @param newErrors the errors encountered which must not be empty.
   */
  public ParseResult(List<ParseError> newErrors) {
    if (newErrors.isEmpty()) {
      throw new InvalidArgumentException("Passed an empty errors list without parsed program.");
    }

    program = Optional.empty();
    errors = newErrors;
  }

  /**
   * Get the parsed program.
   *
   * @return An Optional containing the parsed program if available.
   */
  public Optional<JoshLangParser.Program> getProgram() {
    return program;
  }

  /**
   * Get the list of parsing errors.
   *
   * @return A list of ParseError encountered during parsing.
   */
  public List<ParseError> getErrors() {
    return errors;
  }

  /**
   * Determine if there were errors during parsing.
   *
   * @return true if there are errors, false otherwise.
   */
  public boolean hasErrors() {
    return !errors.isEmpty();
  }

}
