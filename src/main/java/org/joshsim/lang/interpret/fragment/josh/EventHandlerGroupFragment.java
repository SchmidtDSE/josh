/**
 * JoshFragment containing a group of event handlers.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret.fragment.josh;

import org.joshsim.engine.entity.handler.EventHandlerGroupBuilder;
import org.joshsim.lang.interpret.fragment.FragmentType;

/**
 * JoshFragment representing a group of event handlers.
 *
 * <p>This class wraps an EventHandlerGroupBuilder that helps construct and organize
 * multiple event handlers that respond to simulation events.</p>
 */
public class EventHandlerGroupFragment extends JoshFragment {

  private final EventHandlerGroupBuilder eventHandlerGroup;

  /**
   * Creates a new fragment around an event handler group builder.
   *
   * @param eventHandlerGroup The event handler group builder to wrap
   */
  public EventHandlerGroupFragment(EventHandlerGroupBuilder eventHandlerGroup) {
    this.eventHandlerGroup = eventHandlerGroup;
  }

  @Override
  public EventHandlerGroupBuilder getEventHandlerGroup() {
    return eventHandlerGroup;
  }

  @Override
  public FragmentType getFragmentType() {
    return FragmentType.EVENT_HANDLER_GROUP;
  }

}