
/**
 * Implementation of BridgeGetter that allows setting program and simulation details after creation.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret;

import org.joshsim.lang.bridge.EngineBridge;

/**
 * BridgeGetter implementation that supports future configuration.
 *
 * <p>This implementation allows setting the program and simulation name after the getter is
 * created, enabling delayed configuration of the bridge details.</p>
 */
public class FutureBridgeGetter implements BridgeGetter {

  private JoshProgram program;
  private String simulationName;

  /**
   * Creates a new future bridge getter with no initial configuration.
   */
  public FutureBridgeGetter() {
    this.program = null;
    this.simulationName = null;
  }

  /**
   * Set the program to use when getting the bridge.
   *
   * @param program The program to use when getting the bridge.
   */
  public void setProgram(JoshProgram program) {
    this.program = program;
  }

  /**
   * Set the simulation name to use when getting the bridge.
   *
   * @param simulationName The name of the simulation to use when getting the bridge.
   */
  public void setSimulationName(String simulationName) {
    this.simulationName = simulationName;
  }

  @Override
  public EngineBridge get() {
    if (program == null || simulationName == null) {
      throw new RuntimeException("Cannot get bridge before program and simulation name are set.");
    }
    return program.getSimulations().get(simulationName);
  }
}
