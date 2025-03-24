/**
 * Structures for decoupling the language interpreter and engine.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.bridge;

import java.util.Iterator;
import java.util.Optional;
import org.joshsim.engine.entity.Patch;
import org.joshsim.engine.entity.Simulation;
import org.joshsim.engine.func.CompiledCallable;
import org.joshsim.engine.func.SingleValueScope;
import org.joshsim.engine.geometry.GeoPoint;
import org.joshsim.engine.geometry.Geometry;
import org.joshsim.engine.simulation.Query;
import org.joshsim.engine.simulation.Replicate;
import org.joshsim.engine.value.Conversion;
import org.joshsim.engine.value.Converter;
import org.joshsim.engine.value.EngineValue;
import org.joshsim.engine.value.EngineValueFactory;
import org.joshsim.engine.value.Units;


/**
 * Bridge that decouples the engine from the language interpreter.
 */
public class EngineBridge {

  private static final long DEFAULT_START_STEP = 0;
  private static final long DEFAULT_END_STEP = 100;

  private final Simulation simulation;
  private final Replicate replicate;
  private final EngineValueFactory engineValueFactory;
  private final EngineValue endStep;
  private final Converter converter;

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
  public EngineBridge(Simulation simulation, Replicate replicate, Converter converter) {
    this.simulation = simulation;
    this.replicate = replicate;
    this.converter = converter;

    engineValueFactory = new EngineValueFactory();

    currentStep = simulation
      .getAttributeValue("stepCount")
      .orElseGet(() -> engineValueFactory.build(DEFAULT_START_STEP, new Units("count")));

    endStep = simulation
      .getAttributeValue("stepCount")
      .orElseGet(() -> engineValueFactory.build(DEFAULT_END_STEP, new Units("count")));

    inStep = false;
  }

  /**
   * Start a new simulation step.
   *
   * <p>Indicates that a new simulation step is beginning. This must be called before any mutations
   * can occur in the current step.</p>
   *
   * @throws IllegalStateException if called while already in a step.
   */
  public void startStep() {
    if (inStep) {
      throw new IllegalStateException("Tried to start a step before finishing the current one.");
    }

    inStep = true;
  }

  /**
   * End the current simulation step.
   *
   * <p>Indicates that the current simulation step is complete. This must be called after all
   * mutations for the current step have completed.</p>
   *
   * @throws IllegalStateException if called while not in a step.
   */
  public void endStep() {
    if (!inStep) {
      throw new IllegalStateException("Tried to end a step before starting the current one.");
    }

    currentStep = engineValueFactory.build(currentStep.getAsInt() + 1, new Units("count"));
    inStep = false;
  }

  /**
   * Get a patch at a specific geometric point.
   *
   * @param point the geometric location to query.
   * @return Optional containing the patch if found, empty Optional otherwise.
   * @throws IllegalStateException if zero or multiple patches found at the point.
   */
  public Optional<ShadowingEntity> getPatch(GeoPoint point) {
    Query query = new Query(currentStep.getAsInt(), point);
    Iterable<Patch> patches = replicate.query(query);

    Iterator<Patch> iterator = patches.iterator();

    if (!iterator.hasNext()) {
      throw new IllegalStateException("Expected exactly one Patch, but found none.");
    }

    Patch patch = iterator.next();
    ShadowingEntity decorated = new ShadowingEntity(patch, simulation);

    if (iterator.hasNext()) {
      throw new IllegalStateException("Expected exactly one Patch, but found more.");
    }

    return Optional.of(decorated);
  }

  /**
   * Get all patches in the current simulation step.
   *
   * @return Iterable of all patches in the current step.
   */
  public Iterable<ShadowingEntity> getCurrentPatches() {
    Query query = new Query(getCurrentTimestep());
    Iterable<Patch> patches = replicate.query(query);
    Iterable<ShadowingEntity> decorated = () -> new DecoratingShadowIterator(patches.iterator());
    return decorated;
  }

  /**
   * Get patches from the previous step within a specific geometry.
   *
   * @param geometry the geometric area to query.
   * @return Iterable of patches from the previous step within the specified geometry.
   */
  public Iterable<ShadowingEntity> getPriorPatches(Geometry geometry) {
    Query query = new Query(getPriorTimestep(), geometry);
    Iterable<Patch> patches = replicate.query(query);
    Iterable<ShadowingEntity> decorated = () -> new DecoratingShadowIterator(patches.iterator());
    return decorated;
  }

  /**
   * Get patches from the previous step within a specific geometry momento.
   *
   * @param geometryMomento with the momento for the geometric area to query.
   * @return Iterable of patches from the previous step within the specified geometry.
   */
  public Iterable<ShadowingEntity> getPriorPatches(GeometryMomento geometryMomento) {
    return getPriorPatches(geometryMomento.build());
  }

  /**
   * Convert an engine value to different units.
   *
   * @param current the value to convert.
   * @param newUnits the units to convert to.
   * @return the converted value.
   */
  public EngineValue convert(EngineValue current, Units newUnits) {
    Conversion conversion = converter.getConversion(current.getUnits(), newUnits);
    CompiledCallable callable = conversion.getConversionCallable();
    return callable.evaluate(new SingleValueScope(current));
  }

  /**
   * Get the current simulation step as a long value.
   *
   * @return the current simulation step count as a long.
   */
  public long getCurrentTimestep() {
    return currentStep.getAsInt();
  }

  /**
   * Get the prior simulation step as a long value.
   *
   * @return the prior simulation step count as a long.
   */
  public long getPriorTimestep() {
    return currentStep.getAsInt() - 1;
  }

  /**
   * Get the replicate being modified by this EngineBridge.
   *
   * @return Replicate being manipulated by this bridge.
   */
  protected Replicate getReplicate() {
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
