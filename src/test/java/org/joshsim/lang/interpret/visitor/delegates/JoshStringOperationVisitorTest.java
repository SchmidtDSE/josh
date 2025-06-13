package org.joshsim.lang.interpret.visitor.delegates;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.joshsim.lang.antlr.JoshLangParser.ConcatExpressionContext;
import org.joshsim.lang.interpret.action.EventHandlerAction;
import org.joshsim.lang.interpret.fragment.ActionFragment;
import org.joshsim.lang.interpret.fragment.Fragment;
import org.joshsim.lang.interpret.machine.EventHandlerMachine;
import org.joshsim.lang.interpret.visitor.JoshParserToMachineVisitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


class JoshStringOperationVisitorTest {

  private DelegateToolbox toolbox;
  private JoshParserToMachineVisitor parent;
  private JoshStringOperationVisitor visitor;

  @BeforeEach
  void setUp() {
    toolbox = mock(DelegateToolbox.class);
    parent = mock(JoshParserToMachineVisitor.class);
    when(toolbox.getParent()).thenReturn(parent);

    visitor = new JoshStringOperationVisitor(toolbox);
  }

  @Test
  void testVisitConcatExpression() {
    // Mock
    ConcatExpressionContext context = mock(ConcatExpressionContext.class);
    context.left = mock(ConcatExpressionContext.class);
    context.right = mock(ConcatExpressionContext.class);

    Fragment leftFragment = mock(Fragment.class);
    Fragment rightFragment = mock(Fragment.class);
    EventHandlerAction leftAction = mock(EventHandlerAction.class);
    EventHandlerAction rightAction = mock(EventHandlerAction.class);

    when(context.left.accept(parent)).thenReturn(leftFragment);
    when(context.right.accept(parent)).thenReturn(rightFragment);
    when(leftFragment.getCurrentAction()).thenReturn(leftAction);
    when(rightFragment.getCurrentAction()).thenReturn(rightAction);

    // Test
    Fragment result = visitor.visitConcatExpression(context);

    // Validate
    assertNotNull(result);
    assertTrue(result instanceof ActionFragment);

    EventHandlerAction action = result.getCurrentAction();
    assertNotNull(action);

    EventHandlerMachine mockMachine = mock(EventHandlerMachine.class);

    action.apply(mockMachine);

    verify(leftAction).apply(mockMachine);
    verify(rightAction).apply(mockMachine);
    verify(mockMachine).concat();
  }
}
