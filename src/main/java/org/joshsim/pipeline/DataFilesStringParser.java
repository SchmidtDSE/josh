/**
 * Parser for data files string option into job builder configuration.
 *
 * <p>This class consolidates the duplicate parseDataFiles logic that was present
 * in both RunCommand and RunRemoteCommand. It provides a single point for parsing
 * the --data command line option format into JoshJobBuilder instances.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.pipeline;

import org.joshsim.pipeline.job.JoshJobBuilder;
import org.joshsim.pipeline.job.JoshJobFileInfo;

/**
 * Utility class for parsing data files string specifications.
 *
 * <p>This class handles parsing of the --data command line option which follows
 * the format "filename=path,filename=path". It consolidates logic that was
 * previously duplicated across multiple command classes.</p>
 */
public class DataFilesStringParser {

  /**
   * Parses data file specifications and configures the provided job builder.
   *
   * <p>Parses the data files array where each entry follows the format "filename=path".
   * The parsed mappings are added to the provided JoshJobBuilder instance using
   * JoshJobFileInfo objects that extract template names from file paths.</p>
   *
   * @param builder The JoshJobBuilder to configure with file mappings
   * @param dataFiles Array of data file specifications in format "filename=path"
   * @return The modified JoshJobBuilder instance for method chaining
   * @throws IllegalArgumentException if any data file specification is invalid
   */
  public JoshJobBuilder parseDataFiles(JoshJobBuilder builder, String[] dataFiles) {
    if (dataFiles == null) {
      return builder;
    }
    
    for (String dataFile : dataFiles) {
      String[] parts = dataFile.split("=", 2);
      if (parts.length != 2) {
        throw new IllegalArgumentException("Invalid data file format: " + dataFile
            + ". Expected format: filename=path");
      }
      
      String logicalName = parts[0].trim();
      String path = parts[1].trim();
      
      // Create JoshJobFileInfo with extracted filename stem for template name
      JoshJobFileInfo fileInfo = JoshJobFileInfo.fromPath(path);
      builder.setFileInfo(logicalName, fileInfo);
    }
    
    return builder;
  }
}