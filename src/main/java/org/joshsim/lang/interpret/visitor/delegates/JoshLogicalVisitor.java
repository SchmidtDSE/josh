/**
 * Delegate which handles logical operations and conditionals.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret.visitor.delegates;

import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.engine.value.type.EngineValue;
import org.joshsim.lang.antlr.JoshLangParser;
import org.joshsim.lang.interpret.action.ChaniningConditionalBuilder;
import org.joshsim.lang.interpret.action.ConditionalAction;
import org.joshsim.lang.interpret.action.EventHandlerAction;
import org.joshsim.lang.interpret.fragment.ActionFragment;
import org.joshsim.lang.interpret.fragment.Fragment;
import org.joshsim.lang.interpret.visitor.JoshParserToMachineVisitor;


/**
 * Delegate which handles logical operations and conditionals.
 *
 * <p>Delegate which handles logical operations like and / or as well as supporting conditional
 * execution like if statements.</p>
 */
public class JoshLogicalVisitor implements JoshVisitorDelegate {

  private final EngineValueFactory valueFactory;
  private final JoshParserToMachineVisitor parent;
  private final EngineValue trueValue;

  /**
   * Create a new logical delegate.
   *
   * @param toolbox The toolbox through which visitors can access supporting objects.
   */
  public JoshLogicalVisitor(DelegateToolbox toolbox) {
    valueFactory = toolbox.getValueFactory();
    parent = toolbox.getParent();
    trueValue = valueFactory.build(true, Units.EMPTY);
  }

  /**
   * Parse a logical expression operation.
   *
   * <p>Parse a logical expression operation like and, or, xor which can be used as part of a
   * broader expression.</p>
   *
   * @param ctx The ANTLR context from which to parse the logical expression operation.
   * @return Fragment containing the logical expression operation parsed.
   */
  public Fragment visitLogicalExpression(JoshLangParser.LogicalExpressionContext ctx) {
    EventHandlerAction leftAction = ctx.left.accept(parent).getCurrentAction();
    EventHandlerAction rightAction = ctx.right.accept(parent).getCurrentAction();
    String opStr = ctx.op.getText();

    EventHandlerAction innerAction = switch (opStr) {
      case "and" -> (machine) -> machine.and();
      case "or" -> (machine) -> machine.or();
      case "xor" -> (machine) -> machine.xor();
      default -> throw new IllegalArgumentException(opStr + " is not a valid logical expression.");
    };

    EventHandlerAction action = (machine) -> {
      leftAction.apply(machine);
      rightAction.apply(machine);
      innerAction.apply(machine);
      return machine;
    };

    return new ActionFragment(action);
  }

  public Fragment visitCondition(JoshLangParser.ConditionContext ctx) {
    EventHandlerAction leftAction = ctx.left.accept(parent).getCurrentAction();
    EventHandlerAction rightAction = ctx.right.accept(parent).getCurrentAction();
    String opStr = ctx.op.getText();

    EventHandlerAction innerAction = switch (opStr) {
      case "!=" -> (machine) -> machine.neq();
      case ">" -> (machine) -> machine.gt();
      case "<" -> (machine) -> machine.lt();
      case "==" -> (machine) -> machine.eq();
      case "<=" -> (machine) -> machine.lteq();
      case ">=" -> (machine) -> machine.gteq();
      default -> throw new IllegalArgumentException(opStr + " is not a valid comparator.");
    };

    EventHandlerAction action = (machine) -> {
      leftAction.apply(machine);
      rightAction.apply(machine);
      innerAction.apply(machine);
      return machine;
    };

    return new ActionFragment(action);
  }

  public Fragment visitConditional(JoshLangParser.ConditionalContext ctx) {
    EventHandlerAction posAction = ctx.pos.accept(parent).getCurrentAction();
    EventHandlerAction negAction = ctx.neg.accept(parent).getCurrentAction();
    EventHandlerAction condAction = ctx.cond.accept(parent).getCurrentAction();

    EventHandlerAction action = new ConditionalAction(condAction, posAction, negAction);
    return new ActionFragment(action);
  }

  public Fragment visitFullConditional(JoshLangParser.FullConditionalContext ctx) {
    EventHandlerAction condAction = ctx.cond.accept(parent).getCurrentAction();
    EventHandlerAction posAction = ctx.target.accept(parent).getCurrentAction();

    ChaniningConditionalBuilder chainBuilder = new ChaniningConditionalBuilder();
    chainBuilder.add(new ConditionalAction(condAction, posAction));

    int numElse = ctx.getChildCount() - 5;
    for (int elseIndex = 0; elseIndex < numElse; elseIndex++) {
      int childIndex = elseIndex + 5;
      EventHandlerAction elseAction = ctx.getChild(childIndex).accept(parent).getCurrentAction();
      chainBuilder.add(elseAction);
    }

    EventHandlerAction action = chainBuilder.build();
    return new ActionFragment(action);
  }

  public Fragment visitFullElifBranch(JoshLangParser.FullElifBranchContext ctx) {
    EventHandlerAction condAction = ctx.cond.accept(parent).getCurrentAction();
    EventHandlerAction posAction = ctx.target.accept(parent).getCurrentAction();

    EventHandlerAction action = new ConditionalAction(condAction, posAction);
    return new ActionFragment(action);
  }

  public Fragment visitFullElseBranch(JoshLangParser.FullElseBranchContext ctx) {
    EventHandlerAction condAction = (machine) -> machine.push(trueValue);
    EventHandlerAction posAction = ctx.target.accept(parent).getCurrentAction();

    EventHandlerAction action = new ConditionalAction(condAction, posAction);
    return new ActionFragment(action);
  }

}
