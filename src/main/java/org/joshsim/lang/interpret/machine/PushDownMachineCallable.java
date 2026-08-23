/**
 * Decorator to help create CompiledCallable for push down machines.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret.machine;

import java.util.ArrayDeque;
import org.joshsim.engine.func.CompiledCallable;
import org.joshsim.engine.func.Scope;
import org.joshsim.engine.value.type.EngineValue;
import org.joshsim.lang.interpret.BridgeGetter;
import org.joshsim.lang.interpret.action.EventHandlerAction;


/**
 * Adapter for an EventHandlerAction which allows it to act as a CompiledCallable.
 */
public class PushDownMachineCallable implements CompiledCallable {

  // Machines are not thread-safe but evaluation happens on one thread at a time, so each thread
  // keeps its own free list. The list grows to the maximum nesting depth of callable evaluation
  // on that thread and machines are reused across evaluations after a reset. Shared across all
  // callables rather than held per instance so that a machine freed by one callable can be taken
  // by the next, which is what bounds the list to the nesting depth.
  private static final ThreadLocal<ArrayDeque<SingleThreadEventHandlerMachine>> FREE_MACHINES;

  static {
    FREE_MACHINES = ThreadLocal.withInitial(ArrayDeque::new);
  }

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
    ArrayDeque<SingleThreadEventHandlerMachine> freeMachines = FREE_MACHINES.get();
    SingleThreadEventHandlerMachine machine = freeMachines.pollFirst();
    if (machine == null) {
      machine = new SingleThreadEventHandlerMachine(
          bridgeGetter.get(),
          scope,
          bridgeGetter.getDebugOutputFacade()
      );
    } else {
      machine.reset(bridgeGetter.get(), scope, bridgeGetter.getDebugOutputFacade());
    }

    try {
      handlerAction.apply(machine);
      return machine.getResult();
    } finally {
      machine.release();
      freeMachines.addFirst(machine);
    }
  }

}
