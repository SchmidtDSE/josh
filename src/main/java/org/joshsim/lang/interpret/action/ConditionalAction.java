
/**
 * Implements conditional branching logic for event handler actions.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret.action;

import java.util.Optional;
import org.joshsim.lang.interpret.machine.EventHandlerMachine;

/**
 * Represents a conditional action that can branch between different execution paths.
 *
 * <p>This class implements the event handler action interface to provide conditional
 * branching behavior. It maintains a conditional check, a positive path action,
 * and an optional negative path action.</p>
 */
public class ConditionalAction implements EventHandlerAction {

  private final EventHandlerAction conditional;
  private final EventHandlerAction positive;
  private final Optional<EventHandlerAction> negative;

  /**
   * Creates a conditional action with both positive and negative paths.
   *
   * @param conditional The action that performs the conditional check
   * @param positive The action to execute if the condition is true
   * @param negative The action to execute if the condition is false
   */
  public ConditionalAction(EventHandlerAction conditional, EventHandlerAction positive,
      EventHandlerAction negative) {
    this.conditional = conditional;
    this.positive = positive;
    this.negative = Optional.of(negative);
  }

  /**
   * Creates a conditional action with only a positive path.
   *
   * @param conditional The action that performs the conditional check
   * @param positive The action to execute if the condition is true
   */
  public ConditionalAction(EventHandlerAction conditional, EventHandlerAction positive) {
    this.conditional = conditional;
    this.positive = positive;
    this.negative = Optional.empty();
  }

  /**
   * Applies the conditional logic to the event handler machine.
   *
   * @param machine The event handler machine to apply the action to
   * @return The modified event handler machine
   */
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

  /**
   * Chains a new action as the negative path of this conditional.
   *
   * @param newLink The action to chain as the negative path
   * @return A new conditional action with the chained negative path
   * @throws IllegalStateException if a negative path is already set
   */
  public ConditionalAction chain(EventHandlerAction newLink) {
    if (negative.isPresent()) {
      throw new IllegalStateException("Negative already set on this conditional.");
    }

    return new ConditionalAction(conditional, positive, newLink);
  }

}
