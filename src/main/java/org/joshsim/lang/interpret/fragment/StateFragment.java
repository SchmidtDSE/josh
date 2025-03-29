package org.joshsim.lang.interpret.fragment;

import java.util.List;
import org.joshsim.engine.entity.EventHandlerGroupBuilder;

public class StateFragment extends Fragment {

  private final Iterable<EventHandlerGroupBuilder> groups;

  public StateFragment(Iterable<EventHandlerGroupBuilder> groups) {
    this.groups = groups;
  }

  @Override
  public Iterable<EventHandlerGroupBuilder> getEventHandlerGroups() {
    return groups;
  }

  @Override
  public FragmentType getFragmentType() {
    return FragmentType.STATE;
  }

}
