/**
 * Structures describing a compiled action which the interpreter can take.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret.action;


import org.joshsim.lang.interpret.machine.EventHandlerMachine;

/**
 * Strategy to which is an interpreter action compiled to in-memory Java objects
 */
public interface EventHandlerAction {

  /**
   * Apply this action.
   *
   * @param target The machine in which to apply this action and in which to manipulate memory.
   * @return Machine in which to continue actions.
   */
  EventHandlerMachine apply(EventHandlerMachine target);

}
