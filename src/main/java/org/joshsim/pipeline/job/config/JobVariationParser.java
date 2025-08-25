/**
 * Parser for job variation specifications using ANTLR grammar.
 *
 * <p>This class replaces the basic string manipulation approach with a formal
 * ANTLR-based parser that supports semicolon separation and prepares for
 * more complex file specifications in future components.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.pipeline.job.config;

import java.util.Arrays;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.joshsim.lang.antlr.JoshJobVariationLexer;
import org.joshsim.lang.antlr.JoshJobVariationParser;
import org.joshsim.pipeline.job.JoshJobBuilder;

/**
 * Parser for job variation specifications using Josh job variation language.
 *
 * <p>This parser handles the new semicolon-separated format for data file
 * specifications and uses ANTLR to provide formal parsing instead of basic
 * string manipulation. Example format:</p>
 *
 * <pre>example.jshc=test_data/example_1.jshc;other.jshd=test_data/other_1.jshd</pre>
 *
 * <p>This implementation supports the transition from comma to semicolon
 * separation to handle URI schemes and Windows file paths properly.</p>
 */
public class JobVariationParser {

  /**
   * Parses job variation specification using ANTLR grammar.
   *
   * <p>This method maintains compatibility with the existing DataFilesStringParser
   * interface while using the new ANTLR-based parsing approach. It currently
   * processes only the first element of the iterable (Component 9 will handle
   * multiple specifications).</p>
   *
   * @param builder The JoshJobBuilder to configure
   * @param dataFiles Iterable of data file specifications (processes first element only)
   * @return The modified JoshJobBuilder instance
   * @throws IllegalArgumentException if parsing fails or format is invalid
   */
  public JoshJobBuilder parseDataFiles(JoshJobBuilder builder, Iterable<String> dataFiles) {
    if (dataFiles == null || !dataFiles.iterator().hasNext()) {
      return builder;
    }

    // Process only first element for now (Component 9 will handle multiple)
    String specification = dataFiles.iterator().next();
    return parseSpecification(builder, specification);
  }

  /**
   * Convenience method for backward compatibility with String[] interface.
   *
   * @param builder The JoshJobBuilder to configure
   * @param dataFiles Array of data file specifications
   * @return The modified JoshJobBuilder instance
   * @throws IllegalArgumentException if parsing fails or format is invalid
   */
  public JoshJobBuilder parseDataFiles(JoshJobBuilder builder, String[] dataFiles) {
    return parseDataFiles(builder, dataFiles != null ? Arrays.asList(dataFiles) : null);
  }

  /**
   * Parses a single job variation specification string using ANTLR.
   *
   * @param builder The JoshJobBuilder to configure
   * @param specification The job variation specification string
   * @return The modified JoshJobBuilder instance
   * @throws IllegalArgumentException if parsing fails or format is invalid
   */
  private JoshJobBuilder parseSpecification(JoshJobBuilder builder, String specification) {
    if (specification == null || specification.trim().isEmpty()) {
      return builder;
    }

    try {
      // Create ANTLR lexer and parser
      JoshJobVariationLexer lexer = new JoshJobVariationLexer(
          CharStreams.fromString(specification));
      CommonTokenStream tokens = new CommonTokenStream(lexer);
      JoshJobVariationParser parser = new JoshJobVariationParser(tokens);

      // Add error handling following ConfigInterpreter pattern
      parser.removeErrorListeners();
      parser.addErrorListener(new JobVariationErrorListener());

      // Parse and visit
      ParseTree tree = parser.jobVariation();
      JoshJobVariationVisitor visitor = new JoshJobVariationVisitor(builder);
      return visitor.visit(tree);

    } catch (Exception e) {
      String errorMessage = "Failed to parse job variation specification: '" + specification + "'";
      if (specification.contains(",") && !specification.contains(";")) {
        errorMessage += ". Note: Use semicolon (;) to separate file specifications, not comma (,)";
      }
      throw new IllegalArgumentException(errorMessage, e);
    }
  }
}
