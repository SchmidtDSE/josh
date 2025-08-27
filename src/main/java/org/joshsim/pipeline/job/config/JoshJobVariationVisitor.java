/**
 * ANTLR visitor for parsing Josh job variation specifications.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.pipeline.job.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.joshsim.lang.antlr.JoshJobVariationBaseVisitor;
import org.joshsim.lang.antlr.JoshJobVariationParser;
import org.joshsim.pipeline.job.JoshJob;
import org.joshsim.pipeline.job.JoshJobBuilder;
import org.joshsim.pipeline.job.JoshJobFileInfo;

/**
 * ANTLR visitor for parsing job variation specifications with grid search support.
 *
 * <p>This visitor processes ANTLR parse trees generated from job variation
 * strings and creates all possible combinations for grid search. For comma-separated
 * file lists, it generates a Cartesian product of all combinations.</p>
 *
 * <p>Example input: "example.jshc=file1.jshc,file2.jshc;other.jshd=fileA.jshd,fileB.jshd"
 * creates 4 combinations: (file1,fileA), (file1,fileB), (file2,fileA), (file2,fileB)</p>
 */
public class JoshJobVariationVisitor extends JoshJobVariationBaseVisitor<List<JoshJobBuilder>> {

  private final JoshJobBuilder templateBuilder;
  private final Map<String, List<String>> fileSpecifications;

  /**
   * Creates a new visitor with the specified job builder template.
   *
   * @param builder The JoshJobBuilder template to use for creating combinations
   */
  public JoshJobVariationVisitor(JoshJobBuilder builder) {
    if (builder == null) {
      throw new IllegalArgumentException("JoshJobBuilder cannot be null");
    }
    this.templateBuilder = builder;
    this.fileSpecifications = new HashMap<>();
  }

  /**
   * Visits the root job variation context and generates all combinations.
   *
   * @param ctx The ANTLR job variation context
   * @return List of JoshJobBuilder instances for all combinations
   */
  @Override
  public List<JoshJobBuilder> visitJobVariation(JoshJobVariationParser.JobVariationContext ctx) {
    // First pass: collect all file specifications
    for (JoshJobVariationParser.FileSpecContext spec : ctx.fileSpec()) {
      visitFileSpec(spec);
    }

    // Second pass: generate all combinations using Cartesian product
    return generateCombinations();
  }

  /**
   * Visits a file specification context and stores all file paths for combination generation.
   *
   * @param ctx The ANTLR file specification context
   * @return null (not used in this context)
   */
  @Override
  public List<JoshJobBuilder> visitFileSpec(JoshJobVariationParser.FileSpecContext ctx) {
    // Handle null contexts gracefully
    if (ctx.filename() == null || ctx.filepath() == null) {
      return null;
    }

    String logicalName = ctx.filename().getText().trim();

    // Validate logical name
    if (logicalName.isEmpty()) {
      throw new IllegalArgumentException("File logical name cannot be empty");
    }

    // Get the pathList context from filepath
    JoshJobVariationParser.PathListContext pathList = ctx.filepath().pathList();

    // Validate path list is not empty
    if (pathList.singlePath().isEmpty()) {
      throw new IllegalArgumentException(
          "File path list cannot be empty for: " + logicalName + ". "
          + "Format should be: name=path1,path2 or name=path1");
    }

    // Collect all paths for this logical name (for grid search)
    List<String> paths = new ArrayList<>();
    for (JoshJobVariationParser.SinglePathContext singlePath : pathList.singlePath()) {
      String path = reconstructPath(singlePath);

      // Validate path is not empty
      if (path.isEmpty()) {
        throw new IllegalArgumentException("File path cannot be empty for: " + logicalName);
      }

      paths.add(path);
    }

    // Store the paths for this logical name
    fileSpecifications.put(logicalName, paths);

    return null; // Not used in combination generation
  }

  /**
   * Reconstructs a file path from a single path context by concatenating all child tokens.
   *
   * @param pathContext The ANTLR single path context
   * @return The reconstructed path string
   */
  private String reconstructPath(JoshJobVariationParser.SinglePathContext pathContext) {
    StringBuilder pathBuilder = new StringBuilder();
    for (int i = 0; i < pathContext.getChildCount(); i++) {
      pathBuilder.append(pathContext.getChild(i).getText());
    }
    return pathBuilder.toString().trim();
  }

  /**
   * Generates all possible combinations using Cartesian product algorithm.
   *
   * <p>This method creates all possible combinations of file mappings from the parsed
   * specifications. For example, if we have:
   * - example.jshc: [file1, file2]
   * - other.jshd: [fileA, fileB]
   * 
   * It will generate 4 combinations:
   * - (example.jshc=file1, other.jshd=fileA)
   * - (example.jshc=file1, other.jshd=fileB)
   * - (example.jshc=file2, other.jshd=fileA)
   * - (example.jshc=file2, other.jshd=fileB)
   * </p>
   *
   * @return List of JoshJobBuilder instances, one for each combination
   */
  private List<JoshJobBuilder> generateCombinations() {
    // Handle empty specifications
    if (fileSpecifications.isEmpty()) {
      return new ArrayList<>(List.of(templateBuilder));
    }

    // Validate combination count to prevent memory exhaustion
    long totalCombinations = 1;
    for (List<String> paths : fileSpecifications.values()) {
      if (paths.isEmpty()) {
        throw new IllegalArgumentException("File specification cannot have empty path list");
      }
      totalCombinations *= paths.size();
      if (totalCombinations > 1000) { // Safety limit
        throw new IllegalArgumentException(
            "Too many combinations (" + totalCombinations + "). "
            + "Limit is 1000 combinations to prevent memory exhaustion.");
      }
    }

    // Convert map to ordered lists for combination generation
    List<String> logicalNames = new ArrayList<>(fileSpecifications.keySet());
    List<List<String>> pathLists = new ArrayList<>();
    for (String logicalName : logicalNames) {
      pathLists.add(fileSpecifications.get(logicalName));
    }

    // Generate all combinations using recursive Cartesian product
    List<JoshJobBuilder> combinations = new ArrayList<>();
    generateCombinationsRecursive(logicalNames, pathLists, 0, new ArrayList<>(), combinations);

    return combinations;
  }

  /**
   * Recursive helper method for generating Cartesian product combinations.
   *
   * @param logicalNames List of logical file names
   * @param pathLists List of path lists corresponding to logical names
   * @param currentIndex Current index in the combination generation
   * @param currentCombination Current combination being built
   * @param result List to store all generated combinations
   */
  private void generateCombinationsRecursive(
      List<String> logicalNames,
      List<List<String>> pathLists,
      int currentIndex,
      List<String> currentCombination,
      List<JoshJobBuilder> result) {

    // Base case: we've selected a path for all logical names
    if (currentIndex == logicalNames.size()) {
      // Create a new JoshJobBuilder with current combination
      // Note: We need to create a fresh builder and copy all properties from templateBuilder
      // Since JoshJobBuilder doesn't expose getter methods, we'll build a template job to 
      // access its data
      JoshJob templateJob = templateBuilder.build();
      JoshJobBuilder combinationBuilder = new JoshJobBuilder()
          .setReplicates(templateJob.getReplicates())
          .setCustomParameters(templateJob.getCustomParameters());

      // Copy all existing file info from template job
      for (Map.Entry<String, String> entry : templateJob.getFilePaths().entrySet()) {
        // Skip if this logical name will be overridden by current combination
        if (!logicalNames.contains(entry.getKey())) {
          JoshJobFileInfo fileInfo = JoshJobFileInfo.fromPath(entry.getValue());
          combinationBuilder.setFileInfo(entry.getKey(), fileInfo);
        }
      }

      // Set file mappings for this combination
      for (int i = 0; i < logicalNames.size(); i++) {
        String logicalName = logicalNames.get(i);
        String path = currentCombination.get(i);
        JoshJobFileInfo fileInfo = JoshJobFileInfo.fromPath(path);
        combinationBuilder.setFileInfo(logicalName, fileInfo);
      }

      result.add(combinationBuilder);
      return;
    }

    // Recursive case: try each path for the current logical name
    List<String> currentPathList = pathLists.get(currentIndex);
    for (String path : currentPathList) {
      // Add this path to current combination
      List<String> nextCombination = new ArrayList<>(currentCombination);
      nextCombination.add(path);

      // Recursively generate combinations for remaining logical names
      generateCombinationsRecursive(logicalNames, pathLists, currentIndex + 1,
          nextCombination, result);
    }
  }
}
