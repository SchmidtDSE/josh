
/**
 * Implementation of BridgeGetter that allows setting program / simulation details after creation.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret;

import java.util.Optional;
import org.joshsim.engine.entity.base.MutableEntity;
import org.joshsim.engine.entity.prototype.EntityPrototypeStore;
import org.joshsim.engine.geometry.EngineGeometryFactory;
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
  private Optional<EngineGeometryFactory> geometryFactory;

  /**
   * Creates a new future bridge getter with no initial configuration.
   */
  public FutureBridgeGetter() {
    this.program = Optional.empty();
    this.simulationName = Optional.empty();
    this.builtBridge = Optional.empty();
    this.geometryFactory = Optional.empty();
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
   * Set the geometry factory to use when getting the bridge.
   *
   * @param newFactory The geometry factory to use when getting the bridge.
   */
  public void setGeometryFactory(EngineGeometryFactory newFactory) {
    if (geometryFactory.isPresent()) {
      throw new IllegalStateException("Bridge already built.");
    }
    this.geometryFactory = Optional.of(newFactory);
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
    MutableEntity simulation = simulations.getProtoype(simulationNameRealized).build();

    if (geometryFactory.isEmpty()) {
      throw new IllegalStateException("Geometry factory not provided to bridge.");
    }

    EngineGeometryFactory geometryFactoryRealized = geometryFactory.get();

    Converter converter = programRealized.getConverter();
    EntityPrototypeStore prototypeStore = programRealized.getPrototypes();

    EngineBridge newBridge = new MinimalEngineBridge(
        geometryFactoryRealized,
        simulation,
        converter,
        prototypeStore
    );

    builtBridge = Optional.of(newBridge);
  }

}
