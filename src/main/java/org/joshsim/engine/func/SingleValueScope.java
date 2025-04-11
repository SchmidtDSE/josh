/**
 * Structures to describe a scope which contains only current.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.func;

import org.joshsim.engine.value.type.EngineValue;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;


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
  public Set<String> getAttributes() {
    return Set.of("current");
  }

}
