/**
 * Structures to describe a scope which contains only current.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.func;

import org.joshsim.engine.value.type.EngineValue;


/**
 * Simple scope which contains only a single current EngineValue.
 */
public class SingleValueScope implements Scope {

  private final EngineValue value;

  /**
   * Create a scope containing only current.
   *
   * @param value EngineValue to use for current.
   */
  public SingleValueScope(EngineValue value) {
    this.value = value;
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

}
