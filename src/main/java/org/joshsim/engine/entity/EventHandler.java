/**
 * Simulation event callabacks.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.entity;

import java.util.Optional;
import org.joshsim.engine.func.CompiledSelector;


/**
 * Class for an event handler which offers a callback for an event.
 */
public class EventHandler {
  String attributeName;
  String eventName;
  Optional<CompiledSelector> conditional;

  /**
   * Create a new event handler.
   *
   * @param attributeName the name of the attribute associated with this event handler
   * @param eventName the name of the event this handler responds to
   * @param conditional the conditional selector associated with this event handler
   */
  public EventHandler(
      String attributeName,
      String eventName,
      Optional<CompiledSelector> conditional
  ) {
    this.attributeName = attributeName;
    this.eventName = eventName;
    this.conditional = conditional;
  }

  /**
   * Get the name of the attribute associated with this event handler.
   *
   * @return the attribute name
   */
  public String getAttributeName() {
    return attributeName;
  }

  /**
   * Get the name of the event this handler responds to.
   *
   * @return the event name
   */
  public String getEventName() {
    return eventName;
  }

  /**
   * Get the conditional selector associated with this event handler.
   *
   * @return an Optional containing the CompiledSelector if one exists, empty otherwise
   */
  public Optional<CompiledSelector> getConditional() {
    return conditional;
  }
}
