/**
 * Command which converts external data to a jshd format.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.command;

import java.io.File;
import java.util.concurrent.Callable;
import org.joshsim.util.OutputOptions;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;


/**
 * Command line interface handler for preprocessing simulation data.
 *
 * <p>This class implements the 'preprocess' command which prepares input data for use in Josh
 * simulations. It handles the conversion of raw data files into a binary format (.jshd) that can be
 * efficiently loaded during simulation execution. The preprocessing includes:
 *
 * <ul>
 *   <li>Reading and parsing Josh script files</li>
 *   <li>Extracting simulation metadata and grid information</li>
 *   <li>Converting external geospatial data to the simulation's coordinate system</li>
 *   <li>Serializing the processed data to a binary file format</li>
 * </ul>
 *
 * </p>
 *
 * @see PreprocessUtil
 * @see org.joshsim.geo.external.ExternalGeoMapper
 */
@Command(
    name = "preprocess",
    description = "Preprocess data for a simulation"
)
public class PreprocessCommand implements Callable<Integer> {
  private static final int UNKNOWN_ERROR_CODE = 404;

  @Parameters(index = "0", description = "Path to Josh script file")
  private File scriptFile;

  @Parameters(index = "1", description = "Name of simulation to preprocess")
  private String simulation;

  @Parameters(index = "2", description = "Path to data file to preprocess")
  private String dataFile;

  @Parameters(index = "3", description = "Name of the variable to be read or band number")
  private String variable;

  @Parameters(index = "4", description = "Units of the data to use within simulations")
  private String unitsStr;

  @Parameters(index = "5", description = "Path where preprocessed jshd file should be written")
  private File outputFile;

  @Option(
      names = "--amend",
      description = "Amend existing file rather than overwriting",
      defaultValue = "false"
  )
  private boolean amend;

  @Option(
      names = "--crs",
      description = "CRS to use in reading the file.",
      defaultValue = "EPSG:4326"
  )
  private String crsCode;

  @Option(
      names = "--x-coord",
      description = "Name of X coordinate.",
      defaultValue = "lon"
  )
  private String horizCoordName;

  @Option(
      names = "--y-coord",
      description = "Name of Y coordinate.",
      defaultValue = "lat"
  )
  private String vertCoordName;

  @Option(
      names = "--time-dim",
      description = "Time dimension.",
      defaultValue = "calendar_year"
  )
  private String timeName;

  @Option(
      names = "--timestep",
      description = "The single timestep to process.",
      defaultValue = ""
  )
  private String timestep;

  @Option(
      names = "--default-value",
      description = "Default value to fill grid spaces before copying data from source file"
  )
  private String defaultValue;

  @Option(
      names = "--parallel",
      description = "Enable parallel processing of patches within each timestep",
      defaultValue = "false"
  )
  private boolean parallel;

  @Mixin
  private OutputOptions output = new OutputOptions();

  @Override
  public Integer call() {
    try {
      PreprocessUtil.PreprocessOptions options = new PreprocessUtil.PreprocessOptions(
          crsCode, horizCoordName, vertCoordName, timeName,
          timestep, defaultValue, parallel, amend
      );
      PreprocessUtil.preprocess(
          scriptFile, simulation, dataFile, variable, unitsStr, outputFile, options, output
      );
      return 0;
    } catch (IllegalArgumentException e) {
      output.printError(e.getMessage());
      return 1;
    } catch (Exception e) {
      output.printError("Preprocessing failed: " + e.getMessage());
      return UNKNOWN_ERROR_CODE;
    }
  }
}
