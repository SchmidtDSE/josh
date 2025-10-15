/**
 * Structure to describe a single time step after freezing.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.simulation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.joshsim.engine.entity.base.Entity;
import org.joshsim.engine.entity.base.GeoKey;
import org.joshsim.engine.geometry.EngineGeometry;
import org.joshsim.engine.geometry.grid.GridShape;
import org.joshsim.engine.geometry.grid.GridShapeType;

/**
 * Represents a single time step in the simulation, containing mutable or immutable
 * entries depending on whether it is frozen.
 */
public class TimeStep {

  protected long stepNumber;
  protected Entity meta;
  protected Map<GeoKey, Entity> patches;
  private volatile PatchSpatialIndex spatialIndex;

  /**
   * Create a new TimeStep, which contains entities that are frozen / immutable.
   */
  public TimeStep(long stepNumber, Entity meta, Map<GeoKey, Entity> patches) {
    this.stepNumber = stepNumber;
    this.meta = meta;
    this.patches = patches;
  }

  /**
   * Gets or builds the spatial index for this timestep.
   *
   * <p>Uses double-checked locking for thread-safe lazy initialization.
   * The index is built once on first query and reused for all subsequent queries.</p>
   *
   * @return the spatial index for this timestep
   */
  private PatchSpatialIndex getSpatialIndex() {
    if (spatialIndex == null) {
      synchronized (this) {
        if (spatialIndex == null) {
          spatialIndex = new PatchSpatialIndex(patches);
        }
      }
    }
    return spatialIndex;
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
   * <p>For circle queries, uses exact circle-square intersection mathematics to compute
   * precise results. For other geometries (squares, points), uses spatial index with
   * bounding box filtering followed by exact intersection tests.</p>
   *
   * @param geometry the spatial bounds to query
   * @return a list of patches within the geometry
   */
  public List<Entity> getPatches(EngineGeometry geometry) {
    List<Entity> candidates = getSpatialIndex().queryCandidates(geometry);

    GridShape gridGeom = geometry.getOnGrid();
    boolean isCircleQuery = gridGeom != null
        && gridGeom.getGridShapeType() == GridShapeType.CIRCLE;
    if (isCircleQuery) {
      return candidates;
    }

    List<Entity> selectedPatches = new ArrayList<>(candidates.size());
    for (Entity patch : candidates) {
      Optional<EngineGeometry> patchGeometry = patch.getGeometry();
      boolean geometryPresent = patchGeometry.isPresent();
      if (!geometryPresent) {
        continue;
      }
      boolean geometryIntersects = patchGeometry.get().intersects(geometry);
      if (geometryIntersects) {
        selectedPatches.add(patch);
      }
    }

    return selectedPatches;
  }

  /**
   * Get patches with the specified name within the geometry at this time step.
   *
   * @param geometry the spatial bounds to query
   * @param name the patch name to filter by
   * @return a list of matching patches
   */
  public List<Entity> getPatches(EngineGeometry geometry, String name) {
    List<Entity> candidates = getSpatialIndex().queryCandidates(geometry);

    GridShape gridGeom = geometry.getOnGrid();
    boolean isCircleQuery = gridGeom != null
        && gridGeom.getGridShapeType() == GridShapeType.CIRCLE;

    if (isCircleQuery) {
      List<Entity> selectedPatches = new ArrayList<>(candidates.size());
      for (Entity patch : candidates) {
        boolean nameMatches = patch.getName().equals(name);
        boolean hasGeometry = patch.getGeometry().isPresent();
        if (nameMatches && hasGeometry) {
          selectedPatches.add(patch);
        }
      }
      return selectedPatches;
    }

    List<Entity> selectedPatches = new ArrayList<>(candidates.size());
    for (Entity patch : candidates) {
      boolean nameMatches = patch.getName().equals(name);
      if (!nameMatches) {
        continue;
      }
      Optional<EngineGeometry> patchGeometry = patch.getGeometry();
      boolean geometryPresent = patchGeometry.isPresent();
      if (!geometryPresent) {
        continue;
      }
      boolean geometryIntersects = patchGeometry.get().intersects(geometry);
      if (geometryIntersects) {
        selectedPatches.add(patch);
      }
    }

    return selectedPatches;
  }

  /**
   * Get all patches at this time step.
   *
   * @return a list of all patches
   */
  public List<Entity> getPatches() {
    return new ArrayList<>(patches.values());
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
