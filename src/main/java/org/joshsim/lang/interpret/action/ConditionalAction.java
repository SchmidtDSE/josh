package org.joshsim.lang.interpret.action;


import org.joshsim.lang.interpret.machine.EventHandlerMachine;

import java.util.Optional;

public class ConditionalAction implements EventHandlerAction {

  private final EventHandlerAction conditional;
  private final EventHandlerAction positive;
  private final Optional<EventHandlerAction> negative;

  public ConditionalAction(EventHandlerAction conditional, EventHandlerAction positive, EventHandlerAction negative) {
    this.conditional = conditional;
    this.positive = positive;
    this.negative = Optional.of(negative);
  }

  public ConditionalAction(EventHandlerAction conditional, EventHandlerAction positive) {
    this.conditional = conditional;
    this.positive = positive;
    this.negative = Optional.empty();
  }

  @Override
  public EventHandlerMachine apply(EventHandlerMachine machine) {
    conditional.apply(machine);

    if (negative.isEmpty()) {
      machine.branch(positive, negative.get());
    } else {
      machine.condition(positive);
    }

    return machine;
  }

  public ConditionalAction chain(EventHandlerAction newLink) {
    if (negative.isPresent()) {
      throw new IllegalStateException("Negative already set on this conditional.");
    }

    return new ConditionalAction(conditional, positive, newLink);
  }

}
