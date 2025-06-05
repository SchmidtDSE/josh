package org.joshsim.lang.interpret.visitor.delegates;

import org.antlr.v4.runtime.tree.ParseTree;
import org.joshsim.engine.entity.handler.EventHandlerGroup;
import org.joshsim.engine.entity.handler.EventKey;
import org.joshsim.engine.func.CompiledCallable;
import org.joshsim.engine.func.CompiledSelector;
import org.joshsim.lang.antlr.JoshLangParser;
import org.joshsim.lang.interpret.BridgeGetter;
import org.joshsim.lang.interpret.action.EventHandlerAction;
import org.joshsim.lang.interpret.fragment.ActionFragment;
import org.joshsim.lang.interpret.fragment.CompiledCallableFragment;
import org.joshsim.lang.interpret.fragment.EventHandlerGroupFragment;
import org.joshsim.lang.interpret.fragment.Fragment;
import org.joshsim.lang.interpret.machine.EventHandlerMachine;
import org.joshsim.lang.interpret.misc.ReservedWordChecker;
import org.joshsim.lang.interpret.visitor.JoshParserToMachineVisitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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
        JoshLangParser.LambdaContext context = mock(JoshLangParser.LambdaContext.class);
        ParseTree childExpression = mock(ParseTree.class); // e.g., fullBody or expression
        Fragment expressionFragment = mock(Fragment.class);
        EventHandlerAction expressionAction = mock(EventHandlerAction.class);

        when(context.getChild(0)).thenReturn(childExpression);
        when(childExpression.accept(parent)).thenReturn(expressionFragment);
        when(expressionFragment.getCurrentAction()).thenReturn(expressionAction);

        Fragment result = visitor.visitLambda(context);

        assertNotNull(result);
        assertTrue(result instanceof ActionFragment);
        EventHandlerAction finalAction = result.getCurrentAction();
        assertNotNull(finalAction);

        EventHandlerMachine mockMachine = mock(EventHandlerMachine.class);
        finalAction.apply(mockMachine);

        verify(expressionAction).apply(mockMachine);
        verify(mockMachine).end();
    }

    @Test
    void testVisitReturnStatement() {
        JoshLangParser.ReturnStatementContext context = mock(JoshLangParser.ReturnStatementContext.class);
        JoshLangParser.ExpressionContext expressionContext = mock(JoshLangParser.ExpressionContext.class);
        Fragment expressionFragment = mock(Fragment.class);
        EventHandlerAction expressionAction = mock(EventHandlerAction.class);

        when(context.expression()).thenReturn(expressionContext); // Assuming 'expression()' is the method to get the child
        when(expressionContext.accept(parent)).thenReturn(expressionFragment);
        when(expressionFragment.getCurrentAction()).thenReturn(expressionAction);

        // For return statement, context.getChild(1) would be expression if 'return' is child 0
        // Using context.expression() is more robust if ANTLR grammar defines it
        // If context.expression() is not available, will revert to getChild(1)
        // Based on typical grammar: 'return' expression ';'
        // child 0 = 'return', child 1 = expression, child 2 = ';'
        // So, if using getChild, it would be getChild(1)
        // Let's assume context.expression() is canonical. If build fails, this is a likely place to adjust.

        Fragment result = visitor.visitReturnStatement(context);

        assertNotNull(result);
        assertTrue(result instanceof ActionFragment);
        EventHandlerAction finalAction = result.getCurrentAction();
        assertNotNull(finalAction);

        EventHandlerMachine mockMachine = mock(EventHandlerMachine.class);
        finalAction.apply(mockMachine);

        verify(expressionAction).apply(mockMachine);
        verify(mockMachine).end();
    }

    @Test
    void testVisitFullBody_EndsCorrectly() {
        JoshLangParser.FullBodyContext context = mock(JoshLangParser.FullBodyContext.class);
        JoshLangParser.StatementContext stmt1Context = mock(JoshLangParser.StatementContext.class);
        JoshLangParser.StatementContext stmt2Context = mock(JoshLangParser.StatementContext.class); // This one will end

        Fragment stmt1Fragment = mock(Fragment.class);
        EventHandlerAction stmt1Action = mock(EventHandlerAction.class);
        Fragment stmt2Fragment = mock(Fragment.class);
        EventHandlerAction stmt2Action = mock(EventHandlerAction.class);

        when(context.getChildCount()).thenReturn(2);
        when(context.getChild(0)).thenReturn(stmt1Context);
        when(context.getChild(1)).thenReturn(stmt2Context);

        when(stmt1Context.accept(parent)).thenReturn(stmt1Fragment);
        when(stmt1Fragment.getCurrentAction()).thenReturn(stmt1Action);
        when(stmt2Context.accept(parent)).thenReturn(stmt2Fragment);
        when(stmt2Fragment.getCurrentAction()).thenReturn(stmt2Action);

        EventHandlerMachine mockMachine = mock(EventHandlerMachine.class);
        when(mockMachine.isEnded()).thenReturn(false).thenReturn(true); // false after stmt1, true after stmt2

        Fragment result = visitor.visitFullBody(context);
        assertNotNull(result);
        assertTrue(result instanceof ActionFragment);

        result.getCurrentAction().apply(mockMachine);

        verify(stmt1Action).apply(mockMachine);
        verify(stmt2Action).apply(mockMachine);
        verify(mockMachine, times(2)).isEnded(); // Called after each statement
    }

    @Test
    void testVisitFullBody_ThrowsIfNotEnded() {
        JoshLangParser.FullBodyContext context = mock(JoshLangParser.FullBodyContext.class);
        JoshLangParser.StatementContext stmt1Context = mock(JoshLangParser.StatementContext.class);

        Fragment stmt1Fragment = mock(Fragment.class);
        EventHandlerAction stmt1Action = mock(EventHandlerAction.class);

        when(context.getChildCount()).thenReturn(1);
        when(context.getChild(0)).thenReturn(stmt1Context);
        when(stmt1Context.accept(parent)).thenReturn(stmt1Fragment);
        when(stmt1Fragment.getCurrentAction()).thenReturn(stmt1Action);

        EventHandlerMachine mockMachine = mock(EventHandlerMachine.class);
        when(mockMachine.isEnded()).thenReturn(false); // Never ends

        Fragment result = visitor.visitFullBody(context);
        assertNotNull(result);
        assertTrue(result instanceof ActionFragment);

        EventHandlerAction actionToTest = result.getCurrentAction();
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> {
            actionToTest.apply(mockMachine);
        });
        assertEquals("Body did not end with a return statement or an expression.", thrown.getMessage());
        verify(stmt1Action).apply(mockMachine);
        verify(mockMachine).isEnded();
    }


    @Test
    void testVisitEventHandlerGroupMemberInner() {
        JoshLangParser.EventHandlerGroupMemberInnerContext context = mock(JoshLangParser.EventHandlerGroupMemberInnerContext.class);
        JoshLangParser.FullBodyContext targetBodyContext = mock(JoshLangParser.FullBodyContext.class); // Assuming inner is a FullBody
        Fragment targetFragment = mock(Fragment.class); // This will be an ActionFragment from visitFullBody
        EventHandlerAction targetAction = mock(EventHandlerAction.class);

        when(context.fullBody()).thenReturn(targetBodyContext); // Assuming fullBody() is the accessor
        when(targetBodyContext.accept(visitor)).thenReturn(targetFragment); // Corrected: should be visited by *this* visitor
        when(targetFragment.getCurrentAction()).thenReturn(targetAction);

        Fragment result = visitor.visitEventHandlerGroupMemberInner(context);

        assertNotNull(result);
        assertTrue(result instanceof ActionFragment); // visitEventHandlerGroupMemberInner wraps the action from fullBody
        assertNotNull(result.getCurrentAction());

        // Test the action produced by visitEventHandlerGroupMemberInner
        EventHandlerMachine mockMachine = mock(EventHandlerMachine.class);
        result.getCurrentAction().apply(mockMachine); // This applies the wrapped action
        verify(targetAction).apply(mockMachine); // Verifying the original action from fullBody was called
    }

    @Test
    void testVisitConditionalIfEventHandlerGroupMember() {
        JoshLangParser.ConditionalIfEventHandlerGroupMemberContext context = mock(JoshLangParser.ConditionalIfEventHandlerGroupMemberContext.class);
        JoshLangParser.ExpressionContext conditionContext = mock(JoshLangParser.ExpressionContext.class);
        JoshLangParser.EventHandlerGroupMemberInnerContext innerContext = mock(JoshLangParser.EventHandlerGroupMemberInnerContext.class);

        Fragment conditionFragment = mock(Fragment.class);
        EventHandlerAction conditionAction = mock(EventHandlerAction.class); // Action for the condition
        Fragment innerFragment = mock(Fragment.class); // ActionFragment from visitEventHandlerGroupMemberInner
        EventHandlerAction innerAction = mock(EventHandlerAction.class); // Action for the body

        when(context.expression()).thenReturn(conditionContext);
        when(context.eventHandlerGroupMemberInner()).thenReturn(innerContext);

        when(conditionContext.accept(parent)).thenReturn(conditionFragment);
        when(conditionFragment.getCurrentAction()).thenReturn(conditionAction);

        when(innerContext.accept(visitor)).thenReturn(innerFragment); // Inner is visited by *this* visitor
        when(innerFragment.getCurrentAction()).thenReturn(innerAction);


        Fragment result = visitor.visitConditionalIfEventHandlerGroupMember(context);

        assertNotNull(result);
        assertTrue(result instanceof CompiledCallableFragment);
        CompiledCallableFragment ccf = (CompiledCallableFragment) result;
        assertNotNull(ccf.getCompiledCallable());
        assertTrue(ccf.getCompiledSelector().isPresent());
        assertNotNull(ccf.getCompiledSelector().get());

        // To verify the selector and callable would require deeper testing of PushDownMachineCallable/Selector
        // For now, ensuring they are created is sufficient as per typical visitor tests.
    }

    @Test
    void testVisitConditionalElifEventHandlerGroupMember() {
        JoshLangParser.ConditionalElifEventHandlerGroupMemberContext context = mock(JoshLangParser.ConditionalElifEventHandlerGroupMemberContext.class);
        JoshLangParser.ExpressionContext conditionContext = mock(JoshLangParser.ExpressionContext.class);
        JoshLangParser.EventHandlerGroupMemberInnerContext innerContext = mock(JoshLangParser.EventHandlerGroupMemberInnerContext.class);

        Fragment conditionFragment = mock(Fragment.class);
        EventHandlerAction conditionAction = mock(EventHandlerAction.class);
        Fragment innerFragment = mock(Fragment.class);
        EventHandlerAction innerAction = mock(EventHandlerAction.class);

        when(context.expression()).thenReturn(conditionContext);
        when(context.eventHandlerGroupMemberInner()).thenReturn(innerContext);

        when(conditionContext.accept(parent)).thenReturn(conditionFragment);
        when(conditionFragment.getCurrentAction()).thenReturn(conditionAction);
        when(innerContext.accept(visitor)).thenReturn(innerFragment); // Visited by this visitor
        when(innerFragment.getCurrentAction()).thenReturn(innerAction);

        Fragment result = visitor.visitConditionalElifEventHandlerGroupMember(context);

        assertNotNull(result);
        assertTrue(result instanceof CompiledCallableFragment);
        CompiledCallableFragment ccf = (CompiledCallableFragment) result;
        assertNotNull(ccf.getCompiledCallable());
        assertTrue(ccf.getCompiledSelector().isPresent());
        assertNotNull(ccf.getCompiledSelector().get());
    }

    @Test
    void testVisitConditionalElseEventHandlerGroupMember() {
        JoshLangParser.ConditionalElseEventHandlerGroupMemberContext context = mock(JoshLangParser.ConditionalElseEventHandlerGroupMemberContext.class);
        JoshLangParser.EventHandlerGroupMemberInnerContext innerContext = mock(JoshLangParser.EventHandlerGroupMemberInnerContext.class);

        Fragment innerFragment = mock(Fragment.class);
        EventHandlerAction innerAction = mock(EventHandlerAction.class);

        when(context.eventHandlerGroupMemberInner()).thenReturn(innerContext);
        when(innerContext.accept(visitor)).thenReturn(innerFragment); // Visited by this visitor
        when(innerFragment.getCurrentAction()).thenReturn(innerAction);

        Fragment result = visitor.visitConditionalElseEventHandlerGroupMember(context);

        assertNotNull(result);
        assertTrue(result instanceof CompiledCallableFragment);
        CompiledCallableFragment ccf = (CompiledCallableFragment) result;
        assertNotNull(ccf.getCompiledCallable());
        assertFalse(ccf.getCompiledSelector().isPresent()); // Else has no selector
    }

    @Test
    void testVisitEventHandlerGroupSingle() {
        JoshLangParser.EventHandlerGroupSingleContext context = mock(JoshLangParser.EventHandlerGroupSingleContext.class);
        JoshLangParser.EventHandlerGroupMemberContext memberContext = mock(JoshLangParser.EventHandlerGroupMemberContext.class);
        CompiledCallableFragment memberFragment = mock(CompiledCallableFragment.class);
        CompiledCallable mockCallable = mock(CompiledCallable.class);

        when(context.name).thenReturn(mock(org.antlr.v4.runtime.Token.class));
        when(context.name.getText()).thenReturn("attribute.event");
        when(context.eventHandlerGroupMember()).thenReturn(memberContext);
        when(memberContext.accept(visitor)).thenReturn(memberFragment); // Visited by this visitor
        when(memberFragment.getCompiledCallable()).thenReturn(mockCallable);
        when(memberFragment.getCompiledSelector()).thenReturn(Optional.empty());


        Fragment result = visitor.visitEventHandlerGroupSingle(context);

        assertNotNull(result);
        assertTrue(result instanceof EventHandlerGroupFragment);
        EventHandlerGroupFragment ehgf = (EventHandlerGroupFragment) result;
        EventHandlerGroup group = ehgf.getEventHandlerGroup();
        assertNotNull(group);

        assertEquals("attribute", group.getEventKey().getAttributeName());
        assertEquals("event", group.getEventKey().getEventName());
        assertEquals(1, group.getHandlers().size());
    }

    @Test
    void testVisitEventHandlerGroupMultiple_WithAndWithoutSelector() {
        JoshLangParser.EventHandlerGroupMultipleContext context = mock(JoshLangParser.EventHandlerGroupMultipleContext.class);
        JoshLangParser.ConditionalEventHandlerGroupMemberContext condMemberCtx1 = mock(JoshLangParser.ConditionalEventHandlerGroupMemberContext.class);
        JoshLangParser.ConditionalEventHandlerGroupMemberContext condMemberCtx2 = mock(JoshLangParser.ConditionalEventHandlerGroupMemberContext.class);

        CompiledCallableFragment ccf1 = mock(CompiledCallableFragment.class);
        CompiledCallable callable1 = mock(CompiledCallable.class);
        CompiledSelector selector1 = mock(CompiledSelector.class);

        CompiledCallableFragment ccf2 = mock(CompiledCallableFragment.class);
        CompiledCallable callable2 = mock(CompiledCallable.class);


        when(context.name).thenReturn(mock(org.antlr.v4.runtime.Token.class));
        when(context.name.getText()).thenReturn("myGroup.myEvent");

        when(context.conditionalEventHandlerGroupMember()).thenReturn(Arrays.asList(condMemberCtx1, condMemberCtx2));
        when(condMemberCtx1.accept(visitor)).thenReturn(ccf1);
        when(ccf1.getCompiledCallable()).thenReturn(callable1);
        when(ccf1.getCompiledSelector()).thenReturn(Optional.of(selector1));

        when(condMemberCtx2.accept(visitor)).thenReturn(ccf2);
        when(ccf2.getCompiledCallable()).thenReturn(callable2);
        when(ccf2.getCompiledSelector()).thenReturn(Optional.empty()); // No selector for the second one (like an else)

        Fragment result = visitor.visitEventHandlerGroupMultiple(context);

        assertNotNull(result);
        assertTrue(result instanceof EventHandlerGroupFragment);
        EventHandlerGroupFragment ehgf = (EventHandlerGroupFragment) result;
        EventHandlerGroup group = ehgf.getEventHandlerGroup();
        assertNotNull(group);

        assertEquals("myGroup", group.getEventKey().getAttributeName());
        assertEquals("myEvent", group.getEventKey().getEventName());
        assertEquals(2, group.getHandlers().size()); // Two handlers added
        // Could add more detailed assertions about the Handlers if needed
    }

    @Test
    void testVisitEventHandlerGeneral() {
        // Prepare static mock for ReservedWordChecker IF direct verification is needed
        // For "successful path", we just ensure no error for a valid name.
        JoshLangParser.EventHandlerGeneralContext context = mock(JoshLangParser.EventHandlerGeneralContext.class);
        JoshLangParser.EventHandlerGroupContext groupContext = mock(JoshLangParser.EventHandlerGroupContext.class); // The child is an eventHandlerGroup
        EventHandlerGroupFragment groupFragment = mock(EventHandlerGroupFragment.class);
        EventHandlerGroup eventHandlerGroup = mock(EventHandlerGroup.class);

        String validName = "myCustomEventHandler";
        when(context.name).thenReturn(mock(org.antlr.v4.runtime.Token.class));
        when(context.name.getText()).thenReturn(validName);

        // The child of eventHandlerGeneral is eventHandlerGroup.
        // It's not context.getChild(0) directly but context.eventHandlerGroup()
        when(context.eventHandlerGroup()).thenReturn(groupContext);
        when(groupContext.accept(visitor)).thenReturn(groupFragment); // Visited by this visitor
        when(groupFragment.getEventHandlerGroup()).thenReturn(eventHandlerGroup);

        // Test that ReservedWordChecker.checkVariableDeclaration doesn't throw for a valid name
        // This happens implicitly. If it threw, the test would fail.
        // For explicit verification (if it were not a static method or if we wanted to ensure it *was* called):
        // mockStatic(ReservedWordChecker.class) then verify static. But not strictly needed for "successful path".

        Fragment result = visitor.visitEventHandlerGeneral(context);

        assertNotNull(result);
        assertTrue(result instanceof ActionFragment);
        EventHandlerAction action = result.getCurrentAction();
        assertNotNull(action);

        EventHandlerMachine mockMachine = mock(EventHandlerMachine.class);
        action.apply(mockMachine);

        verify(mockMachine).putAttribute(eq(validName), eq(eventHandlerGroup));
    }
}
