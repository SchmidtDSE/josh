
/**
 * Implementation of BridgeGetter that allows setting program / simulation details after creation.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.joshsim.engine.entity.base.Entity;
import org.joshsim.engine.entity.base.GeoKey;
import org.joshsim.engine.entity.prototype.EntityPrototypeStore;
import org.joshsim.engine.entity.type.Patch;
import org.joshsim.engine.simulation.Replicate;
import org.joshsim.engine.value.converter.Converter;
import org.joshsim.lang.bridge.EngineBridge;
import org.joshsim.lang.bridge.EngineBridgeSimulationStore;
import org.joshsim.lang.bridge.MinimalEngineBridge;

/**
 * BridgeGetter implementation that builds and caches using future simulation details.
 *
 * <p>Stuctures which allow the interpreter to pre-compile statements into Java prior to the 
 * simulation being fully constructed. This allows Josh to stop string manipulation as soon as
 * possible for efficiency.</p>
 */
public class FutureBridgeGetter implements BridgeGetter {

  private Optional<JoshProgram> program;
  private Optional<String> simulationName;
  private Optional<EngineBridge> builtBridge;

  /**
   * Creates a new future bridge getter with no initial configuration.
   */
  public FutureBridgeGetter() {
    this.program = Optional.empty();
    this.simulationName = Optional.empty();
    this.builtBridge = Optional.empty();
  }

  /**
   * Set the program to use when getting the bridge.
   *
   * @param newProgram The program to use when getting the bridge.
   */
  public void setProgram(JoshProgram newProgram) {
    if (program.isPresent()) {
      throw new IllegalStateException("Bridge already built.");
    }
    this.program = Optional.of(newProgram);
  }

  /**
   * Set the simulation name to use when getting the bridge.
   *
   * @param newName The name of the simulation to use when getting the bridge.
   */
  public void setSimulationName(String newName) {
    if (simulationName.isPresent()) {
      throw new IllegalStateException("Bridge already built.");
    }
    this.simulationName = Optional.of(newName);
  }

  @Override
  public EngineBridge get() {
    if (builtBridge.isEmpty()) {
      buildBridge();
    }

    return builtBridge.orElseThrow();
  }

  private void buildBridge() {
    if (program.isEmpty()) {
      throw new IllegalStateException("Program not provided to bridge.");
    }

    JoshProgram programRealized = program.get();
    EngineBridgeSimulationStore simulations = programRealized.getSimulations();

    if (simulationName.isEmpty()) {
      throw new IllegalStateException("Simluation name not provided to bridge.");
    }

    String simulationNameRealized = simulationName.get();
    Entity simulation = simulations.getProtoype(simulationNameRealized).build();
    
    Converter converter = programRealized.getConverter();
    EntityPrototypeStore prototypeStore = programRealized.getPrototypes();

    EngineBridge newBridge = new MinimalEngineBridge(
        simulation,
        converter,
        prototypeStore
    );
    
    builtBridge = Optional.of(newBridge);
  }

}
