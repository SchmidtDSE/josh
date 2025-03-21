/**
 * Entity which can operate within a JoshSim.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.entity;

import java.util.Optional;
import org.joshsim.engine.value.EngineValue;


/**
 * Base entity in system which has mutable attributes.
 */
public interface Entity {

  /**
   * Get the name of this tyep of entity.
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

  /**
   * Acquire a global lock on this entity for thread safety.
   * 
   * <p>This is a convenience method for client code and is not automatically  enforced by getters
   * and setters. The method will block until the lock is acquired.</p>
   */
  void lock();

  /**
   * Release the global lock on this entity.
   * 
   * <p>This is a convenience method for client code and should be called after thread-safe
   * operations are complete.</p>
   */
  void unlock();
}
