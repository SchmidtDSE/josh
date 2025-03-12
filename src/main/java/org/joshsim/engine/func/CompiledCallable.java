/**
 * Structures to describe pre-compiled logic.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.func;

import org.joshsim.engine.value.EngineValue;


/**
 * An invocable piece of pre-compiled code.
 * 
 * <p>Represents a compiled callable that can be evaluated to produce a value. This is the
 * foundation for functions, operators, and expressions in the Josh language and simulation engine.
 * These may be created through Josh DSL code.
 * </p>
 */
public interface CompiledCallable {
  /**
   * Evaluates this callable using the provided scope.
   *
   * @param scope the execution scope providing context for evaluation
   * @return the resulting value from evaluating this callable
   */
  EngineValue evaluate(Scope scope);
}