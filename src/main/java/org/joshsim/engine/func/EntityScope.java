/**
 * Structures to describe a scope which contains an entity.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.func;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.joshsim.engine.entity.Entity;
import org.joshsim.engine.entity.EventHandler;
import org.joshsim.engine.entity.MutableEntity;
import org.joshsim.engine.entity.prototype.EmptyEntityPrototypeStore;
import org.joshsim.engine.entity.prototype.EntityPrototypeStore;
import org.joshsim.engine.value.Converter;
import org.joshsim.engine.value.EmptyConverter;
import org.joshsim.engine.value.EngineValue;


/**
 * Simple scope which contains an entity.
 */
public class EntityScope implements Scope {

  private final Entity value;
  private final Set<String> expectedAttrs;
  private final Converter converter;
  private final EntityPrototypeStore prototypeStore;

  /**
   * Create a scope decorator around this entity.
   *
   * @param value EngineValue to use for current.
   */
  public EntityScope(Entity value) {
    this.value = value;
    this.expectedAttrs = getAttributes(value);

    converter = new EmptyConverter();
    prototypeStore = new EmptyEntityPrototypeStore();
  }

  /**
   * Create a scope decorator around this entity with context.
   *
   * @param value EngineValue to use for current.
   */
  public EntityScope(MutableEntity value, Converter converter, EntityPrototypeStore prototypeStore) {
    this.value = value;
    this.expectedAttrs = getAttributes(value);
    this.converter = converter;
    this.prototypeStore = prototypeStore;
  }

  @Override
  public EngineValue get(String name) {
    Optional<EngineValue> attrValue = value.getAttributeValue(name);
    if (attrValue.isEmpty()) {
      String message = String.format("Cannot find %s on this entity. May be uninitalized.", name);
      throw new IllegalArgumentException(message);
    } else {
      return attrValue.get();
    }
  }

  @Override
  public boolean has(String name) {
    return expectedAttrs.contains(name);
  }


  @Override
  public Iterable<String> getAttributes() {
    return expectedAttrs;
  }

  @Override
  public Converter getConverter() {
    return converter;
  }

  @Override
  public EntityPrototypeStore getPrototypeStore() {
    return prototypeStore;
  }

  /**
   * Extract all attribute names from an entity's event handlers or set attributes.
   *
   * @param target the Entity from which to extract attribute names.
   * @return Set of attribute names found in the entity's event handlers or set attributes.
   */
  private Set<String> getAttributes(Entity target) {
    return StreamSupport
        .stream(value.getAttributeNames().spliterator(), false)
        .collect(Collectors.toSet());
  }
}
