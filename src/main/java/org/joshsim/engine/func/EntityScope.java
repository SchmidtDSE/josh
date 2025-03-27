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
import org.joshsim.engine.value.EngineValue;


/**
 * Simple scope which contains an entity.
 */
public class EntityScope implements Scope {

  private final Entity value;
  private final Set<String> expectedAttrs;

  /**
   * Create a scope decorator around this entity.
   *
   * @param value EngineValue to use for current.
   */
  public EntityScope(Entity value) {
    this.value = value;
    this.expectedAttrs = getAttributes(value);
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

  /**
   * Extract all attribute names from an entity's event handlers.
   *
   * @param target the Entity from which to extract attribute names.
   * @return Set of attribute names found in the entity's event handlers.
   */
  private Set<String> getAttributes(Entity target) {
    return StreamSupport.stream(target.getEventHandlers().values().spliterator(), false)
      .flatMap(group -> StreamSupport.stream(group.getEventHandlers().spliterator(), false))
      .map(handler -> handler.getAttributeName())
      .collect(Collectors.toSet());
  }
}
