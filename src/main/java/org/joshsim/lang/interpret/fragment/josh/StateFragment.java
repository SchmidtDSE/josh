/**
 * Structure containing information for constructing a state within an entity.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret.fragment.josh;

import org.joshsim.engine.entity.handler.EventHandlerGroupBuilder;
import org.joshsim.lang.interpret.fragment.FragmentType;


/**
 * Create a new fragement containing information from a state stanza within an entity.
 */
public class StateFragment extends JoshFragment {

  private final Iterable<EventHandlerGroupBuilder> groups;

  /**
   * Create a new state fragment which can be used in constructing entities.
   *
   * @param groups The event handler groups which were defined within this state stanza.
   */
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
