package org.joshsim.lang.interpret.visitor.delegates;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.engine.value.type.EngineValue;
import org.joshsim.lang.antlr.JoshLangParser.AdditionExpressionContext;
import org.joshsim.lang.antlr.JoshLangParser.ExpressionContext;
import org.joshsim.lang.antlr.JoshLangParser.LimitBoundExpressionContext;
import org.joshsim.lang.antlr.JoshLangParser.LimitMaxExpressionContext;
import org.joshsim.lang.antlr.JoshLangParser.LimitMinExpressionContext;
import org.joshsim.lang.antlr.JoshLangParser.MapLinearContext;
import org.joshsim.lang.antlr.JoshLangParser.MapParamContext;
import org.joshsim.lang.antlr.JoshLangParser.MapParamParamContext;
import org.joshsim.lang.antlr.JoshLangParser.MultiplyExpressionContext;
import org.joshsim.lang.antlr.JoshLangParser.ParenExpressionContext;
import org.joshsim.lang.antlr.JoshLangParser.PowExpressionContext;
import org.joshsim.lang.antlr.JoshLangParser.SingleParamFunctionCallContext;
import org.joshsim.lang.interpret.action.EventHandlerAction;
import org.joshsim.lang.interpret.fragment.ActionFragment;
import org.joshsim.lang.interpret.fragment.Fragment;
import org.joshsim.lang.interpret.machine.EventHandlerMachine;
import org.joshsim.lang.interpret.visitor.JoshParserToMachineVisitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.joshsim.lang.antlr.JoshLangParser.IdentifierContext;
import org.joshsim.lang.antlr.JoshLangParser.FuncNameContext;
import org.antlr.v4.runtime.Token;

class JoshMathematicsVisitorTest {

  private DelegateToolbox toolbox;
  private JoshParserToMachineVisitor parent;
  private EngineValueFactory valueFactory;
  private JoshMathematicsVisitor visitor;
  private EngineValue trueValue;

  @BeforeEach
  void setUp() {
    toolbox = mock(DelegateToolbox.class);
    parent = mock(JoshParserToMachineVisitor.class);
    valueFactory = mock(EngineValueFactory.class);
    trueValue = mock(EngineValue.class);

    when(toolbox.getParent()).thenReturn(parent);
    when(toolbox.getValueFactory()).thenReturn(valueFactory);
    when(valueFactory.build(true, Units.EMPTY)).thenReturn(trueValue);

    visitor = new JoshMathematicsVisitor(toolbox);
  }

  @Test
  void testVisitMapLinear() {
    // Mock
    MapLinearContext context = mock(MapLinearContext.class);
    context.operand = mock(ExpressionContext.class);
    context.fromlow = mock(ExpressionContext.class);
    context.fromhigh = mock(ExpressionContext.class);
    context.tolow = mock(ExpressionContext.class);
    context.tohigh = mock(ExpressionContext.class);

    Fragment operandFragment = mock(Fragment.class);
    Fragment fromLowFragment = mock(Fragment.class);
    Fragment fromHighFragment = mock(Fragment.class);
    Fragment toLowFragment = mock(Fragment.class);
    Fragment toHighFragment = mock(Fragment.class);

    EventHandlerAction operandAction = mock(EventHandlerAction.class);
    EventHandlerAction fromLowAction = mock(EventHandlerAction.class);
    EventHandlerAction fromHighAction = mock(EventHandlerAction.class);
    EventHandlerAction toLowAction = mock(EventHandlerAction.class);
    EventHandlerAction toHighAction = mock(EventHandlerAction.class);

    when(context.operand.accept(parent)).thenReturn(operandFragment);
    when(context.fromlow.accept(parent)).thenReturn(fromLowFragment);
    when(context.fromhigh.accept(parent)).thenReturn(fromHighFragment);
    when(context.tolow.accept(parent)).thenReturn(toLowFragment);
    when(context.tohigh.accept(parent)).thenReturn(toHighFragment);

    when(operandFragment.getCurrentAction()).thenReturn(operandAction);
    when(fromLowFragment.getCurrentAction()).thenReturn(fromLowAction);
    when(fromHighFragment.getCurrentAction()).thenReturn(fromHighAction);
    when(toLowFragment.getCurrentAction()).thenReturn(toLowAction);
    when(toHighFragment.getCurrentAction()).thenReturn(toHighAction);

    // Test
    Fragment result = visitor.visitMapLinear(context);

    // Validate
    assertNotNull(result);
    assertTrue(result instanceof ActionFragment);

    EventHandlerAction action = result.getCurrentAction();
    assertNotNull(action);

    EventHandlerMachine mockMachine = mock(EventHandlerMachine.class);

    action.apply(mockMachine);

    verify(operandAction).apply(mockMachine);
    verify(fromLowAction).apply(mockMachine);
    verify(fromHighAction).apply(mockMachine);
    verify(toLowAction).apply(mockMachine);
    verify(toHighAction).apply(mockMachine);
    verify(mockMachine).push(trueValue);
    verify(mockMachine).applyMap("linear");
  }

  @Test
  void testVisitMapParam() {
    // Mock
    MapParamContext context = mock(MapParamContext.class);
    context.operand = mock(ExpressionContext.class);
    context.fromlow = mock(ExpressionContext.class);
    context.fromhigh = mock(ExpressionContext.class);
    context.tolow = mock(ExpressionContext.class);
    context.tohigh = mock(ExpressionContext.class);
    context.method = mock(IdentifierContext.class);

    Fragment operandFragment = mock(Fragment.class);
    Fragment fromLowFragment = mock(Fragment.class);
    Fragment fromHighFragment = mock(Fragment.class);
    Fragment toLowFragment = mock(Fragment.class);
    Fragment toHighFragment = mock(Fragment.class);

    EventHandlerAction operandAction = mock(EventHandlerAction.class);
    EventHandlerAction fromLowAction = mock(EventHandlerAction.class);
    EventHandlerAction fromHighAction = mock(EventHandlerAction.class);
    EventHandlerAction toLowAction = mock(EventHandlerAction.class);
    EventHandlerAction toHighAction = mock(EventHandlerAction.class);

    when(context.operand.accept(parent)).thenReturn(operandFragment);
    when(context.fromlow.accept(parent)).thenReturn(fromLowFragment);
    when(context.fromhigh.accept(parent)).thenReturn(fromHighFragment);
    when(context.tolow.accept(parent)).thenReturn(toLowFragment);
    when(context.tohigh.accept(parent)).thenReturn(toHighFragment);
    when(context.method.getText()).thenReturn("linear");

    when(operandFragment.getCurrentAction()).thenReturn(operandAction);
    when(fromLowFragment.getCurrentAction()).thenReturn(fromLowAction);
    when(fromHighFragment.getCurrentAction()).thenReturn(fromHighAction);
    when(toLowFragment.getCurrentAction()).thenReturn(toLowAction);
    when(toHighFragment.getCurrentAction()).thenReturn(toHighAction);

    // Test
    Fragment result = visitor.visitMapParam(context);

    // Validate
    assertNotNull(result);
    assertTrue(result instanceof ActionFragment);

    EventHandlerAction action = result.getCurrentAction();
    assertNotNull(action);

    EventHandlerMachine mockMachine = mock(EventHandlerMachine.class);

    action.apply(mockMachine);

    verify(operandAction).apply(mockMachine);
    verify(fromLowAction).apply(mockMachine);
    verify(fromHighAction).apply(mockMachine);
    verify(toLowAction).apply(mockMachine);
    verify(toHighAction).apply(mockMachine);
    verify(mockMachine).push(trueValue);
    verify(mockMachine).applyMap("linear");
  }

  @Test
  void testVisitMapParamParam() {
    // Mock
    MapParamParamContext context = mock(MapParamParamContext.class);
    context.operand = mock(ExpressionContext.class);
    context.fromlow = mock(ExpressionContext.class);
    context.fromhigh = mock(ExpressionContext.class);
    context.tolow = mock(ExpressionContext.class);
    context.tohigh = mock(ExpressionContext.class);
    context.methodarg = mock(ExpressionContext.class);
    context.method = mock(IdentifierContext.class);

    Fragment operandFragment = mock(Fragment.class);
    Fragment fromLowFragment = mock(Fragment.class);
    Fragment fromHighFragment = mock(Fragment.class);
    Fragment toLowFragment = mock(Fragment.class);
    Fragment toHighFragment = mock(Fragment.class);
    Fragment methodArgFragment = mock(Fragment.class);

    EventHandlerAction operandAction = mock(EventHandlerAction.class);
    EventHandlerAction fromLowAction = mock(EventHandlerAction.class);
    EventHandlerAction fromHighAction = mock(EventHandlerAction.class);
    EventHandlerAction toLowAction = mock(EventHandlerAction.class);
    EventHandlerAction toHighAction = mock(EventHandlerAction.class);
    EventHandlerAction methodArgAction = mock(EventHandlerAction.class);

    when(context.operand.accept(parent)).thenReturn(operandFragment);
    when(context.fromlow.accept(parent)).thenReturn(fromLowFragment);
    when(context.fromhigh.accept(parent)).thenReturn(fromHighFragment);
    when(context.tolow.accept(parent)).thenReturn(toLowFragment);
    when(context.tohigh.accept(parent)).thenReturn(toHighFragment);
    when(context.methodarg.accept(parent)).thenReturn(methodArgFragment);
    when(context.method.getText()).thenReturn("sigmoid");

    when(operandFragment.getCurrentAction()).thenReturn(operandAction);
    when(fromLowFragment.getCurrentAction()).thenReturn(fromLowAction);
    when(fromHighFragment.getCurrentAction()).thenReturn(fromHighAction);
    when(toLowFragment.getCurrentAction()).thenReturn(toLowAction);
    when(toHighFragment.getCurrentAction()).thenReturn(toHighAction);
    when(methodArgFragment.getCurrentAction()).thenReturn(methodArgAction);

    // Test
    Fragment result = visitor.visitMapParamParam(context);

    // Validate
    assertNotNull(result);
    assertTrue(result instanceof ActionFragment);

    EventHandlerAction action = result.getCurrentAction();
    assertNotNull(action);

    EventHandlerMachine mockMachine = mock(EventHandlerMachine.class);

    action.apply(mockMachine);

    verify(operandAction).apply(mockMachine);
    verify(fromLowAction).apply(mockMachine);
    verify(fromHighAction).apply(mockMachine);
    verify(toLowAction).apply(mockMachine);
    verify(toHighAction).apply(mockMachine);
    verify(methodArgAction).apply(mockMachine);
    verify(mockMachine).applyMap("sigmoid");
  }

  @Test
  void testVisitAdditionExpression() {
    // Mock
    AdditionExpressionContext context = mock(AdditionExpressionContext.class);
    context.left = mock(ExpressionContext.class);
    context.right = mock(ExpressionContext.class);
    context.op = mock(Token.class);

    Fragment leftFragment = mock(Fragment.class);
    Fragment rightFragment = mock(Fragment.class);
    EventHandlerAction leftAction = mock(EventHandlerAction.class);
    EventHandlerAction rightAction = mock(EventHandlerAction.class);

    when(context.left.accept(parent)).thenReturn(leftFragment);
    when(context.right.accept(parent)).thenReturn(rightFragment);
    when(leftFragment.getCurrentAction()).thenReturn(leftAction);
    when(rightFragment.getCurrentAction()).thenReturn(rightAction);
    when(context.op.getText()).thenReturn("+");

    // Test
    Fragment result = visitor.visitAdditionExpression(context);

    // Validate
    assertNotNull(result);
    assertTrue(result instanceof ActionFragment);

    EventHandlerAction action = result.getCurrentAction();
    assertNotNull(action);

    EventHandlerMachine mockMachine = mock(EventHandlerMachine.class);
    when(mockMachine.add()).thenReturn(mockMachine);

    action.apply(mockMachine);

    verify(leftAction).apply(mockMachine);
    verify(rightAction).apply(mockMachine);
    verify(mockMachine).add();
  }

  @Test
  void testVisitMultiplyExpression() {
    // Mock
    MultiplyExpressionContext context = mock(MultiplyExpressionContext.class);
    context.left = mock(ExpressionContext.class);
    context.right = mock(ExpressionContext.class);
    context.op = mock(Token.class);

    Fragment leftFragment = mock(Fragment.class);
    Fragment rightFragment = mock(Fragment.class);
    EventHandlerAction leftAction = mock(EventHandlerAction.class);
    EventHandlerAction rightAction = mock(EventHandlerAction.class);

    when(context.left.accept(parent)).thenReturn(leftFragment);
    when(context.right.accept(parent)).thenReturn(rightFragment);
    when(leftFragment.getCurrentAction()).thenReturn(leftAction);
    when(rightFragment.getCurrentAction()).thenReturn(rightAction);
    when(context.op.getText()).thenReturn("*");

    // Test
    Fragment result = visitor.visitMultiplyExpression(context);

    // Validate
    assertNotNull(result);
    assertTrue(result instanceof ActionFragment);

    EventHandlerAction action = result.getCurrentAction();
    assertNotNull(action);

    EventHandlerMachine mockMachine = mock(EventHandlerMachine.class);
    when(mockMachine.multiply()).thenReturn(mockMachine);

    action.apply(mockMachine);

    verify(leftAction).apply(mockMachine);
    verify(rightAction).apply(mockMachine);
    verify(mockMachine).multiply();
  }

  @Test
  void testVisitPowExpression() {
    // Mock
    PowExpressionContext context = mock(PowExpressionContext.class);
    context.left = mock(ExpressionContext.class);
    context.right = mock(ExpressionContext.class);

    Fragment leftFragment = mock(Fragment.class);
    Fragment rightFragment = mock(Fragment.class);
    EventHandlerAction leftAction = mock(EventHandlerAction.class);
    EventHandlerAction rightAction = mock(EventHandlerAction.class);

    when(context.left.accept(parent)).thenReturn(leftFragment);
    when(context.right.accept(parent)).thenReturn(rightFragment);
    when(leftFragment.getCurrentAction()).thenReturn(leftAction);
    when(rightFragment.getCurrentAction()).thenReturn(rightAction);

    // Test
    Fragment result = visitor.visitPowExpression(context);

    // Validate
    assertNotNull(result);
    assertTrue(result instanceof ActionFragment);

    EventHandlerAction action = result.getCurrentAction();
    assertNotNull(action);

    EventHandlerMachine mockMachine = mock(EventHandlerMachine.class);

    action.apply(mockMachine);

    verify(leftAction).apply(mockMachine);
    verify(rightAction).apply(mockMachine);
    verify(mockMachine).pow();
  }

  @Test
  void testVisitParenExpression() {
    // Mock
    ParenExpressionContext context = mock(ParenExpressionContext.class);
    ExpressionContext childContext = mock(ExpressionContext.class);
    Fragment childFragment = mock(Fragment.class);

    when(context.getChild(1)).thenReturn(childContext);
    when(childContext.accept(parent)).thenReturn(childFragment);

    // Test
    Fragment result = visitor.visitParenExpression(context);

    // Validate
    assertNotNull(result);
    assertTrue(result == childFragment); // Should return the same fragment
  }

  @Test
  void testVisitLimitBoundExpression() {
    // Mock
    LimitBoundExpressionContext context = mock(LimitBoundExpressionContext.class);
    context.operand = mock(ExpressionContext.class);
    context.lower = mock(ExpressionContext.class);
    context.upper = mock(ExpressionContext.class);

    Fragment operandFragment = mock(Fragment.class);
    Fragment lowerFragment = mock(Fragment.class);
    Fragment upperFragment = mock(Fragment.class);
    EventHandlerAction operandAction = mock(EventHandlerAction.class);
    EventHandlerAction lowerAction = mock(EventHandlerAction.class);
    EventHandlerAction upperAction = mock(EventHandlerAction.class);

    when(context.operand.accept(parent)).thenReturn(operandFragment);
    when(context.lower.accept(parent)).thenReturn(lowerFragment);
    when(context.upper.accept(parent)).thenReturn(upperFragment);
    when(operandFragment.getCurrentAction()).thenReturn(operandAction);
    when(lowerFragment.getCurrentAction()).thenReturn(lowerAction);
    when(upperFragment.getCurrentAction()).thenReturn(upperAction);

    // Test
    Fragment result = visitor.visitLimitBoundExpression(context);

    // Validate
    assertNotNull(result);
    assertTrue(result instanceof ActionFragment);

    EventHandlerAction action = result.getCurrentAction();
    assertNotNull(action);

    EventHandlerMachine mockMachine = mock(EventHandlerMachine.class);

    action.apply(mockMachine);

    verify(operandAction).apply(mockMachine);
    verify(lowerAction).apply(mockMachine);
    verify(upperAction).apply(mockMachine);
    verify(mockMachine).bound(true, true);
  }

  @Test
  void testVisitLimitMinExpression() {
    // Mock
    LimitMinExpressionContext context = mock(LimitMinExpressionContext.class);
    context.operand = mock(ExpressionContext.class);
    context.limit = mock(ExpressionContext.class);

    Fragment operandFragment = mock(Fragment.class);
    Fragment limitFragment = mock(Fragment.class);
    EventHandlerAction operandAction = mock(EventHandlerAction.class);
    EventHandlerAction limitAction = mock(EventHandlerAction.class);

    when(context.operand.accept(parent)).thenReturn(operandFragment);
    when(context.limit.accept(parent)).thenReturn(limitFragment);
    when(operandFragment.getCurrentAction()).thenReturn(operandAction);
    when(limitFragment.getCurrentAction()).thenReturn(limitAction);

    // Test
    Fragment result = visitor.visitLimitMinExpression(context);

    // Validate
    assertNotNull(result);
    assertTrue(result instanceof ActionFragment);

    EventHandlerAction action = result.getCurrentAction();
    assertNotNull(action);

    EventHandlerMachine mockMachine = mock(EventHandlerMachine.class);

    action.apply(mockMachine);

    verify(operandAction).apply(mockMachine);
    verify(limitAction).apply(mockMachine);
    verify(mockMachine).bound(true, false);
  }

  @Test
  void testVisitLimitMaxExpression() {
    // Mock
    LimitMaxExpressionContext context = mock(LimitMaxExpressionContext.class);
    context.operand = mock(ExpressionContext.class);
    context.limit = mock(ExpressionContext.class);

    Fragment operandFragment = mock(Fragment.class);
    Fragment limitFragment = mock(Fragment.class);
    EventHandlerAction operandAction = mock(EventHandlerAction.class);
    EventHandlerAction limitAction = mock(EventHandlerAction.class);

    when(context.operand.accept(parent)).thenReturn(operandFragment);
    when(context.limit.accept(parent)).thenReturn(limitFragment);
    when(operandFragment.getCurrentAction()).thenReturn(operandAction);
    when(limitFragment.getCurrentAction()).thenReturn(limitAction);

    // Test
    Fragment result = visitor.visitLimitMaxExpression(context);

    // Validate
    assertNotNull(result);
    assertTrue(result instanceof ActionFragment);

    EventHandlerAction action = result.getCurrentAction();
    assertNotNull(action);

    EventHandlerMachine mockMachine = mock(EventHandlerMachine.class);

    action.apply(mockMachine);

    verify(operandAction).apply(mockMachine);
    verify(limitAction).apply(mockMachine);
    verify(mockMachine).bound(false, true);
  }

  @Test
  void testVisitSingleParamFunctionCall() {
    // Mock
    SingleParamFunctionCallContext context = mock(SingleParamFunctionCallContext.class);
    context.operand = mock(ExpressionContext.class);
    context.name = mock(FuncNameContext.class);

    Fragment operandFragment = mock(Fragment.class);
    EventHandlerAction operandAction = mock(EventHandlerAction.class);

    when(context.operand.accept(parent)).thenReturn(operandFragment);
    when(operandFragment.getCurrentAction()).thenReturn(operandAction);
    when(context.name.getText()).thenReturn("abs");

    // Test
    Fragment result = visitor.visitSingleParamFunctionCall(context);

    // Validate
    assertNotNull(result);
    assertTrue(result instanceof ActionFragment);

    EventHandlerAction action = result.getCurrentAction();
    assertNotNull(action);

    EventHandlerMachine mockMachine = mock(EventHandlerMachine.class);

    action.apply(mockMachine);

    verify(operandAction).apply(mockMachine);
    verify(mockMachine).abs();
  }
}
