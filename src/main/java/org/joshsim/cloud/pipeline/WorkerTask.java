/**
 * Task configuration for worker execution.
 *
 * <p>This class encapsulates all the parameters needed to execute a single
 * replicate task on a worker instance.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.cloud.pipeline;

/**
 * Task configuration for worker execution.
 *
 * <p>This class encapsulates all the parameters needed to execute a single
 * replicate task on a worker instance.</p>
 */
public class WorkerTask {
  private final String code;
  private final String simulationName;
  private final String apiKey;
  private final String externalData;
  private final boolean favorBigDecimal;
  private final int replicateNumber;

  /**
   * Creates a new WorkerTask.
   *
   * @param code The Josh simulation code
   * @param simulationName The name of the simulation
   * @param apiKey The API key for authentication
   * @param externalData External data for the simulation
   * @param favorBigDecimal Whether to favor BigDecimal precision
   * @param replicateNumber The replicate number for this task
   */
  public WorkerTask(String code, String simulationName, String apiKey,
                   String externalData, boolean favorBigDecimal, int replicateNumber) {
    this.code = code;
    this.simulationName = simulationName;
    this.apiKey = apiKey;
    this.externalData = externalData;
    this.favorBigDecimal = favorBigDecimal;
    this.replicateNumber = replicateNumber;
  }

  /**
   * Gets the Josh simulation code for this task.
   *
   * @return The simulation code as a string
   */
  public String getCode() {
    return code;
  }

  /**
   * Gets the simulation name for this task.
   *
   * @return The name of the simulation
   */
  public String getSimulationName() {
    return simulationName;
  }

  /**
   * Gets the API key for authentication.
   *
   * @return The API key as a string
   */
  public String getApiKey() {
    return apiKey;
  }

  /**
   * Gets the external data for this task.
   *
   * @return The external data as a string
   */
  public String getExternalData() {
    return externalData;
  }

  /**
   * Determines whether to favor BigDecimal precision for calculations.
   *
   * @return true if BigDecimal precision should be favored, false otherwise
   */
  public boolean isFavorBigDecimal() {
    return favorBigDecimal;
  }

  /**
   * Gets the replicate number for this task.
   *
   * @return The replicate number as an integer
   */
  public int getReplicateNumber() {
    return replicateNumber;
  }
}
