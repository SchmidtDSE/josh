/**
 * @license BSD-3-Clause
 */
package org.joshsim.engine.entity;


/**
 * Builder interface for creating Entity instances.
 * Provides methods to add event handlers and build the final entity.
 */
public interface EntityBuilder {
    /**
     * Adds event handlers to the entity being built.
     *
     * @param group the event handler group to add
     * @return this builder for method chaining
     */
    EntityBuilder addEventHandlers(EventHandlerGroup group);
    
    /**
     * Builds and returns an Entity based on the added event handlers.
     *
     * @return a new Entity instance
     */
    Entity build();
}