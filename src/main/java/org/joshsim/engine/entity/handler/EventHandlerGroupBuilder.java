/**
 * A Builder class for creating {@link EventHandlerGroup} instances.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.entity.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Builder class for creating {@link EventHandlerGroup} instances.
 *
 * <p>This builder manages collections of event handlers along with their associated state,
 * attribute, and event information. Use this class to construct properly configured
 * EventHandlerGroup objects.
 */
public class EventHandlerGroupBuilder {
  
  private List<EventHandler> eventHandlers = new ArrayList<>();
  private Optional<String> state = Optional.empty();
  private Optional<String> attribute = Optional.empty();
  private Optional<String> event = Optional.empty();

  /**
   * Builds and returns an EventHandlerGroup with the current configuration.
   *
   * @return a new EventHandlerGroup instance
   */
  public EventHandlerGroup build() {
    return new EventHandlerGroup(eventHandlers, buildKey());
  }

  /**
   * Build an EventKey using the current state, attribute, and event.
   *
   * <p>This method encapsulates the logic for generating an immutable EventKey from the provided
   * state, attribute, and event information of this builder.</p>
   *
   * @return A new EventKey instance which is immutable.
   * @throws IllegalStateException if any of state, attribute, or event have not been set.
   */
  public EventKey buildKey() {
    return new EventKey(getState(), getAttribute(), getEvent());
  }

  /**
   * Sets the state for this event handler group.
   *
   * @param state the state to set
   */
  public void setState(String state) {
    this.state = Optional.of(state);
  }

  /**
   * Gets the current state.
   *
   * @return the state
   * @throws IllegalStateException if the state has not been set
   */
  private String getState() {
    return state.orElseThrow(() -> new IllegalStateException("State not set"));
  }

  /**
   * Sets the attribute for this event handler group.
   *
   * @param attribute the attribute to set
   */
  public void setAttribute(String attribute) {
    this.attribute = Optional.of(attribute);
  }

  /**
   * Gets the attribute value.
   *
   * @return the attribute value
   * @throws IllegalStateException if the attribute has not been set
   */
  private String getAttribute() {
    return attribute.orElseThrow(() -> new IllegalStateException("Attribute not set"));
  }

  /**
   * Sets the event for this event handler group.
   *
   * @param event the event to set
   */
  public void setEvent(String event) {
    this.event = Optional.of(event);
  }

  /**
   * Gets the event value.
   *
   * @return the event value
   * @throws IllegalStateException if the event has not been set
   */
  private String getEvent() {
    return event.orElseThrow(() -> new IllegalStateException("Event not set"));
  }

  /**
   * Parse attributes of the given EventKey into this builder.
   *
   * @param eventKey The EventKey from which to parse attributes.
   */
  public void setEventKey(EventKey eventKey) {
    setEvent(eventKey.getEvent());
    setAttribute(eventKey.getAttribute());
    setState(eventKey.getState());
  }

  /**
   * Adds a single event handler to this group.
   *
   * @param handler the event handler to add
   */
  public void addEventHandler(EventHandler handler) {
    eventHandlers.add(handler);
  }

  /**
   * Adds multiple event handlers to this group.
   *
   * @param handlers the list of event handlers to add
   */
  public void addEventHandler(List<EventHandler> handlers) {
    eventHandlers.addAll(handlers);
  }

  /**
   * Clears all data from this builder, resetting it to its initial state.
   */
  public void clear() {
    eventHandlers.clear();
    state = Optional.empty();
    attribute = Optional.empty();
    event = Optional.empty();
  }
}
