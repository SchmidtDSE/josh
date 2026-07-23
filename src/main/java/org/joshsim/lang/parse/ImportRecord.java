/**
 * Structure describing a single {@code import "path"} statement discovered in a Josh source.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.parse;


/**
 * A single {@code import "path"} statement recorded while walking a Josh model's imports.
 *
 * <p>Carries enough to identify the import from outside the engine: the literal path exactly as
 * written in the source, the path resolved relative to the entry file's directory, the identifier
 * of the file that contains the statement, and the line number within that file. Diamond imports
 * (the same file reachable via two different import chains) produce one record per occurrence,
 * matching the splicing semantics of {@link JoshImportPreprocessor} (no deduplication).</p>
 */
public class ImportRecord {

  private final String path;
  private final String resolvedPath;
  private final String sourceFile;
  private final int line;

  /**
   * Constructs a new ImportRecord.
   *
   * @param path the literal import path exactly as written in the source (quotes stripped).
   * @param resolvedPath the import path resolved relative to the entry file's directory.
   * @param sourceFile the identifier of the file that contains this import statement.
   * @param line the 1-based line number of the import statement within {@code sourceFile}.
   */
  public ImportRecord(String path, String resolvedPath, String sourceFile, int line) {
    this.path = path;
    this.resolvedPath = resolvedPath;
    this.sourceFile = sourceFile;
    this.line = line;
  }

  /**
   * Get the literal import path exactly as written in the source.
   *
   * @return the literal import path (quotes stripped).
   */
  public String getPath() {
    return path;
  }

  /**
   * Get the import path resolved relative to the entry file's directory.
   *
   * @return the resolved import path.
   */
  public String getResolvedPath() {
    return resolvedPath;
  }

  /**
   * Get the identifier of the file that contains this import statement.
   *
   * @return the source file identifier.
   */
  public String getSourceFile() {
    return sourceFile;
  }

  /**
   * Get the 1-based line number of the import statement within its source file.
   *
   * @return the line number.
   */
  public int getLine() {
    return line;
  }
}
