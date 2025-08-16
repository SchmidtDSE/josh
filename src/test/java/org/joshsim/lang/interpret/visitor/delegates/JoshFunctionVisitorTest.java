package org.joshsim.lang.interpret.visitor.delegates;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.joshsim.engine.entity.handler.EventHandlerGroupBuilder;
import org.joshsim.engine.func.CompiledCallable;
import org.joshsim.engine.func.CompiledSelector;
import org.joshsim.lang.antlr.JoshLangParser.CallableContext;
import org.joshsim.lang.antlr.JoshLangParser.ConditionalElifEventHandlerGroupMemberContext;
import org.joshsim.lang.antlr.JoshLangParser.ConditionalElseEventHandlerGroupMemberContext;
import org.joshsim.lang.antlr.JoshLangParser.ConditionalIfEventHandlerGroupMemberContext;
import org.joshsim.lang.antlr.JoshLangParser.EventHandlerGeneralContext;
import org.joshsim.lang.antlr.JoshLangParser.EventHandlerGroupMemberInnerContext;
import org.joshsim.lang.antlr.JoshLangParser.EventHandlerGroupMultipleContext;
import org.joshsim.lang.antlr.JoshLangParser.EventHandlerGroupSingleContext;
import org.joshsim.lang.antlr.JoshLangParser.FullBodyContext;
import org.joshsim.lang.antlr.JoshLangParser.IdentifierContext;
import org.joshsim.lang.antlr.JoshLangParser.LambdaContext;
import org.joshsim.lang.antlr.JoshLangParser.ReturnContext;
import org.joshsim.lang.interpret.BridgeGetter;
import org.joshsim.lang.interpret.action.EventHandlerAction;
import org.joshsim.lang.interpret.fragment.josh.ActionFragment;
import org.joshsim.lang.interpret.fragment.josh.CompiledCallableFragment;
import org.joshsim.lang.interpret.fragment.josh.EventHandlerGroupFragment;
import org.joshsim.lang.interpret.fragment.josh.JoshFragment;
import org.joshsim.lang.interpret.machine.EventHandlerMachine;
import org.joshsim.lang.interpret.visitor.JoshParserToMachineVisitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JoshFunctionVisitorTest {

  private DelegateToolbox toolbox;
  private JoshParserToMachineVisitor parent;
  private BridgeGetter bridgeGetter;
  private JoshFunctionVisitor visitor;

  @BeforeEach
  void setUp() {
    toolbox = mock(DelegateToolbox.class);
    parent = mock(JoshParserToMachineVisitor.class);
    bridgeGetter = mock(BridgeGetter.class);

    when(toolbox.getParent()).thenReturn(parent);
    when(toolbox.getBridgeGetter()).thenReturn(bridgeGetter);

    visitor = new JoshFunctionVisitor(toolbox);
  }

  @Test
  void testVisitLambda() {
    // Mock
    LambdaContext context = mock(LambdaContext.class);
    JoshFragment childFragment = mock(JoshFragment.class);
    EventHandlerAction childAction = mock(EventHandlerAction.class);

    when(context.getChild(0)).thenReturn(context);
    when(context.accept(parent)).thenReturn(childFragment);
    when(childFragment.getCurrentAction()).thenReturn(childAction);

    // Test
    JoshFragment result = visitor.visitLambda(context);

    // Validate
    assertNotNull(result);
    assertTrue(result instanceof ActionFragment);

    EventHandlerAction action = result.getCurrentAction();
    assertNotNull(action);

    EventHandlerMachine mockMachine = mock(EventHandlerMachine.class);

    action.apply(mockMachine);

    verify(childAction).apply(mockMachine);
    verify(mockMachine).end();
  }

  @Test
  void testVisitReturn() {
    // Mock
    ReturnContext context = mock(ReturnContext.class);
    JoshFragment childFragment = mock(JoshFragment.class);
    EventHandlerAction childAction = mock(EventHandlerAction.class);

    when(context.getChild(1)).thenReturn(context);
    when(context.accept(parent)).thenReturn(childFragment);
    when(childFragment.getCurrentAction()).thenReturn(childAction);

    // Test
    JoshFragment result = visitor.visitReturn(context);

    // Validate
    assertNotNull(result);
    assertTrue(result instanceof ActionFragment);

    EventHandlerAction action = result.getCurrentAction();
    assertNotNull(action);

    EventHandlerMachine mockMachine = mock(EventHandlerMachine.class);

    action.apply(mockMachine);

    verify(childAction).apply(mockMachine);
    verify(mockMachine).end();
  }

  @Test
  void testVisitFullBody() {
    // Mock
    FullBodyContext context = mock(FullBodyContext.class);
    JoshFragment childFragment = mock(JoshFragment.class);
    EventHandlerAction childAction = mock(EventHandlerAction.class);

    when(context.getChildCount()).thenReturn(3); // { statement }
    when(context.getChild(1)).thenReturn(context);
    when(context.accept(parent)).thenReturn(childFragment);
    when(childFragment.getCurrentAction()).thenReturn(childAction);

    EventHandlerMachine mockMachine = mock(EventHandlerMachine.class);
    when(mockMachine.isEnded()).thenReturn(true); // Simulate a return statement

    // Test
    JoshFragment result = visitor.visitFullBody(context);

    // Validate
    assertNotNull(result);
    assertTrue(result instanceof ActionFragment);

    EventHandlerAction action = result.getCurrentAction();
    assertNotNull(action);

    action.apply(mockMachine);

    verify(childAction).apply(mockMachine);
    // isEnded() is called twice in the implementation, once in the loop and once after
    verify(mockMachine, org.mockito.Mockito.times(2)).isEnded();
  }

  @Test
  void testVisitEventHandlerGroupMemberInner() {
    // Mock
    EventHandlerGroupMemberInnerContext context = mock(EventHandlerGroupMemberInnerContext.class);
    context.target = mock(CallableContext.class);
    JoshFragment targetFragment = mock(JoshFragment.class);
    EventHandlerAction targetAction = mock(EventHandlerAction.class);

    when(context.target.accept(parent)).thenReturn(targetFragment);
    when(targetFragment.getCurrentAction()).thenReturn(targetAction);

    // Test
    JoshFragment result = visitor.visitEventHandlerGroupMemberInner(context);

    // Validate
    assertNotNull(result);
    assertTrue(result instanceof ActionFragment);

    EventHandlerAction action = result.getCurrentAction();
    assertNotNull(action);
    assertTrue(action == targetAction); // Should return the same action
  }

  @Test
  void testVisitConditionalIfEventHandlerGroupMember() {
    // Mock
    ConditionalIfEventHandlerGroupMemberContext context =
        mock(ConditionalIfEventHandlerGroupMemberContext.class);
    context.inner = mock(EventHandlerGroupMemberInnerContext.class);
    context.target = mock(CallableContext.class);
    JoshFragment innerFragment = mock(JoshFragment.class);
    JoshFragment targetFragment = mock(JoshFragment.class);
    EventHandlerAction innerAction = mock(EventHandlerAction.class);
    EventHandlerAction targetAction = mock(EventHandlerAction.class);

    when(context.inner.accept(parent)).thenReturn(innerFragment);
    when(context.target.accept(parent)).thenReturn(targetFragment);
    when(innerFragment.getCurrentAction()).thenReturn(innerAction);
    when(targetFragment.getCurrentAction()).thenReturn(targetAction);

    // Test
    JoshFragment result = visitor.visitConditionalIfEventHandlerGroupMember(context);

    // Validate
    assertNotNull(result);
    assertTrue(result instanceof CompiledCallableFragment);
    assertTrue(result.getCompiledCallable() != null);
    assertTrue(result.getCompiledSelector().isPresent());
  }

  @Test
  void testVisitConditionalElifEventHandlerGroupMember() {
    // Mock
    ConditionalElifEventHandlerGroupMemberContext context =
        mock(ConditionalElifEventHandlerGroupMemberContext.class);
    context.inner = mock(EventHandlerGroupMemberInnerContext.class);
    context.target = mock(CallableContext.class);
    JoshFragment innerFragment = mock(JoshFragment.class);
    JoshFragment targetFragment = mock(JoshFragment.class);
    EventHandlerAction innerAction = mock(EventHandlerAction.class);
    EventHandlerAction targetAction = mock(EventHandlerAction.class);

    when(context.inner.accept(parent)).thenReturn(innerFragment);
    when(context.target.accept(parent)).thenReturn(targetFragment);
    when(innerFragment.getCurrentAction()).thenReturn(innerAction);
    when(targetFragment.getCurrentAction()).thenReturn(targetAction);

    // Test
    JoshFragment result = visitor.visitConditionalElifEventHandlerGroupMember(context);

    // Validate
    assertNotNull(result);
    assertTrue(result instanceof CompiledCallableFragment);
    assertTrue(result.getCompiledCallable() != null);
    assertTrue(result.getCompiledSelector().isPresent());
  }

  @Test
  void testVisitConditionalElseEventHandlerGroupMember() {
    // Mock
    ConditionalElseEventHandlerGroupMemberContext context =
        mock(ConditionalElseEventHandlerGroupMemberContext.class);
    context.inner = mock(EventHandlerGroupMemberInnerContext.class);
    JoshFragment innerFragment = mock(JoshFragment.class);
    EventHandlerAction innerAction = mock(EventHandlerAction.class);

    when(context.inner.accept(parent)).thenReturn(innerFragment);
    when(innerFragment.getCurrentAction()).thenReturn(innerAction);

    // Test
    JoshFragment result = visitor.visitConditionalElseEventHandlerGroupMember(context);

    // Validate
    assertNotNull(result);
    assertTrue(result instanceof CompiledCallableFragment);
    assertTrue(result.getCompiledCallable() != null);
    assertTrue(!result.getCompiledSelector().isPresent());
  }

  @Test
  void testVisitEventHandlerGroupSingle() {
    // Mock
    EventHandlerGroupSingleContext context = mock(EventHandlerGroupSingleContext.class);
    context.name = mock(IdentifierContext.class);
    JoshFragment innerFragment = mock(JoshFragment.class);
    EventHandlerAction innerAction = mock(EventHandlerAction.class);

    when(context.name.getText()).thenReturn("entity.init");
    when(context.getChild(1)).thenReturn(context);
    when(context.accept(parent)).thenReturn(innerFragment);
    when(innerFragment.getCurrentAction()).thenReturn(innerAction);

    // Test
    JoshFragment result = visitor.visitEventHandlerGroupSingle(context);

    // Validate
    assertNotNull(result);
    assertTrue(result instanceof EventHandlerGroupFragment);

    EventHandlerGroupBuilder builder = result.getEventHandlerGroup();
    assertNotNull(builder);
  }

  @Test
  void testVisitEventHandlerGroupMultiple() {
    // Mock
    EventHandlerGroupMultipleContext context = mock(EventHandlerGroupMultipleContext.class);
    context.name = mock(IdentifierContext.class);
    JoshFragment childFragment = mock(CompiledCallableFragment.class);
    CompiledCallable compiledCallable = mock(CompiledCallable.class);
    CompiledSelector compiledSelector = mock(CompiledSelector.class);

    when(context.name.getText()).thenReturn("entity.init");
    when(context.getChildCount()).thenReturn(2); // name + 1 branch
    when(context.getChild(1)).thenReturn(context);
    when(context.accept(parent)).thenReturn(childFragment);
    when(childFragment.getCompiledCallable()).thenReturn(compiledCallable);
    when(childFragment.getCompiledSelector()).thenReturn(Optional.of(compiledSelector));

    // Test
    JoshFragment result = visitor.visitEventHandlerGroupMultiple(context);

    // Validate
    assertNotNull(result);
    assertTrue(result instanceof EventHandlerGroupFragment);

    EventHandlerGroupBuilder builder = result.getEventHandlerGroup();
    assertNotNull(builder);
  }

  @Test
  void testVisitEventHandlerGeneral() {
    // Mock
    EventHandlerGeneralContext context = mock(EventHandlerGeneralContext.class);
    JoshFragment childFragment = mock(EventHandlerGroupFragment.class);
    EventHandlerGroupBuilder groupBuilder = mock(EventHandlerGroupBuilder.class);

    when(context.getChild(0)).thenReturn(context);
    when(context.accept(parent)).thenReturn(childFragment);
    when(childFragment.getEventHandlerGroup()).thenReturn(groupBuilder);
    when(groupBuilder.getAttribute()).thenReturn("validAttribute");

    // Test
    JoshFragment result = visitor.visitEventHandlerGeneral(context);

    // Validate
    assertNotNull(result);
    assertTrue(result == childFragment); // Should return the same fragment
  }
}
