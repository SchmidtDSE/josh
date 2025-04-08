/**
 * Base entity for a mutable entity using a re-entrant lock.
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
 * Represents a base entity that is mutable with a re-entrant lock.
 */
public abstract class DirectLockMutableEntity implements MutableEntity {

  private final String name;
  private final Map<EventKey, EventHandlerGroup> eventHandlerGroups;
  private final Map<String, EngineValue> attributes;
  private final Lock lock;
  
  private Optional<String> substep;

  /**
   * Constructor for Entity.
   *
   * @param name Name of the entity.
   * @param eventHandlerGroups A map of event keys to their corresponding EventHandlerGroups.
   * @param attributes A map of attribute names to their corresponding EngineValues.
   */
  public DirectLockMutableEntity(
      String name,
      Map<EventKey, EventHandlerGroup> eventHandlerGroups,
      Map<String, EngineValue> attributes
  ) {
    this.name = name;
    this.eventHandlerGroups = new HashMap<>(eventHandlerGroups);
    this.attributes = new HashMap<>(attributes);
    lock = new ReentrantLock();
    substep = Optional.empty();
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Iterable<EventHandlerGroup> getEventHandlers() {
    return eventHandlerGroups.values();
  }

  @Override
  public Optional<EventHandlerGroup> getEventHandlers(EventKey eventKey) {
    return Optional.ofNullable(eventHandlerGroups.get(eventKey));
  }

  @Override
  public Optional<EngineValue> getAttributeValue(String name) {
    return Optional.ofNullable(attributes.get(name));
  }

  @Override
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

  @Override
  public void startSubstep(String name) {
    if (substep.isPresent()) {
      String message = String.format(
          "Cannot start %s before %s is completed.",
          substep.get(),
          name
      );
      throw new IllegalStateException(message);
    }

    lock();
    substep = Optional.of(name);
  }

  @Override
  public void endSubstep() {
    substep = Optional.empty();
    unlock();
  }

  public Optional<String> getSubstep() {
    return substep;
  }

}
