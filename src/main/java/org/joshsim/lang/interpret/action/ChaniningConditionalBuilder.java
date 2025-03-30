
/**
 * Builder for creating chains of conditional actions.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret.action;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder pattern implementation for creating chains of conditional actions.
 *
 * <p>Builder which maintains an ordered list of conditional actions and builds them into a properly
 * chained sequence of ConditionalActions.</p>
 */
public class ChaniningConditionalBuilder {

  private final List<ConditionalAction> actions;

  /**
   * Creates a new builder for chaining conditional actions which is empty.
   */
  public ChaniningConditionalBuilder() {
    actions = new ArrayList<>();
  }

  /**
   * Adds an event handler action to the chain.
   *
   * @param action The event handler action to add
   * @throws IllegalArgumentException if the action is not a ConditionalAction
   */
  public void add(EventHandlerAction action) {
    if (!(action instanceof ConditionalAction)) {
      throw new IllegalArgumentException("Conditional actions must be of type ConditionalAction.");
    }

    add((ConditionalAction) action);
  }

  /**
   * Adds a conditional action to the chain.
   *
   * @param action The conditional action to add
   */
  public void add(ConditionalAction action) {
    actions.add(action);
  }

  /**
   * Builds the chain of conditional actions.
   * 
   * <p>Create a chain by linking each action to the next one in reverse order, returning the action
   * at the start of that chain.</p>
   *
   * @return The first conditional action in the completed chain
   * @throws IllegalStateException if no conditional actions have been added
   */
  public ConditionalAction build() {
    int numActions = actions.size();

    if (numActions == 0) {
      throw new IllegalStateException("No conditional actions found.");
    }

    ConditionalAction chain = actions.get(numActions - 1);
    for (int i = numActions - 2; i >= 0; i--) {
      chain = actions.get(i).chain(chain);
    }

    return chain;
  }

}
