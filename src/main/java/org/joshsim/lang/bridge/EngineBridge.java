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
  
  public void startStep() {
    if (inStep) {
      throw new IllegalStateException("Tried to start a step before finishing the current one.");
    }

    inStep = true;
  }

  public void endStep() {
    if (!inStep) {
      throw new IllegalStateException("Tried to end a step before starting the current one.");
    }

    currentStep = engineValueFactory.build(engineStep.getAsInt(), "count");
    inStep = false;
  }

  public Optional<ShadowingEntity> getPatch(GeoPoint point) {
    Query query = new Query(engineStep.getAsInt(), point);
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

  public Iterable<ShadowingEntity> getCurrentPatches() {
    Query query = new Query(engineStep.getAsInt());
    Iterable<Patch> patches = replicate.query(query);
    return () -> new DecoratingShadowIterator(patches);
  }

  public Iterable<ShadowingEntity> getPriorPatches(Geometry geometry) {
    Query query = new Query(engineStep.getAsInt() - 1, geometry);
    Iterable<Patch> patches = replicate.query(query);
    return () -> new DecoratingShadowIterator(patches);
  }

  public EngineValue convert(EngineValue current, String newUnits) {
    Conversion conversion = converter.getConversion(current.getUnits(), newUnits);
    CompiledCallable callable = conversion.getConversionCallable();
    return callable.evaluate(new SingleValueScope(current));
  }

  private class DecoratingShadowIterator implements Iterator<ShadowingEntity> {

    private final Iterator<Patch> patches;

    public DecoratingShadowIterator(Iterator<Patch> patches) {
      this.patches = patches;
    }
    
    @Override
    public boolean hasNext() {
      return patchIterator.hasNext();
    }

    @Override
    public ShadowingEntity next() {
      Patch patch = patchIterator.next();
      return new ShadowingEntity(patch, simulation);
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }
  
}
