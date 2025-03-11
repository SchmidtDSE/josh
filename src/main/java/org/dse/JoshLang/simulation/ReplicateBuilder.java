/**
 * @license BSD-3-Clause
 */
package org.dse.JoshLang.simulation;

import org.dse.JoshLang.entities.meta.EntityBuilder;

/**
 * Interface for building Replicate objects.
 * Provides methods to add entities and finalize the building process.
 */
public interface ReplicateBuilder {
    /**
     * Adds an entity builder to the replicate being constructed.
     *
     * @param entity the entity builder to add
     * @return this builder for method chaining
     */
    ReplicateBuilder addEntity(EntityBuilder entity);

    /**
     * Finalizes the building process and returns the constructed Replicate.
     *
     * @return the constructed Replicate object
     */
    Replicate step();
}