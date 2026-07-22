
/**
 * Structures to represent the outcome of resolving {@code import} statements in a Josh source.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.parse;

import java.util.List;
import java.util.Optional;


/**
 * Structure representing the result of splicing a Josh source's imports into a single string.
 */
public class PreprocessResult {

  private final Optional<String> source;
  private final List<ParseError> errors;

  /**
   * Constructs a PreprocessResult with the given combined source and no errors.
   *
   * @param source the fully spliced source, ready to be parsed for real.
   */
  public PreprocessResult(String source) {
    this.source = Optional.of(source);
    this.errors = List.of();
  }

  /**
   * Constructs a PreprocessResult with the specified errors and no source.
   *
   * @param errors the errors encountered which must not be empty.
   * @throws IllegalArgumentException if errors is empty.
   */
  public PreprocessResult(List<ParseError> errors) {
    if (errors.isEmpty()) {
      throw new IllegalArgumentException("Passed an empty errors list without a combined source.");
    }

    this.source = Optional.empty();
    this.errors = errors;
  }

  /**
   * Get the fully spliced source with all imports resolved.
   *
   * @return An Optional containing the combined source if preprocessing succeeded.
   */
  public Optional<String> getSource() {
    return source;
  }

  /**
   * Get the list of errors encountered while resolving imports.
   *
   * @return A list of ParseError encountered while preprocessing.
   */
  public List<ParseError> getErrors() {
    return errors;
  }

  /**
   * Determine if there were errors while resolving imports.
   *
   * @return true if there are errors, false otherwise.
   */
  public boolean hasErrors() {
    return !errors.isEmpty();
  }

}
