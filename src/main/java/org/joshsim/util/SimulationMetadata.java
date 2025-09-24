/**
 * Container class for simulation metadata.
 *
 * <p>This class encapsulates key simulation parameters extracted from Josh scripts,
 * providing easy access to step range information needed for progress calculations.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.util;

/**
 * Container class for simulation metadata.
 *
 * <p>This class encapsulates key simulation parameters extracted from Josh scripts,
 * providing easy access to step range information needed for progress calculations.</p>
 */
public class SimulationMetadata {
  private final long stepsLow;
  private final long stepsHigh;
  private final long totalSteps;

  /**
   * Constructor for SimulationMetadata.
   *
   * @param stepsLow The lower bound of simulation steps (inclusive)
   * @param stepsHigh The upper bound of simulation steps (inclusive)
   * @param totalSteps The total number of steps in the simulation
   */
  public SimulationMetadata(long stepsLow, long stepsHigh, long totalSteps) {
    this.stepsLow = stepsLow;
    this.stepsHigh = stepsHigh;
    this.totalSteps = totalSteps;
  }

  /**
   * Gets the lower bound of simulation steps.
   *
   * @return The steps.low value from the simulation
   */
  public long getStepsLow() {
    return stepsLow;
  }

  /**
   * Gets the upper bound of simulation steps.
   *
   * @return The steps.high value from the simulation
   */
  public long getStepsHigh() {
    return stepsHigh;
  }

  /**
   * Gets the total number of steps in the simulation.
   *
   * @return The total steps (stepsHigh - stepsLow + 1)
   */
  public long getTotalSteps() {
    return totalSteps;
  }

  @Override
  public String toString() {
    return String.format(
        "SimulationMetadata{stepsLow=%d, stepsHigh=%d, totalSteps=%d}",
        stepsLow,
        stepsHigh,
        totalSteps);
  }
}
