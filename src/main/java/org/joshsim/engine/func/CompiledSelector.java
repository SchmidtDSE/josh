/**
 * Structures describing precompiled selectors.
 * 
 * @license BSD-3-Clause
 */

package org.joshsim.engine.func;


/**
 * Pre-compiled selector which is similar to a CompiledCallable but can only evaluate to a boolean.
 *
 * <p>Pre-compiled selector that can evaluate to a boolean value typically used for conditional
 * expressions or filters. This may be created through Josh DSL code.
 * </p>
 */
public interface CompiledSelector {
  /**
   * Evaluates this selector using the provided scope.
   *
   * @param scope the execution scope providing context for evaluation
   * @return true if the selector condition is met, false otherwise
   */
  boolean evaluate(Scope scope);
}