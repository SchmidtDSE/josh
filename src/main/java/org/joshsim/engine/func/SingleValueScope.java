/**
 * Structures to describe a scope which contains only current.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.func;

import org.joshsim.engine.entity.prototype.EmptyEntityPrototypeStore;
import org.joshsim.engine.entity.prototype.EntityPrototypeStore;
import org.joshsim.engine.value.Converter;
import org.joshsim.engine.value.EmptyConverter;
import org.joshsim.engine.value.EngineValue;


/**
 * Simple scope which contains only a single current EngineValue.
 */
public class SingleValueScope implements Scope {

  private final EngineValue value;
  private final Converter converter;
  private final EntityPrototypeStore prototypeStore;

  /**
   * Create a scope containing only current.
   *
   * @param value EngineValue to use for current.
   */
  public SingleValueScope(EngineValue value) {
    this.value = value;

    converter = new EmptyConverter();
    prototypeStore = new EmptyEntityPrototypeStore();
  }

  /**
   * Create a scope containing only current with access to conversion and other entity prototypes.
   *
   * @param value EngineValue to use for current.
   * @param converter Converter to use to convert between values in operations on variables in this scope.
   */
  public SingleValueScope(EngineValue value, Converter converter, EntityPrototypeStore prototypeStore) {
    this.value = value;
    this.converter = converter;
    this.prototypeStore = prototypeStore;
  }

  @Override
  public EngineValue get(String name) {
    if (!has(name)) {
      throw new IllegalArgumentException("Single value scope only has current.");
    }

    return value;
  }

  @Override
  public boolean has(String name) {
    return "current".equals(name);
  }

  @Override
  public Iterable<String> getAttributes() {
    return java.util.Collections.singletonList("current");
  }

  @Override
  public Converter getConverter() {
    return converter;
  }

  @Override
  public EntityPrototypeStore getPrototypeStore() {
    return prototypeStore;
  }

}
