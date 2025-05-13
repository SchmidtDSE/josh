/**
 * Structures describing keys for cells within a simulation across time steps.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.entity.base;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;
import org.joshsim.engine.geometry.EngineGeometry;

/**
 * Represents a key to uniquely identify a Patch within a simulation across time steps.
 */
public class GeoKey {

  private final Optional<EngineGeometry> geometry;
  private final String entityName;

  /**
   * Craete a new key with the specified entity.
   *
   * @param entity The patch to be keyed.
   */
  public GeoKey(Entity entity) {
    geometry = entity.getGeometry();
    entityName = entity.getName();
  }

  /**
   * Create a new key with the given properties.
   *
   * @param geometry The geometry of the entity represented by this key or empty if this entity
   *     does not have geometry.
   * @param entityName The name of this type of entity.
   */
  public GeoKey(Optional<EngineGeometry> geometry, String entityName) {
    this.geometry = geometry;
    this.entityName = entityName;
  }

  /**
   * Get the horizontal center of this position.
   *
   * @returns The x or horizontal position as reported in the space in which this key was made.
   */
  public BigDecimal getCenterX() {
    return getGeometry().orElseThrow().getCenterX();
  }

  /**
   * Get the vertical center of this position.
   *
   * @returns The y or vertical position as reported in the space in which this key was made.
   */
  public BigDecimal getCenterY() {
    return getGeometry().orElseThrow().getCenterY();
  }

  public Optional<EngineGeometry> getGeometry() {
    return geometry;
  }

  public String getEntityName() {
    return entityName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof GeoKey)) {
      return false;
    }
    GeoKey other = (GeoKey) o;

    // Compare entities by their geometry
    EngineGeometry thisGeom = getGeometry().orElse(null);
    EngineGeometry otherGeom = other.getGeometry().orElse(null);

    if (thisGeom == null || otherGeom == null) {
      return false;
    }

    return thisGeom.getOnGrid().equals(otherGeom.getOnGrid());
  }

  @Override
  public int hashCode() {
    EngineGeometry geom = getGeometry().orElse(null);
    if (geom == null) {
      return 0;
    }
    return Objects.hash(geom.getOnGrid());
  }

  @Override
  public String toString() {
    EngineGeometry geometry = getGeometry().orElseThrow();
    return String.format(
        "Entity of type %s at (%.6f, %.6f)",
        getEntityName(),
        geometry.getCenterX(),
        geometry.getCenterY()
    );
  }
}
