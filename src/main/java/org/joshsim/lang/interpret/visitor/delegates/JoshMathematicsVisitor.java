/**
 * Visitor delegate which performs mathematical operations.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret.visitor.delegates;

import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.lang.antlr.JoshLangParser;
import org.joshsim.lang.interpret.action.EventHandlerAction;
import org.joshsim.lang.interpret.fragment.ActionFragment;
import org.joshsim.lang.interpret.fragment.Fragment;
import org.joshsim.lang.interpret.visitor.JoshParserToMachineVisitor;


/**
 * Delegate which handles mathematical operations including arithmetic.
 *
 * <p>Delegate which handles mathematical operations including arithmetic like addition and
 * multiplication as well as operations like mapping.</p>
 */
public class JoshMathematicsVisitor implements JoshVisitorDelegate {

  private final JoshParserToMachineVisitor parent;
  private final EngineValueFactory engineValueFactory;

  /**
   * Constructs a new instance of the JoshMathematicsVisitor class.
   *
   * @param toolbox The toolbox through which visitors can access supporting objects.
   */
  public JoshMathematicsVisitor(DelegateToolbox toolbox) {
    parent = toolbox.getParent();
    engineValueFactory = toolbox.getValueFactory();
  }

  /**
   * Perform a mapping operation from a domain to a range.
   *
   * <p>Perform a mapping operation from a domain to a range using linear interpolation between the
   * start and end of the domain into the start and end of the range.</p>
   *
   * @param ctx The ANTLR context from which to parse the mapping operation.
   * @return Fragment containing the mapping operation parsed.
   */
  public Fragment visitMapLinear(JoshLangParser.MapLinearContext ctx) {
    EventHandlerAction operandAction = ctx.operand.accept(parent).getCurrentAction();
    EventHandlerAction fromLowAction = ctx.fromlow.accept(parent).getCurrentAction();
    EventHandlerAction fromHighAction = ctx.fromhigh.accept(parent).getCurrentAction();
    EventHandlerAction toLowAction = ctx.tolow.accept(parent).getCurrentAction();
    EventHandlerAction toHighAction = ctx.tohigh.accept(parent).getCurrentAction();

    EventHandlerAction action = (machine) -> {
      operandAction.apply(machine);
      fromLowAction.apply(machine);
      fromHighAction.apply(machine);
      toLowAction.apply(machine);
      toHighAction.apply(machine);
      machine.push(engineValueFactory.build(true, Units.EMPTY));
      machine.applyMap("linear");
      return machine;
    };

    return new ActionFragment(action);
  }

  /**
   * Perform a mapping operation from a domain to a range with a named curve shape.
   *
   * <p>Perform a mapping operation where the shape of the curve in that mapping operation is
   * parameterized by only a name. This may be linear, for example.</p>
   *
   * @param ctx The ANTLR context from which to parse the mapping operation.
   * @return Fragment containing the mapping operation parsed.
   */
  public Fragment visitMapParam(JoshLangParser.MapParamContext ctx) {
    EventHandlerAction operandAction = ctx.operand.accept(parent).getCurrentAction();
    EventHandlerAction fromLowAction = ctx.fromlow.accept(parent).getCurrentAction();
    EventHandlerAction fromHighAction = ctx.fromhigh.accept(parent).getCurrentAction();
    EventHandlerAction toLowAction = ctx.tolow.accept(parent).getCurrentAction();
    EventHandlerAction toHighAction = ctx.tohigh.accept(parent).getCurrentAction();
    String method = ctx.method.getText();

    EventHandlerAction action = (machine) -> {
      operandAction.apply(machine);
      fromLowAction.apply(machine);
      fromHighAction.apply(machine);
      toLowAction.apply(machine);
      toHighAction.apply(machine);
      machine.push(engineValueFactory.build(true, Units.EMPTY));
      machine.applyMap(method);
      return machine;
    };

    return new ActionFragment(action);
  }

  /**
   * Perform a mapping operation from a domain to a range with a parameterized curve shape.
   *
   * <p>Perform a mapping operation where the shape of the curve in that mapping operation is
   * specified by a name and that curve is parameterized by some argument value. This may be
   * sigmoid, for example.</p>
   *
   * @param ctx The ANTLR context from which to parse the mapping operation.
   * @return Fragment containing the mapping operation parsed.
   */
  public Fragment visitMapParamParam(JoshLangParser.MapParamParamContext ctx) {
    EventHandlerAction operandAction = ctx.operand.accept(parent).getCurrentAction();
    EventHandlerAction fromLowAction = ctx.fromlow.accept(parent).getCurrentAction();
    EventHandlerAction fromHighAction = ctx.fromhigh.accept(parent).getCurrentAction();
    EventHandlerAction toLowAction = ctx.tolow.accept(parent).getCurrentAction();
    EventHandlerAction toHighAction = ctx.tohigh.accept(parent).getCurrentAction();
    EventHandlerAction paramAction = ctx.methodarg.accept(parent).getCurrentAction();
    String method = ctx.method.getText();

    EventHandlerAction action = (machine) -> {
      operandAction.apply(machine);
      fromLowAction.apply(machine);
      fromHighAction.apply(machine);
      toLowAction.apply(machine);
      toHighAction.apply(machine);
      paramAction.apply(machine);
      machine.applyMap(method);
      return machine;
    };

    return new ActionFragment(action);
  }

  public Fragment visitAdditionExpression(JoshLangParser.AdditionExpressionContext ctx) {
    EventHandlerAction leftAction = ctx.left.accept(parent).getCurrentAction();
    EventHandlerAction rightAction = ctx.right.accept(parent).getCurrentAction();
    boolean isAddition = ctx.op.getText().equals("+");

    EventHandlerAction action = (machine) -> {
      leftAction.apply(machine);
      rightAction.apply(machine);
      return isAddition ? machine.add() : machine.subtract();
    };

    return new ActionFragment(action);
  }

  public Fragment visitMultiplyExpression(JoshLangParser.MultiplyExpressionContext ctx) {
    EventHandlerAction leftAction = ctx.left.accept(parent).getCurrentAction();
    EventHandlerAction rightAction = ctx.right.accept(parent).getCurrentAction();
    boolean isMultiplication = ctx.op.getText().equals("*");

    EventHandlerAction action = (machine) -> {
      leftAction.apply(machine);
      rightAction.apply(machine);
      return isMultiplication ? machine.multiply() : machine.divide();
    };

    return new ActionFragment(action);
  }

  public Fragment visitPowExpression(JoshLangParser.PowExpressionContext ctx) {
    EventHandlerAction leftAction = ctx.left.accept(parent).getCurrentAction();
    EventHandlerAction rightAction = ctx.right.accept(parent).getCurrentAction();

    EventHandlerAction action = (machine) -> {
      leftAction.apply(machine);
      rightAction.apply(machine);
      machine.pow();
      return machine;
    };

    return new ActionFragment(action);
  }

  public Fragment visitParenExpression(JoshLangParser.ParenExpressionContext ctx) {
    return ctx.getChild(1).accept(parent);
  }

  public Fragment visitLimitBoundExpression(JoshLangParser.LimitBoundExpressionContext ctx) {
    EventHandlerAction operandAction = ctx.operand.accept(parent).getCurrentAction();
    EventHandlerAction lowerBoundAction = ctx.lower.accept(parent).getCurrentAction();
    EventHandlerAction upperBoundAction = ctx.upper.accept(parent).getCurrentAction();

    EventHandlerAction action = (machine) -> {
      operandAction.apply(machine);
      lowerBoundAction.apply(machine);
      upperBoundAction.apply(machine);
      machine.bound(true, true);
      return machine;
    };

    return new ActionFragment(action);
  }

  public Fragment visitLimitMinExpression(JoshLangParser.LimitMinExpressionContext ctx) {
    EventHandlerAction operandAction = ctx.operand.accept(parent).getCurrentAction();
    EventHandlerAction limitAction = ctx.limit.accept(parent).getCurrentAction();
    EventHandlerAction action = (machine) -> {
      operandAction.apply(machine);
      limitAction.apply(machine);
      machine.bound(true, false);
      return machine;
    };

    return new ActionFragment(action);
  }

  public Fragment visitLimitMaxExpression(JoshLangParser.LimitMaxExpressionContext ctx) {
    EventHandlerAction operandAction = ctx.operand.accept(parent).getCurrentAction();
    EventHandlerAction limitAction = ctx.limit.accept(parent).getCurrentAction();
    EventHandlerAction action = (machine) -> {
      operandAction.apply(machine);
      limitAction.apply(machine);
      machine.bound(false, true);
      return machine;
    };

    return new ActionFragment(action);
  }

}
