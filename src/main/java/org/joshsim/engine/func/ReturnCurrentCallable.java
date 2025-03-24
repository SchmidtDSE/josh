
/**
 * Callable that returns the current value from scope.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.func;

import org.joshsim.engine.value.EngineValue;

/**
 * A callable that returns the "current" value from the scope.
 */
public class ReturnCurrentCallable implements CompiledCallable {

  @Override
  public EngineValue evaluate(Scope scope) {
    return scope.getValue("current");
  }
}
