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
public class EventHandlerGroup {
  private final Iterable<EventHandler> eventHandlers;
  private final EventKey eventKey;

  /**
   * Constructor for an EventHandlerGroup.
   */
  public EventHandlerGroup(
      Iterable<EventHandler> eventHandlers,
      EventKey eventKey
  ) {
    this.eventHandlers = eventHandlers;
    this.eventKey = eventKey;
  }

  /**
   * Get all event handlers in this group.
   *
   * @return an Iterable of EventHandler objects
   */
  public Iterable<EventHandler> getEventHandlers() {
    return eventHandlers;
  }


  /**
   * Get the event key associated with this group of event handlers.
   *
   * @return the EventKey Identifying the event for which this event handler group should be
   *     evaluated.
   */
  public EventKey getEventKey() {
    return this.eventKey;
  }
}
