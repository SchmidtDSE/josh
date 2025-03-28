package org.joshsim.lang.interpret.fragment;

import org.joshsim.engine.entity.EventHandlerGroupBuilder;

import java.util.List;

public class StateFragment extends Fragment {

  private final Iterable<EventHandlerGroupBuilder> groups;

  public StateFragment(Iterable<EventHandlerGroupBuilder> groups) {
    this.groups = groups;
  }

  @Override
  public Iterable<EventHandlerGroupBuilder> getEventHandlerGroups() {
    return groups;
  }

}
