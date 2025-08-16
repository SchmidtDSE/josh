/**
 * Visitor delegate which performs string operations.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret.visitor.delegates;

import org.joshsim.lang.antlr.JoshLangParser;
import org.joshsim.lang.interpret.action.EventHandlerAction;
import org.joshsim.lang.interpret.fragment.josh.ActionFragment;
import org.joshsim.lang.interpret.fragment.josh.JoshFragment;
import org.joshsim.lang.interpret.visitor.JoshParserToMachineVisitor;


/**
 * Visitor delegate for handling string operations.
 */
public class JoshStringOperationVisitor implements JoshVisitorDelegate {

  private final JoshParserToMachineVisitor parent;

  /**
   * Create a new visitor for string operations.
   *
   * @param toolbox The toolbox through which visitors can access supporting objects.
   */
  public JoshStringOperationVisitor(DelegateToolbox toolbox) {
    parent = toolbox.getParent();
  }

  /**
   * Parse a string concatenation expression.
   *
   * @param ctx The ANTLR context from which to parse the concatenation expression.
   * @return JoshFragment containing the concatenation expression parsed.
   */
  public JoshFragment visitConcatExpression(JoshLangParser.ConcatExpressionContext ctx) {
    EventHandlerAction leftAction = ctx.left.accept(parent).getCurrentAction();
    EventHandlerAction rightAction = ctx.right.accept(parent).getCurrentAction();

    EventHandlerAction action = (machine) -> {
      leftAction.apply(machine);
      rightAction.apply(machine);
      machine.concat();
      return machine;
    };

    return new ActionFragment(action);
  }

}
