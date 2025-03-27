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
  private final Optional<String> state;
  private final String attribute;
  private final String event;

  /**
   * Constructor for an EventHandlerGroup.
   */
  public EventHandlerGroup(
      Iterable<EventHandler> eventHandlers,
      Optional<String> state, 
      String attribute,
      String event
  ) {
    this.eventHandlers = eventHandlers;
    this.state = state;
    this.attribute = attribute;
    this.event = event;
  }

  /**
   * Get all event handlers in this group.
   *
   * @return an Iterable of EventHandler objects
   */
  Iterable<EventHandler> getEventHandlers() {
    return eventHandlers;
  }

  /**
   * Get the state associated with this event handler group.
   *
   * @return an Optional containing the state String if one exists, empty otherwise
   */
  Optional<String> getState() {
    return state;
  }

  /**
   * Get the attribute associated with this event handler group.
   *
   * @return the attribute String
   */
  String getAttribute() {
    return attribute;
  }

  /**
   * Get the event associated with this event handler group.
   *
   * @return the event String
   */
  String getEvent() {
    return event;
  }
}
