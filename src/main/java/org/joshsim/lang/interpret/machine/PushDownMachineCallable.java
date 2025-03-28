package org.joshsim.lang.interpret.machine;

import org.joshsim.engine.func.CompiledCallable;
import org.joshsim.engine.func.Scope;
import org.joshsim.engine.value.EngineValue;
import org.joshsim.lang.interpret.action.EventHandlerAction;


public class PushDownMachineCallable implements CompiledCallable {

  private final EventHandlerAction handlerAction;

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
