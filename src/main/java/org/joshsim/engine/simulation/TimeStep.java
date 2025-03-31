/**
 * Structures describing an individual timestep within a replicate.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.simulation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.joshsim.engine.entity.base.Entity;
import org.joshsim.engine.entity.base.GeoKey;
import org.joshsim.engine.geometry.Geometry;

/**
 * Structure representing a discrete time step within a simulation.
 *
 * <p>Provides methods to retrieve entities at a specific point in time.
 * </p>
 */
public class TimeStep {
  private long stepNumber;
  private HashMap<GeoKey, Entity> patches;
  private Boolean isFrozen;

  /**
   * Create a new time step. If it is frozen, the entities are immutable, and the 
   * step number cannot be incremented.
   *
   * @param timeStep the integer time step number
   * @param patches the patches at this time step
   */
  TimeStep(long stepNumber, HashMap<GeoKey, Entity> patches, Boolean isFrozen) {
    this.stepNumber = stepNumber;
    this.patches = patches;
    this.isFrozen = isFrozen;
  }

  /**
   * Get the time step number.
   *
   * @return the integer time step number
   */
  public long getStep() {
    return stepNumber;
  }

  /**
   * Increment the time step number.   *
   */
  public void incrementStep() {
    if (isFrozen) {
      throw new IllegalStateException("Cannot increment a frozen time step.");
    }
    stepNumber = stepNumber + 1;
  }

  /**
   * Get entities within the specified geometry at this time step.
   *
   * @param geometry the spatial bounds to query
   * @return an iterable of entities within the geometry
   */
  public Iterable<Entity> getEntities(Geometry geometry) {
    List<Entity> selectedEntities = patches.values().stream()
        .filter(patch -> patch.getGeometry()
                  .map(geo -> geo.intersects(geometry))
                  .orElse(false))
        .collect(Collectors.toList());
    return selectedEntities;
  }

  /**
   * Get entities with the specified name within the geometry at this time step.
   *
   * @param geometry the spatial bounds to query
   * @param name the entity name to filter by
   * @return an iterable of matching entities
   */
  public Iterable<Entity> getEntities(Geometry geometry, String name) {
    List<Entity> selectedEntities = patches.values().stream()
        .filter(patch -> patch.getName().equals(name))
        .filter(patch -> patch.getGeometry()
                  .map(geo -> geo.intersects(geometry))
                  .orElse(false))
        .collect(Collectors.toList());
    return selectedEntities;
  }

  /**
   * Get all entities at this time step.
   *
   * @return an iterable of all entities
   */
  public Iterable<Entity> getEntities() {
    return patches.values().stream().collect(Collectors.toList());
  }

  /**
   * Get a patch by its key.
   *
   * @param key the GeoKey to look up
   * @return the patch associated with the key, or null if not found
   */
  public Entity getPatchByKey(GeoKey key) {
    return patches.get(key);
  }

  /**
   * Create a frozen copy of this time step with immutable entities.
   *
   * @return A new TimeStep with frozen entities.
   */
  public TimeStep freeze() {
    HashMap<GeoKey, Entity> frozenPatches = new HashMap<>();

    for (Map.Entry<GeoKey, Entity> entry : patches.entrySet()) {
      Entity frozenEntity = entry.getValue().freeze();
      frozenPatches.put(entry.getKey(), frozenEntity);
    }

    TimeStep frozenTimeStep = new TimeStep(getStep(), frozenPatches, true);
    return frozenTimeStep;
  }
}
