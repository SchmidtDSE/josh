/**
 * Decorator to help create CompiledCallable for push down machines.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret.machine;

import org.joshsim.engine.func.CompiledCallable;
import org.joshsim.engine.func.Scope;
import org.joshsim.engine.value.type.EngineValue;
import org.joshsim.lang.interpret.BridgeGetter;
import org.joshsim.lang.interpret.action.EventHandlerAction;


/**
 * Adapter for an EventHandlerAction which allows it to act as a CompiledCallable.
 */
public class PushDownMachineCallable implements CompiledCallable {

  private final EventHandlerAction handlerAction;
  private final BridgeGetter bridgeGetter;

  /**
   * Create a new decorator.
   *
   * @param handlerAction The action to be decorated so that it acts like a CompiledCallable.
   * @param bridgeGetter Getter for future EngineBridge through which to execute engine operations.
   */
  public PushDownMachineCallable(EventHandlerAction handlerAction, BridgeGetter bridgeGetter) {
    this.handlerAction = handlerAction;
    this.bridgeGetter = bridgeGetter;
  }

  @Override
  public EngineValue evaluate(Scope scope) {
    EventHandlerMachine machine = new SingleThreadEventHandlerMachine(
        bridgeGetter.get(),
        scope,
        bridgeGetter.getDebugWriter()
    );
    handlerAction.apply(machine);
    return machine.getResult();
  }

}
