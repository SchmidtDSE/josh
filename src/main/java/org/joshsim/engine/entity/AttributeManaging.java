/**
 * Interface that defines an entity that can manage attributes, including event handling callbacks.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.entity;

import java.util.HashMap;
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
   * @returns Hashmap of (state x attribute x event) to EventHandlerGroups.
   */
  HashMap<EventKey, EventHandlerGroup> getEventHandlers();

  /**
   * Get event handlers for a specific attribute and event.
   *
   * @param attribute the attribute name
   * @param event the event name
   * @return the event handler group, or empty if it does not exist
   */
  Optional<EventHandlerGroup> getEventHandlers(
      String state,
      String attribute,
      String event
  );

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
