/**
 * Structure describing an InterpreterMachineBuilder containing a single action.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret;


/**
 * Fragment of a InterpreterMachineSaffold containing a single action.
 */
public class SingleActionMachineScaffold implements InterpreterMachineScaffold {

  private final InterpreterAction action;

  public SingleActionMachineScaffold(InterpreterAction action) {
    this.action = action;
  }
  
  public InterpreterAction getCurrentAction() {
    return action;
  }

}
