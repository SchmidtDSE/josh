/**
 * Structures for managing stateful step operations.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.bridge;

import java.util.Iterator;
import java.util.Optional;
import org.joshsim.engine.entity.base.Entity;
import org.joshsim.engine.entity.type.Patch;
import org.joshsim.engine.func.CompiledCallable;
import org.joshsim.engine.func.SingleValueScope;
import org.joshsim.engine.geometry.GeoPoint;
import org.joshsim.engine.geometry.Geometry;
import org.joshsim.engine.simulation.Query;
import org.joshsim.engine.simulation.Replicate;
import org.joshsim.engine.simulation.Simulation;
import org.joshsim.engine.value.converter.Conversion;
import org.joshsim.engine.value.converter.Converter;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.engine.value.type.EngineValue;


/**
 * Bridge that decouples the engine from the language interpreter and manages step state.
 */
public class MinimalEngineBridge implements EngineBridge {

  private static final long DEFAULT_START_STEP = 0;
  private static final long DEFAULT_END_STEP = 100;

  private final Simulation simulation;
  private final Replicate replicate;
  private final EngineValueFactory engineValueFactory;
  private final EngineValue endStep;
  private final Converter converter;

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
   * @param simulation The simulation instance to be used for retrieving or manipulating simulation
   *     data.
   * @param replicate The replicate instance for querying patches and other simulation data.
   * @param converter The converter for handling unit conversions between different engine values.
   */
  public MinimalEngineBridge(Simulation simulation, Replicate replicate, Converter converter) {
    this.simulation = simulation;
    this.replicate = replicate;
    this.converter = converter;

    engineValueFactory = new EngineValueFactory();

    currentStep = simulation
      .getAttributeValue("step.start")
      .orElseGet(() -> engineValueFactory.build(DEFAULT_START_STEP, new Units("count")));

    endStep = simulation
      .getAttributeValue("step.end")
      .orElseGet(() -> engineValueFactory.build(DEFAULT_END_STEP, new Units("count")));

    absoluteStep = 0;
    inStep = false;
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

    currentStep = engineValueFactory.build(currentStep.getAsInt() + 1, new Units("count"));
    absoluteStep++;
    // TODO: TimeStep
    inStep = false;
  }

  @Override
  public Optional<Entity> getPatch(GeoPoint point) {
    Query query = new Query(currentStep.getAsInt(), point);
    Iterable<Entity> patches = replicate.query(query);

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
  public Iterable<ShadowingEntity> getCurrentPatches() {
    Query query = new Query(getCurrentTimestep());
    Iterable<Patch> patches = replicate.getCurrentPatches();
    Iterable<ShadowingEntity> decorated = () -> new DecoratingShadowIterator(patches.iterator());
    return decorated;
  }

  @Override
  public Iterable<Entity> getPriorPatches(Geometry geometry) {
    Query query = new Query(getPriorTimestep(), geometry);
    Iterable<Entity> patches = replicate.query(query);
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
    return callable.evaluate(new SingleValueScope(current));
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
  public Replicate getReplicate() {
    return replicate;
  }

  /**
   * Iterator that decorates patches with shadow tracking.
   */
  private class DecoratingShadowIterator implements Iterator<ShadowingEntity> {

    private final Iterator<Patch> patches;

    /**
     * Create a new decorating iterator.
     *
     * @param patches the iterator of patches to decorate.
     */
    public DecoratingShadowIterator(Iterator<Patch> patches) {
      this.patches = patches;
    }

    @Override
    public boolean hasNext() {
      return patches.hasNext();
    }

    @Override
    public ShadowingEntity next() {
      Patch patch = patches.next();
      return new ShadowingEntity(patch, simulation);
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

}
