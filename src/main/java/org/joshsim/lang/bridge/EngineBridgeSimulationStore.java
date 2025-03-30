/**
 * Structures for housing simulations read from a Josh script.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.bridge;

import java.util.Map;
import org.joshsim.engine.entity.prototype.EntityPrototype;


/**
 * Structure describing a set of simulations read from a Josh source.
 */
public class EngineBridgeSimulationStore {

  private final Map<String, EngineBridgeOperation> simulationSteps;
  private final Map<String, EntityPrototype> simulationProtoypes;

  /**
   * Create a new immutable record of available simulations.
   *
   * @param simulationSteps EngineBridgeOperations by simulation name where each operation
   *     execute a single time step when given an EngineBridge on which to operate.
   */
  public EngineBridgeSimulationStore(Map<String, EngineBridgeOperation> simulationSteps,
      Map<String, EntityPrototype> simulationProtoypes) {
    this.simulationSteps = simulationSteps;
    this.simulationProtoypes = simulationProtoypes;
  }

  /**
   * Retrieves the step function for the specified simulation name.
   *
   * @param name The name of the simulation for which to retrieve a simulation step function.
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

  /**
   * Retrieves the prototype for making the simulation entity.
   *
   * @param name The name of the simulation for which to retrieve a prototype.
   * @return The Prototype corresponding to the given name which, when executed, runs
   *     a single time step.
   * @throws UnsupportedOperationException Thrown if logic for the simulation specified by name is
   *     not found.
   */
  public EntityPrototype getProtoype(String name) {
    if (!simulationProtoypes.containsKey(name)) {
      throw new UnsupportedOperationException("Unknown simulation: " + name);
    }

    return simulationProtoypes.get(name);
  }

}
