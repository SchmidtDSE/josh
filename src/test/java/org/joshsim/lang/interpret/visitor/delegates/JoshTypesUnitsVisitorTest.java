package org.joshsim.lang.interpret.visitor.delegates;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.antlr.v4.runtime.tree.ParseTree;
import org.joshsim.engine.func.CompiledCallable;
import org.joshsim.engine.value.converter.Conversion;
import org.joshsim.engine.value.converter.DirectConversion;
import org.joshsim.engine.value.converter.NoopConversion;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.engine.value.type.EngineValue;
import org.joshsim.lang.antlr.JoshLangParser.ActiveConversionContext;
import org.joshsim.lang.antlr.JoshLangParser.AssignmentContext;
import org.joshsim.lang.antlr.JoshLangParser.AttrExpressionContext;
import org.joshsim.lang.antlr.JoshLangParser.CastContext;
import org.joshsim.lang.antlr.JoshLangParser.CastForceContext;
import org.joshsim.lang.antlr.JoshLangParser.CreateSingleExpressionContext;
import org.joshsim.lang.antlr.JoshLangParser.CreateVariableExpressionContext;
import org.joshsim.lang.antlr.JoshLangParser.ExpressionContext;
import org.joshsim.lang.antlr.JoshLangParser.IdentifierContext;
import org.joshsim.lang.antlr.JoshLangParser.NoopConversionContext;
import org.joshsim.lang.antlr.JoshLangParser.PositionContext;
import org.joshsim.lang.antlr.JoshLangParser.SpatialQueryContext;
import org.joshsim.lang.interpret.BridgeGetter;
import org.joshsim.lang.interpret.ReservedWordChecker;
import org.joshsim.lang.interpret.ValueResolver;
import org.joshsim.lang.interpret.action.EventHandlerAction;
import org.joshsim.lang.interpret.fragment.ActionFragment;
import org.joshsim.lang.interpret.fragment.ConversionFragment;
import org.joshsim.lang.interpret.fragment.Fragment;
import org.joshsim.lang.interpret.machine.EventHandlerMachine;
import org.joshsim.lang.interpret.machine.PushDownMachineCallable;
import org.joshsim.lang.interpret.visitor.JoshParserToMachineVisitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JoshTypesUnitsVisitorTest {

  private DelegateToolbox toolbox;
  private JoshParserToMachineVisitor parent;
  private BridgeGetter bridgeGetter;
  private EngineValueFactory valueFactory;
  private JoshTypesUnitsVisitor visitor;
  private EngineValue mockValue;
  private ReservedWordChecker reservedWordChecker;

  @BeforeEach
  void setUp() {
    toolbox = mock(DelegateToolbox.class);
    parent = mock(JoshParserToMachineVisitor.class);
    bridgeGetter = mock(BridgeGetter.class);
    valueFactory = mock(EngineValueFactory.class);
    mockValue = mock(EngineValue.class);
    reservedWordChecker = mock(ReservedWordChecker.class);

    when(toolbox.getParent()).thenReturn(parent);
    when(toolbox.getBridgeGetter()).thenReturn(bridgeGetter);
    when(toolbox.getValueFactory()).thenReturn(valueFactory);

    visitor = new JoshTypesUnitsVisitor(toolbox);
  }

  @Test
  void testVisitCast() {
    // Mock
    CastContext context = mock(CastContext.class);
    ExpressionContext operandContext = mock(ExpressionContext.class);
    IdentifierContext targetContext = mock(IdentifierContext.class);
    Fragment operandFragment = mock(Fragment.class);
    final EventHandlerAction operandAction = mock(EventHandlerAction.class);

    // Set up the context.operand and context.target fields
    context.operand = operandContext;
    context.target = targetContext;
    when(operandContext.accept(parent)).thenReturn(operandFragment);
    when(operandFragment.getCurrentAction()).thenReturn(operandAction);
    when(targetContext.getText()).thenReturn("meters");

    // Test
    Fragment result = visitor.visitCast(context);

    // Validate
    assertNotNull(result);
    assertTrue(result instanceof ActionFragment);

    EventHandlerAction action = result.getCurrentAction();
    assertNotNull(action);

    EventHandlerMachine mockMachine = mock(EventHandlerMachine.class);

    action.apply(mockMachine);

    verify(operandAction).apply(mockMachine);
    verify(mockMachine).cast(Units.of("meters"), false);
  }

  @Test
  void testVisitCastForce() {
    // Mock
    CastForceContext context = mock(CastForceContext.class);
    ExpressionContext operandContext = mock(ExpressionContext.class);
    IdentifierContext targetContext = mock(IdentifierContext.class);
    Fragment operandFragment = mock(Fragment.class);
    final EventHandlerAction operandAction = mock(EventHandlerAction.class);

    // Set up the context.operand and context.target fields
    context.operand = operandContext;
    context.target = targetContext;
    when(operandContext.accept(parent)).thenReturn(operandFragment);
    when(operandFragment.getCurrentAction()).thenReturn(operandAction);
    when(targetContext.getText()).thenReturn("meters");

    // Test
    Fragment result = visitor.visitCastForce(context);

    // Validate
    assertNotNull(result);
    assertTrue(result instanceof ActionFragment);

    EventHandlerAction action = result.getCurrentAction();
    assertNotNull(action);

    EventHandlerMachine mockMachine = mock(EventHandlerMachine.class);

    action.apply(mockMachine);

    verify(operandAction).apply(mockMachine);
    verify(mockMachine).cast(Units.of("meters"), true);
  }

  @Test
  void testVisitNoopConversion() {
    // Mock
    NoopConversionContext context = mock(NoopConversionContext.class);
    IdentifierContext unitContext = mock(IdentifierContext.class);

    when(context.getChild(1)).thenReturn(unitContext);
    when(unitContext.getText()).thenReturn("meters");

    // Test
    Fragment result = visitor.visitNoopConversion(context);

    // Validate
    assertNotNull(result);
    assertTrue(result instanceof ConversionFragment);

    Conversion conversion = result.getConversion();
    assertNotNull(conversion);
    assertTrue(conversion instanceof NoopConversion);
  }

  @Test
  void testVisitActiveConversion() {
    // Mock
    ActiveConversionContext context = mock(ActiveConversionContext.class);
    IdentifierContext unitContext = mock(IdentifierContext.class);
    ExpressionContext exprContext = mock(ExpressionContext.class);
    Fragment exprFragment = mock(Fragment.class);
    EventHandlerAction exprAction = mock(EventHandlerAction.class);

    when(context.getChild(0)).thenReturn(unitContext);
    when(context.getChild(2)).thenReturn(exprContext);
    when(unitContext.getText()).thenReturn("meters");
    when(exprContext.accept(parent)).thenReturn(exprFragment);
    when(exprFragment.getCurrentAction()).thenReturn(exprAction);

    // Test
    Fragment result = visitor.visitActiveConversion(context);

    // Validate
    assertNotNull(result);
    assertTrue(result instanceof ConversionFragment);

    Conversion conversion = result.getConversion();
    assertNotNull(conversion);
    assertTrue(conversion instanceof DirectConversion);
  }

  @Test
  void testVisitCreateVariableExpression() {
    // Mock
    CreateVariableExpressionContext context = mock(CreateVariableExpressionContext.class);
    IdentifierContext targetContext = mock(IdentifierContext.class);
    ExpressionContext countContext = mock(ExpressionContext.class);
    final Fragment countFragment = mock(Fragment.class);
    final EventHandlerAction countAction = mock(EventHandlerAction.class);

    // Set up the context.target and context.count fields
    context.target = targetContext;
    context.count = countContext;
    when(targetContext.getText()).thenReturn("testVar");
    when(countContext.accept(parent)).thenReturn(countFragment);
    when(countFragment.getCurrentAction()).thenReturn(countAction);

    // Test
    Fragment result = visitor.visitCreateVariableExpression(context);

    // Validate
    assertNotNull(result);
    assertTrue(result instanceof ActionFragment);

    EventHandlerAction action = result.getCurrentAction();
    assertNotNull(action);

    EventHandlerMachine mockMachine = mock(EventHandlerMachine.class);

    action.apply(mockMachine);

    verify(countAction).apply(mockMachine);
    verify(mockMachine).createEntity("testVar");
  }

  @Test
  void testVisitAttrExpression() {
    // Mock
    AttrExpressionContext context = mock(AttrExpressionContext.class);
    ExpressionContext exprContext = mock(ExpressionContext.class);
    IdentifierContext attrContext = mock(IdentifierContext.class);
    Fragment exprFragment = mock(Fragment.class);
    EventHandlerAction exprAction = mock(EventHandlerAction.class);

    when(context.getChild(0)).thenReturn(exprContext);
    when(context.getChild(2)).thenReturn(attrContext);
    when(exprContext.accept(parent)).thenReturn(exprFragment);
    when(exprFragment.getCurrentAction()).thenReturn(exprAction);
    when(attrContext.getText()).thenReturn("testAttr");

    // Test
    Fragment result = visitor.visitAttrExpression(context);

    // Validate
    assertNotNull(result);
    assertTrue(result instanceof ActionFragment);

    EventHandlerAction action = result.getCurrentAction();
    assertNotNull(action);

    EventHandlerMachine mockMachine = mock(EventHandlerMachine.class);

    action.apply(mockMachine);

    verify(exprAction).apply(mockMachine);
    verify(mockMachine).pushAttribute(any(ValueResolver.class));
  }

  @Test
  void testVisitSpatialQuery() {
    // Mock
    SpatialQueryContext context = mock(SpatialQueryContext.class);
    IdentifierContext targetContext = mock(IdentifierContext.class);
    ExpressionContext distanceContext = mock(ExpressionContext.class);
    final Fragment distanceFragment = mock(Fragment.class);
    final EventHandlerAction distanceAction = mock(EventHandlerAction.class);

    // Set up the context.target and context.distance fields
    context.target = targetContext;
    context.distance = distanceContext;
    when(targetContext.toString()).thenReturn("agent");
    when(distanceContext.accept(parent)).thenReturn(distanceFragment);
    when(distanceFragment.getCurrentAction()).thenReturn(distanceAction);

    // Test
    Fragment result = visitor.visitSpatialQuery(context);

    // Validate
    assertNotNull(result);
    assertTrue(result instanceof ActionFragment);

    EventHandlerAction action = result.getCurrentAction();
    assertNotNull(action);

    EventHandlerMachine mockMachine = mock(EventHandlerMachine.class);

    action.apply(mockMachine);

    verify(distanceAction).apply(mockMachine);
    verify(mockMachine).executeSpatialQuery(any(ValueResolver.class));
  }

  @Test
  void testVisitPosition() {
    // Mock
    PositionContext context = mock(PositionContext.class);
    ExpressionContext coord1Context = mock(ExpressionContext.class);
    ExpressionContext coord2Context = mock(ExpressionContext.class);
    IdentifierContext type1Context = mock(IdentifierContext.class);
    IdentifierContext type2Context = mock(IdentifierContext.class);
    Fragment coord1Fragment = mock(Fragment.class);
    Fragment coord2Fragment = mock(Fragment.class);
    EventHandlerAction coord1Action = mock(EventHandlerAction.class);
    EventHandlerAction coord2Action = mock(EventHandlerAction.class);

    when(context.getChild(0)).thenReturn(coord1Context);
    when(context.getChild(1)).thenReturn(type1Context);
    when(context.getChild(3)).thenReturn(coord2Context);
    when(context.getChild(4)).thenReturn(type2Context);
    when(coord1Context.accept(parent)).thenReturn(coord1Fragment);
    when(coord2Context.accept(parent)).thenReturn(coord2Fragment);
    when(type1Context.getText()).thenReturn("x");
    when(type2Context.getText()).thenReturn("y");
    when(coord1Fragment.getCurrentAction()).thenReturn(coord1Action);
    when(coord2Fragment.getCurrentAction()).thenReturn(coord2Action);

    // Test
    Fragment result = visitor.visitPosition(context);

    // Validate
    assertNotNull(result);
    assertTrue(result instanceof ActionFragment);

    EventHandlerAction action = result.getCurrentAction();
    assertNotNull(action);

    EventHandlerMachine mockMachine = mock(EventHandlerMachine.class);

    action.apply(mockMachine);

    verify(coord1Fragment.getCurrentAction()).apply(mockMachine);
    verify(coord2Fragment.getCurrentAction()).apply(mockMachine);
    verify(mockMachine).makePosition();
  }

  @Test
  void testVisitCreateSingleExpression() {
    // Mock
    CreateSingleExpressionContext context = mock(CreateSingleExpressionContext.class);
    IdentifierContext targetContext = mock(IdentifierContext.class);
    final EngineValue mockSingleCount = mock(EngineValue.class);

    // Set up the context.target field
    context.target = targetContext;
    when(targetContext.getText()).thenReturn("agent");

    // Create a new visitor with a mocked singleCount
    DelegateToolbox toolbox = mock(DelegateToolbox.class);
    JoshParserToMachineVisitor parent = mock(JoshParserToMachineVisitor.class);
    BridgeGetter bridgeGetter = mock(BridgeGetter.class);
    EngineValueFactory valueFactory = mock(EngineValueFactory.class);

    when(toolbox.getParent()).thenReturn(parent);
    when(toolbox.getBridgeGetter()).thenReturn(bridgeGetter);
    when(toolbox.getValueFactory()).thenReturn(valueFactory);
    when(valueFactory.build(1, Units.of("count"))).thenReturn(mockSingleCount);

    JoshTypesUnitsVisitor testVisitor = new JoshTypesUnitsVisitor(toolbox);

    // Test
    Fragment result = testVisitor.visitCreateSingleExpression(context);

    // Validate
    assertNotNull(result);
    assertTrue(result instanceof ActionFragment);

    EventHandlerAction action = result.getCurrentAction();
    assertNotNull(action);

    EventHandlerMachine mockMachine = mock(EventHandlerMachine.class);

    action.apply(mockMachine);

    verify(mockMachine).push(mockSingleCount);
    verify(mockMachine).createEntity("agent");
  }

  @Test
  void testVisitAssignment() {
    // Mock
    AssignmentContext context = mock(AssignmentContext.class);
    IdentifierContext identifierContext = mock(IdentifierContext.class);
    ExpressionContext valContext = mock(ExpressionContext.class);
    final Fragment valFragment = mock(Fragment.class);
    final EventHandlerAction valAction = mock(EventHandlerAction.class);

    // Set up the context.getChild(1) to return the identifier context
    when(context.getChild(1)).thenReturn(identifierContext);
    when(identifierContext.getText()).thenReturn("testVar");

    // Set up the context.val field
    context.val = valContext;
    when(valContext.accept(parent)).thenReturn(valFragment);
    when(valFragment.getCurrentAction()).thenReturn(valAction);

    // Test
    Fragment result = visitor.visitAssignment(context);

    // Validate
    assertNotNull(result);
    assertTrue(result instanceof ActionFragment);

    EventHandlerAction action = result.getCurrentAction();
    assertNotNull(action);

    EventHandlerMachine mockMachine = mock(EventHandlerMachine.class);

    action.apply(mockMachine);

    verify(valAction).apply(mockMachine);
    verify(mockMachine).saveLocalVariable("testVar");
  }
}
