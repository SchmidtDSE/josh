/**
 * Structure representing the result of listing a Josh source's imports.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.parse;

import java.util.List;
import java.util.Optional;


/**
 * Structure representing the result of listing every {@code import} reachable from a Josh source.
 *
 * <p>Like {@link PreprocessResult} but produced by {@link JoshImportPreprocessor#listImports},
 * which walks the same recursive, order-preserving traversal used by {@code preprocess} and
 * {@code flatten}, recording each {@code import "path"} statement (including those nested inside
 * imported files) instead of splicing sources together. A resolution failure (missing import,
 * circular import, rejected protocol/absolute path) is reported through the same
 * {@link ParseError} channel, attributed to the offending statement's originating file.</p>
 */
public class ImportsResult {

  private final Optional<List<ImportRecord>> imports;
  private final List<ParseError> errors;

  /**
   * Constructs an ImportsResult with the discovered imports and no errors.
   *
   * <p>Static factory avoids the {@code List} type-erasure clash with the errors constructor.</p>
   *
   * @param imports the imports reachable from the entry file, in traversal order.
   * @return a successful ImportsResult carrying the discovered imports.
   */
  public static ImportsResult of(List<ImportRecord> imports) {
    return new ImportsResult(Optional.of(imports), List.of());
  }

  /**
   * Constructs an ImportsResult with the specified errors and no imports.
   *
   * @param errors the errors encountered which must not be empty.
   * @throws IllegalArgumentException if errors is empty.
   */
  public ImportsResult(List<ParseError> errors) {
    if (errors.isEmpty()) {
      throw new IllegalArgumentException("Passed an empty errors list without discovered imports.");
    }

    this.imports = Optional.empty();
    this.errors = errors;
  }

  private ImportsResult(Optional<List<ImportRecord>> imports, List<ParseError> errors) {
    this.imports = imports;
    this.errors = errors;
  }

  /**
   * Get the imports reachable from the entry file, in traversal order.
   *
   * @return An Optional containing the discovered imports if listing succeeded.
   */
  public Optional<List<ImportRecord>> getImports() {
    return imports;
  }

  /**
   * Get the list of errors encountered while listing imports.
   *
   * @return A list of ParseError encountered while listing imports.
   */
  public List<ParseError> getErrors() {
    return errors;
  }

  /**
   * Determine if there were errors while listing imports.
   *
   * @return true if there are errors, false otherwise.
   */
  public boolean hasErrors() {
    return !errors.isEmpty();
  }
}
