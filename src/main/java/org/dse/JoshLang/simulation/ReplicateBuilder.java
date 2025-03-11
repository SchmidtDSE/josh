/**
 * @license BSD-3-Clause
 */
package org.dse.JoshLang.simulation;

import java.util.Optional;

import org.dse.JoshLang.entities.meta.Entity;
import org.dse.JoshLang.geometry.Geometry;

/**
 * Interface representing a full simulation replicate.
 * Provides methods to access time steps and query entities across time steps.
 */
public interface Replicate {
    /**
     * Adds a time step to this replicate.
     *
     * @param timeStep the time step to add
     */
    void addTimeStep(TimeStep timeStep);

    /**
     * Gets a time step by its step number.
     *
     * @param stepNumber the step number to retrieve
     * @return an Optional containing the time step if found, or empty if not found
     */
    Optional<TimeStep> getTimeStep(int stepNumber);

    /**
     * Queries entities across time steps based on the provided query.
     *
     * @param query the query defining spatial and temporal bounds
     * @return an iterable of matching entities
     */
    Iterable<Entity> query(Query query);
}