package org.joshsim.lang.interpret.action;

import java.util.ArrayList;
import java.util.List;

public class ChaniningConditionalBuilder {

  private final List<ConditionalAction> actions;

  public ChaniningConditionalBuilder() {
    actions = new ArrayList<>();
  }

  public void add(EventHandlerAction action) {
    if (!(action instanceof ConditionalAction)) {
      throw new IllegalArgumentException("Conditional actions must be of type ConditionalAction.");
    }

    add((ConditionalAction) action);
  }

  public void add(ConditionalAction action) {
    actions.add(action);
  }

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
