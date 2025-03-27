/**
 * Base entity which is a mutable entity.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.entity;

import java.util.HashMap;
import java.util.Optional;
import org.joshsim.engine.value.EngineValue;

/**
 * Represents a base entity that is mutable.
 * This class provides mechanisms for managing attributes and event handlers,
 * and supports locking to be thread-safe.
 */
public abstract class Entity implements Lockable, AttributeContainer {
  String name;
  HashMap<EventKey, EventHandlerGroup> eventHandlerGroups;
  HashMap<String, EngineValue> attributes;
  boolean isLocked;

  /**
   * Constructor for Entity.
   *
   * @param name Name of the entity.
   * @param eventHandlerGroups A map of event keys to their corresponding EventHandlerGroups.
   * @param attributes A map of attribute names to their corresponding EngineValues.
   */
  public Entity(
      String name,
      HashMap<EventKey, EventHandlerGroup> eventHandlerGroups,
      HashMap<String, EngineValue> attributes
  ) {
    this.name = name;
    this.eventHandlerGroups = eventHandlerGroups != null 
        ? eventHandlerGroups : new HashMap<>();
    this.attributes = attributes != null
        ? attributes : new HashMap<>();
    isLocked = false;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public HashMap<EventKey, EventHandlerGroup> getEventHandlers() {
    return eventHandlerGroups;
  }

  @Override
  public Optional<EventHandlerGroup> getEventHandlers(EventKey eventKey) {
    return Optional.of(eventHandlerGroups.get(eventKey));
  }

  @Override
  public Optional<EngineValue> getAttributeValue(String name) {
    return Optional.ofNullable(attributes.get(name));
  }

  @Override
  public void setAttributeValue(String name, EngineValue value) {
    if (isLocked) {
      throw new IllegalStateException("Entity is locked");
    }
    isLocked = true;
    attributes.put(name, value);
    isLocked = false;
  }

  @Override
  public void lock() {
    isLocked = true;
  }

  @Override
  public void unlock() {
    isLocked = false;
  }
  
}
