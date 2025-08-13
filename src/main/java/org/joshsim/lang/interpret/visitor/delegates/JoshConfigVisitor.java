/**
 * Delegate handling config value parsing.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret.visitor.delegates;

import org.joshsim.lang.antlr.JoshLangParser;
import org.joshsim.lang.interpret.action.EventHandlerAction;
import org.joshsim.lang.interpret.fragment.ActionFragment;
import org.joshsim.lang.interpret.fragment.Fragment;


/**
 * Delegate which handles config value parsing.
 */
public class JoshConfigVisitor implements JoshVisitorDelegate {

  /**
   * Constructs a new instance of the JoshConfigVisitor class.
   *
   * @param toolbox The toolbox through which visitors can access supporting objects.
   */
  public JoshConfigVisitor(DelegateToolbox toolbox) {
    // Toolbox taken for consistency but not needed at this time
  }

  /**
   * Parse a config value reference.
   *
   * @param ctx The context from which to parse the config value reference.
   * @return Fragment containing the config value reference parsed.
   */
  public Fragment visitConfigValue(JoshLangParser.ConfigValueContext ctx) {
    String name = ctx.name.getText();
    EventHandlerAction action = (machine) -> {
      machine.pushConfig(name);
      return machine;
    };
    return new ActionFragment(action);
  }

}