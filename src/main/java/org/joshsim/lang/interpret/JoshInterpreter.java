/**
 * Structures for interpreting a Josh source.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret;

import org.joshsim.lang.bridge.EngineBridgeSimulationStore;



/**
 * Strategy to interpret a Josh program into a single EngineBridgeSimulation.
 */
public interface JoshInterpreter {

  /**
   * Interpret a Josh source into a simulation.
   *
   * @param source String source code for the Josh simulation to interpret.
   * @return Parsed simulation.
   */
  EngineBridgeSimulationStore interpret(String source);

}
