/**
 * Key class for mapping state x attribute x event to an EventHandlerGroup.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.entity.handler;

import java.util.Objects;

/**
 * Composite key for mapping state, attribute, and event.
 */
public class EventKey {
  private final String state;
  private final String attribute;
  private final String event;

  /**
   * Key for mapping attribute x event to an EventHandlerGroup using empty default state.
   *
   * @param attribute attribute string
   * @param event event string
   */
  public EventKey(String attribute, String event) {
    this.state = "";
    this.attribute = attribute;
    this.event = event;
  }

  /**
   * Composite key class for mapping state x attribute x event to an EventHandlerGroup.
   *
   * @param state state string or empty if default state
   * @param attribute attribute string
   * @param event event string
   */
  public EventKey(String state, String attribute, String event) {
    this.state = state;
    this.attribute = attribute;
    this.event = event;
  }

  /**
   * Gets the state component of this event key.
   *
   * @return the state string, or empty if default state
   */
  public String getState() {
    return state;
  }

  /**
   * Gets the attribute component of this event key.
   *
   * @return the attribute string
   */
  public String getAttribute() {
    return attribute;
  }

  /**
   * Gets the event component of this event key.
   *
   * @return the event string
   */
  public String getEvent() {
    return event;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    EventKey eventKey = (EventKey) o;
    return Objects.equals(state, eventKey.state)
          && Objects.equals(attribute, eventKey.attribute)
          && Objects.equals(event, eventKey.event);
  }

  @Override
  public int hashCode() {
    return toString().hashCode();
  }

  @Override
  public String toString() {
    return String.format("EventKey(%s, %s, %s)", state, attribute, event);
  }
}
