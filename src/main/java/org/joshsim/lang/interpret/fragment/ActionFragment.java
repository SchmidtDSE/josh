/**
 * Structure describing an InterpreterMachineBuilder containing a single action.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret.fragment;


import org.joshsim.lang.interpret.action.EventHandlerAction;

/**
 * Fragment of a InterpreterMachineSaffold containing a single action.
 */
public class ActionFragment extends Fragment {

  private final EventHandlerAction action;

  /**
   * Create a new fragment around a given action.
   *
   * @param action to decorate.
   */
  public ActionFragment(EventHandlerAction action) {
    this.action = action;
  }

  @Override
  public EventHandlerAction getCurrentAction() {
    return action;
  }

  @Override
  public FragmentType getFragmentType() {
    return FragmentType.ACTION;
  }

}
