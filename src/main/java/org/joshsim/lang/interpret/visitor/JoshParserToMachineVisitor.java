/**
 * Visitor for Josh sources that parses to an interpreter runtime builder.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret.visitor;

import java.util.ArrayList;
import java.util.List;
import org.joshsim.compat.CompatibilityLayerKeeper;
import org.joshsim.compat.CompatibleStringJoiner;
import org.joshsim.engine.entity.base.EntityBuilder;
import org.joshsim.engine.entity.handler.EventHandler;
import org.joshsim.engine.entity.handler.EventHandlerGroupBuilder;
import org.joshsim.engine.entity.handler.EventKey;
import org.joshsim.engine.entity.prototype.EntityPrototype;
import org.joshsim.engine.entity.prototype.ParentlessEntityPrototype;
import org.joshsim.engine.entity.type.EntityType;
import org.joshsim.engine.func.CompiledCallable;
import org.joshsim.engine.func.CompiledSelector;
import org.joshsim.engine.func.CompiledSelectorFromCallable;
import org.joshsim.engine.value.converter.Conversion;
import org.joshsim.engine.value.converter.DirectConversion;
import org.joshsim.engine.value.converter.NoopConversion;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.engine.value.type.EngineValue;
import org.joshsim.lang.antlr.JoshLangBaseVisitor;
import org.joshsim.lang.antlr.JoshLangParser;
import org.joshsim.lang.interpret.BridgeGetter;
import org.joshsim.lang.interpret.ReservedWordChecker;
import org.joshsim.lang.interpret.ValueResolver;
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
import org.joshsim.lang.interpret.fragment.ProgramBuilder;
import org.joshsim.lang.interpret.fragment.ProgramFragment;
import org.joshsim.lang.interpret.fragment.StateFragment;
import org.joshsim.lang.interpret.machine.PushDownMachineCallable;
import org.joshsim.lang.interpret.visitor.delegates.*;


/**
 * Visitor which parses Josh source by using Fragments.
 *
 * <p>Visitor which acts as composite of delegates which handle sections of the language
 * functionality where each produces a Fragment. These fragments handle the fact that each handler
 * produces a separate component with a different type that get combined into callable actions which
 * actually get executed at runtime.</p>
 */
@SuppressWarnings("checkstyle:MissingJavaDocMethod")  // Can't use override because of generics.
public class JoshParserToMachineVisitor extends JoshLangBaseVisitor<Fragment> {

  private final BridgeGetter bridgeGetter;
  private final EngineValueFactory engineValueFactory;

  private final JoshValueVisitor valueVisitor;
  private final JoshMathematicsVisitor mathematicsVisitor;
  private final JoshStringOpeationVisitor stringOpeationVisitor;
  private final JoshLogicalVisitor logicalVisitor;
  private final JoshDistributionVisitor distributionVisitor;

  /**
   * Create a new visitor which has some commonly used values cached.
   *
   * @param valueFactory The factory to use in building engine values within this visitor.
   * @param bridgeGetter The bridge getter to use in accessing a bridge for operations.
   */
  public JoshParserToMachineVisitor(EngineValueFactory valueFactory, BridgeGetter bridgeGetter) {
    super();

    DelegateToolbox toolbox = new DelegateToolbox(this, valueFactory);
    valueVisitor = new JoshValueVisitor(toolbox);
    mathematicsVisitor = new JoshMathematicsVisitor(toolbox);
    stringOpeationVisitor = new JoshStringOpeationVisitor(toolbox);
    logicalVisitor = new JoshLogicalVisitor(toolbox);
    distributionVisitor = new JoshDistributionVisitor(toolbox);

    this.bridgeGetter = bridgeGetter;
    this.engineValueFactory = valueFactory;
  }

  public Fragment visitIdentifier(JoshLangParser.IdentifierContext ctx) {
    return valueVisitor.visitIdentifier(ctx);
  }

  public Fragment visitNumber(JoshLangParser.NumberContext ctx) {
    return valueVisitor.visitNumber(ctx);
  }

  public Fragment visitUnitsValue(JoshLangParser.UnitsValueContext ctx) {
    return valueVisitor.visitUnitsValue(ctx);
  }

  public Fragment visitString(JoshLangParser.StringContext ctx) {
    return valueVisitor.visitString(ctx);
  }

  public Fragment visitBool(JoshLangParser.BoolContext ctx) {
    return valueVisitor.visitBool(ctx);
  }

  public Fragment visitAllExpression(JoshLangParser.AllExpressionContext ctx) {
    return valueVisitor.visitAllExpression(ctx);
  }

  public Fragment visitExternalValue(JoshLangParser.ExternalValueContext ctx) {
    return valueVisitor.visitExternalValue(ctx);
  }

  public Fragment visitExternalValueAtTime(JoshLangParser.ExternalValueAtTimeContext ctx) {
    return valueVisitor.visitExternalValueAtTime(ctx);
  }

  public Fragment visitMapLinear(JoshLangParser.MapLinearContext ctx) {
    return mathematicsVisitor.visitMapLinear(ctx);
  }

  public Fragment visitMapParam(JoshLangParser.MapParamContext ctx) {
    return mathematicsVisitor.visitMapParam(ctx);
  }

  public Fragment visitMapParamParam(JoshLangParser.MapParamParamContext ctx) {
    return mathematicsVisitor.visitMapParamParam(ctx);
  }

  public Fragment visitAdditionExpression(JoshLangParser.AdditionExpressionContext ctx) {
    return mathematicsVisitor.visitAdditionExpression(ctx);
  }

  public Fragment visitMultiplyExpression(JoshLangParser.MultiplyExpressionContext ctx) {
    return mathematicsVisitor.visitMultiplyExpression(ctx);
  }

  public Fragment visitPowExpression(JoshLangParser.PowExpressionContext ctx) {
    return mathematicsVisitor.visitPowExpression(ctx);
  }

  public Fragment visitParenExpression(JoshLangParser.ParenExpressionContext ctx) {
    return mathematicsVisitor.visitParenExpression(ctx);
  }

  public Fragment visitLimitBoundExpression(JoshLangParser.LimitBoundExpressionContext ctx) {
    return mathematicsVisitor.visitLimitBoundExpression(ctx);
  }

  public Fragment visitLimitMinExpression(JoshLangParser.LimitMinExpressionContext ctx) {
    return mathematicsVisitor.visitLimitMinExpression(ctx);
  }

  public Fragment visitLimitMaxExpression(JoshLangParser.LimitMaxExpressionContext ctx) {
    return mathematicsVisitor.visitLimitMaxExpression(ctx);
  }

  public Fragment visitConcatExpression(JoshLangParser.ConcatExpressionContext ctx) {
    return stringOpeationVisitor.visitConcatExpression(ctx);
  }

  public Fragment visitLogicalExpression(JoshLangParser.LogicalExpressionContext ctx) {
    return logicalVisitor.visitLogicalExpression(ctx);
  }

  public Fragment visitCondition(JoshLangParser.ConditionContext ctx) {
    return logicalVisitor.visitCondition(ctx);
  }

  public Fragment visitConditional(JoshLangParser.ConditionalContext ctx) {
    return logicalVisitor.visitConditional(ctx);
  }

  public Fragment visitFullConditional(JoshLangParser.FullConditionalContext ctx) {
    return logicalVisitor.visitFullConditional(ctx);
  }

  public Fragment visitFullElifBranch(JoshLangParser.FullElifBranchContext ctx) {
    return logicalVisitor.visitFullElifBranch(ctx);
  }

  public Fragment visitFullElseBranch(JoshLangParser.FullElseBranchContext ctx) {
    return logicalVisitor.visitFullElseBranch(ctx);
  }

  public Fragment visitSlice(JoshLangParser.SliceContext ctx) {
    return distributionVisitor.visitSlice(ctx);
  }

  public Fragment visitSampleSimple(JoshLangParser.SampleSimpleContext ctx) {
    return distributionVisitor.visitSampleSimple(ctx);
  }

  public Fragment visitSampleParam(JoshLangParser.SampleParamContext ctx) {
    return distributionVisitor.visitSampleParam(ctx);
  }

  public Fragment visitSampleParamReplacement(JoshLangParser.SampleParamReplacementContext ctx) {
    return distributionVisitor.visitSampleParamReplacement(ctx);
  }

  public Fragment visitUniformSample(JoshLangParser.UniformSampleContext ctx) {
    return distributionVisitor.visitUniformSample(ctx);
  }

  public Fragment visitNormalSample(JoshLangParser.NormalSampleContext ctx) {
    return distributionVisitor.visitNormalSample(ctx);
  }

  public Fragment visitCast(JoshLangParser.CastContext ctx) {
    EventHandlerAction operandAction = ctx.operand.accept(this).getCurrentAction();
    Units newUnits = Units.of(ctx.target.getText());

    EventHandlerAction action = (machine) -> {
      operandAction.apply(machine);
      machine.cast(newUnits, false);
      return machine;
    };

    return new ActionFragment(action);
  }

  public Fragment visitCastForce(JoshLangParser.CastForceContext ctx) {
    EventHandlerAction operandAction = ctx.operand.accept(this).getCurrentAction();
    Units newUnits = Units.of(ctx.target.getText());

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
    String entityName = ctx.target.getText();

    EventHandlerAction action = (machine) -> {
      countAction.apply(machine);
      machine.createEntity(entityName);
      return machine;
    };

    return new ActionFragment(action);
  }

  public Fragment visitAttrExpression(JoshLangParser.AttrExpressionContext ctx) {
    EventHandlerAction expressionAction = ctx.getChild(0).accept(this).getCurrentAction();
    String attrName = ctx.getChild(2).getText();
    ValueResolver resolver = new ValueResolver(engineValueFactory, attrName);

    EventHandlerAction action = (machine) -> {
      expressionAction.apply(machine);
      machine.pushAttribute(resolver);
      return machine;
    };

    return new ActionFragment(action);
  }

  public Fragment visitSpatialQuery(JoshLangParser.SpatialQueryContext ctx) {
    ValueResolver targetResolver = new ValueResolver(engineValueFactory, ctx.target.toString());
    EventHandlerAction distanceAction = ctx.distance.accept(this).getCurrentAction();

    EventHandlerAction action = (machine) -> {
      distanceAction.apply(machine);
      machine.executeSpatialQuery(targetResolver);
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
    // unitsValue (LATITUDE_ | LONGITUDE_) COMMA_ unitsValue (LATITUDE_ | LONGITUDE_)
    Fragment unitsFragment1 = ctx.getChild(0).accept(this);
    String type1 = ctx.getChild(1).getText();
    Fragment unitsFragment2 = ctx.getChild(3).accept(this);
    String type2 = ctx.getChild(4).getText();

    EventHandlerAction action = (machine) -> {
      unitsFragment1.getCurrentAction().apply(machine);
      machine.push(engineValueFactory.build(type1, Units.of("")));
      unitsFragment2.getCurrentAction().apply(machine);
      machine.push(engineValueFactory.build(type2, Units.of("")));
      machine.makePosition();
      return machine;
    };

    return new ActionFragment(action);
  }

  public Fragment visitCreateSingleExpression(JoshLangParser.CreateSingleExpressionContext ctx) {
    String entityName = ctx.target.getText();

    EventHandlerAction action = (machine) -> {
      machine.push(singleCount);
      machine.createEntity(entityName);
      return machine;
    };

    return new ActionFragment(action);
  }

  public Fragment visitAssignment(JoshLangParser.AssignmentContext ctx) {
    String identifierName = ctx.getChild(1).getText();
    ReservedWordChecker.checkVariableDeclaration(identifierName);
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
      int childIndex = statementIndex + 1;

      EventHandlerAction statementAction = ctx
          .getChild(childIndex)
          .accept(this)
          .getCurrentAction();

      innerActions.add(statementAction);
    }

    EventHandlerAction action = (machine) -> {
      for (EventHandlerAction innerAction : innerActions) {
        innerAction.apply(machine);
        if (machine.isEnded()) {
          break;
        }
      }

      if (!machine.isEnded()) {
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

    CompiledCallable decoratedInterpreterAction = makeCallableMachine(innerAction);
    CompiledCallable decoratedConditionAction = makeCallableMachine(conditionAction);
    CompiledSelector decoratedConditionSelector = new CompiledSelectorFromCallable(
        decoratedConditionAction
    );

    return new CompiledCallableFragment(decoratedInterpreterAction, decoratedConditionSelector);
  }

  public Fragment visitConditionalElifEventHandlerGroupMember(
      JoshLangParser.ConditionalElifEventHandlerGroupMemberContext ctx) {
    EventHandlerAction innerAction = ctx.inner.accept(this).getCurrentAction();
    EventHandlerAction conditionAction = ctx.target.accept(this).getCurrentAction();

    CompiledCallable decoratedInterpreterAction = makeCallableMachine(innerAction);
    CompiledCallable decoratedConditionAction = makeCallableMachine(conditionAction);
    CompiledSelector decoratedConditionSelector = new CompiledSelectorFromCallable(
        decoratedConditionAction
    );

    return new CompiledCallableFragment(decoratedInterpreterAction, decoratedConditionSelector);
  }

  public Fragment visitConditionalElseEventHandlerGroupMember(
      JoshLangParser.ConditionalElseEventHandlerGroupMemberContext ctx) {
    EventHandlerAction innerAction = ctx.inner.accept(this).getCurrentAction();
    CompiledCallable decoratedInterpreterAction = makeCallableMachine(innerAction);
    return new CompiledCallableFragment(decoratedInterpreterAction);
  }

  public Fragment visitEventHandlerGroupSingle(JoshLangParser.EventHandlerGroupSingleContext ctx) {
    String fullName = ctx.name.getText();
    Fragment innerFragment = ctx.getChild(1).accept(this);

    CompiledCallable innerCallable = makeCallableMachine(innerFragment.getCurrentAction());

    EventKey eventKey = buildEventKey(fullName);
    EventHandler eventHandler = new EventHandler(
        innerCallable,
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
    groupBuilder.setEventKey(eventKey);

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

  public Fragment visitEventHandlerGeneral(JoshLangParser.EventHandlerGeneralContext ctx) {
    Fragment fragment = ctx.getChild(0).accept(this);
    String attributeName = fragment.getEventHandlerGroup().getAttribute();
    ReservedWordChecker.checkVariableDeclaration(attributeName);
    return fragment;
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

      for (EventHandlerGroupBuilder groupBuilder : childFragment.getEventHandlerGroups()) {
        entityBuilder.addEventHandlerGroup(groupBuilder.buildKey(), groupBuilder.build());
      }
    }

    EntityPrototype prototype = new ParentlessEntityPrototype(
        identifier,
        getEntityType(entityType),
        entityBuilder
    );

    return new EntityFragment(prototype);
  }

  public Fragment visitNoopConversion(JoshLangParser.NoopConversionContext ctx) {
    String aliasName = ctx.getChild(1).getText();
    Conversion conversion = new NoopConversion(Units.of(aliasName));
    return new ConversionFragment(conversion);
  }

  public Fragment visitActiveConversion(JoshLangParser.ActiveConversionContext ctx) {
    String destinationUnitsName = ctx.getChild(0).getText();
    Units destinationUnits = Units.of(destinationUnitsName);
    EventHandlerAction action = ctx.getChild(2).accept(this).getCurrentAction();
    CompiledCallable conversionLogic = makeCallableMachine(action);

    Conversion conversion = new DirectConversion(
        destinationUnits,
        destinationUnits,
        conversionLogic
    );

    return new ConversionFragment(conversion);
  }

  public Fragment visitUnitStanza(JoshLangParser.UnitStanzaContext ctx) {
    String sourceUnitName = ctx.getChild(2).getText();
    Units sourceUnits = Units.of(sourceUnitName);

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
    ProgramBuilder builder = new ProgramBuilder();
    int numChildren = ctx.getChildCount();
    for (int i = 0; i < numChildren; i++) {
      Fragment childFragment = ctx.getChild(i).accept(this);
      builder.add(childFragment);
    }
    return new ProgramFragment(builder);
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

    CompatibleStringJoiner attributeNameJoiner = CompatibilityLayerKeeper
        .get()
        .createStringJoiner(".");

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
      case "organism" -> EntityType.AGENT;
      case "management" -> EntityType.AGENT;
      case "disturbance" -> EntityType.DISTURBANCE;
      case "external" -> EntityType.EXTERNAL_RESOURCE;
      case "patch" -> EntityType.PATCH;
      case "simulation" -> EntityType.SIMULATION;
      default -> throw new IllegalArgumentException("Unknown entity type: " + entityType);
    };
  }

  private PushDownMachineCallable makeCallableMachine(EventHandlerAction action) {
    return new PushDownMachineCallable(action, bridgeGetter);
  }
}
