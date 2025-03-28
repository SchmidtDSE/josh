/**
 * Simulation event callabacks.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.entity;

import java.util.Optional;

import org.joshsim.engine.func.CompiledCallable;
import org.joshsim.engine.func.CompiledSelector;


/**
 * Class for an event handler which offers a callback for an event.
 */
public class EventHandler {
  CompiledCallable callable;
  String attributeName;
  String eventName;
  Optional<CompiledSelector> conditional;

  /**
   * Create a new event handler without a conditional.
   *
   * @param callable The callable to execute on this event.
   * @param attributeName The name of the attribute associated with this event handler.
   * @param eventName The name of the event that this handler responds to.
   */
  public EventHandler(
      CompiledCallable callable,
      String attributeName,
      String eventName
  ) {
    this.callable = callable;
    this.attributeName = attributeName;
    this.eventName = eventName;
    this.conditional = Optional.empty();
  }

  /**
   * Create a new event handler with a conditional
   *
   * @param callable The callable to execute on this event.
   * @param attributeName The name of the attribute associated with this event handler.
   * @param eventName The name of the event this handler responds to.
   * @param conditional The conditional selector associated with this event handler.
   */
  public EventHandler(
      CompiledCallable callable,
      String attributeName,
      String eventName,
      CompiledSelector conditional
  ) {
    this.callable = callable;
    this.attributeName = attributeName;
    this.eventName = eventName;
    this.conditional = Optional.of(conditional);
  }

  /**
   * Get the callable to execute on this event.
   *
   * @return Callable to execute.
   */
  public CompiledCallable getCallable() {
    return callable;
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
