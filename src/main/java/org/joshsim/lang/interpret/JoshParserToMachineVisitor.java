/**
 * Visitor for Josh sources that parses to an interpreter runtime builder.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret;

import org.joshsim.engine.entity.Agent;
import org.joshsim.engine.entity.Entity;
import org.joshsim.engine.entity.ReferenceGeometryEntity;
import org.joshsim.engine.entity.SpatialEntity;
import org.joshsim.engine.geometry.GeoPoint;
import org.joshsim.engine.geometry.Geometry;
import org.joshsim.engine.geometry.GeometryFactory;
import org.joshsim.engine.value.EngineValue;
import org.joshsim.engine.value.EngineValueFactory;
import org.joshsim.engine.value.Units;
import org.joshsim.lang.antlr.JoshLangBaseVisitor;
import org.joshsim.lang.antlr.JoshLangParser;

import java.math.BigDecimal;


public class JoshParserToMachineVisitor extends JoshLangBaseVisitor<InterpreterMachineScaffold> {

  private final EngineValueFactory engineValueFactory;
  private final EngineValue singleCount;
  private final EngineValue allString;

  public JoshParserToMachineVisitor() {
    super();
    engineValueFactory = new EngineValueFactory();
    singleCount = engineValueFactory.build(1, new Units("count"));
    allString = engineValueFactory.build("all", new Units(""));
  }

  public InterpreterMachineScaffold visitIdentifier(JoshLangParser.IdentifierContext ctx) {
    String identifierName = ctx.getText();
    InterpreterAction action = (machine) -> machine.pushIdentifier(identifierName);
    return new SingleActionMachineScaffold(action);
  }

  public InterpreterMachineScaffold visitNumber(JoshLangParser.NumberContext ctx) {
    BigDecimal number = BigDecimal.valueOf(Double.parseDouble(ctx.getChild(0).getText()));
    EngineValue value = engineValueFactory.build(number, new Units(""));
    InterpreterAction action = (machine) -> machine.push(value);
    return new SingleActionMachineScaffold(action);
  }

  public InterpreterMachineScaffold visitUnitsValue(JoshLangParser.UnitsValueContext ctx) {
    EngineValue value = parseUnitsValue(ctx);
    InterpreterAction action = (machine) -> machine.push(value);
    return new SingleActionMachineScaffold(action);
  }

  public InterpreterMachineScaffold visitString(JoshLangParser.StringContext ctx) {
    String string = ctx.getText();
    EngineValue value = engineValueFactory.build(string, new Units(""));
    InterpreterAction action = (machine) -> machine.push(value);
    return new SingleActionMachineScaffold(action);
  }

  public InterpreterMachineScaffold visitAllExpression(JoshLangParser.AllExpressionContext ctx) {
    InterpreterAction action = (machine) -> machine.push(allString);
    return new SingleActionMachineScaffold(action);
  }

  public InterpreterMachineScaffold visitSimpleBoolExpression(JoshLangParser.SimpleBoolExpressionContext ctx) {
    boolean bool = ctx.getChild(0).getText().equals("true");
    EngineValue value = engineValueFactory.build(bool, new Units(""));
    InterpreterAction action = (machine) -> machine.push(value);
    return new SingleActionMachineScaffold(action);
  }

  public InterpreterMachineScaffold visitMapLinear(JoshLangParser.MapLinearContext ctx) {
    InterpreterAction operandAction = ctx.operand.accept(this).getCurrentAction();
    InterpreterAction fromLowAction = ctx.fromlow.accept(this).getCurrentAction();
    InterpreterAction fromHighAction = ctx.fromhigh.accept(this).getCurrentAction();
    InterpreterAction toLowAction = ctx.tolow.accept(this).getCurrentAction();
    InterpreterAction toHighAction = ctx.tohigh.accept(this).getCurrentAction();

    InterpreterAction action = (machine) -> {
      operandAction.apply(machine);
      fromLowAction.apply(machine);
      fromHighAction.apply(machine);
      toLowAction.apply(machine);
      toHighAction.apply(machine);
      machine.applyMap("linear");
      return machine;
    };

    return new SingleActionMachineScaffold(action);
  }

  public InterpreterMachineScaffold visitMapParam(JoshLangParser.MapParamContext ctx) {
    InterpreterAction operandAction = ctx.operand.accept(this).getCurrentAction();
    InterpreterAction fromLowAction = ctx.fromlow.accept(this).getCurrentAction();
    InterpreterAction fromHighAction = ctx.fromhigh.accept(this).getCurrentAction();
    InterpreterAction toLowAction = ctx.tolow.accept(this).getCurrentAction();
    InterpreterAction toHighAction = ctx.tohigh.accept(this).getCurrentAction();
    String method = ctx.method.getText();

    InterpreterAction action = (machine) -> {
      operandAction.apply(machine);
      fromLowAction.apply(machine);
      fromHighAction.apply(machine);
      toLowAction.apply(machine);
      toHighAction.apply(machine);
      machine.applyMap(method);
      return machine;
    };

    return new SingleActionMachineScaffold(action);
  }

  public InterpreterMachineScaffold visitAdditionExpression(JoshLangParser.AdditionExpressionContext ctx) {
    InterpreterAction leftAction = ctx.left.accept(this).getCurrentAction();
    InterpreterAction rightAction = ctx.left.accept(this).getCurrentAction();
    boolean isAddition = ctx.op.getText().equals("+");

    InterpreterAction action = (machine) -> {
      leftAction.apply(machine);
      rightAction.apply(machine);
      return isAddition ? machine.add() : machine.subtract();
    };

    return new SingleActionMachineScaffold(action);
  }

  public InterpreterMachineScaffold visitMultiplyExpression(JoshLangParser.MultiplyExpressionContext ctx) {
    InterpreterAction leftAction = ctx.left.accept(this).getCurrentAction();
    InterpreterAction rightAction = ctx.right.accept(this).getCurrentAction();
    boolean isMultiplication = ctx.op.getText().equals("*");

    InterpreterAction action = (machine) -> {
      leftAction.apply(machine);
      rightAction.apply(machine);
      return isMultiplication ? machine.multiply() : machine.divide();
    };

    return new SingleActionMachineScaffold(action);
  }

  public InterpreterMachineScaffold visitPowExpression(JoshLangParser.PowExpressionContext ctx) {
    InterpreterAction leftAction = ctx.left.accept(this).getCurrentAction();
    InterpreterAction rightAction = ctx.right.accept(this).getCurrentAction();

    InterpreterAction action = (machine) -> {
      leftAction.apply(machine);
      rightAction.apply(machine);
      machine.pow();
      return machine;
    };

    return new SingleActionMachineScaffold(action);
  }

  public InterpreterMachineScaffold visitLogicalExpression(JoshLangParser.LogicalExpressionContext ctx) {
    InterpreterAction leftAction = ctx.left.accept(this).getCurrentAction();
    InterpreterAction rightAction = ctx.right.accept(this).getCurrentAction();
    String opStr = ctx.op.getText();

    InterpreterAction innerAction = switch (opStr) {
      case "and" -> (machine) -> machine.and();
      case "or" -> (machine) -> machine.or();
      case "xor" -> (machine) -> machine.xor();
      default -> throw new IllegalArgumentException(opStr + " is not a valid logical expression.");
    };

    InterpreterAction action = (machine) -> {
      leftAction.apply(machine);
      rightAction.apply(machine);
      innerAction.apply(machine);
      return machine;
    };

    return new SingleActionMachineScaffold(action);
  }

  public InterpreterMachineScaffold visitSlice(JoshLangParser.SliceContext ctx) {
    InterpreterAction subjectAction = ctx.subject.accept(this).getCurrentAction();
    InterpreterAction selectionAction = ctx.selection.accept(this).getCurrentAction();

    InterpreterAction action = (machine) -> {
      subjectAction.apply(machine);
      selectionAction.apply(machine);
      return machine.slice();
    };

    return new SingleActionMachineScaffold(action);
  }

  public InterpreterMachineScaffold visitCondition(JoshLangParser.ConditionContext ctx) {
    InterpreterAction leftAction = ctx.left.accept(this).getCurrentAction();
    InterpreterAction rightAction = ctx.right.accept(this).getCurrentAction();
    String opStr = ctx.op.getText();

    InterpreterAction innerAction = switch (opStr) {
      case "!=" -> (machine) -> machine.neq();
      case ">" -> (machine) -> machine.gt();
      case "<" -> (machine) -> machine.lt();
      case "==" -> (machine) -> machine.eq();
      case "<=" -> (machine) -> machine.lteq();
      case ">=" -> (machine) -> machine.gteq();
      default -> throw new IllegalArgumentException(opStr + " is not a valid comparator.");
    };

    return new SingleActionMachineScaffold(innerAction);
  }

  public InterpreterMachineScaffold visitConditional(JoshLangParser.ConditionalContext ctx) {
    InterpreterAction posAction = ctx.pos.accept(this).getCurrentAction();
    InterpreterAction negAction = ctx.neg.accept(this).getCurrentAction();
    InterpreterAction condAction = ctx.cond.accept(this).getCurrentAction();

    InterpreterAction action = new ConditionalAction(condAction, posAction, negAction);
    return new SingleActionMachineScaffold(action);
  }

  public InterpreterMachineScaffold visitFullConditional(JoshLangParser.FullConditionalContext ctx) {

  }

  public InterpreterMachineScaffold visitFullElifBranch(JoshLangParser.FullElifBranchContext ctx) {

  }

  public InterpreterMachineScaffold visitFullElseBranch(JoshLangParser.FullElseBranchContext ctx) {

  }

  public InterpreterMachineScaffold visitSampleSimple(JoshLangParser.SampleSimpleContext ctx) {
    InterpreterAction targetAction = ctx.target.accept(this).getCurrentAction();

    InterpreterAction action = (machine) -> {
      machine.push(singleCount);
      targetAction.apply(machine);
      machine.sample(true);
      return machine;
    };

    return new SingleActionMachineScaffold(action);
  }

  public InterpreterMachineScaffold visitSampleParam(JoshLangParser.SampleParamContext ctx) {
    InterpreterAction countAction = ctx.count.accept(this).getCurrentAction();
    InterpreterAction targetAction = ctx.target.accept(this).getCurrentAction();

    InterpreterAction action = (machine) -> {
      countAction.apply(machine);
      targetAction.apply(machine);
      machine.sample(true);
      return machine;
    };

    return new SingleActionMachineScaffold(action);
  }

  public InterpreterMachineScaffold visitSampleParamReplacement(JoshLangParser.SampleParamReplacementContext ctx) {
    InterpreterAction countAction = ctx.count.accept(this).getCurrentAction();
    InterpreterAction targetAction = ctx.target.accept(this).getCurrentAction();
    String replacementStr = ctx.replace.getText();
    boolean withReplacement = replacementStr.equals("with");

    InterpreterAction action = (machine) -> {
      countAction.apply(machine);
      targetAction.apply(machine);
      machine.sample(withReplacement);
      return machine;
    };

    return new SingleActionMachineScaffold(action);
  }

  public InterpreterMachineScaffold visitLimitBoundExpression(JoshLangParser.LimitBoundExpressionContext ctx) {
    InterpreterAction operandAction = ctx.operand.accept(this).getCurrentAction();
    InterpreterAction lowerBoundAction = ctx.lower.accept(this).getCurrentAction();
    InterpreterAction upperBoundAction = ctx.upper.accept(this).getCurrentAction();

    InterpreterAction action = (machine) -> {
      operandAction.apply(machine);
      lowerBoundAction.apply(machine);
      upperBoundAction.apply(machine);
      machine.bound(true, true);
      return machine;
    };

    return new SingleActionMachineScaffold(action);
  }

  public InterpreterMachineScaffold visitLimitMinExpression(JoshLangParser.LimitMinExpressionContext ctx) {
    InterpreterAction operandAction = ctx.operand.accept(this).getCurrentAction();
    InterpreterAction limitAction = ctx.limit.accept(this).getCurrentAction();
    InterpreterAction action = (machine) -> {
      operandAction.apply(machine);
      limitAction.apply(machine);
      machine.bound(true, false);
      return machine;
    };

    return new SingleActionMachineScaffold(action);
  }

  public InterpreterMachineScaffold visitLimitMaxExpression(JoshLangParser.LimitMinExpressionContext ctx) {
    InterpreterAction operandAction = ctx.operand.accept(this).getCurrentAction();
    InterpreterAction limitAction = ctx.limit.accept(this).getCurrentAction();
    InterpreterAction action = (machine) -> {
      operandAction.apply(machine);
      limitAction.apply(machine);
      machine.bound(false, true);
      return machine;
    };

    return new SingleActionMachineScaffold(action);
  }

  public InterpreterMachineScaffold visitCast(JoshLangParser.CastContext ctx) {
    InterpreterAction operandAction = ctx.operand.accept(this).getCurrentAction();
    String newUnits = ctx.target.getText();

    InterpreterAction action = (machine) -> {
      operandAction.apply(machine);
      machine.cast(newUnits, false);
      return machine;
    };

    return new SingleActionMachineScaffold(action);
  }

  public InterpreterMachineScaffold visitCastForce(JoshLangParser.CastForceContext ctx) {
    InterpreterAction operandAction = ctx.operand.accept(this).getCurrentAction();
    String newUnits = ctx.target.getText();

    InterpreterAction action = (machine) -> {
      operandAction.apply(machine);
      machine.cast(newUnits, true);
      return machine;
    };

    return new SingleActionMachineScaffold(action);
  }

  public InterpreterMachineScaffold visitCreateVariableExpression(JoshLangParser.CreateVariableExpressionContext ctx) {
    InterpreterAction countAction = ctx.count.accept(this).getCurrentAction();
    String entityType = ctx.target.getText();

    InterpreterAction action = (machine) -> {
      countAction.apply(machine);
      machine.makeEntity(entityType);
      return machine;
    };

    return new SingleActionMachineScaffold(action);
  }

  public InterpreterMachineScaffold visitAttrExpression(JoshLangParser.AttrExpressionContext ctx) {
    String attrName = ctx.getChild(2).getText();

    InterpreterAction action = (machine) -> {
      machine.pushAttribute(attrName);
      return machine;
    };

    return new SingleActionMachineScaffold(action);
  }

  public InterpreterMachineScaffold visitSpatialQuery(JoshLangParser.SpatialQueryContext ctx) {
    InterpreterAction targetAction = ctx.target.accept(this).getCurrentAction();
    InterpreterAction distanceAction = ctx.distance.accept(this).getCurrentAction();

    InterpreterAction action = (machine) -> {
      targetAction.apply(machine);
      distanceAction.apply(machine);
      machine.executeSpatialQuery();
      return machine;
    };

    return new SingleActionMachineScaffold(action);
  }

  public InterpreterMachineScaffold visitSingleParamFunctionCall(JoshLangParser.SingleParamFunctionCallContext ctx) {
    InterpreterAction operandAction = ctx.operand.accept(this).getCurrentAction();
    String funcName = ctx.name.getText();

    InterpreterAction functionAction = switch (funcName) {
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

    InterpreterAction action = (machine) -> {
      operandAction.apply(machine);
      functionAction.apply(machine);
      return machine;
    };

    return new SingleActionMachineScaffold(action);
  }

  public InterpreterMachineScaffold visitPosition(JoshLangParser.PositionContext ctx) {
    EngineValue first = parseUnitsValue((JoshLangParser.UnitsValueContext) ctx.getChild(0));
    String firstTypeStr = ctx.getChild(1).getText();
    boolean firstTypeLatitude = firstTypeStr.equals("latitude");

    EngineValue second = parseUnitsValue((JoshLangParser.UnitsValueContext) ctx.getChild(3));
    String secondTypeStr = ctx.getChild(4).getText();
    boolean secondTypeLatitude = secondTypeStr.equals("latitude");

    boolean allDegrees = first.getUnits().equals("degrees") && second.getUnits().equals("degrees");
    if (!allDegrees) {
      String message = String.format(
          "Units for a geographic point must be degrees. Got %s, %s.",
          first.getUnits(),
          second.getUnits()
      );
      throw new IllegalArgumentException(message);
    }

    boolean onlyOneAxis = firstTypeLatitude == secondTypeLatitude;
    if (onlyOneAxis) {
      throw new IllegalArgumentException("Both latitude and longitude must be specified.");
    }

    EngineValue latitude = firstTypeLatitude ? first : second;
    EngineValue longitude = secondTypeLatitude ? first : second;

    Geometry point = GeometryFactory.createPoint(latitude.getAsDecimal(), longitude.getAsDecimal());
    Entity entity = new ReferenceGeometryEntity(point);
    EngineValue decoratedEntity = engineValueFactory.build(entity);

    InterpreterAction action = (machine) -> {
      machine.push(decoratedEntity);
      return machine;
    };

    return new SingleActionMachineScaffold(action);
  }

  public InterpreterMachineScaffold visitCreateSingleExpression(JoshLangParser.CreateSingleExpressionContext ctx) {
    String entityName = ctx.getChild(0).getText();

    InterpreterAction action = (machine) -> {
      machine.push(singleCount);
      machine.create();
      return machine;
    };

    return new SingleActionMachineScaffold(action);
  }

  public InterpreterMachineScaffold visitAssignment(JoshLangParser.AssignmentContext ctx) {
    String identifierName = ctx.getChild(1).getText();
    InterpreterAction valAction = ctx.val.accept(this).getCurrentAction();

    InterpreterAction action = (machine) -> {
      valAction.apply(machine);
      machine.saveLocalVariable(identifierName);
      return machine;
    };

    return new SingleActionMachineScaffold(action);
  }

  public InterpreterMachineScaffold visitReturn(JoshLangParser.ReturnContext ctx) {
    return ctx.getChild(1).accept(this);
  }

  private EngineValue parseUnitsValue(JoshLangParser.UnitsValueContext ctx) {
    BigDecimal number = BigDecimal.valueOf(Double.parseDouble(ctx.getChild(0).getText()));
    String unitsText = ctx.getChild(1).getText();
    return engineValueFactory.build(number, new Units(unitsText));
  }
}
