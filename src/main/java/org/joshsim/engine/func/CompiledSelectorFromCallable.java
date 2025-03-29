/**
 * Decorator around which allows use of CompiledCaller as a CompiledSelector.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.func;


/**
 * Decorator around which allows use of CompiledCaller as a CompiledSelector.
 */
public class CompiledSelectorFromCallable implements CompiledSelector {

  private final CompiledCallable inner;

  /**
   * Create a new decorator around a compiled caller.
   *
   * @param inner The CompiledCallable to adapt as a CompiledSelector.
   */
  public CompiledSelectorFromCallable(CompiledCallable inner) {
    this.inner = inner;
  }

  @Override
  public boolean evaluate(Scope scope) {
    return inner.evaluate(scope).getAsBoolean();
  }
}
