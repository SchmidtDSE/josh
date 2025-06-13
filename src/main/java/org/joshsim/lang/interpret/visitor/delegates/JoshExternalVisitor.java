
/**
 * Delegate handling external value parsing.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret.visitor.delegates;

import org.joshsim.lang.antlr.JoshLangParser;
import org.joshsim.lang.interpret.action.EventHandlerAction;
import org.joshsim.lang.interpret.fragment.ActionFragment;
import org.joshsim.lang.interpret.fragment.Fragment;


/**
 * Delegate which handles reading external values from source.
 */
public class JoshExternalVisitor implements JoshVisitorDelegate {

  /**
   * Constructs a new instance of the JoshExternalVisitor class.
   *
   * @param toolbox The toolbox through which visitors can access supporting objects.
   */
  public JoshExternalVisitor(DelegateToolbox toolbox) {
    // Toolbox taken for consistency but its properties not needed at this time.
  }

  /**
   * Parse an external value reference.
   *
   * <p>Parse a reference to an external value at the current time step.</p>
   *
   * @param ctx The context from which to parse the external value reference.
   * @return Fragment containing the external value reference parsed.
   */
  public Fragment visitExternalValue(JoshLangParser.ExternalValueContext ctx) {
    String name = ctx.name.getText();
    EventHandlerAction action = (machine) -> {
      long stepCount = machine.getStepCount();
      machine.pushExternal(name, stepCount);
      return machine;
    };
    return new ActionFragment(action);
  }

  /**
   * Parse an external value reference at a specific time.
   *
   * <p>Parse a reference to an external value at a specified time step.</p>
   *
   * @param ctx The context from which to parse the external value at time reference.
   * @return Fragment containing the external value at time reference parsed.
   */
  public Fragment visitExternalValueAtTime(JoshLangParser.ExternalValueAtTimeContext ctx) {
    String name = ctx.name.getText();
    long step = Long.parseLong(ctx.step.getText());
    EventHandlerAction action = (machine) -> {
      machine.pushExternal(name, step);
      return machine;
    };
    return new ActionFragment(action);
  }

}
