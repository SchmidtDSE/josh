/**
 * Abstraction over Josh compute backends for MCP tool handlers.
 *
 * <p>Defines the four operations exposed by the Phase 1 MCP tool surface. All file arguments are
 * typed as {@link java.nio.file.Path} so that the interface is oblivious to whether a path was
 * supplied directly by a local-mode client or materialised from an MCP resource in a future
 * hosted-mode deployment.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.mcp;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Backend interface for the four Josh MCP tools.
 *
 * <p>Each method returns a small result record. Tool handlers convert these to
 * {@link io.modelcontextprotocol.spec.McpSchema.CallToolResult} responses; the backend itself
 * never touches MCP types so it can be tested independently.</p>
 */
public interface Backend {

  /**
   * Result of a validate operation.
   */
  class ValidateResult {
    private final boolean success;
    private final String message;

    /**
     * Constructs a ValidateResult.
     *
     * @param success true if the script is valid
     * @param message human-readable summary or error description
     */
    public ValidateResult(boolean success, String message) {
      this.success = success;
      this.message = message;
    }

    public boolean isSuccess() {
      return success;
    }

    public String getMessage() {
      return message;
    }
  }

  /**
   * Result of a discoverConfig operation.
   */
  class DiscoverConfigResult {
    private final boolean success;
    private final String output;

    /**
     * Constructs a DiscoverConfigResult.
     *
     * @param success true if discovery succeeded
     * @param output formatted config variable listing or error message
     */
    public DiscoverConfigResult(boolean success, String output) {
      this.success = success;
      this.output = output;
    }

    public boolean isSuccess() {
      return success;
    }

    public String getOutput() {
      return output;
    }
  }

  /**
   * Result of a preprocess operation.
   */
  class PreprocessResult {
    private final boolean success;
    private final String message;

    /**
     * Constructs a PreprocessResult.
     *
     * @param success true if preprocessing succeeded
     * @param message human-readable summary or error description
     */
    public PreprocessResult(boolean success, String message) {
      this.success = success;
      this.message = message;
    }

    public boolean isSuccess() {
      return success;
    }

    public String getMessage() {
      return message;
    }
  }

  /**
   * Result of a runSimulation operation.
   */
  class RunSimulationResult {
    private final boolean success;
    private final String message;
    private final long stepsCompleted;

    /**
     * Constructs a RunSimulationResult.
     *
     * @param success true if the simulation completed successfully
     * @param message human-readable summary or error description
     * @param stepsCompleted number of simulation steps that completed
     */
    public RunSimulationResult(boolean success, String message, long stepsCompleted) {
      this.success = success;
      this.message = message;
      this.stepsCompleted = stepsCompleted;
    }

    public boolean isSuccess() {
      return success;
    }

    public String getMessage() {
      return message;
    }

    public long getStepsCompleted() {
      return stepsCompleted;
    }
  }

  /**
   * Validates a Josh script file for syntax errors.
   *
   * @param script path to the {@code .josh} script file
   * @return validation result
   */
  ValidateResult validate(Path script);

  /**
   * Discovers configuration variables used in a Josh script.
   *
   * @param script path to the {@code .josh} script file
   * @return discovery result containing formatted variable listing
   */
  DiscoverConfigResult discoverConfig(Path script);

  /**
   * Preprocesses an external data file into {@code .jshd} format.
   *
   * @param script path to the {@code .josh} script file
   * @param simulation name of the simulation whose grid definition should be used
   * @param dataFile path to the input data file (NetCDF, GeoTIFF, or jshd)
   * @param variable variable name or band number to extract
   * @param unitsStr units of the data for use within simulations
   * @param outputFile path where the preprocessed {@code .jshd} file should be written
   * @param options additional preprocessing options; pass {@code null} for defaults
   * @return preprocess result
   */
  PreprocessResult preprocess(
      Path script,
      String simulation,
      Path dataFile,
      String variable,
      String unitsStr,
      Path outputFile,
      Optional<PreprocessOptions> options
  );

  /**
   * Runs a Josh simulation.
   *
   * @param script path to the {@code .josh} script file
   * @param simulation name of the simulation to run
   * @param replicates number of replicates to run (default 1)
   * @param serialPatches if true, patches are processed serially
   * @param seed optional random seed for reproducibility
   * @return run result including step count
   */
  RunSimulationResult runSimulation(
      Path script,
      String simulation,
      int replicates,
      boolean serialPatches,
      Optional<Long> seed
  );

  /**
   * Optional preprocessing parameters matching the CLI flags.
   */
  class PreprocessOptions {
    private final String crsCode;
    private final String horizCoordName;
    private final String vertCoordName;
    private final String timeName;
    private final String timestep;
    private final String defaultValue;
    private final boolean parallel;
    private final boolean amend;

    /**
     * Constructs PreprocessOptions.
     *
     * @param crsCode coordinate reference system code, e.g. {@code EPSG:4326}
     * @param horizCoordName name of horizontal coordinate dimension
     * @param vertCoordName name of vertical coordinate dimension
     * @param timeName name of time dimension
     * @param timestep single timestep to process, or empty string for all
     * @param defaultValue default fill value, or {@code null}
     * @param parallel whether to enable parallel patch processing
     * @param amend whether to amend an existing output file
     */
    public PreprocessOptions(
        String crsCode,
        String horizCoordName,
        String vertCoordName,
        String timeName,
        String timestep,
        String defaultValue,
        boolean parallel,
        boolean amend
    ) {
      this.crsCode = crsCode;
      this.horizCoordName = horizCoordName;
      this.vertCoordName = vertCoordName;
      this.timeName = timeName;
      this.timestep = timestep;
      this.defaultValue = defaultValue;
      this.parallel = parallel;
      this.amend = amend;
    }

    public String getCrsCode() {
      return crsCode;
    }

    public String getHorizCoordName() {
      return horizCoordName;
    }

    public String getVertCoordName() {
      return vertCoordName;
    }

    public String getTimeName() {
      return timeName;
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

}
