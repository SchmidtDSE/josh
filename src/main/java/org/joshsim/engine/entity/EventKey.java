/**
 * Key class for mapping state x attribute x event to an EventHandlerGroup.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.entity;

import java.util.Objects;

/**
 * Represents a composite key for mapping state, attribute, and event
 * to an EventHandlerGroup.
 */
public class EventKey {
  private final String state;
  private final String attribute;
  private final String event;
  

  /**
   * Composite key class for mapping state x attribute x event to an EventHandlerGroup.
   *
   * @param state state string
   * @param attribute attribute string
   * @param event event string
   */
  public EventKey(String state, String attribute, String event) {
    this.state = state;
    this.attribute = attribute;
    this.event = event;
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
    return Objects.hash(state, attribute, event);
  }
}