/**
 * Visitor delegate which performs mathematical operations.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret.visitor.delegates;

import java.util.ArrayList;
import java.util.List;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.lang.antlr.JoshLangParser;
import org.joshsim.lang.interpret.action.EventHandlerAction;
import org.joshsim.lang.interpret.fragment.josh.ActionFragment;
import org.joshsim.lang.interpret.fragment.josh.JoshFragment;
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
   * @return JoshFragment containing the mapping operation parsed.
   */
  public JoshFragment visitMapLinear(JoshLangParser.MapLinearContext ctx) {
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
   * @return JoshFragment containing the mapping operation parsed.
   */
  public JoshFragment visitMapParam(JoshLangParser.MapParamContext ctx) {
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
   * @return JoshFragment containing the mapping operation parsed.
   */
  public JoshFragment visitMapParamParam(JoshLangParser.MapParamParamContext ctx) {
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

  /**
   * Parse an addition or subtraction expression.
   *
   * <p>Parse an expression that adds or subtracts two values, determining the operation
   * based on the operator token (+ or -).</p>
   *
   * @param ctx The ANTLR context from which to parse the addition/subtraction expression.
   * @return JoshFragment containing the addition/subtraction operation parsed.
   */
  public JoshFragment visitAdditionExpression(JoshLangParser.AdditionExpressionContext ctx) {
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

  /**
   * Parse a multiplication or division expression.
   *
   * <p>Parse an expression that multiplies or divides two values, determining the operation
   * based on the operator token (* or /).</p>
   *
   * @param ctx The ANTLR context from which to parse the multiplication/division expression.
   * @return JoshFragment containing the multiplication/division operation parsed.
   */
  public JoshFragment visitMultiplyExpression(JoshLangParser.MultiplyExpressionContext ctx) {
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

  /**
   * Parse a power expression.
   *
   * <p>Parse an expression that raises one value to the power of another value.</p>
   *
   * @param ctx The ANTLR context from which to parse the power expression.
   * @return JoshFragment containing the power operation parsed.
   */
  public JoshFragment visitPowExpression(JoshLangParser.PowExpressionContext ctx) {
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

  /**
   * Parse a parenthesized expression.
   *
   * <p>Parse an expression enclosed in parentheses, which affects the order of operations through
   * the parse tree.</p>
   *
   * @param ctx The ANTLR context from which to parse the parenthesized expression.
   * @return JoshFragment containing the expression within the parentheses.
   */
  public JoshFragment visitParenExpression(JoshLangParser.ParenExpressionContext ctx) {
    return ctx.getChild(1).accept(parent);
  }

  /**
   * Parse a bounded limit expression.
   *
   * <p>Parse an expression that constrains a value to be within both lower and upper bounds,
   * capping if beyond the allowed range.</p>
   *
   * @param ctx The ANTLR context from which to parse the bounded limit expression.
   * @return JoshFragment containing the bounded limit operation parsed.
   */
  public JoshFragment visitLimitBoundExpression(JoshLangParser.LimitBoundExpressionContext ctx) {
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

  /**
   * Parse a minimum limit expression.
   *
   * <p>Parse an expression that constrains a value to be at least a specified minimum value. In
   * other words, a range with infinity as maximum.</p>
   *
   * @param ctx The ANTLR context from which to parse the minimum limit expression.
   * @return JoshFragment containing the minimum limit operation parsed.
   */
  public JoshFragment visitLimitMinExpression(JoshLangParser.LimitMinExpressionContext ctx) {
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

  /**
   * Parse a maximum limit expression.
   *
   * <p>Parse an expression that constrains a value to be at most a specified maximum value. In
   * other words, a range with infinity as minimum.</p>
   *
   * @param ctx The ANTLR context from which to parse the maximum limit expression.
   * @return JoshFragment containing the maximum limit operation parsed.
   */
  public JoshFragment visitLimitMaxExpression(JoshLangParser.LimitMaxExpressionContext ctx) {
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

  /**
   * Parse a single parameter function call.
   *
   * <p>Parse a built-in (native) function call with a single parameter, such as abs, ceil, floor,
   * etc. The function to execute is determined by the function name.</p>
   *
   * @param ctx The ANTLR context from which to parse the function call.
   * @return JoshFragment containing the function call operation parsed.
   * @throws IllegalArgumentException if the function name is unknown.
   */
  public JoshFragment visitSingleParamFunctionCall(
      JoshLangParser.SingleParamFunctionCallContext ctx) {
    EventHandlerAction operandAction = ctx.operand.accept(parent).getCurrentAction();
    String funcName = ctx.name.getText();

    EventHandlerAction functionAction = switch (funcName) {
      case "abs" -> (machine) -> machine.abs();
      case "ceil" -> (machine) -> machine.ceil();
      case "count" -> (machine) -> machine.count();
      case "floor" -> (machine) -> machine.floor();
      case "log10" -> (machine) -> machine.log10();
      case "ln" -> (machine) -> machine.ln();
      case "max" -> (machine) -> machine.max();
      case "mean" -> (machine) -> machine.mean();
      case "min" -> (machine) -> machine.min();
      case "round" -> (machine) -> machine.round();
      case "std" -> (machine) -> machine.std();
      case "sum" -> (machine) -> machine.sum();
      default -> throw new IllegalArgumentException("Unknown function name: " + funcName);
    };

    EventHandlerAction action = (machine) -> {
      operandAction.apply(machine);
      functionAction.apply(machine);
      return machine;
    };

    return new ActionFragment(action);
  }

  /**
   * Parse a variadic function call.
   *
   * <p>Parse a function call that accepts multiple arguments, such as debug().
   * The function behavior is determined by the function name.</p>
   *
   * @param ctx The ANTLR context from which to parse the function call.
   * @return JoshFragment containing the function call operation parsed.
   * @throws IllegalArgumentException if the function name is unknown.
   */
  public JoshFragment visitVariadicFunctionCall(
      JoshLangParser.VariadicFunctionCallContext ctx) {
    String funcName = ctx.name.getText();

    // Parse all argument expressions
    List<EventHandlerAction> argActions = new ArrayList<>();
    JoshLangParser.ExpressionListContext argsCtx = ctx.args;

    int numArgs = (argsCtx.getChildCount() + 1) / 2;  // expression (COMMA expression)*
    for (int i = 0; i < numArgs; i++) {
      int childIndex = i * 2;  // Skip commas
      EventHandlerAction argAction = argsCtx.getChild(childIndex).accept(parent).getCurrentAction();
      argActions.add(argAction);
    }

    EventHandlerAction action;

    if ("debug".equals(funcName)) {
      // Handle debug function
      action = (machine) -> {
        // Evaluate all arguments (pushes them onto stack)
        for (EventHandlerAction argAction : argActions) {
          argAction.apply(machine);
        }
        // Call writeDebug with argument count
        machine.writeDebug(argActions.size());
        return machine;
      };
    } else {
      throw new IllegalArgumentException("Unknown variadic function: " + funcName);
    }

    return new ActionFragment(action);
  }

}
