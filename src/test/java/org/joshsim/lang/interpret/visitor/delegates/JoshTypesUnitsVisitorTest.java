package org.joshsim.lang.interpret.visitor.delegates;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.engine.value.type.EngineValue;
import org.joshsim.lang.antlr.JoshLangParser.CastContext;
import org.joshsim.lang.antlr.JoshLangParser.CastForceContext;
import org.joshsim.lang.antlr.JoshLangParser.ExpressionContext;
import org.joshsim.lang.antlr.JoshLangParser.IdentifierContext;
import org.joshsim.lang.interpret.BridgeGetter;
import org.joshsim.lang.interpret.action.EventHandlerAction;
import org.joshsim.lang.interpret.fragment.ActionFragment;
import org.joshsim.lang.interpret.fragment.Fragment;
import org.joshsim.lang.interpret.machine.EventHandlerMachine;
import org.joshsim.lang.interpret.visitor.JoshParserToMachineVisitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.antlr.v4.runtime.tree.ParseTree;

class JoshTypesUnitsVisitorTest {

  private DelegateToolbox toolbox;
  private JoshParserToMachineVisitor parent;
  private BridgeGetter bridgeGetter;
  private EngineValueFactory valueFactory;
  private JoshTypesUnitsVisitor visitor;
  private EngineValue mockValue;

  @BeforeEach
  void setUp() {
    toolbox = mock(DelegateToolbox.class);
    parent = mock(JoshParserToMachineVisitor.class);
    bridgeGetter = mock(BridgeGetter.class);
    valueFactory = mock(EngineValueFactory.class);
    mockValue = mock(EngineValue.class);

    when(toolbox.getParent()).thenReturn(parent);
    when(toolbox.getBridgeGetter()).thenReturn(bridgeGetter);
    when(toolbox.getValueFactory()).thenReturn(valueFactory);

    visitor = new JoshTypesUnitsVisitor(toolbox);
  }

  @Test
  void testVisitCast() {
    // Mock
    CastContext context = mock(CastContext.class);
    ExpressionContext valueContext = mock(ExpressionContext.class);
    IdentifierContext unitContext = mock(IdentifierContext.class);
    Fragment valueFragment = mock(Fragment.class);
    EventHandlerAction valueAction = mock(EventHandlerAction.class);

    when(context.value).thenReturn(valueContext);
    when(context.unit).thenReturn(unitContext);
    when(valueContext.accept(parent)).thenReturn(valueFragment);
    when(valueFragment.getCurrentAction()).thenReturn(valueAction);
    when(unitContext.getText()).thenReturn("meters");

    // Test
    Fragment result = visitor.visitCast(context);

    // Validate
    assertNotNull(result);
    assertTrue(result instanceof ActionFragment);

    EventHandlerAction action = result.getCurrentAction();
    assertNotNull(action);

    EventHandlerMachine mockMachine = mock(EventHandlerMachine.class);

    action.apply(mockMachine);

    verify(valueAction).apply(mockMachine);
    verify(mockMachine).cast(Units.of("meters"));
  }

  @Test
  void testVisitCastForce() {
    // Mock
    CastForceContext context = mock(CastForceContext.class);
    ExpressionContext valueContext = mock(ExpressionContext.class);
    IdentifierContext unitContext = mock(IdentifierContext.class);
    Fragment valueFragment = mock(Fragment.class);
    EventHandlerAction valueAction = mock(EventHandlerAction.class);

    when(context.value).thenReturn(valueContext);
    when(context.unit).thenReturn(unitContext);
    when(valueContext.accept(parent)).thenReturn(valueFragment);
    when(valueFragment.getCurrentAction()).thenReturn(valueAction);
    when(unitContext.getText()).thenReturn("meters");

    // Test
    Fragment result = visitor.visitCastForce(context);

    // Validate
    assertNotNull(result);
    assertTrue(result instanceof ActionFragment);

    EventHandlerAction action = result.getCurrentAction();
    assertNotNull(action);

    EventHandlerMachine mockMachine = mock(EventHandlerMachine.class);

    action.apply(mockMachine);

    verify(valueAction).apply(mockMachine);
    verify(mockMachine).castForce(Units.of("meters"));
  }

  @Test
  void testVisitNoopConversion() {
    // Mock
    NoopConversionContext context = mock(NoopConversionContext.class);
    IdentifierContext unitContext = mock(IdentifierContext.class);

    when(context.unit).thenReturn(unitContext);
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

    when(context.unit).thenReturn(unitContext);
    when(context.expr).thenReturn(exprContext);
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
    IdentifierContext nameContext = mock(IdentifierContext.class);
    ExpressionContext valueContext = mock(ExpressionContext.class);
    Fragment valueFragment = mock(Fragment.class);
    EventHandlerAction valueAction = mock(EventHandlerAction.class);

    when(context.name).thenReturn(nameContext);
    when(context.value).thenReturn(valueContext);
    when(nameContext.getText()).thenReturn("testVar");
    when(valueContext.accept(parent)).thenReturn(valueFragment);
    when(valueFragment.getCurrentAction()).thenReturn(valueAction);
    when(reservedWordChecker.isReservedWord("testVar")).thenReturn(false);

    // Test
    Fragment result = visitor.visitCreateVariableExpression(context);

    // Validate
    assertNotNull(result);
    assertTrue(result instanceof ActionFragment);

    EventHandlerAction action = result.getCurrentAction();
    assertNotNull(action);

    EventHandlerMachine mockMachine = mock(EventHandlerMachine.class);

    action.apply(mockMachine);

    verify(valueAction).apply(mockMachine);
    verify(mockMachine).createVariable("testVar");
  }

  @Test
  void testVisitAttrExpression() {
    // Mock
    AttrExpressionContext context = mock(AttrExpressionContext.class);
    IdentifierContext nameContext = mock(IdentifierContext.class);

    when(context.name).thenReturn(nameContext);
    when(nameContext.getText()).thenReturn("testAttr");

    // Test
    Fragment result = visitor.visitAttrExpression(context);

    // Validate
    assertNotNull(result);
    assertTrue(result instanceof ActionFragment);

    EventHandlerAction action = result.getCurrentAction();
    assertNotNull(action);

    EventHandlerMachine mockMachine = mock(EventHandlerMachine.class);

    action.apply(mockMachine);

    verify(mockMachine).getAttribute("testAttr");
  }

  @Test
  void testVisitSpatialQuery() {
    // Mock
    SpatialQueryContext context = mock(SpatialQueryContext.class);
    IdentifierContext typeContext = mock(IdentifierContext.class);

    when(context.type).thenReturn(typeContext);
    when(typeContext.getText()).thenReturn("agent");

    // Test
    Fragment result = visitor.visitSpatialQuery(context);

    // Validate
    assertNotNull(result);
    assertTrue(result instanceof ActionFragment);

    EventHandlerAction action = result.getCurrentAction();
    assertNotNull(action);

    EventHandlerMachine mockMachine = mock(EventHandlerMachine.class);

    action.apply(mockMachine);

    verify(mockMachine).spatialQuery("agent");
  }

  @Test
  void testVisitPosition() {
    // Mock
    PositionContext context = mock(PositionContext.class);
    ExpressionContext xContext = mock(ExpressionContext.class);
    ExpressionContext yContext = mock(ExpressionContext.class);
    Fragment xFragment = mock(Fragment.class);
    Fragment yFragment = mock(Fragment.class);
    EventHandlerAction xAction = mock(EventHandlerAction.class);
    EventHandlerAction yAction = mock(EventHandlerAction.class);

    when(context.x).thenReturn(xContext);
    when(context.y).thenReturn(yContext);
    when(xContext.accept(parent)).thenReturn(xFragment);
    when(yContext.accept(parent)).thenReturn(yFragment);
    when(xFragment.getCurrentAction()).thenReturn(xAction);
    when(yFragment.getCurrentAction()).thenReturn(yAction);

    // Test
    Fragment result = visitor.visitPosition(context);

    // Validate
    assertNotNull(result);
    assertTrue(result instanceof ActionFragment);

    EventHandlerAction action = result.getCurrentAction();
    assertNotNull(action);

    EventHandlerMachine mockMachine = mock(EventHandlerMachine.class);

    action.apply(mockMachine);

    verify(xAction).apply(mockMachine);
    verify(yAction).apply(mockMachine);
    verify(mockMachine).createPosition();
  }

  @Test
  void testVisitCreateSingleExpression() {
    // Mock
    CreateSingleExpressionContext context = mock(CreateSingleExpressionContext.class);
    IdentifierContext typeContext = mock(IdentifierContext.class);

    when(context.type).thenReturn(typeContext);
    when(typeContext.getText()).thenReturn("agent");

    // Test
    Fragment result = visitor.visitCreateSingleExpression(context);

    // Validate
    assertNotNull(result);
    assertTrue(result instanceof ActionFragment);

    EventHandlerAction action = result.getCurrentAction();
    assertNotNull(action);

    EventHandlerMachine mockMachine = mock(EventHandlerMachine.class);

    action.apply(mockMachine);

    verify(mockMachine).createSingle("agent");
  }

  @Test
  void testVisitAssignment() {
    // Mock
    AssignmentContext context = mock(AssignmentContext.class);
    IdentifierContext nameContext = mock(IdentifierContext.class);
    ExpressionContext valueContext = mock(ExpressionContext.class);
    Fragment valueFragment = mock(Fragment.class);
    EventHandlerAction valueAction = mock(EventHandlerAction.class);

    when(context.name).thenReturn(nameContext);
    when(context.value).thenReturn(valueContext);
    when(nameContext.getText()).thenReturn("testVar");
    when(valueContext.accept(parent)).thenReturn(valueFragment);
    when(valueFragment.getCurrentAction()).thenReturn(valueAction);

    // Test
    Fragment result = visitor.visitAssignment(context);

    // Validate
    assertNotNull(result);
    assertTrue(result instanceof ActionFragment);

    EventHandlerAction action = result.getCurrentAction();
    assertNotNull(action);

    EventHandlerMachine mockMachine = mock(EventHandlerMachine.class);

    action.apply(mockMachine);

    verify(valueAction).apply(mockMachine);
    verify(mockMachine).setVariable("testVar");
  }

  @Test
  void testMakeCallableMachine() {
    // Mock
    EventHandlerAction action = mock(EventHandlerAction.class);
    EventHandlerMachine mockMachine = mock(EventHandlerMachine.class);
    EngineValue mockValue = mock(EngineValue.class);

    when(action.apply(mockMachine)).thenReturn(mockMachine);
    when(mockMachine.pop()).thenReturn(mockValue);

    // Test
    PushDownMachineCallable result = visitor.makeCallableMachine(action);

    // Validate
    assertNotNull(result);

    CompiledCallable callable = result.getCallable();
    assertNotNull(callable);
  }
}
