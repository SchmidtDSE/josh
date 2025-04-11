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
import org.joshsim.engine.entity.base.Entity;
import org.joshsim.engine.value.type.EngineValue;


/**
 * Simple scope which contains an entity.
 */
public class EntityScope implements Scope {

  private final Entity value;
  private final Set<String> expectedAttrs;

  /**
   * Create a scope decorator around this entity.
   *
   * @param value EngineValue to use for the root.
   */
  public EntityScope(Entity value) {
    this.value = value;
    this.expectedAttrs = value.getAttributeNames();
  }

  @Override
  public EngineValue get(String name) {
    Optional<EngineValue> attrValue = value.getAttributeValue(name);
    if (attrValue.isEmpty()) {
      String message = String.format("Cannot find %s on %s. May be uninitalized.", name, value);
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
  public Set<String> getAttributes() {
    return expectedAttrs;
  }

}
