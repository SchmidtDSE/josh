/**
 * ANTLR visitor for parsing Josh job variation specifications.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.pipeline.job.config;

import org.joshsim.lang.antlr.JoshJobVariationBaseVisitor;
import org.joshsim.lang.antlr.JoshJobVariationParser;
import org.joshsim.pipeline.job.JoshJobBuilder;
import org.joshsim.pipeline.job.JoshJobFileInfo;

/**
 * ANTLR visitor for parsing job variation specifications.
 *
 * <p>This visitor processes ANTLR parse trees generated from job variation
 * strings and configures JoshJobBuilder instances with the parsed file mappings.</p>
 *
 * <p>Example input: "example.jshc=test_data/example_1.jshc;other.jshd=test_data/other_1.jshd"</p>
 */
public class JoshJobVariationVisitor extends JoshJobVariationBaseVisitor<JoshJobBuilder> {

  private final JoshJobBuilder builder;

  /**
   * Creates a new visitor with the specified job builder.
   *
   * @param builder The JoshJobBuilder to configure with parsed file mappings
   */
  public JoshJobVariationVisitor(JoshJobBuilder builder) {
    if (builder == null) {
      throw new IllegalArgumentException("JoshJobBuilder cannot be null");
    }
    this.builder = builder;
  }

  /**
   * Visits the root job variation context and processes all file specifications.
   *
   * @param ctx The ANTLR job variation context
   * @return The modified JoshJobBuilder instance
   */
  @Override
  public JoshJobBuilder visitJobVariation(JoshJobVariationParser.JobVariationContext ctx) {
    // Process each file specification using visitor pattern
    for (JoshJobVariationParser.FileSpecContext spec : ctx.fileSpec()) {
      spec.accept(this);
    }
    return builder;
  }

  /**
   * Visits a file specification context and adds the mapping to the builder.
   *
   * @param ctx The ANTLR file specification context  
   * @return The modified JoshJobBuilder instance
   */
  @Override  
  public JoshJobBuilder visitFileSpec(JoshJobVariationParser.FileSpecContext ctx) {
    // Handle null contexts gracefully
    if (ctx.filename() == null || ctx.filepath() == null) {
      return builder;
    }
    
    String logicalName = ctx.filename().getText().trim();
    
    // Reconstruct full filepath from TEXT_ tokens and EQUALS_ tokens
    StringBuilder pathBuilder = new StringBuilder();
    for (int i = 0; i < ctx.filepath().getChildCount(); i++) {
      pathBuilder.append(ctx.filepath().getChild(i).getText());
    }
    String path = pathBuilder.toString().trim();
    
    // Validate that we have non-empty strings
    if (logicalName.isEmpty()) {
      throw new IllegalArgumentException("File logical name cannot be empty");
    }
    if (path.isEmpty()) {
      throw new IllegalArgumentException("File path cannot be empty for: " + logicalName);
    }
    
    // Create JoshJobFileInfo with extracted template name from path
    JoshJobFileInfo fileInfo = JoshJobFileInfo.fromPath(path);
    builder.setFileInfo(logicalName, fileInfo);
    
    return builder;
  }
}