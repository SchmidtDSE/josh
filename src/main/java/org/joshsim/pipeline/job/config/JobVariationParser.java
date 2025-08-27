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
import java.util.List;
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
   * Parses job variation specification using ANTLR grammar for grid search.
   *
   * <p>This method processes data file specifications to generate all possible
   * combinations for grid search. For comma-separated file lists, it creates
   * a Cartesian product of all combinations. For single-file specifications,
   * it maintains backward compatibility by returning a single-element list.</p>
   *
   * @param builder The JoshJobBuilder template to use for all combinations
   * @param dataFiles Iterable of data file specifications
   * @return List of JoshJobBuilder instances, one for each combination
   * @throws IllegalArgumentException if parsing fails or format is invalid
   */
  public List<JoshJobBuilder> parseDataFiles(JoshJobBuilder builder, Iterable<String> dataFiles) {
    if (dataFiles == null || !dataFiles.iterator().hasNext()) {
      return Arrays.asList(builder);
    }

    // Process only first element for grid search expansion
    String specification = dataFiles.iterator().next();
    return parseSpecification(builder, specification);
  }

  /**
   * Convenience method for backward compatibility with String[] interface.
   *
   * @param builder The JoshJobBuilder template to use for all combinations
   * @param dataFiles Array of data file specifications
   * @return List of JoshJobBuilder instances, one for each combination
   * @throws IllegalArgumentException if parsing fails or format is invalid
   */
  public List<JoshJobBuilder> parseDataFiles(JoshJobBuilder builder, String[] dataFiles) {
    return parseDataFiles(builder, dataFiles != null ? Arrays.asList(dataFiles) : null);
  }

  /**
   * Parses a single job variation specification string using ANTLR.
   *
   * @param builder The JoshJobBuilder template to use for all combinations
   * @param specification The job variation specification string
   * @return List of JoshJobBuilder instances, one for each combination
   * @throws IllegalArgumentException if parsing fails or format is invalid
   */
  private List<JoshJobBuilder> parseSpecification(JoshJobBuilder builder, String specification) {
    if (specification == null || specification.trim().isEmpty()) {
      return Arrays.asList(builder);
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
