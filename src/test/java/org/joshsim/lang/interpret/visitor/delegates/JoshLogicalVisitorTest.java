package org.joshsim.lang.interpret.visitor.delegates;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.engine.value.type.EngineValue;
import org.joshsim.lang.antlr.JoshLangParser.ConditionContext;
import org.joshsim.lang.antlr.JoshLangParser.ConditionalContext;
import org.joshsim.lang.antlr.JoshLangParser.ExpressionContext;
import org.joshsim.lang.antlr.JoshLangParser.FullBodyContext;
import org.joshsim.lang.antlr.JoshLangParser.FullConditionalContext;
import org.joshsim.lang.antlr.JoshLangParser.FullElifBranchContext;
import org.joshsim.lang.antlr.JoshLangParser.FullElseBranchContext;
import org.joshsim.lang.antlr.JoshLangParser.LogicalExpressionContext;
import org.joshsim.lang.interpret.action.ConditionalAction;
import org.joshsim.lang.interpret.action.EventHandlerAction;
import org.joshsim.lang.interpret.fragment.josh.ActionFragment;
import org.joshsim.lang.interpret.fragment.josh.JoshFragment;
import org.joshsim.lang.interpret.machine.EventHandlerMachine;
import org.joshsim.lang.interpret.visitor.JoshParserToMachineVisitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JoshLogicalVisitorTest {

  private DelegateToolbox toolbox;
  private JoshParserToMachineVisitor parent;
  private EngineValueFactory valueFactory;
  private JoshLogicalVisitor visitor;
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

    visitor = new JoshLogicalVisitor(toolbox);
  }

  @Test
  void testVisitLogicalExpression() {
    // Mock
    LogicalExpressionContext context = mock(LogicalExpressionContext.class);
    context.left = mock(ExpressionContext.class);
    context.right = mock(ExpressionContext.class);
    context.op = mock(org.antlr.v4.runtime.Token.class);

    JoshFragment leftFragment = mock(JoshFragment.class);
    JoshFragment rightFragment = mock(JoshFragment.class);
    EventHandlerAction leftAction = mock(EventHandlerAction.class);
    EventHandlerAction rightAction = mock(EventHandlerAction.class);

    when(context.left.accept(parent)).thenReturn(leftFragment);
    when(context.right.accept(parent)).thenReturn(rightFragment);
    when(leftFragment.getCurrentAction()).thenReturn(leftAction);
    when(rightFragment.getCurrentAction()).thenReturn(rightAction);
    when(context.op.getText()).thenReturn("and");

    // Test
    JoshFragment result = visitor.visitLogicalExpression(context);

    // Validate
    assertNotNull(result);
    assertTrue(result instanceof ActionFragment);

    EventHandlerAction action = result.getCurrentAction();
    assertNotNull(action);

    EventHandlerMachine mockMachine = mock(EventHandlerMachine.class);

    action.apply(mockMachine);

    verify(leftAction).apply(mockMachine);
    verify(rightAction).apply(mockMachine);
    verify(mockMachine).and();
  }

  @Test
  void testVisitCondition() {
    // Mock
    ConditionContext context = mock(ConditionContext.class);
    context.left = mock(ExpressionContext.class);
    context.right = mock(ExpressionContext.class);
    context.op = mock(org.antlr.v4.runtime.Token.class);

    JoshFragment leftFragment = mock(JoshFragment.class);
    JoshFragment rightFragment = mock(JoshFragment.class);
    EventHandlerAction leftAction = mock(EventHandlerAction.class);
    EventHandlerAction rightAction = mock(EventHandlerAction.class);

    when(context.left.accept(parent)).thenReturn(leftFragment);
    when(context.right.accept(parent)).thenReturn(rightFragment);
    when(leftFragment.getCurrentAction()).thenReturn(leftAction);
    when(rightFragment.getCurrentAction()).thenReturn(rightAction);
    when(context.op.getText()).thenReturn("==");

    // Test
    JoshFragment result = visitor.visitCondition(context);

    // Validate
    assertNotNull(result);
    assertTrue(result instanceof ActionFragment);

    EventHandlerAction action = result.getCurrentAction();
    assertNotNull(action);

    EventHandlerMachine mockMachine = mock(EventHandlerMachine.class);

    action.apply(mockMachine);

    verify(leftAction).apply(mockMachine);
    verify(rightAction).apply(mockMachine);
    verify(mockMachine).eq();
  }

  @Test
  void testVisitConditional() {
    // Mock
    ConditionalContext context = mock(ConditionalContext.class);
    context.cond = mock(ExpressionContext.class);
    context.pos = mock(ExpressionContext.class);
    context.neg = mock(ExpressionContext.class);

    JoshFragment condFragment = mock(JoshFragment.class);
    JoshFragment posFragment = mock(JoshFragment.class);
    JoshFragment negFragment = mock(JoshFragment.class);
    EventHandlerAction condAction = mock(EventHandlerAction.class);
    EventHandlerAction posAction = mock(EventHandlerAction.class);
    EventHandlerAction negAction = mock(EventHandlerAction.class);

    when(context.cond.accept(parent)).thenReturn(condFragment);
    when(context.pos.accept(parent)).thenReturn(posFragment);
    when(context.neg.accept(parent)).thenReturn(negFragment);
    when(condFragment.getCurrentAction()).thenReturn(condAction);
    when(posFragment.getCurrentAction()).thenReturn(posAction);
    when(negFragment.getCurrentAction()).thenReturn(negAction);

    // Test
    JoshFragment result = visitor.visitConditional(context);

    // Validate
    assertNotNull(result);
    assertTrue(result instanceof ActionFragment);

    EventHandlerAction action = result.getCurrentAction();
    assertNotNull(action);
    assertTrue(action instanceof ConditionalAction);
  }

  @Test
  void testVisitFullConditional() {
    // Mock
    FullConditionalContext context = mock(FullConditionalContext.class);
    context.cond = mock(ExpressionContext.class);
    context.target = mock(FullBodyContext.class);

    JoshFragment condFragment = mock(JoshFragment.class);
    JoshFragment targetFragment = mock(JoshFragment.class);
    EventHandlerAction condAction = mock(EventHandlerAction.class);
    EventHandlerAction targetAction = mock(EventHandlerAction.class);

    when(context.cond.accept(parent)).thenReturn(condFragment);
    when(context.target.accept(parent)).thenReturn(targetFragment);
    when(condFragment.getCurrentAction()).thenReturn(condAction);
    when(targetFragment.getCurrentAction()).thenReturn(targetAction);
    when(context.getChildCount()).thenReturn(5); // No else branches

    // Test
    JoshFragment result = visitor.visitFullConditional(context);

    // Validate
    assertNotNull(result);
    assertTrue(result instanceof ActionFragment);

    EventHandlerAction action = result.getCurrentAction();
    assertNotNull(action);
  }

  @Test
  void testVisitFullElifBranch() {
    // Mock
    FullElifBranchContext context = mock(FullElifBranchContext.class);
    context.cond = mock(ExpressionContext.class);
    context.target = mock(FullBodyContext.class);

    JoshFragment condFragment = mock(JoshFragment.class);
    JoshFragment targetFragment = mock(JoshFragment.class);
    EventHandlerAction condAction = mock(EventHandlerAction.class);
    EventHandlerAction targetAction = mock(EventHandlerAction.class);

    when(context.cond.accept(parent)).thenReturn(condFragment);
    when(context.target.accept(parent)).thenReturn(targetFragment);
    when(condFragment.getCurrentAction()).thenReturn(condAction);
    when(targetFragment.getCurrentAction()).thenReturn(targetAction);

    // Test
    JoshFragment result = visitor.visitFullElifBranch(context);

    // Validate
    assertNotNull(result);
    assertTrue(result instanceof ActionFragment);

    EventHandlerAction action = result.getCurrentAction();
    assertNotNull(action);
    assertTrue(action instanceof ConditionalAction);
  }

  @Test
  void testVisitFullElseBranch() {
    // Mock
    FullElseBranchContext context = mock(FullElseBranchContext.class);
    context.target = mock(FullBodyContext.class);

    JoshFragment targetFragment = mock(JoshFragment.class);
    EventHandlerAction targetAction = mock(EventHandlerAction.class);

    when(context.target.accept(parent)).thenReturn(targetFragment);
    when(targetFragment.getCurrentAction()).thenReturn(targetAction);

    // Test
    JoshFragment result = visitor.visitFullElseBranch(context);

    // Validate
    assertNotNull(result);
    assertTrue(result instanceof ActionFragment);

    EventHandlerAction action = result.getCurrentAction();
    assertNotNull(action);
    assertTrue(action instanceof ConditionalAction);

    EventHandlerMachine mockMachine = mock(EventHandlerMachine.class);

    // Test that the condition action pushes the true value
    action.apply(mockMachine);

    verify(mockMachine).push(trueValue);
  }
}
