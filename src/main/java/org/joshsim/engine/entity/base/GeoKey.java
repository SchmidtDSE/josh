/**
 * Structures describing keys for cells within a simulation across time steps.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.entity.base;

import java.math.BigDecimal;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import org.joshsim.engine.geometry.EngineGeometry;

/**
 * Represents a key to uniquely identify a Patch within a simulation across time steps.
 */
public class GeoKey {

  private final Optional<EngineGeometry> geometry;
  private final String entityName;

  private final long horizontalLong;
  private final long verticalLong;

  /**
   * Craete a new key with the specified entity.
   *
   * @param entity The patch to be keyed.
   */
  public GeoKey(Entity entity) {
    this(entity.getGeometry(), entity.getName());
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
    this.horizontalLong = cacheGeometryAsLongIfPresent(geometry, EngineGeometry::getCenterX);
    this.verticalLong = cacheGeometryAsLongIfPresent(geometry, EngineGeometry::getCenterY);
  }

  /**
   * Convert one center coordinate of a geometry to a long, or zero if there is no geometry.
   *
   * <p>The BigDecimal to long conversion is comparatively expensive and grid lookups run it once
   * per external read, so it is performed once per key here and the result kept.</p>
   *
   * <p>This runs during construction rather than lazily on first request because a key is cached
   * per entity and so shared across the threads processing patches in parallel. A lazily assigned
   * flag and value pair could be observed half written by another thread, which would yield a
   * silent zero coordinate and a read from the wrong grid cell. Converting here lets the fields
   * be final, which also makes a key safely published through the plain field that caches it.</p>
   *
   * @param geometry The geometry to read the coordinate from, or empty if the key has none.
   * @param getCenter Accessor for the coordinate of interest, such as the horizontal center.
   * @return The requested center coordinate as a long, or zero if there is no geometry. Callers
   *     must consult the geometry before reporting the zero, since it stands for absence rather
   *     than for a position.
   */
  private static long cacheGeometryAsLongIfPresent(
      Optional<EngineGeometry> geometry, Function<EngineGeometry, BigDecimal> getCenter) {
    if (geometry.isEmpty()) {
      return 0;
    }
    return getCenter.apply(geometry.get()).longValue();
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

  /**
   * Get the horizontal center of this position as a long.
   *
   * <p>Returns the result of the BigDecimal to long conversion which grid data lookups perform
   * on every external value read, computed once when this key was created.</p>
   *
   * @returns The x or horizontal position as a long.
   * @throws NoSuchElementException if this key has no geometry.
   */
  public long getHorizontalAsLong() {
    if (geometry.isEmpty()) {
      throw new NoSuchElementException("Cannot get horizontal position without geometry.");
    }
    return horizontalLong;
  }

  /**
   * Get the vertical center of this position as a long.
   *
   * <p>Returns the result of the BigDecimal to long conversion which grid data lookups perform
   * on every external value read, computed once when this key was created.</p>
   *
   * @returns The y or vertical position as a long.
   * @throws NoSuchElementException if this key has no geometry.
   */
  public long getVerticalAsLong() {
    if (geometry.isEmpty()) {
      throw new NoSuchElementException("Cannot get vertical position without geometry.");
    }
    return verticalLong;
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
