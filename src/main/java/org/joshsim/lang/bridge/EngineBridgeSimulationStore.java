/**
 * Structures for housing simulations read from a Josh script.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.bridge;

import java.util.Map;


/**
 * Structure describing a set of simulations read from a Josh source.
 */
public class EngineBridgeSimulationStore {

  private final Map<String, EngineBridgeOperation> simulationSteps;

  /**
   * Create a new immutable record of available simulations.
   *
   * @param simulationSteps EngineBridgeOperations by simulation name where each operation
   *     execute a single time step when given an EngineBridge on which to operate.
   */
  public EngineBridgeSimulationStore(Map<String, EngineBridgeOperation> simulationSteps) {
    this.simulationSteps = simulationSteps;
  }

  /**
   * Retrieves the step function for the specified simulation name.
   *
   * @param name the name of the simulation step to retrieve.
   * @return the EngineBridgeOperation corresponding to the given name which, when executed, runs
   *     a single time step.
   * @throws UnsupportedOperationException if logic for the simulation specified by name is not
   *     found.
   */
  public EngineBridgeOperation getStepFunction(String name) {
    if (!simulationSteps.containsKey(name)) {
      throw new UnsupportedOperationException("Unknown simulation: " + name);
    }

    return simulationSteps.get(name);
  }

}
