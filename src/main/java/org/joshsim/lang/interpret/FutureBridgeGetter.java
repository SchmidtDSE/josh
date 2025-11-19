
/**
 * Implementation of BridgeGetter that allows setting program / simulation details after creation.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret;

import java.util.Optional;
import java.util.Random;
import org.joshsim.engine.config.JshcConfigGetter;
import org.joshsim.engine.entity.base.MutableEntity;
import org.joshsim.engine.entity.prototype.EntityPrototypeStore;
import org.joshsim.engine.geometry.EngineGeometryFactory;
import org.joshsim.engine.value.converter.Converter;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.lang.bridge.EngineBridge;
import org.joshsim.lang.bridge.EngineBridgeSimulationStore;
import org.joshsim.lang.bridge.MinimalEngineBridge;
import org.joshsim.lang.io.InputOutputLayer;
import org.joshsim.precompute.JshdExternalGetter;
import org.joshsim.util.SynchronizedRandom;

/**
 * BridgeGetter implementation that builds and caches using future simulation details.
 *
 * <p>Stuctures which allow the interpreter to pre-compile statements into Java prior to the
 * simulation being fully constructed. This allows Josh to stop string manipulation as soon as
 * possible for efficiency.</p>
 */
public class FutureBridgeGetter implements BridgeGetter {

  private final EngineValueFactory valueFactory;
  private Optional<JoshProgram> program;
  private Optional<String> simulationName;
  private Optional<EngineBridge> builtBridge;
  private Optional<EngineGeometryFactory> geometryFactory;
  private Optional<InputOutputLayer> inputOutputLayer;
  private Optional<org.joshsim.lang.io.CombinedTextWriter> debugWriter;
  private Optional<Long> seed;
  private Random sharedRandom;

  /**
   * Creates a new future bridge getter with no initial configuration.
   *
   * @param valueFactory The value factory to use in constructing returned and supporting values.
   */
  public FutureBridgeGetter(EngineValueFactory valueFactory) {
    this.valueFactory = valueFactory;
    this.program = Optional.empty();
    this.simulationName = Optional.empty();
    this.builtBridge = Optional.empty();
    this.geometryFactory = Optional.empty();
    this.inputOutputLayer = Optional.empty();
    this.debugWriter = Optional.empty();
    this.seed = Optional.empty();
    this.sharedRandom = null;
  }

  /**
   * Set the program to use when getting the bridge.
   *
   * @param newProgram The program to use when getting the bridge.
   */
  public void setProgram(JoshProgram newProgram) {
    if (program.isPresent()) {
      throw new IllegalStateException("Program already set.");
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
      throw new IllegalStateException("Geometry factory already set.");
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
      throw new IllegalStateException("Simulation name already set.");
    }
    this.simulationName = Optional.of(newName);
  }

  /**
   * Sets the platform-specific input/output layer for this bridge getter.
   *
   * @param inputOutputLayer The input/output layer to be set. Provides platform-specific
   *     functionality for input and output operations.
   */
  public void setInputOutputLayer(InputOutputLayer inputOutputLayer) {
    if (this.inputOutputLayer.isPresent()) {
      throw new IllegalStateException("Input output layer already set.");
    }
    this.inputOutputLayer = Optional.of(inputOutputLayer);
  }

  /**
   * Sets the bridge to use instead of building one.
   *
   * <p>This allows injecting the main simulation bridge so that external data
   * requests use the same bridge instance that gets updated during simulation steps.</p>
   *
   * @param bridge The bridge to use for all operations.
   */
  public void setBridge(EngineBridge bridge) {
    this.builtBridge = Optional.of(bridge);
  }

  /**
   * Sets the debug writer to use for debug output.
   *
   * <p>This allows injecting the debug writer so that debug() function calls can produce
   * output. If not set, debug() calls will be no-ops.</p>
   *
   * @param debugWriter The debug writer to use for debug output.
   */
  @Override
  public void setDebugWriter(Optional<org.joshsim.lang.io.CombinedTextWriter> debugWriter) {
    this.debugWriter = debugWriter;
  }

  /**
   * Get the debug writer for this bridge.
   *
   * @return Optional debug writer for writing debug messages. Empty if debug output is not
   *     configured.
   */
  @Override
  public Optional<org.joshsim.lang.io.CombinedTextWriter> getDebugWriter() {
    return debugWriter;
  }

  /**
   * Sets the seed for random number generation.
   *
   * <p>This method creates a shared Random instance (SynchronizedRandom for thread safety)
   * that will be used by all organisms and event handlers in the simulation. This ensures
   * that random values are drawn from a sequential stream rather than each organism creating
   * its own Random instance.</p>
   *
   * @param seed Optional seed value for deterministic random number generation.
   */
  @Override
  public void setSeed(Optional<Long> seed) {
    this.seed = seed;
    if (seed.isPresent()) {
      this.sharedRandom = new SynchronizedRandom(seed.get());
    } else {
      this.sharedRandom = new SynchronizedRandom();
    }
  }

  /**
   * Gets the seed for random number generation.
   *
   * @return Optional seed value. Empty if no seed has been set.
   * @deprecated Use getSharedRandom() instead to ensure proper random state sharing.
   */
  @Override
  @Deprecated
  public Optional<Long> getSeed() {
    return seed;
  }

  /**
   * Gets the shared Random instance for random number generation.
   *
   * <p>This method provides access to a shared Random instance that is used consistently
   * across all organisms and event handlers in the simulation. The instance is lazily
   * initialized if setSeed() was not called.</p>
   *
   * <p>For seeded simulations, this returns a SynchronizedRandom instance initialized
   * with the provided seed. For unseeded simulations, this returns a SynchronizedRandom
   * instance initialized with system time.</p>
   *
   * @return The shared Random instance for this simulation.
   */
  @Override
  public Random getSharedRandom() {
    if (sharedRandom == null) {
      // Lazy initialization for cases where setSeed was not called
      sharedRandom = new SynchronizedRandom();
    }
    return sharedRandom;
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
      throw new IllegalStateException("Simulation name not provided to bridge.");
    }

    String simulationNameRealized = simulationName.get();
    MutableEntity simulation = simulations.getProtoype(simulationNameRealized).build();

    if (geometryFactory.isEmpty()) {
      throw new IllegalStateException("Geometry factory not provided to bridge.");
    }

    EngineGeometryFactory geometryFactoryRealized = geometryFactory.get();

    if (inputOutputLayer.isEmpty()) {
      throw new IllegalStateException("Input output layer not provided to bridge.");
    }

    InputOutputLayer inputOutputLayerRealized = inputOutputLayer.get();

    Converter converter = programRealized.getConverter();
    EntityPrototypeStore prototypeStore = programRealized.getPrototypes();

    EngineBridge newBridge = new MinimalEngineBridge(
        valueFactory,
        geometryFactoryRealized,
        simulation,
        converter,
        prototypeStore,
        new JshdExternalGetter(inputOutputLayerRealized.getInputStrategy(), valueFactory),
        new JshcConfigGetter(inputOutputLayerRealized.getInputStrategy(), valueFactory)
    );

    builtBridge = Optional.of(newBridge);
  }

}
