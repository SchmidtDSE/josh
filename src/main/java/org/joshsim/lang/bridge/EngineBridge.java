/**
 * Structures for decoupling the language interpreter and engine.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.bridge;

import java.util.List;
import java.util.Optional;
import org.joshsim.engine.entity.base.Entity;
import org.joshsim.engine.entity.base.GeoKey;
import org.joshsim.engine.entity.base.MutableEntity;
import org.joshsim.engine.entity.prototype.EntityPrototype;
import org.joshsim.engine.geometry.EngineGeometry;
import org.joshsim.engine.geometry.EngineGeometryFactory;
import org.joshsim.engine.geometry.EnginePoint;
import org.joshsim.engine.simulation.Replicate;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.engine.value.type.EngineValue;


/**
 * Bridge that decouples the engine from the language interpreter.
 */
public interface EngineBridge {

  /**
   * Get the factory to use in building geometries.
   *
   * @return The factory to use in building engine geometries.
   */
  EngineGeometryFactory getGeometryFactory();

  /**
   * Get the current simulation entity.
   *
   * @return the current simulation entity.
   */
  MutableEntity getSimulation();

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
   * @param enginePoint the geometric location to query.
   * @return Optional containing the patch if found, empty Optional otherwise.
   * @throws IllegalStateException if zero or multiple patches found at the point.
   */
  Optional<Entity> getPatch(EnginePoint enginePoint);

  /**
   * Get all patches in the current simulation step.
   *
   * @return Iterable of all patches in the current step.
   */
  Iterable<MutableEntity> getCurrentPatches();

  /**
   * Get patches from the previous step within a specific geometry.
   *
   * @param geometry the geometric area to query.
   * @return List of patches from the previous step within the specified geometry.
   */
  List<Entity> getPriorPatches(EngineGeometry geometry);

  /**
   * Get patches from the previous step within a specific geometry momento.
   *
   * @param geometryMomento with the momento for the geometric area to query.
   * @return List of patches from the previous step within the specified geometry.
   */
  List<Entity> getPriorPatches(GeometryMomento geometryMomento);

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
   * Determine at what timestep the simulation starts.
   *
   * @return User defined timestep where the simulation starts.
   */
  long getStartTimestep();

  /**
   * Determine at what timestep the simulation ends.
   *
   * @return User defined timestep where the simulation ends.
   */
  long getEndTimestep();

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

  /**
   * Get the prototype for an entity.
   *
   * @param name The name of the entity type like ForeverTree.
   * @return The prototype for constructing that entity.
   */
  EntityPrototype getPrototype(String name);

  /**
   * Determine if the simulation has surpassed its final step.
   *
   * @return True if the simulation has surpassed the final step and false otherwise.
   */
  boolean isComplete();

  /**
   * Get a value from an external resource.
   *
   * @param key The location at which the data is requested.
   * @param name The name of the external resource.
   * @param step The timestep at which the value from the external resource should be returned.
   * @return The value from the external resource at the given step.
   */
  EngineValue getExternal(GeoKey key, String name, long step);

  /**
   * Get a configuration value by name, returning empty if not found.
   *
   * @param name The name of the configuration value to retrieve.
   * @return Optional containing the configuration value, or empty if not found.
   */
  Optional<EngineValue> getConfigOptional(String name);


  /**
   * Get the factory to use in building engine values.
   *
   * @return Value factory to use across the simulation this bridge supports.
   */
  EngineValueFactory getEngineValueFactory();

}
