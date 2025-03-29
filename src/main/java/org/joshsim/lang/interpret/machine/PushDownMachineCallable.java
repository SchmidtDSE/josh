/**
 * Decorator to help create CompiledCallable for push down machines.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret.machine;

import org.joshsim.engine.func.CompiledCallable;
import org.joshsim.engine.func.Scope;
import org.joshsim.engine.value.EngineValue;
import org.joshsim.lang.interpret.action.EventHandlerAction;


/**
 * Adapter for an EventHandlerAction which allows it to act as a CompiledCallable.
 */
public class PushDownMachineCallable implements CompiledCallable {

  private final EventHandlerAction handlerAction;

  /**
   * Create a new decorator.
   *
   * @param handlerAction The action to be decorated so that it acts like a CompiledCallable.
   */
  public PushDownMachineCallable(EventHandlerAction handlerAction) {
    this.handlerAction = handlerAction;
  }

  @Override
  public EngineValue evaluate(Scope scope) {
    EventHandlerMachine machine = new PushDownEventHandlerMachine(scope);
    handlerAction.apply(machine);
    return machine.getResult();
  }

}
