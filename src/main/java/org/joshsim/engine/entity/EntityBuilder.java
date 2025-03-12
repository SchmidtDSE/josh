/**
 * Convienence functions to create entities.
 * 
 * @license BSD-3-Clause
 */

package org.joshsim.engine.entity;


/**
 * Builder to assist in constructing entities.
 * 
 * <p>Builder for creating Entity instances, providing methods to add event handlers and build the
 * final entity.
 * </p>
 */
public interface EntityBuilder {
  /**
   * Add event handlers to the entity being built.
   *
   * @param group the event handler group to add
   * @return this builder for method chaining
   */
  EntityBuilder addEventHandlerGroup(EventHandlerGroup group);
  
  /**
   * Build and returns an Entity based on the added event handlers.
   *
   * @return a new Entity instance
   */
  Entity build();
}