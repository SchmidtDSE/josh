/**
 * Structures representing the outcome of flattening a Josh source into one self-contained file.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.parse;

import java.util.List;
import java.util.Optional;


/**
 * Structure representing the result of flattening a Josh source's imports into a single string.
 *
 * <p>Like {@link PreprocessResult} but produced by {@link JoshImportPreprocessor#flatten}, which
 * additionally rejects the duplicate-entity condition; a violation is reported through the same
 * {@link ParseError} channel, attributed to the offending declaration's originating file.</p>
 */
public class FlattenResult {

  private final Optional<String> source;
  private final List<ParseError> errors;

  /**
   * Constructs a FlattenResult with the given flattened source and no errors.
   *
   * @param source the fully flattened, self-contained source.
   */
  public FlattenResult(String source) {
    this.source = Optional.of(source);
    this.errors = List.of();
  }

  /**
   * Constructs a FlattenResult with the specified errors and no source.
   *
   * @param errors the errors encountered which must not be empty.
   * @throws IllegalArgumentException if errors is empty.
   */
  public FlattenResult(List<ParseError> errors) {
    if (errors.isEmpty()) {
      throw new IllegalArgumentException("Passed an empty errors list without a flattened source.");
    }

    this.source = Optional.empty();
    this.errors = errors;
  }

  /**
   * Get the fully flattened source with all imports inlined.
   *
   * @return An Optional containing the flattened source if flattening succeeded.
   */
  public Optional<String> getSource() {
    return source;
  }

  /**
   * Get the list of errors encountered while flattening.
   *
   * @return A list of ParseError encountered while flattening.
   */
  public List<ParseError> getErrors() {
    return errors;
  }

  /**
   * Determine if there were errors while flattening.
   *
   * @return true if there are errors, false otherwise.
   */
  public boolean hasErrors() {
    return !errors.isEmpty();
  }

}
