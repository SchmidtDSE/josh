/**
 * Base entity for a mutable entity using a re-entrant lock.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.entity.base;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
  private final Lock lock;

  private Map<String, EngineValue> attributes;
  private Map<String, EngineValue> priorAttributes;
  private Set<String> onlyOnPrior;

  private Optional<String> substep;
  private Set<String> attributeNames;

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

    if (eventHandlerGroups == null) {
      this.eventHandlerGroups = new HashMap<>();
    } else {
      this.eventHandlerGroups = new HashMap<>(eventHandlerGroups);
    }

    if (attributes == null) {
      this.attributes = new HashMap<>();
    } else {
      this.attributes = new HashMap<>(attributes);
    }

    lock = new ReentrantLock();
    substep = Optional.empty();
    priorAttributes = new HashMap<>();
    onlyOnPrior = new HashSet<>();

    attributeNames = computeAttributeNames();
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
    if (attributes.containsKey(name)) {
      return Optional.of(attributes.get(name));
    } else {
      return Optional.ofNullable(priorAttributes.get(name));
    }
  }

  @Override
  public void setAttributeValue(String name, EngineValue value) {
    onlyOnPrior.remove(name);
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
    for (String key : onlyOnPrior) {
      attributes.put(key, priorAttributes.get(key));
    }

    priorAttributes = attributes;
    attributes = new HashMap<>();

    onlyOnPrior = new HashSet<>(priorAttributes.keySet());

    return new FrozenEntity(
        getEntityType(),
        name,
        priorAttributes,
        getGeometry()
    );
  }

  @Override
  public Set<String> getAttributeNames() {
    return attributeNames;
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

  /**
   * Determine unique attribute names.
   *
   * @return Set of unique attribute names.
   */
  private Set<String> computeAttributeNames() {
    return StreamSupport.stream(getEventHandlers().spliterator(), false)
        .flatMap(group -> StreamSupport.stream(group.getEventHandlers().spliterator(), false))
        .map(EventHandler::getAttributeName)
        .collect(Collectors.toSet());
  }

}
