/**
 * @license BSD-3-Clause
 */
package org.joshsim.engine.entity;

import org.joshsim.engine.value.Geometry;


/**
 * Represents an entity with spatial properties in the system.
 * This interface defines methods for accessing location information.
 */
public interface SpatialEntity extends Entity {
    /**
     * Gets the geographic location of this spatial entity.
     *
     * @return the geographic point representing this entity's location
     */
    Geometry getGeometry();
}