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
 *
 * <p><strong>IMPORTANT DESIGN DECISION: equals/hashCode Contract Violation</strong></p>
 *
 * <p>This class intentionally includes {@code sequenceId} in its state but EXCLUDES it from
 * {@code equals()} and {@code hashCode()}. This violates the standard equals/hashCode contract
 * where equal objects must have equal hash codes and all significant fields should participate
 * in equality comparison.</p>
 *
 * <p><strong>Rationale:</strong></p>
 * <ul>
 *   <li>Preserves location-based equality for {@code Map<GeoKey, Entity>} spatial lookups</li>
 *   <li>Multiple entities at the same location share the same GeoKey for map operations</li>
 *   <li>Sequence ID is display/debug metadata only, not part of simulation identity</li>
 *   <li>External data mapping works correctly (all entities at location share same data)</li>
 * </ul>
 *
 * <p><strong>Safety Guarantees:</strong></p>
 * <ul>
 *   <li>No {@code Set<GeoKey>} usage in codebase (verified - would break with this design)</li>
 *   <li>{@code Map<GeoKey, Entity>} semantics preserved (one entry per location)</li>
 *   <li>External data mapping semantics preserved (location-based lookups)</li>
 * </ul>
 *
 * <p><strong>toString() includes hash:</strong> The string representation includes
 * a 6-character hash derived from the sequence ID, entity name, and location for debugging
 * and logging purposes, displayed as "[hash - Name @ (x, y)]".</p>
 *
 * <p>This design choice was made after careful consideration of the codebase's specific needs
 * where location-based identity is essential for simulation correctness, while sequence IDs
 * serve only to distinguish entities in debugging output.</p>
 */
public class GeoKey {

  private final Optional<EngineGeometry> geometry;
  private final String entityName;
  private final long sequenceId;

  /**
   * Craete a new key with the specified entity.
   *
   * @param entity The patch to be keyed.
   */
  public GeoKey(Entity entity) {
    geometry = entity.getGeometry();
    entityName = entity.getName();
    sequenceId = entity.getSequenceId();
  }

  /**
   * Create a new key with the given properties (backward compatible, sequence defaults to 0).
   *
   * @param geometry The geometry of the entity represented by this key or empty if this entity
   *     does not have geometry.
   * @param entityName The name of this type of entity.
   */
  public GeoKey(Optional<EngineGeometry> geometry, String entityName) {
    this(geometry, entityName, 0L);
  }

  /**
   * Create a new key with the given properties including sequence ID.
   *
   * @param geometry The geometry of the entity represented by this key or empty if this entity
   *     does not have geometry.
   * @param entityName The name of this type of entity.
   * @param sequenceId The sequence ID distinguishing this entity from others at the same location.
   */
  public GeoKey(Optional<EngineGeometry> geometry, String entityName, long sequenceId) {
    this.geometry = geometry;
    this.entityName = entityName;
    this.sequenceId = sequenceId;
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

  /**
   * Get the sequence ID for this entity.
   *
   * @return The sequence ID that distinguishes this entity from others at the same location.
   */
  public long getSequenceId() {
    return sequenceId;
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

    // Generate a short hash for unique identification
    // Combine sequenceId, entity name, and location for uniqueness
    String hashInput = String.format("%d-%s-%.6f-%.6f",
        sequenceId,
        getEntityName(),
        geometry.getCenterX(),
        geometry.getCenterY()
    );
    int hashCode = hashInput.hashCode();
    String shortHash = String.format("%06x", hashCode & 0xFFFFFF).substring(0, 6);

    return String.format(
        "[%s - %s @ (%.6f, %.6f)]",
        shortHash,
        getEntityName(),
        geometry.getCenterX(),
        geometry.getCenterY()
    );
  }
}
