package org.joshsim.engine.simulation;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import org.joshsim.engine.entity.base.Entity;
import org.joshsim.engine.entity.base.GeoKey;
import org.joshsim.engine.geometry.Geometry;

/**
 * Represents a single time step in the simulation, containing mutable or immutable
 * entries depending on whether it is frozen.
 */
public class TimeStep {
  protected long stepNumber;
  protected HashMap<GeoKey, Entity> patches;

  /**
   * Create a new TimeStep, which contains entities that are frozen / immutable.
   */
  public TimeStep(long stepNumber, HashMap<GeoKey, Entity> patches) {
    this.stepNumber = stepNumber;
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
   * Get patches within the specified geometry at this time step.
   *
   * @param geometry the spatial bounds to query
   * @return an iterable of patches within the geometry
   */
  public Iterable<Entity> getPatches(Geometry geometry) {
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
  public Iterable<Entity> getPatches(Geometry geometry, String name) {
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
