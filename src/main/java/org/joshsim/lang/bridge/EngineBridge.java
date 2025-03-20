/**
 * Structures for decoupling the language interpreter and engine.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.bridge;
import org.joshsim.engine.entity.Simulation;
import org.joshsim.engine.geometry.GeoPoint;
import org.joshsim.engine.geometry.Geometry;
import org.joshsim.engine.simulation.Replicate;
import org.joshsim.engine.value.EngineValue;


/**
 * Bridge that decouples the engine from the language interpreter.
 */
public class EngineBridge {

  private static final int DEFAULT_START_STEP = 0;
  private static final int DEFAULT_END_STEP = 100;

  private final Simulation simulation;
  private final Replicate replicate;
  private final int endStep;
  
  private int currentStep;
  private boolean inStep;

  public EngineBridge(Simulation simulation, Replicate replicate) {
    this.simulation = simulation;
    this.replicate = replicate;

    currentStep = simulation.getAttributeValue("").orElse(DEFAULT_START_STEP);
    endStep = simulation.getAttributeValue("").orElse(DEFAULT_END_STEP);
    inStep = false;
  }
  
  public void startStep() {
    
  }

  public void endStep() {
    
  }

  public ShadowingEntity getPatch(GeoPoint point) {
    
  }

  public Iterable<ShadowingEntity> getCurrentPatches() {
    
  }

  public Iterable<ShadowingEntity> getPriorPatches(Geometry query) {
    
  }

  public EngineValue convert(EngineValue current, String newUnits) {
    
  }
  
}
