/**
 * Delegate handling config value parsing.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret.visitor.delegates;

import org.joshsim.lang.antlr.JoshLangParser;
import org.joshsim.lang.interpret.action.EventHandlerAction;
import org.joshsim.lang.interpret.fragment.josh.ActionFragment;
import org.joshsim.lang.interpret.fragment.josh.JoshFragment;


/**
 * Delegate which handles config value parsing.
 */
public class JoshConfigVisitor implements JoshVisitorDelegate {

  private final DelegateToolbox toolbox;

  /**
   * Constructs a new instance of the JoshConfigVisitor class.
   *
   * @param toolbox The toolbox through which visitors can access supporting objects.
   */
  public JoshConfigVisitor(DelegateToolbox toolbox) {
    this.toolbox = toolbox;
  }

  /**
   * Parse a config value reference.
   *
   * @param ctx The context from which to parse the config value reference.
   * @return JoshFragment containing the config value reference parsed.
   */
  public JoshFragment visitConfigValue(JoshLangParser.ConfigValueContext ctx) {
    String name = ctx.name.getText();
    EventHandlerAction action = (machine) -> {
      machine.pushConfig(name);
      return machine;
    };
    return new ActionFragment(action);
  }

  /**
   * Parse a config value reference with default value.
   *
   * @param ctx The context from which to parse the config value reference with default.
   * @return JoshFragment containing the config value reference with default parsed.
   */
  public JoshFragment visitConfigValueWithDefault(JoshLangParser.ConfigValueWithDefaultContext ctx) {
    String name = ctx.name.getText();
    JoshFragment defaultFragment = toolbox.getParent().visit(ctx.defaultValue);
    EventHandlerAction action = (machine) -> {
      // Execute default value first to put it on stack
      defaultFragment.getCurrentAction().apply(machine);
      machine.pushConfigWithDefault(name);
      return machine;
    };
    return new ActionFragment(action);
  }

}
