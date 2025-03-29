/**
 * Visitor for Josh sources that parses to an interpreter runtime builder.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import org.joshsim.engine.entity.EntityBuilder;
import org.joshsim.engine.entity.EntityType;
import org.joshsim.engine.entity.EventHandler;
import org.joshsim.engine.entity.EventHandlerGroupBuilder;
import org.joshsim.engine.entity.EventKey;
import org.joshsim.engine.entity.prototype.EntityPrototype;
import org.joshsim.engine.func.CompiledCallable;
import org.joshsim.engine.func.CompiledSelector;
import org.joshsim.engine.func.CompiledSelectorFromCallable;
import org.joshsim.engine.value.Conversion;
import org.joshsim.engine.value.DirectConversion;
import org.joshsim.engine.value.EngineValue;
import org.joshsim.engine.value.EngineValueFactory;
import org.joshsim.engine.value.NoopConversion;
import org.joshsim.engine.value.Units;
import org.joshsim.lang.antlr.JoshLangBaseVisitor;
import org.joshsim.lang.antlr.JoshLangParser;
import org.joshsim.lang.interpret.action.ChaniningConditionalBuilder;
import org.joshsim.lang.interpret.action.ConditionalAction;
import org.joshsim.lang.interpret.action.EventHandlerAction;
import org.joshsim.lang.interpret.fragment.ActionFragment;
import org.joshsim.lang.interpret.fragment.CompiledCallableFragment;
import org.joshsim.lang.interpret.fragment.ConversionFragment;
import org.joshsim.lang.interpret.fragment.ConversionsFragment;
import org.joshsim.lang.interpret.fragment.EntityFragment;
import org.joshsim.lang.interpret.fragment.EventHandlerGroupFragment;
import org.joshsim.lang.interpret.fragment.Fragment;
import org.joshsim.lang.interpret.fragment.StateFragment;
import org.joshsim.lang.interpret.machine.PushDownMachineCallable;


/**
 * Visitor which parses Josh soruce by using Fragments.
 */
@SuppressWarnings("checkstyle:MissingJavaDocMethod")  // Can't use override because of generics.
public class JoshParserToMachineVisitor extends JoshLangBaseVisitor<Fragment> {

  private final EngineValueFactory engineValueFactory;
  private final EngineValue singleCount;
  private final EngineValue allString;
  private final EngineValue trueValue;

  /**
   * Create a new visitor which has some commonly used values cached.
   */
  public JoshParserToMachineVisitor() {
    super();
    engineValueFactory = new EngineValueFactory();
    singleCount = engineValueFactory.build(1, new Units("count"));
    allString = engineValueFactory.build("all", new Units(""));
    trueValue = engineValueFactory.build(true, new Units(""));
  }

  public Fragment visitIdentifier(JoshLangParser.IdentifierContext ctx) {
    String identifierName = ctx.getText();
    ValueResolver resolver = new ValueResolver(identifierName);
    EventHandlerAction action = (machine) -> machine.push(resolver);
    return new ActionFragment(action);
  }

  public Fragment visitNumber(JoshLangParser.NumberContext ctx) {
    BigDecimal number = BigDecimal.valueOf(Double.parseDouble(ctx.getChild(0).getText()));
    EngineValue value = engineValueFactory.build(number, new Units(""));
    EventHandlerAction action = (machine) -> machine.push(value);
    return new ActionFragment(action);
  }

  public Fragment visitUnitsValue(JoshLangParser.UnitsValueContext ctx) {
    EngineValue value = parseUnitsValue(ctx);
    EventHandlerAction action = (machine) -> machine.push(value);
    return new ActionFragment(action);
  }

  public Fragment visitString(JoshLangParser.StringContext ctx) {
    String string = ctx.getText();
    EngineValue value = engineValueFactory.build(string, new Units(""));
    EventHandlerAction action = (machine) -> machine.push(value);
    return new ActionFragment(action);
  }

  public Fragment visitBool(JoshLangParser.BoolContext ctx) {
    boolean bool = ctx.getChild(0).getText().equals("true");
    EngineValue value = engineValueFactory.build(bool, new Units(""));
    EventHandlerAction action = (machine) -> machine.push(value);
    return new ActionFragment(action);
  }

  public Fragment visitAllExpression(JoshLangParser.AllExpressionContext ctx) {
    EventHandlerAction action = (machine) -> machine.push(allString);
    return new ActionFragment(action);
  }

  public Fragment visitMapLinear(JoshLangParser.MapLinearContext ctx) {
    EventHandlerAction operandAction = ctx.operand.accept(this).getCurrentAction();
    EventHandlerAction fromLowAction = ctx.fromlow.accept(this).getCurrentAction();
    EventHandlerAction fromHighAction = ctx.fromhigh.accept(this).getCurrentAction();
    EventHandlerAction toLowAction = ctx.tolow.accept(this).getCurrentAction();
    EventHandlerAction toHighAction = ctx.tohigh.accept(this).getCurrentAction();

    EventHandlerAction action = (machine) -> {
      operandAction.apply(machine);
      fromLowAction.apply(machine);
      fromHighAction.apply(machine);
      toLowAction.apply(machine);
      toHighAction.apply(machine);
      machine.applyMap("linear");
      return machine;
    };

    return new ActionFragment(action);
  }

  public Fragment visitMapParam(JoshLangParser.MapParamContext ctx) {
    EventHandlerAction operandAction = ctx.operand.accept(this).getCurrentAction();
    EventHandlerAction fromLowAction = ctx.fromlow.accept(this).getCurrentAction();
    EventHandlerAction fromHighAction = ctx.fromhigh.accept(this).getCurrentAction();
    EventHandlerAction toLowAction = ctx.tolow.accept(this).getCurrentAction();
    EventHandlerAction toHighAction = ctx.tohigh.accept(this).getCurrentAction();
    String method = ctx.method.getText();

    EventHandlerAction action = (machine) -> {
      operandAction.apply(machine);
      fromLowAction.apply(machine);
      fromHighAction.apply(machine);
      toLowAction.apply(machine);
      toHighAction.apply(machine);
      machine.applyMap(method);
      return machine;
    };

    return new ActionFragment(action);
  }

  public Fragment visitAdditionExpression(JoshLangParser.AdditionExpressionContext ctx) {
    EventHandlerAction leftAction = ctx.left.accept(this).getCurrentAction();
    EventHandlerAction rightAction = ctx.left.accept(this).getCurrentAction();
    boolean isAddition = ctx.op.getText().equals("+");

    EventHandlerAction action = (machine) -> {
      leftAction.apply(machine);
      rightAction.apply(machine);
      return isAddition ? machine.add() : machine.subtract();
    };

    return new ActionFragment(action);
  }

  public Fragment visitMultiplyExpression(JoshLangParser.MultiplyExpressionContext ctx) {
    EventHandlerAction leftAction = ctx.left.accept(this).getCurrentAction();
    EventHandlerAction rightAction = ctx.right.accept(this).getCurrentAction();
    boolean isMultiplication = ctx.op.getText().equals("*");

    EventHandlerAction action = (machine) -> {
      leftAction.apply(machine);
      rightAction.apply(machine);
      return isMultiplication ? machine.multiply() : machine.divide();
    };

    return new ActionFragment(action);
  }

  public Fragment visitPowExpression(JoshLangParser.PowExpressionContext ctx) {
    EventHandlerAction leftAction = ctx.left.accept(this).getCurrentAction();
    EventHandlerAction rightAction = ctx.right.accept(this).getCurrentAction();

    EventHandlerAction action = (machine) -> {
      leftAction.apply(machine);
      rightAction.apply(machine);
      machine.pow();
      return machine;
    };

    return new ActionFragment(action);
  }

  public Fragment visitLogicalExpression(JoshLangParser.LogicalExpressionContext ctx) {
    EventHandlerAction leftAction = ctx.left.accept(this).getCurrentAction();
    EventHandlerAction rightAction = ctx.right.accept(this).getCurrentAction();
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

  public Fragment visitSlice(JoshLangParser.SliceContext ctx) {
    EventHandlerAction subjectAction = ctx.subject.accept(this).getCurrentAction();
    EventHandlerAction selectionAction = ctx.selection.accept(this).getCurrentAction();

    EventHandlerAction action = (machine) -> {
      subjectAction.apply(machine);
      selectionAction.apply(machine);
      return machine.slice();
    };

    return new ActionFragment(action);
  }

  public Fragment visitCondition(JoshLangParser.ConditionContext ctx) {
    EventHandlerAction leftAction = ctx.left.accept(this).getCurrentAction();
    EventHandlerAction rightAction = ctx.right.accept(this).getCurrentAction();
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

    return new ActionFragment(innerAction);
  }

  public Fragment visitConditional(JoshLangParser.ConditionalContext ctx) {
    EventHandlerAction posAction = ctx.pos.accept(this).getCurrentAction();
    EventHandlerAction negAction = ctx.neg.accept(this).getCurrentAction();
    EventHandlerAction condAction = ctx.cond.accept(this).getCurrentAction();

    EventHandlerAction action = new ConditionalAction(condAction, posAction, negAction);
    return new ActionFragment(action);
  }

  public Fragment visitFullConditional(JoshLangParser.FullConditionalContext ctx) {
    EventHandlerAction condAction = ctx.cond.accept(this).getCurrentAction();
    EventHandlerAction posAction = ctx.target.accept(this).getCurrentAction();

    ChaniningConditionalBuilder chainBuilder = new ChaniningConditionalBuilder();
    chainBuilder.add(new ConditionalAction(condAction, posAction));

    int numElse = ctx.getChildCount() - 5;
    for (int elseIndex = 0; elseIndex < numElse; elseIndex++) {
      int childIndex = elseIndex + 5;
      EventHandlerAction elseAction = ctx.getChild(childIndex).accept(this).getCurrentAction();
      chainBuilder.add(elseAction);
    }

    EventHandlerAction action = chainBuilder.build();
    return new ActionFragment(action);
  }

  public Fragment visitFullElifBranch(JoshLangParser.FullElifBranchContext ctx) {
    EventHandlerAction condAction = ctx.cond.accept(this).getCurrentAction();
    EventHandlerAction posAction = ctx.target.accept(this).getCurrentAction();

    EventHandlerAction action = new ConditionalAction(condAction, posAction);
    return new ActionFragment(action);
  }

  public Fragment visitFullElseBranch(JoshLangParser.FullElseBranchContext ctx) {
    EventHandlerAction condAction = (machine) -> machine.push(trueValue);
    EventHandlerAction posAction = ctx.target.accept(this).getCurrentAction();

    EventHandlerAction action = new ConditionalAction(condAction, posAction);
    return new ActionFragment(action);
  }

  public Fragment visitSampleSimple(JoshLangParser.SampleSimpleContext ctx) {
    EventHandlerAction targetAction = ctx.target.accept(this).getCurrentAction();

    EventHandlerAction action = (machine) -> {
      machine.push(singleCount);
      targetAction.apply(machine);
      machine.sample(true);
      return machine;
    };

    return new ActionFragment(action);
  }

  public Fragment visitSampleParam(JoshLangParser.SampleParamContext ctx) {
    EventHandlerAction countAction = ctx.count.accept(this).getCurrentAction();
    EventHandlerAction targetAction = ctx.target.accept(this).getCurrentAction();

    EventHandlerAction action = (machine) -> {
      countAction.apply(machine);
      targetAction.apply(machine);
      machine.sample(true);
      return machine;
    };

    return new ActionFragment(action);
  }

  public Fragment visitSampleParamReplacement(JoshLangParser.SampleParamReplacementContext ctx) {
    EventHandlerAction countAction = ctx.count.accept(this).getCurrentAction();
    EventHandlerAction targetAction = ctx.target.accept(this).getCurrentAction();
    String replacementStr = ctx.replace.getText();
    boolean withReplacement = replacementStr.equals("with");

    EventHandlerAction action = (machine) -> {
      countAction.apply(machine);
      targetAction.apply(machine);
      machine.sample(withReplacement);
      return machine;
    };

    return new ActionFragment(action);
  }

  public Fragment visitUniformSample(JoshLangParser.UniformSampleContext ctx) {
    EventHandlerAction lowAction = ctx.low.accept(this).getCurrentAction();
    EventHandlerAction highAction = ctx.high.accept(this).getCurrentAction();

    EventHandlerAction action = (machine) -> {
      lowAction.apply(machine);
      highAction.apply(machine);
      machine.randUniform();
      return machine;
    };

    return new ActionFragment(action);
  }

  public Fragment visitNormalSample(JoshLangParser.NormalSampleContext ctx) {
    EventHandlerAction meanAction = ctx.mean.accept(this).getCurrentAction();
    EventHandlerAction stdAction = ctx.stdev.accept(this).getCurrentAction();

    EventHandlerAction action = (machine) -> {
      meanAction.apply(machine);
      stdAction.apply(machine);
      machine.randNorm();
      return machine;
    };

    return new ActionFragment(action);
  }

  public Fragment visitLimitBoundExpression(JoshLangParser.LimitBoundExpressionContext ctx) {
    EventHandlerAction operandAction = ctx.operand.accept(this).getCurrentAction();
    EventHandlerAction lowerBoundAction = ctx.lower.accept(this).getCurrentAction();
    EventHandlerAction upperBoundAction = ctx.upper.accept(this).getCurrentAction();

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
    EventHandlerAction operandAction = ctx.operand.accept(this).getCurrentAction();
    EventHandlerAction limitAction = ctx.limit.accept(this).getCurrentAction();
    EventHandlerAction action = (machine) -> {
      operandAction.apply(machine);
      limitAction.apply(machine);
      machine.bound(true, false);
      return machine;
    };

    return new ActionFragment(action);
  }

  public Fragment visitLimitMaxExpression(JoshLangParser.LimitMinExpressionContext ctx) {
    EventHandlerAction operandAction = ctx.operand.accept(this).getCurrentAction();
    EventHandlerAction limitAction = ctx.limit.accept(this).getCurrentAction();
    EventHandlerAction action = (machine) -> {
      operandAction.apply(machine);
      limitAction.apply(machine);
      machine.bound(false, true);
      return machine;
    };

    return new ActionFragment(action);
  }

  public Fragment visitCast(JoshLangParser.CastContext ctx) {
    EventHandlerAction operandAction = ctx.operand.accept(this).getCurrentAction();
    String newUnits = ctx.target.getText();

    EventHandlerAction action = (machine) -> {
      operandAction.apply(machine);
      machine.cast(newUnits, false);
      return machine;
    };

    return new ActionFragment(action);
  }

  public Fragment visitCastForce(JoshLangParser.CastForceContext ctx) {
    EventHandlerAction operandAction = ctx.operand.accept(this).getCurrentAction();
    String newUnits = ctx.target.getText();

    EventHandlerAction action = (machine) -> {
      operandAction.apply(machine);
      machine.cast(newUnits, true);
      return machine;
    };

    return new ActionFragment(action);
  }

  public Fragment visitCreateVariableExpression(
      JoshLangParser.CreateVariableExpressionContext ctx) {
    EventHandlerAction countAction = ctx.count.accept(this).getCurrentAction();
    String entityType = ctx.target.getText();

    EventHandlerAction action = (machine) -> {
      countAction.apply(machine);
      machine.makeEntity(entityType);
      return machine;
    };

    return new ActionFragment(action);
  }

  public Fragment visitAttrExpression(JoshLangParser.AttrExpressionContext ctx) {
    String attrName = ctx.getChild(2).getText();

    EventHandlerAction action = (machine) -> {
      machine.pushAttribute(attrName);
      return machine;
    };

    return new ActionFragment(action);
  }

  public Fragment visitSpatialQuery(JoshLangParser.SpatialQueryContext ctx) {
    EventHandlerAction targetAction = ctx.target.accept(this).getCurrentAction();
    EventHandlerAction distanceAction = ctx.distance.accept(this).getCurrentAction();

    EventHandlerAction action = (machine) -> {
      targetAction.apply(machine);
      distanceAction.apply(machine);
      machine.executeSpatialQuery();
      return machine;
    };

    return new ActionFragment(action);
  }

  public Fragment visitSingleParamFunctionCall(JoshLangParser.SingleParamFunctionCallContext ctx) {
    EventHandlerAction operandAction = ctx.operand.accept(this).getCurrentAction();
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

  public Fragment visitPosition(JoshLangParser.PositionContext ctx) {
    String payload = ctx.getText();
    EngineValue decoratedPayload = engineValueFactory.build(
        payload,
        new Units("position")
    );

    EventHandlerAction action = (machine) -> {
      machine.push(decoratedPayload);
      return machine;
    };

    return new ActionFragment(action);
  }

  public Fragment visitCreateSingleExpression(JoshLangParser.CreateSingleExpressionContext ctx) {
    String entityName = ctx.getChild(0).getText();

    EventHandlerAction action = (machine) -> {
      machine.push(singleCount);
      machine.create(entityName);
      return machine;
    };

    return new ActionFragment(action);
  }

  public Fragment visitAssignment(JoshLangParser.AssignmentContext ctx) {
    String identifierName = ctx.getChild(1).getText();
    EventHandlerAction valAction = ctx.val.accept(this).getCurrentAction();

    EventHandlerAction action = (machine) -> {
      valAction.apply(machine);
      machine.saveLocalVariable(identifierName);
      return machine;
    };

    return new ActionFragment(action);
  }

  public Fragment visitLambda(JoshLangParser.LambdaContext ctx) {
    EventHandlerAction innerAction = ctx
        .getChild(0)
        .accept(this)
        .getCurrentAction();

    EventHandlerAction action = (machine) -> {
      innerAction.apply(machine);
      machine.end();
      return machine;
    };

    return new ActionFragment(action);
  }

  public Fragment visitReturn(JoshLangParser.ReturnContext ctx) {
    EventHandlerAction innerAction = ctx
        .getChild(1)
        .accept(this)
        .getCurrentAction();

    EventHandlerAction action = (machine) -> {
      innerAction.apply(machine);
      machine.end();
      return machine;
    };

    return new ActionFragment(action);
  }

  public Fragment visitFullBody(JoshLangParser.FullBodyContext ctx) {
    List<EventHandlerAction> innerActions = new ArrayList<>();

    int numChildren = ctx.getChildCount();
    int numStatements = numChildren - 2;
    for (int statementIndex = 0; statementIndex < numStatements; statementIndex++) {
      int childIndex = statementIndex + 2;

      EventHandlerAction statementAction = ctx
          .getChild(childIndex)
          .accept(this)
          .getCurrentAction();

      innerActions.add(statementAction);
    }

    EventHandlerAction action = (machine) -> {

      for (EventHandlerAction innerAction : innerActions) {
        innerAction.apply(machine);
        if (!machine.isEnded()) {
          break;
        }
      }

      if (machine.isEnded()) {
        throw new IllegalStateException("Event handler finished without returning a value.");
      }

      return machine;
    };

    return new ActionFragment(action);
  }

  public Fragment visitEventHandlerGroupMemberInner(
      JoshLangParser.EventHandlerGroupMemberInnerContext ctx) {
    EventHandlerAction handlerAction = ctx.target.accept(this).getCurrentAction();
    return new ActionFragment(handlerAction);
  }

  public Fragment visitConditionalIfEventHandlerGroupMember(
      JoshLangParser.ConditionalIfEventHandlerGroupMemberContext ctx) {
    EventHandlerAction innerAction = ctx.inner.accept(this).getCurrentAction();
    EventHandlerAction conditionAction = ctx.target.accept(this).getCurrentAction();

    CompiledCallable decoratedInterpreterAction = new PushDownMachineCallable(innerAction);
    CompiledCallable decoratedConditionAction = new PushDownMachineCallable(conditionAction);
    CompiledSelector decoratedConditionSelector = new CompiledSelectorFromCallable(
        decoratedConditionAction
    );

    return new CompiledCallableFragment(decoratedInterpreterAction, decoratedConditionSelector);
  }

  public Fragment visitConditionalElifEventHandlerGroupMember(
      JoshLangParser.ConditionalElifEventHandlerGroupMemberContext ctx) {
    EventHandlerAction innerAction = ctx.inner.accept(this).getCurrentAction();
    EventHandlerAction conditionAction = ctx.target.accept(this).getCurrentAction();

    CompiledCallable decoratedInterpreterAction = new PushDownMachineCallable(innerAction);
    CompiledCallable decoratedConditionAction = new PushDownMachineCallable(conditionAction);
    CompiledSelector decoratedConditionSelector = new CompiledSelectorFromCallable(
        decoratedConditionAction
    );

    return new CompiledCallableFragment(decoratedInterpreterAction, decoratedConditionSelector);
  }

  public Fragment visitConditionalElseEventHandlerGroupMember(
      JoshLangParser.ConditionalElifEventHandlerGroupMemberContext ctx) {
    EventHandlerAction innerAction = ctx.inner.accept(this).getCurrentAction();
    CompiledCallable decoratedInterpreterAction = new PushDownMachineCallable(innerAction);
    return new CompiledCallableFragment(decoratedInterpreterAction);
  }

  public Fragment visitEventHandlerGroupSingle(JoshLangParser.EventHandlerGroupSingleContext ctx) {
    String fullName = ctx.name.getText();
    Fragment innerFragment = ctx.getChild(1).accept(this);

    if (innerFragment.getCompiledSelector().isPresent()) {
      throw new RuntimeException("Unexpected selector on non-conditional event handler");
    }

    EventKey eventKey = buildEventKey(fullName);
    EventHandler eventHandler = new EventHandler(
        innerFragment.getCompiledCallable(),
        eventKey.getAttribute(),
        eventKey.getEvent()
    );

    EventHandlerGroupBuilder eventHandlerGroupBuilder = new EventHandlerGroupBuilder();
    eventHandlerGroupBuilder.addEventHandler(eventHandler);
    eventHandlerGroupBuilder.setEventKey(eventKey);

    return new EventHandlerGroupFragment(eventHandlerGroupBuilder);
  }

  public Fragment visitEventHandlerGroupMultiple(
      JoshLangParser.EventHandlerGroupMultipleContext ctx) {
    String fullName = ctx.name.getText();
    EventKey eventKey = buildEventKey(fullName);

    EventHandlerGroupBuilder groupBuilder = new EventHandlerGroupBuilder();

    int numBranches = ctx.getChildCount() - 1;
    for (int branchIndex = 0; branchIndex < numBranches; branchIndex++) {
      int childIndex = branchIndex + 1;
      Fragment childFragment = ctx.getChild(childIndex).accept(this);

      if (childFragment.getCompiledSelector().isPresent()) {
        groupBuilder.addEventHandler(new EventHandler(
            childFragment.getCompiledCallable(),
            eventKey.getAttribute(),
            eventKey.getEvent(),
            childFragment.getCompiledSelector().get()
        ));
      } else {
        groupBuilder.addEventHandler(new EventHandler(
            childFragment.getCompiledCallable(),
            eventKey.getAttribute(),
            eventKey.getEvent()
        ));
      }
    }

    return new EventHandlerGroupFragment(groupBuilder);
  }

  public Fragment visitStateStanza(JoshLangParser.StateStanzaContext ctx) {
    List<EventHandlerGroupBuilder> groups = new ArrayList<>();
    String stateName = ctx.getChild(2).getText();

    int numHandlerGroups = ctx.getChildCount() - 5;
    for (int handlerGroupIndex = 0; handlerGroupIndex < numHandlerGroups; handlerGroupIndex++) {
      int childIndex = handlerGroupIndex + 3;
      Fragment childFragment = ctx.getChild(childIndex).accept(this);
      EventHandlerGroupBuilder groupBuilder = childFragment.getEventHandlerGroup();
      groupBuilder.setState(stateName);
      groups.add(groupBuilder);
    }

    return new StateFragment(groups);
  }

  public Fragment visitEntityStanza(JoshLangParser.EntityStanzaContext ctx) {
    int numChildren = ctx.getChildCount();
    int numInner = numChildren - 5;

    String entityType = ctx.getChild(1).getText();
    String identifier = ctx.getChild(2).getText();
    String closeEntityType = ctx.getChild(numChildren - 1).getText();
    if (!entityType.equals(closeEntityType)) {
      String message = String.format(
          "Stanza start and end type different: %s, %s",
          entityType,
          closeEntityType
      );
      throw new IllegalArgumentException(message);
    }

    EntityBuilder entityBuilder = new EntityBuilder();
    entityBuilder.setName(identifier);

    for (int innerIndex = 0; innerIndex < numInner; innerIndex++) {
      int childIndex = innerIndex + 3;
      Fragment childFragment = ctx.getChild(childIndex).accept(this);
      EventHandlerGroupBuilder groupBuilder = childFragment.getEventHandlerGroup();
      entityBuilder.addEventHandlerGroup(groupBuilder.buildKey(), groupBuilder.build());
    }

    EntityPrototype prototype = new EntityPrototype(
        identifier,
        getEntityType(entityType),
        entityBuilder
    );

    return new EntityFragment(prototype);
  }

  public Fragment visitNoopConversion(JoshLangParser.NoopConversionContext ctx) {
    String aliasName = ctx.getChild(1).getText();
    Conversion conversion = new NoopConversion(new Units(aliasName));
    return new ConversionFragment(conversion);
  }

  public Fragment visitActiveConversion(JoshLangParser.ActiveConversionContext ctx) {
    String destinationUnitsName = ctx.getChild(0).getText();
    Units destinationUnits = new Units(destinationUnitsName);
    EventHandlerAction action = ctx.getChild(2).accept(this).getCurrentAction();
    CompiledCallable conversionLogic = new PushDownMachineCallable(action);

    Conversion conversion = new DirectConversion(
        destinationUnits,
        destinationUnits,
        conversionLogic
    );

    return new ConversionFragment(conversion);
  }

  public Fragment visitUnitStanza(JoshLangParser.UnitStanzaContext ctx) {
    String sourceUnitName = ctx.getChild(2).getText();
    Units sourceUnits = new Units(sourceUnitName);

    List<Conversion> conversions = new ArrayList<>();
    int numChildren = ctx.getChildCount();
    int numConversions = numChildren - 5;
    for (int conversionIndex = 0; conversionIndex < numConversions; conversionIndex++) {
      int childIndex = conversionIndex + 3;
      Fragment childFragment = ctx.getChild(childIndex).accept(this);
      Conversion incompleteConversion = childFragment.getConversion();
      Conversion completeConversion = new DirectConversion(
          sourceUnits,
          incompleteConversion.getDestinationUnits(),
          incompleteConversion.getConversionCallable()
      );
      conversions.add(completeConversion);
    }

    return new ConversionsFragment(conversions);
  }

  public Fragment visitConfigStatement(JoshLangParser.ConfigStatementContext ctx) {
    throw new RuntimeException("Configuration statements reserved for future use.");
  }

  public Fragment visitImportStatement(JoshLangParser.ImportStatementContext ctx) {
    throw new RuntimeException("Import statements reserved for future use.");
  }

  public Fragment visitProgram(JoshLangParser.ProgramContext ctx) {
    return null;  // TODO
  }

  private EngineValue parseUnitsValue(JoshLangParser.UnitsValueContext ctx) {
    BigDecimal number = BigDecimal.valueOf(Double.parseDouble(ctx.getChild(0).getText()));
    String unitsText = ctx.getChild(1).getText();
    return engineValueFactory.build(number, new Units(unitsText));
  }

  private boolean isEventName(String candidate) {
    return switch (candidate) {
      case "init", "start", "step", "end", "remove", "constant" -> true;
      default -> false;
    };
  }

  private EventKey buildEventKey(String fullName) {
    String[] namePieces = fullName.split("\\.");
    String candidateEventName = namePieces[namePieces.length - 1];
    boolean endsWithEventName = isEventName(candidateEventName);

    StringJoiner attributeNameJoiner = new StringJoiner(".");
    for (int i = 0; i < namePieces.length - 1; i++) {
      attributeNameJoiner.add(namePieces[i]);
    }

    String attributeName;
    String eventName;
    if (endsWithEventName) {
      attributeName = attributeNameJoiner.toString();
      eventName = candidateEventName;
    } else {
      attributeNameJoiner.add(candidateEventName);
      attributeName = attributeNameJoiner.toString();
      eventName = "constant";
    }

    return new EventKey(attributeName, eventName);
  }

  private EntityType getEntityType(String entityType) {
    return switch (entityType) {
      case "agent" -> EntityType.AGENT;
      case "disturbance" -> EntityType.DISTURBANCE;
      case "external" -> EntityType.EXTERNAL_RESOURCE;
      case "patch" -> EntityType.PATCH;
      case "simulation" -> EntityType.SIMULATION;
      default -> throw new IllegalArgumentException("Unknown entity type: " + entityType);
    };
  }
}
