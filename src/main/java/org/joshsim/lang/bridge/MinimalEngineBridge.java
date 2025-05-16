/**
 * Structures for managing stateful step operations.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.bridge;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import org.joshsim.compat.CompatibilityLayerKeeper;
import org.joshsim.engine.entity.base.Entity;
import org.joshsim.engine.entity.base.GeoKey;
import org.joshsim.engine.entity.base.MutableEntity;
import org.joshsim.engine.entity.prototype.EntityPrototype;
import org.joshsim.engine.entity.prototype.EntityPrototypeStore;
import org.joshsim.engine.func.CompiledCallable;
import org.joshsim.engine.func.SingleValueScope;
import org.joshsim.engine.geometry.EngineGeometry;
import org.joshsim.engine.geometry.EngineGeometryFactory;
import org.joshsim.engine.geometry.EnginePoint;
import org.joshsim.engine.geometry.PatchSet;
import org.joshsim.engine.simulation.Query;
import org.joshsim.engine.simulation.Replicate;
import org.joshsim.engine.value.converter.Conversion;
import org.joshsim.engine.value.converter.Converter;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.engine.value.type.EngineValue;
import org.joshsim.precompute.DataGridLayer;


/**
 * Bridge that decouples the engine from the language interpreter and manages step state.
 */
public class MinimalEngineBridge implements EngineBridge {

  private static final long DEFAULT_START_STEP = 0;
  private static final long DEFAULT_END_STEP = 100;

  private final EngineGeometryFactory geometryFactory;
  private final MutableEntity simulation;
  private final EngineValueFactory engineValueFactory;
  private final EngineValue startStep;
  private final EngineValue endStep;
  private final Converter converter;
  private final EntityPrototypeStore prototypeStore;
  private final Map<String, DataGridLayer> externalData;
  private final ExternalResourceGetter externalResourceGetter;

  private Optional<Replicate> replicate;
  private long absoluteStep;
  private EngineValue currentStep;
  private boolean inStep;

  /**
   * Constructs an EngineBridge to manipulate the specified simulation, replicate, and converter.
   *
   * <p>This class provides a bridge to decouple the simulation engine from the language
   * interpreter. It facilitates interactions with the engine's simulation and replicate components,
   * as well as value conversion.</p>
   *
   * @param geometryFactory The factory to use for building engine geometries.
   * @param simulation The simulation instance to be used for retrieving or manipulating simulation
   *     data.
   * @param converter The converter for handling unit conversions between different engine values.
   */
  public MinimalEngineBridge(EngineGeometryFactory geometryFactory, MutableEntity simulation,
        Converter converter, EntityPrototypeStore prototypeStore,
        ExternalResourceGetter externalResourceGetter) {
    this.geometryFactory = geometryFactory;
    this.simulation = simulation;
    this.converter = converter;
    this.prototypeStore = prototypeStore;
    this.externalResourceGetter = externalResourceGetter;

    replicate = Optional.empty();

    engineValueFactory = CompatibilityLayerKeeper.get().getEngineValueFactory();

    simulation.startSubstep("constant");

    startStep = simulation
      .getAttributeValue("steps.low")
      .orElseGet(() -> engineValueFactory.build(DEFAULT_START_STEP, Units.of("count")));

    currentStep = startStep;

    endStep = simulation
      .getAttributeValue("steps.high")
      .orElseGet(() -> engineValueFactory.build(DEFAULT_END_STEP, Units.of("count")));

    simulation.endSubstep();

    absoluteStep = 0;
    inStep = false;
    externalData = new HashMap<>();
  }

  /**
   * Constructs an EngineBridge with a given Replicate for testing.
   *
   * @param simulation The simulation instance to be used for retrieving or manipulating simulation
   *     data.
   * @param converter The converter for handling unit conversions between different engine values.
   * @param externalResourceGetter Strategy to use in loading external resources.
   * @param replicate The replicate to use for testing.
   */
  public MinimalEngineBridge(EngineGeometryFactory geometryFactory, MutableEntity simulation,
        Converter converter, EntityPrototypeStore prototypeStore,
        ExternalResourceGetter externalResourceGetter, Replicate replicate) {
    this.geometryFactory = geometryFactory;
    this.simulation = simulation;
    this.converter = converter;
    this.prototypeStore = prototypeStore;
    this.externalResourceGetter = externalResourceGetter;

    this.replicate = Optional.of(replicate);

    engineValueFactory = CompatibilityLayerKeeper.get().getEngineValueFactory();

    simulation.startSubstep("constant");

    startStep = simulation
      .getAttributeValue("steps.low")
      .orElseGet(() -> engineValueFactory.build(DEFAULT_START_STEP, Units.of("count")));

    currentStep = startStep;

    endStep = simulation
      .getAttributeValue("steps.high")
      .orElseGet(() -> engineValueFactory.build(DEFAULT_END_STEP, Units.of("count")));

    simulation.endSubstep();

    absoluteStep = 0;
    inStep = false;
    externalData = new HashMap<>();
  }

  @Override
  public EngineGeometryFactory getGeometryFactory() {
    return geometryFactory;
  }

  @Override
  public MutableEntity getSimulation() {
    return simulation;
  }

  @Override
  public void startStep() {
    if (inStep) {
      throw new IllegalStateException("Tried to start a step before finishing the current one.");
    }

    inStep = true;
  }

  @Override
  public void endStep() {
    if (!inStep) {
      throw new IllegalStateException("Tried to end a step before starting the current one.");
    }

    getReplicate().saveTimeStep(currentStep.getAsInt());

    currentStep = engineValueFactory.build(currentStep.getAsInt() + 1, Units.of("count"));
    absoluteStep++;
    inStep = false;
  }

  @Override
  public boolean isComplete() {
    return currentStep.greaterThan(endStep).getAsBoolean();
  }

  @Override
  public EngineValue getExternal(GeoKey key, String name, long step) {
    if (!externalData.containsKey(name)) {
      externalData.put(name, externalResourceGetter.getResource(name));
    }
    return externalData.get(name).getAt(key, step);
  }

  @Override
  public Optional<Entity> getPatch(EnginePoint enginePoint) {
    Query query = new Query(currentStep.getAsInt(), enginePoint);
    Iterable<Entity> patches = getReplicate().query(query);

    Iterator<Entity> iterator = patches.iterator();

    if (!iterator.hasNext()) {
      throw new IllegalStateException("Expected exactly one Patch, but found none.");
    }

    Entity patch = iterator.next();

    if (iterator.hasNext()) {
      throw new IllegalStateException("Expected exactly one Patch, but found more.");
    }

    return Optional.of(patch);
  }

  @Override
  public Iterable<MutableEntity> getCurrentPatches() {
    Query query = new Query(getCurrentTimestep());
    Iterable<MutableEntity> patches = getReplicate().getCurrentPatches();
    Iterable<MutableEntity> decorated = () -> new DecoratingShadowIterator(patches.iterator());
    return decorated;
  }

  @Override
  public Iterable<Entity> getPriorPatches(EngineGeometry geometry) {
    Query query = new Query(getPriorTimestep(), geometry);
    Iterable<Entity> patches = getReplicate().query(query);
    return patches;
  }

  @Override
  public Iterable<Entity> getPriorPatches(GeometryMomento geometryMomento) {
    return getPriorPatches(geometryMomento.build());
  }

  @Override
  public EngineValue convert(EngineValue current, Units newUnits) {
    Conversion conversion = converter.getConversion(current.getUnits(), newUnits);
    CompiledCallable callable = conversion.getConversionCallable();
    EngineValue newValue = callable.evaluate(new SingleValueScope(current));
    return newValue.replaceUnits(newUnits);
  }

  @Override
  public long getCurrentTimestep() {
    return currentStep.getAsInt();
  }

  @Override
  public long getPriorTimestep() {
    return currentStep.getAsInt() - 1;
  }

  @Override
  public long getAbsoluteTimestep() {
    return absoluteStep;
  }

  @Override
  public long getStartTimestep() {
    return startStep.getAsInt();
  }

  @Override
  public long getEndTimestep() {
    return endStep.getAsInt();
  }

  @Override
  public Replicate getReplicate() {
    if (replicate.isEmpty()) {
      GridFromSimFactory factory = new GridFromSimFactory(this);
      PatchSet grid = factory.build(simulation);
      replicate = Optional.of(new Replicate(simulation, grid));
    }

    return replicate.get();
  }

  @Override
  public EntityPrototype getPrototype(String name) {
    if (!prototypeStore.has(name)) {
      throw new IllegalArgumentException("Unknown entity type: " + name);
    }

    return prototypeStore.get(name);
  }

  /**
   * Iterator that decorates patches with shadow tracking.
   */
  private class DecoratingShadowIterator implements Iterator<MutableEntity> {

    private final Iterator<MutableEntity> patches;

    /**
     * Create a new decorating iterator.
     *
     * @param patches the iterator of patches to decorate.
     */
    public DecoratingShadowIterator(Iterator<MutableEntity> patches) {
      this.patches = patches;
    }

    @Override
    public boolean hasNext() {
      return patches.hasNext();
    }

    @Override
    public MutableEntity next() {
      MutableEntity patch = patches.next();
      return new ShadowingEntity(patch, simulation);
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

}
