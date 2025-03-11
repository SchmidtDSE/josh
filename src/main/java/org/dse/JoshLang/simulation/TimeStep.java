/**
 * @license BSD-3-Clause
 */
package org.dse.JoshLang.core.replicate;

import org.dse.JoshLang.entity.Entity;
import org.dse.JoshLang.geometry.Geometry;

/**
 * Interface representing a discrete time step within a simulation.
 * Provides methods to retrieve entities at a specific point in time.
 */
public interface TimeStep {
    /**
     * Gets the time step number.
     *
     * @return the integer time step number
     */
    int getTimeStep();

    /**
     * Gets entities within the specified geometry at this time step.
     *
     * @param geometry the spatial bounds to query
     * @return an iterable of entities within the geometry
     */
    Iterable<Entity> getEntities(Geometry geometry);

    /**
     * Gets entities with the specified name within the geometry at this time step.
     *
     * @param geometry the spatial bounds to query
     * @param name the entity name to filter by
     * @return an iterable of matching entities
     */
    Iterable<Entity> getEntities(Geometry geometry, String name);

    /**
     * Gets all entities at this time step.
     *
     * @return an iterable of all entities
     */
    Iterable<Entity> getEntities();
}