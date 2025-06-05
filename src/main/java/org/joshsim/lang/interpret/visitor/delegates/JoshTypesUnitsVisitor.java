/**
 * Delegate for types and units.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret.visitor.delegates;

import org.joshsim.engine.func.CompiledCallable;
import org.joshsim.engine.value.converter.Conversion;
import org.joshsim.engine.value.converter.DirectConversion;
import org.joshsim.engine.value.converter.NoopConversion;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.engine.value.type.EngineValue;
import org.joshsim.lang.antlr.JoshLangParser;
import org.joshsim.lang.interpret.BridgeGetter;
import org.joshsim.lang.interpret.ReservedWordChecker;
import org.joshsim.lang.interpret.ValueResolver;
import org.joshsim.lang.interpret.action.EventHandlerAction;
import org.joshsim.lang.interpret.fragment.ActionFragment;
import org.joshsim.lang.interpret.fragment.ConversionFragment;
import org.joshsim.lang.interpret.fragment.Fragment;
import org.joshsim.lang.interpret.machine.PushDownMachineCallable;
import org.joshsim.lang.interpret.visitor.JoshParserToMachineVisitor;


/**
 * Visitor delegate for handling special operations which manipulate and instantiate types / units.
 *
 * <p>Visitor delegate for handling operations which interact with type, type instantiation, and
 * type definitions or manipulate units.</p>
 */
public class JoshTypesUnitsVisitor implements JoshVisitorDelegate {

  private final JoshParserToMachineVisitor parent;
  private final EngineValueFactory engineValueFactory;
  private final EngineValue singleCount;
  private final BridgeGetter bridgeGetter;

  /**
   * Create a new visitor for types and units.
   *
   * @param toolbox The toolbox through which visitors can access supporting objects.
   */
  public JoshTypesUnitsVisitor(DelegateToolbox toolbox) {
    parent = toolbox.getParent();
    engineValueFactory = toolbox.getValueFactory();
    bridgeGetter = toolbox.getBridgeGetter();
    singleCount = engineValueFactory.build(1, Units.of("count"));
  }

  /**
   * Visit a user requested casting operation without forcing.
   *
   * <p>Visit a user-requested casting operation that operates without forcing. Without forcing
   * means that a runtime error will be generated if a conversion is not supported. In contrast,
   * forcing means that the cast will happen even if the engine does not believe it to be valid.</p>
   *
   * @param ctx The ANTLR context from which to parse the cast expression.
   * @return Fragment containing the cast expression parsed.
   */
  public Fragment visitCast(JoshLangParser.CastContext ctx) {
    EventHandlerAction operandAction = ctx.operand.accept(parent).getCurrentAction();
    Units newUnits = Units.of(ctx.target.getText());

    EventHandlerAction action = (machine) -> {
      operandAction.apply(machine);
      machine.cast(newUnits, false);
      return machine;
    };

    return new ActionFragment(action);
  }

  public Fragment visitCastForce(JoshLangParser.CastForceContext ctx) {
    EventHandlerAction operandAction = ctx.operand.accept(parent).getCurrentAction();
    Units newUnits = Units.of(ctx.target.getText());

    EventHandlerAction action = (machine) -> {
      operandAction.apply(machine);
      machine.cast(newUnits, true);
      return machine;
    };

    return new ActionFragment(action);
  }

  public Fragment visitNoopConversion(JoshLangParser.NoopConversionContext ctx) {
    String aliasName = ctx.getChild(1).getText();
    Conversion conversion = new NoopConversion(Units.of(aliasName));
    return new ConversionFragment(conversion);
  }

  public Fragment visitActiveConversion(JoshLangParser.ActiveConversionContext ctx) {
    String destinationUnitsName = ctx.getChild(0).getText();
    Units destinationUnits = Units.of(destinationUnitsName);
    EventHandlerAction action = ctx.getChild(2).accept(parent).getCurrentAction();
    CompiledCallable conversionLogic = makeCallableMachine(action);

    Conversion conversion = new DirectConversion(
        destinationUnits,
        destinationUnits,
        conversionLogic
    );

    return new ConversionFragment(conversion);
  }

  public Fragment visitCreateVariableExpression(
      JoshLangParser.CreateVariableExpressionContext ctx) {
    EventHandlerAction countAction = ctx.count.accept(parent).getCurrentAction();
    String entityName = ctx.target.getText();

    EventHandlerAction action = (machine) -> {
      countAction.apply(machine);
      machine.createEntity(entityName);
      return machine;
    };

    return new ActionFragment(action);
  }

  public Fragment visitAttrExpression(JoshLangParser.AttrExpressionContext ctx) {
    EventHandlerAction expressionAction = ctx.getChild(0).accept(parent).getCurrentAction();
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
    EventHandlerAction distanceAction = ctx.distance.accept(parent).getCurrentAction();

    EventHandlerAction action = (machine) -> {
      distanceAction.apply(machine);
      machine.executeSpatialQuery(targetResolver);
      return machine;
    };

    return new ActionFragment(action);
  }

  public Fragment visitPosition(JoshLangParser.PositionContext ctx) {
    Fragment unitsFragment1 = ctx.getChild(0).accept(parent);
    String type1 = ctx.getChild(1).getText();
    Fragment unitsFragment2 = ctx.getChild(3).accept(parent);
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
    EventHandlerAction valAction = ctx.val.accept(parent).getCurrentAction();

    EventHandlerAction action = (machine) -> {
      valAction.apply(machine);
      machine.saveLocalVariable(identifierName);
      return machine;
    };

    return new ActionFragment(action);
  }

  private PushDownMachineCallable makeCallableMachine(EventHandlerAction action) {
    return new PushDownMachineCallable(action, bridgeGetter);
  }

}
