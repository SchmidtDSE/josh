/**
 * Structure to describe a single time step after freezing.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.simulation;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.joshsim.engine.entity.base.Entity;
import org.joshsim.engine.entity.base.GeoKey;
import org.joshsim.engine.geometry.EngineGeometry;


/**
 * Represents a single time step in the simulation, containing mutable or immutable
 * entries depending on whether it is frozen.
 */
public class TimeStep {

  protected long stepNumber;
  protected Entity meta;
  protected Map<GeoKey, Entity> patches;

  /**
   * Create a new TimeStep, which contains entities that are frozen / immutable.
   */
  public TimeStep(long stepNumber, Entity meta, Map<GeoKey, Entity> patches) {
    this.stepNumber = stepNumber;
    this.meta = meta;
    this.patches = patches;
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
   * Get simulation metadata.
   *
   * @return the simulation entity record with metadata
   */
  public Entity getMeta() {
    return meta;
  }

  /**
   * Get patches within the specified geometry at this time step.
   *
   * @param geometry the spatial bounds to query
   * @return an iterable of patches within the geometry
   */
  public Iterable<Entity> getPatches(EngineGeometry geometry) {
    List<Entity> selectedPatches = patches.values().stream()
        .filter(patch -> patch.getGeometry()
              .map(geo -> geo.intersects(geometry))
              .orElse(false))
        .collect(Collectors.toList());
    return selectedPatches;
  }

  /**
   * Get patches with the specified name within the geometry at this time step.
   *
   * @param geometry the spatial bounds to query
   * @param name the patch name to filter by
   * @return an iterable of matching patches
   */
  public Iterable<Entity> getPatches(EngineGeometry geometry, String name) {
    List<Entity> selectedPatches = patches.values().stream()
        .filter(patch -> patch.getName().equals(name))
        .filter(patch -> patch.getGeometry()
              .map(geo -> geo.intersects(geometry))
              .orElse(false))
        .collect(Collectors.toList());
    return selectedPatches;
  }

  /**
   * Get all patches at this time step.
   *
   * @return an iterable of all patches
   */
  public Iterable<Entity> getPatches() {
    return patches.values();
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

}
