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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.joshsim.command.PreprocessOptions;

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
   * @param options additional preprocessing options; pass empty for CLI defaults
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
   * @param replicateIndices an explicit, ordered list of replicate indices to run (e.g. 3,7,8),
   *     or empty to run the {@code [0, replicates)} range; mutually exclusive with a
   *     {@code replicates} greater than 1
   * @param serialPatches if true, patches are processed serially
   * @param seed optional random seed for reproducibility
   * @param dataFiles map of external data resource names (as referenced by {@code external
   *     <name>} in the script, including extension) to their resolved file paths; empty to resolve
   *     external data by filename from the working directory
   * @return run result including step count
   */
  RunSimulationResult runSimulation(
      Path script,
      String simulation,
      int replicates,
      Optional<List<Integer>> replicateIndices,
      boolean serialPatches,
      Optional<Long> seed,
      Map<String, Path> dataFiles
  );

}
