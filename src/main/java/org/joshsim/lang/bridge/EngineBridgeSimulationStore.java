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

  private final Map<String, EntityPrototype> simulationProtoypes;

  /**
   * Create a new immutable record of available simulations.
   *
   * @param simulationProtoypes Prototypes to build the simulations.
   */
  public EngineBridgeSimulationStore(Map<String, EntityPrototype> simulationProtoypes) {
    this.simulationProtoypes = simulationProtoypes;
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

  /**
   * Get simulations for which prototypes are available.
   *
   * @return List of prototype names.
   */
  public Iterable<String> getSimulations() {
    return simulationProtoypes.keySet();
  }

}
