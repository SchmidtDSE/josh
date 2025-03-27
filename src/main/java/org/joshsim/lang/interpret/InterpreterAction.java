/**
 * Structures describing a compiled action which the interpreter can take.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret;


/**
 * Strategy to which is an interpreter action compiled to in-memory Java objects
 */
public interface InterpreterAction {

  /**
   * Apply this action.
   *
   * @param scope The scope in which to apply this action and in which to manipulate memory.
   */
  InterpreterScope apply(InterpreterScope scope);

}
