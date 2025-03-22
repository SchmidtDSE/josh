/**
 * Structures for a group of related event handlers.
 *
 * @license BSD-3-Clause
 */
package org.joshsim.engine.entity;

import java.util.Optional;

/**
 * Interface representing a group of event handlers.
 *
 * <p>A set of event handlers where one may execute conditional on another not executing like in an
 * if, else if, else relationship established through selectors.
 * </p>
 */
public interface EventHandlerGroup {
  /**
   * Get all event handlers in this group.
   *
   * @return an Iterable of EventHandler objects
   */
  Iterable<EventHandler> getEventHandlers();

  /**
   * Get the state associated with this event handler group.
   *
   * @return an Optional containing the state String if one exists, empty otherwise
   */
  Optional<String> getState();
}
