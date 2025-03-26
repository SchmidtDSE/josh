/**
 * Interface that defines an entity that can manage attributes, including event handling callbacks.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.entity;

import java.util.Optional;
import org.joshsim.engine.value.EngineValue;

/**
 * Interface that defines an entity behavior that can manage attributes, including event handling
 * callbacks.
 */
public interface AttributeManaging {

  /**
   * Get the name of this type of entity.
   *
   * @returns unique name of this entity type.
   */
  String getName();

  /**
   * Get event handlers for all attributes and events.
   *
   * @returns Iterable over all registered event handler groups.
   */
  Iterable<EventHandlerGroup> getEventHandlers();

  /**
   * Get event handlers for a specific attribute and event.
   *
   * @param attribute the attribute name
   * @param event the event name
   * @return an iterable collection of event handler groups
   */
  Iterable<EventHandlerGroup> getEventHandlers(String attribute, String event);

  /**
   * Get the value of an attribute by name.
   *
   * @param name the attribute name
   * @return an Optional containing the attribute value, or empty if not found
   */
  Optional<EngineValue> getAttributeValue(String name);

  /**
   * Set the value of an attribute by name.
   *
   * @param name the attribute name
   * @param value the value to set
   */
  void setAttributeValue(String name, EngineValue value);
}
