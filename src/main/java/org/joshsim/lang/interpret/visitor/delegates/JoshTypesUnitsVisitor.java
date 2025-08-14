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
import org.joshsim.lang.interpret.fragment.josh.ActionFragment;
import org.joshsim.lang.interpret.fragment.josh.ConversionFragment;
import org.joshsim.lang.interpret.fragment.josh.JoshFragment;
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
   * @return JoshFragment containing the cast expression parsed.
   */
  public JoshFragment visitCast(JoshLangParser.CastContext ctx) {
    EventHandlerAction operandAction = ctx.operand.accept(parent).getCurrentAction();
    Units newUnits = Units.of(ctx.target.getText());

    EventHandlerAction action = (machine) -> {
      operandAction.apply(machine);
      machine.cast(newUnits, false);
      return machine;
    };

    return new ActionFragment(action);
  }

  /**
   * Visit a user requested casting operation with forcing.
   *
   * <p>Visit a user-requested casting operation that operates with forcing. With forcing
   * means that the cast will happen even if the engine does not believe it to be valid,
   * which may lead to unexpected results but avoids runtime errors.</p>
   *
   * @param ctx The ANTLR context from which to parse the forced cast expression.
   * @return JoshFragment containing the forced cast expression parsed.
   */
  public JoshFragment visitCastForce(JoshLangParser.CastForceContext ctx) {
    EventHandlerAction operandAction = ctx.operand.accept(parent).getCurrentAction();
    Units newUnits = Units.of(ctx.target.getText());

    EventHandlerAction action = (machine) -> {
      operandAction.apply(machine);
      machine.cast(newUnits, true);
      return machine;
    };

    return new ActionFragment(action);
  }

  /**
   * Visit a no-operation unit conversion.
   *
   * <p>Process a unit conversion that doesn't perform any actual conversion but
   * creates an alias for a unit.</p>
   *
   * @param ctx The ANTLR context from which to parse the no-op conversion.
   * @return JoshFragment containing the no-op conversion.
   */
  public JoshFragment visitNoopConversion(JoshLangParser.NoopConversionContext ctx) {
    String aliasName = ctx.getChild(1).getText();
    Conversion conversion = new NoopConversion(Units.of(aliasName));
    return new ConversionFragment(conversion);
  }

  /**
   * Visit an active unit conversion.
   *
   * <p>Process a unit conversion that actively converts values from one unit to another
   * using the provided conversion logic.</p>
   *
   * @param ctx The ANTLR context from which to parse the active conversion.
   * @return JoshFragment containing the active conversion.
   */
  public JoshFragment visitActiveConversion(JoshLangParser.ActiveConversionContext ctx) {
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

  /**
   * Visit an expression that creates a specified number of entities.
   *
   * <p>Process an expression that creates a specified number of entities of a given type.</p>
   *
   * @param ctx The ANTLR context from which to parse the create variable expression.
   * @return JoshFragment containing the create variable expression parsed.
   */
  public JoshFragment visitCreateVariableExpression(
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

  /**
   * Visit an attribute access expression.
   *
   * <p>Process an expression that accesses an attribute of an entity or value based on that
   * complex type's definition.</p>
   *
   * @param ctx The ANTLR context from which to parse the attribute expression.
   * @return JoshFragment containing the attribute expression parsed.
   */
  public JoshFragment visitAttrExpression(JoshLangParser.AttrExpressionContext ctx) {
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

  /**
   * Visit a spatial query expression.
   *
   * <p>Process an expression that performs a spatial query to find entities within a specified
   * distance.</p>
   *
   * @param ctx The ANTLR context from which to parse the spatial query.
   * @return JoshFragment containing the spatial query parsed.
   */
  public JoshFragment visitSpatialQuery(JoshLangParser.SpatialQueryContext ctx) {
    ValueResolver targetResolver = new ValueResolver(engineValueFactory, ctx.target.getText());
    EventHandlerAction distanceAction = ctx.distance.accept(parent).getCurrentAction();

    EventHandlerAction action = (machine) -> {
      distanceAction.apply(machine);
      machine.executeSpatialQuery(targetResolver);
      return machine;
    };

    return new ActionFragment(action);
  }

  /**
   * Visit a position creation expression.
   *
   * <p>Process an expression that creates a position from two coordinate values with their
   * respective types.</p>
   *
   * @param ctx The ANTLR context from which to parse the position creation.
   * @return JoshFragment containing the position creation parsed.
   */
  public JoshFragment visitPosition(JoshLangParser.PositionContext ctx) {
    JoshFragment unitsFragment1 = ctx.getChild(0).accept(parent);
    String type1 = ctx.getChild(1).getText();
    JoshFragment unitsFragment2 = ctx.getChild(3).accept(parent);
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

  /**
   * Visit an expression that creates a single entity.
   *
   * <p>Process an expression that creates exactly one entity of a given type.</p>
   *
   * @param ctx The ANTLR context from which to parse the create single expression.
   * @return JoshFragment containing the create single expression parsed.
   */
  public JoshFragment visitCreateSingleExpression(JoshLangParser.CreateSingleExpressionContext ctx) {
    String entityName = ctx.target.getText();

    EventHandlerAction action = (machine) -> {
      machine.push(singleCount);
      machine.createEntity(entityName);
      return machine;
    };

    return new ActionFragment(action);
  }

  /**
   * Visit a variable assignment expression.
   *
   * <p>Process an expression that assigns a value to a variable.</p>
   *
   * @param ctx The ANTLR context from which to parse the assignment.
   * @return JoshFragment containing the assignment parsed.
   * @throws IllegalArgumentException if the identifier name is a reserved word.
   */
  public JoshFragment visitAssignment(JoshLangParser.AssignmentContext ctx) {
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

  /**
   * Creates a PushDownMachineCallable from an EventHandlerAction.
   *
   * <p>Wraps an EventHandlerAction in a PushDownMachineCallable to make it usable
   * as a CompiledCallable in the conversion system.</p>
   *
   * @param action The EventHandlerAction to wrap.
   * @return A PushDownMachineCallable that wraps the provided action.
   */
  private PushDownMachineCallable makeCallableMachine(EventHandlerAction action) {
    // Unit conversion actions need to end the machine, unlike mathematical operations
    // used within larger expressions
    EventHandlerAction wrappedAction = (machine) -> {
      action.apply(machine);
      machine.end();
      return machine;
    };
    return new PushDownMachineCallable(wrappedAction, bridgeGetter);
  }

}
