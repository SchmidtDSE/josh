package org.joshsim.lang.interpret;


public class ConditionalAction implements InterpreterAction {

  private final InterpreterAction conditional;
  private final InterpreterAction positive;
  private final InterpreterAction negative;

  public ConditionalAction(InterpreterAction conditional, InterpreterAction positive, InterpreterAction negative) {
    this.conditional = conditional;
    this.positive = positive;
    this.negative = negative;
  }

  @Override
  public InterpreterMachine apply(InterpreterMachine machine) {
    conditional.apply(machine);
    machine.branch(positive, negative);
    return machine;
  }

}
