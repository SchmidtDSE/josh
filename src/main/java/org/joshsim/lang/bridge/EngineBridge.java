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
public interface EngineBridge {

  Simulation getSimulation();
  
  void startStep();

  void endStep();

  void startSubstep(String phase);

  void endSubstep();

  ShadowingEntity getPatch(GeoPoint point);

  Iterable<ShadowingEntity> getCurrentPatches();

  Iterable<ShadowingEntity> getPriorPatches();

  Iterable<ShadowingEntity> getPriorPatches(Geometry query);

  EngineValue convert(EngineValue current, String newUnits);

  Replicate end();
  
}
