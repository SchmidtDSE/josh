package org.joshsim.lang.interpret.fragment;

import org.joshsim.engine.entity.EventHandlerGroupBuilder;

public class EventHandlerGroupFragment extends Fragment {

  private final EventHandlerGroupBuilder eventHandlerGroup;

  public EventHandlerGroupFragment(EventHandlerGroupBuilder eventHandlerGroup) {
    this.eventHandlerGroup = eventHandlerGroup;
  }

  @Override
  public EventHandlerGroupBuilder getEventHandlerGroup() {
    return eventHandlerGroup;
  }

}
