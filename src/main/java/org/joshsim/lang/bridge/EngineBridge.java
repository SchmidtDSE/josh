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
public interface EngineBridge {

  /**
   * Start a new simulation step.
   *
   * <p>Indicates that a new simulation step is beginning. This must be called before any mutations
   * can occur in the current step.</p>
   *
   * @throws IllegalStateException if called while already in a step.
   */
  void startStep();

  /**
   * End the current simulation step.
   *
   * <p>Indicates that the current simulation step is complete. This must be called after all
   * mutations for the current step have completed.</p>
   *
   * @throws IllegalStateException if called while not in a step.
   */
  void endStep();

  /**
   * Get a patch at a specific geometric point.
   *
   * @param point the geometric location to query.
   * @return Optional containing the patch if found, empty Optional otherwise.
   * @throws IllegalStateException if zero or multiple patches found at the point.
   */
  Optional<ShadowingEntity> getPatch(GeoPoint point);

  /**
   * Get all patches in the current simulation step.
   *
   * @return Iterable of all patches in the current step.
   */
  Iterable<ShadowingEntity> getCurrentPatches();

  /**
   * Get patches from the previous step within a specific geometry.
   *
   * @param geometry the geometric area to query.
   * @return Iterable of patches from the previous step within the specified geometry.
   */
  Iterable<ShadowingEntity> getPriorPatches(Geometry geometry);

  /**
   * Get patches from the previous step within a specific geometry momento.
   *
   * @param geometryMomento with the momento for the geometric area to query.
   * @return Iterable of patches from the previous step within the specified geometry.
   */
  Iterable<ShadowingEntity> getPriorPatches(GeometryMomento geometryMomento);

  /**
   * Convert an engine value to different units.
   *
   * @param current the value to convert.
   * @param newUnits the units to convert to.
   * @return the converted value.
   */
  EngineValue convert(EngineValue current, Units newUnits);

  /**
   * Get the current simulation step as a long value.
   *
   * @return the current simulation step count as a long.
   */
  long getCurrentTimestep();

  /**
   * Get the prior simulation step as a long value.
   *
   * @return the prior simulation step count as a long.
   */
  long getPriorTimestep();

  /**
   * Get the number of timesteps completed.
   *
   * @return Integer count of full timesteps which have been completed in their entirety.
   */
  long getAbsoluteTimestep();

  /**
   * Get the replicate being modified by this EngineBridge.
   *
   * @return Replicate being manipulated by this bridge.
   */
  Replicate getReplicate();

}
