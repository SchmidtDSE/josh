/**
 * Immutable data class for preprocess-specific parameters.
 *
 * <p>Carries all parameters needed by {@link RemotePreprocessTarget} implementations
 * to dispatch a preprocessing job. Corresponds to the form fields of the
 * {@code /preprocessBatch} server endpoint and the CLI flags of
 * {@code preprocessBatch}.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.pipeline.target;


/**
 * Parameters for a remote preprocessing job.
 *
 * <p>Contains both required positional parameters (data file, variable, units,
 * output file) and optional preprocessing options (CRS, coordinate names,
 * timestep filtering, parallel mode, amend mode, default value).</p>
 */
public class PreprocessParams {

  private final String dataFile;
  private final String variable;
  private final String units;
  private final String outputFile;
  private final String crs;
  private final String horizCoord;
  private final String vertCoord;
  private final String timeDim;
  private final String timestep;
  private final String defaultValue;
  private final boolean parallel;
  private final boolean amend;

  /**
   * Constructs PreprocessParams with all fields.
   *
   * @param dataFile Filename of the data file within workDir.
   * @param variable Variable name or band number to extract.
   * @param units Units string for simulation use.
   * @param outputFile Filename for the output .jshd file.
   * @param crs Coordinate reference system code (e.g., {@code EPSG:4326}).
   * @param horizCoord Name of the horizontal coordinate dimension.
   * @param vertCoord Name of the vertical coordinate dimension.
   * @param timeDim Name of the time dimension.
   * @param timestep Single timestep to process, or null/empty for all.
   * @param defaultValue Default fill value, or null.
   * @param parallel Whether to enable parallel patch processing.
   * @param amend Whether to amend an existing output file.
   */
  public PreprocessParams(String dataFile, String variable, String units, String outputFile,
      String crs, String horizCoord, String vertCoord, String timeDim, String timestep,
      String defaultValue, boolean parallel, boolean amend) {
    this.dataFile = dataFile;
    this.variable = variable;
    this.units = units;
    this.outputFile = outputFile;
    this.crs = crs != null ? crs : "EPSG:4326";
    this.horizCoord = horizCoord != null ? horizCoord : "lon";
    this.vertCoord = vertCoord != null ? vertCoord : "lat";
    this.timeDim = timeDim != null ? timeDim : "calendar_year";
    this.timestep = timestep;
    this.defaultValue = defaultValue;
    this.parallel = parallel;
    this.amend = amend;
  }

  public String getDataFile() {
    return dataFile;
  }

  public String getVariable() {
    return variable;
  }

  public String getUnits() {
    return units;
  }

  public String getOutputFile() {
    return outputFile;
  }

  public String getCrs() {
    return crs;
  }

  public String getHorizCoord() {
    return horizCoord;
  }

  public String getVertCoord() {
    return vertCoord;
  }

  public String getTimeDim() {
    return timeDim;
  }

  public String getTimestep() {
    return timestep;
  }

  public String getDefaultValue() {
    return defaultValue;
  }

  public boolean isParallel() {
    return parallel;
  }

  public boolean isAmend() {
    return amend;
  }
}
