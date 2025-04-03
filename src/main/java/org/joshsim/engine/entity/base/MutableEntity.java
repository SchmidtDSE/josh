/**
 * Base entity which is a mutable entity.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.entity.base;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.joshsim.engine.entity.handler.EventHandler;
import org.joshsim.engine.entity.handler.EventHandlerGroup;
import org.joshsim.engine.entity.handler.EventKey;
import org.joshsim.engine.value.type.EngineValue;


/**
 * Represents a base entity that is mutable.
 * This class provides mechanisms for managing attributes and event handlers,
 * and supports locking to be thread-safe.
 */
public abstract class MutableEntity implements Entity, Lockable {

  private final String name;
  private final Map<EventKey, EventHandlerGroup> eventHandlerGroups;
  private final Map<String, EngineValue> attributes;
  private final Lock lock;

  /**
   * Constructor for Entity.
   *
   * @param name Name of the entity.
   * @param eventHandlerGroups A map of event keys to their corresponding EventHandlerGroups.
   * @param attributes A map of attribute names to their corresponding EngineValues.
   */
  public MutableEntity(
      String name,
      Map<EventKey, EventHandlerGroup> eventHandlerGroups,
      Map<String, EngineValue> attributes
  ) {
    this.name = name;
    this.eventHandlerGroups = eventHandlerGroups != null
        ? eventHandlerGroups : new HashMap<>();
    this.attributes = attributes != null
        ? attributes : new HashMap<>();
    lock = new ReentrantLock();
  }

  @Override
  public String getName() {
    return name;
  }

  /**
   * Get event handlers for all attributes and events.
   *
   * @returns Hashmap of (state x attribute x event) to EventHandlerGroups.
   */
  public Iterable<EventHandlerGroup> getEventHandlers() {
    return eventHandlerGroups.values();
  }

  /**
   * Get event handlers for a specific attribute and event.
   *
   * @param eventKey The event for which handler groups should be returned.
   * @return the event handler group, or empty if it does not exist
   */
  public Optional<EventHandlerGroup> getEventHandlers(EventKey eventKey) {
    return Optional.ofNullable(eventHandlerGroups.get(eventKey));
  }

  @Override
  public Optional<EngineValue> getAttributeValue(String name) {
    return Optional.ofNullable(attributes.get(name));
  }

  /**
   * Set the value of an attribute by name.
   *
   * @param name the attribute name
   * @param value the value to set
   */
  public void setAttributeValue(String name, EngineValue value) {
    attributes.put(name, value);
  }

  @Override
  public void lock() {
    lock.lock();
  }

  @Override
  public void unlock() {
    lock.unlock();
  }

  @Override
  public Entity freeze() {
    Map<String, EngineValue> frozenAttributes = new HashMap<>();
    
    for (String attributeName : attributes.keySet()) {
      EngineValue unfrozenValue = attributes.get(attributeName);
      EngineValue frozenValue = unfrozenValue.freeze();
      frozenAttributes.put(attributeName, frozenValue);
    }
    
    return new FrozenEntity(
        getEntityType(),
        name,
        frozenAttributes,
        getGeometry()
    );
  }

  @Override
  public Iterable<String> getAttributeNames() {
    return StreamSupport.stream(getEventHandlers().spliterator(), false)
        .flatMap(group -> StreamSupport.stream(group.getEventHandlers().spliterator(), false))
        .map(EventHandler::getAttributeName)
        .collect(Collectors.toSet());
  }

  @Override
  public Optional<GeoKey> getKey() {
    if (getGeometry().isEmpty()) {
      return Optional.empty();
    } else {
      return Optional.of(new GeoKey(this));
    }
  }

}
