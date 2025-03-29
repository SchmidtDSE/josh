
/**
 * Structures for representing a parsed Josh program.
 *
 * <p>Contains the core components needed to execute a Josh program, including the converter
 * for unit conversions and the simulation store for managing simulations.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret;

import org.joshsim.engine.value.Converter;
import org.joshsim.lang.bridge.EngineBridgeSimulationStore;


/**
 * Represents a parsed and executable Josh program.
 * 
 * <p>This class holds the essential components required for executing a Josh program,
 * including facilities for value conversion and simulation management.</p>
 */
public class JoshProgram {

  private final Converter converter;
  private final EngineBridgeSimulationStore simulations;

  /**
   * Creates a new Josh program instance.
   *
   * @param converter The converter used for unit conversions within the program
   * @param simulations The store containing all simulations for this program
   */
  public JoshProgram(Converter converter, EngineBridgeSimulationStore simulations) {
    this.converter = converter;
    this.simulations = simulations;
  }

  /**
   * Gets the converter used for unit conversions.
   *
   * @return The converter instance used by this program
   */
  public Converter getConverter() {
    return converter;
  }

  /**
   * Gets the simulation store containing all program simulations.
   *
   * @return The simulation store for this program
   */
  public EngineBridgeSimulationStore getSimulations() {
    return simulations;
  }

}
