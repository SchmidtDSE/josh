package org.joshsim.lang.interpret.visitor.delegates;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.engine.value.type.EngineValue;
import org.joshsim.lang.antlr.JoshLangParser;
import org.joshsim.lang.interpret.action.EventHandlerAction;
import org.joshsim.lang.interpret.fragment.ActionFragment;
import org.joshsim.lang.interpret.fragment.Fragment;
import org.joshsim.lang.interpret.machine.EventHandlerMachine;
import org.joshsim.lang.interpret.visitor.JoshParserToMachineVisitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JoshMathematicsVisitorTest {

    private DelegateToolbox toolbox;
    private JoshParserToMachineVisitor parent;
    private EngineValueFactory engineValueFactory;
    private JoshMathematicsVisitor visitor;
    private EngineValue mockTrueValueEmptyUnits;


    @BeforeEach
    void setUp() {
        toolbox = mock(DelegateToolbox.class);
        parent = mock(JoshParserToMachineVisitor.class);
        engineValueFactory = mock(EngineValueFactory.class);
        mockTrueValueEmptyUnits = mock(EngineValue.class);

        when(toolbox.getParent()).thenReturn(parent);
        when(toolbox.getValueFactory()).thenReturn(engineValueFactory);
        when(engineValueFactory.build(true, Units.EMPTY)).thenReturn(mockTrueValueEmptyUnits);

        visitor = new JoshMathematicsVisitor(toolbox);
    }

    private EventHandlerAction mockActionForExpression(JoshLangParser.ExpressionContext exprCtx) {
        Fragment fragment = mock(Fragment.class);
        EventHandlerAction action = mock(EventHandlerAction.class);
        when(exprCtx.accept(parent)).thenReturn(fragment);
        when(fragment.getCurrentAction()).thenReturn(action);
        return action;
    }

    @Test
    void testVisitMapLinear() {
        JoshLangParser.MapLinearContext context = mock(JoshLangParser.MapLinearContext.class);
        context.operand = mock(JoshLangParser.ExpressionContext.class);
        context.fromLow = mock(JoshLangParser.ExpressionContext.class);
        context.fromHigh = mock(JoshLangParser.ExpressionContext.class);
        context.toLow = mock(JoshLangParser.ExpressionContext.class);
        context.toHigh = mock(JoshLangParser.ExpressionContext.class);

        EventHandlerAction operandAction = mockActionForExpression(context.operand);
        EventHandlerAction fromLowAction = mockActionForExpression(context.fromLow);
        EventHandlerAction fromHighAction = mockActionForExpression(context.fromHigh);
        EventHandlerAction toLowAction = mockActionForExpression(context.toLow);
        EventHandlerAction toHighAction = mockActionForExpression(context.toHigh);

        Fragment result = visitor.visitMapLinear(context);
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
        verify(mockMachine).push(mockTrueValueEmptyUnits); // for clamp = true
        verify(mockMachine).applyMap("linear");
    }

    @Test
    void testVisitMapParam() {
        JoshLangParser.MapParamContext context = mock(JoshLangParser.MapParamContext.class);
        context.operand = mock(JoshLangParser.ExpressionContext.class);
        context.from = mock(JoshLangParser.ExpressionContext.class);
        context.to = mock(JoshLangParser.ExpressionContext.class);
        context.method = mock(Token.class);

        when(context.method.getText()).thenReturn("testMethod");
        EventHandlerAction operandAction = mockActionForExpression(context.operand);
        EventHandlerAction fromAction = mockActionForExpression(context.from);
        EventHandlerAction toAction = mockActionForExpression(context.to);

        Fragment result = visitor.visitMapParam(context);
        assertNotNull(result);
        assertTrue(result instanceof ActionFragment);
        EventHandlerAction action = result.getCurrentAction();
        assertNotNull(action);

        EventHandlerMachine mockMachine = mock(EventHandlerMachine.class);
        action.apply(mockMachine);

        verify(operandAction).apply(mockMachine);
        verify(fromAction).apply(mockMachine);
        verify(toAction).apply(mockMachine);
        verify(mockMachine).push(mockTrueValueEmptyUnits); // for clamp = true
        verify(mockMachine).applyMap("testMethod");
    }

    @Test
    void testVisitMapParamParam() {
        JoshLangParser.MapParamParamContext context = mock(JoshLangParser.MapParamParamContext.class);
        context.operand = mock(JoshLangParser.ExpressionContext.class);
        context.from = mock(JoshLangParser.ExpressionContext.class);
        context.to = mock(JoshLangParser.ExpressionContext.class);
        context.param = mock(JoshLangParser.ExpressionContext.class);
        context.method = mock(Token.class);

        when(context.method.getText()).thenReturn("testMethodWithParam");
        EventHandlerAction operandAction = mockActionForExpression(context.operand);
        EventHandlerAction fromAction = mockActionForExpression(context.from);
        EventHandlerAction toAction = mockActionForExpression(context.to);
        EventHandlerAction paramAction = mockActionForExpression(context.param);

        Fragment result = visitor.visitMapParamParam(context);
        assertNotNull(result);
        assertTrue(result instanceof ActionFragment);
        EventHandlerAction action = result.getCurrentAction();
        assertNotNull(action);

        EventHandlerMachine mockMachine = mock(EventHandlerMachine.class);
        action.apply(mockMachine);

        verify(operandAction).apply(mockMachine);
        verify(fromAction).apply(mockMachine);
        verify(toAction).apply(mockMachine);
        verify(paramAction).apply(mockMachine); // Param is applied before map
        verify(mockMachine).applyMap("testMethodWithParam");
    }


    @Test
    void testVisitAdditionExpression_Add() {
        JoshLangParser.AdditionExpressionContext context = mock(JoshLangParser.AdditionExpressionContext.class);
        context.left = mock(JoshLangParser.ExpressionContext.class);
        context.right = mock(JoshLangParser.ExpressionContext.class);
        context.op = mock(Token.class);
        when(context.op.getText()).thenReturn("+");

        EventHandlerAction leftAction = mockActionForExpression(context.left);
        EventHandlerAction rightAction = mockActionForExpression(context.right);

        Fragment result = visitor.visitAdditionExpression(context);
        assertNotNull(result);
        assertTrue(result instanceof ActionFragment);
        EventHandlerAction action = result.getCurrentAction();
        assertNotNull(action);

        EventHandlerMachine mockMachine = mock(EventHandlerMachine.class);
        action.apply(mockMachine);

        verify(leftAction).apply(mockMachine);
        verify(rightAction).apply(mockMachine);
        verify(mockMachine).add();
    }

    @Test
    void testVisitMultiplyExpression_Multiply() {
        JoshLangParser.MultiplyExpressionContext context = mock(JoshLangParser.MultiplyExpressionContext.class);
        context.left = mock(JoshLangParser.ExpressionContext.class);
        context.right = mock(JoshLangParser.ExpressionContext.class);
        context.op = mock(Token.class);
        when(context.op.getText()).thenReturn("*");

        EventHandlerAction leftAction = mockActionForExpression(context.left);
        EventHandlerAction rightAction = mockActionForExpression(context.right);

        Fragment result = visitor.visitMultiplyExpression(context);
        assertNotNull(result);
        assertTrue(result instanceof ActionFragment);
        EventHandlerAction action = result.getCurrentAction();
        assertNotNull(action);

        EventHandlerMachine mockMachine = mock(EventHandlerMachine.class);
        action.apply(mockMachine);

        verify(leftAction).apply(mockMachine);
        verify(rightAction).apply(mockMachine);
        verify(mockMachine).multiply();
    }

    @Test
    void testVisitPowExpression() {
        JoshLangParser.PowExpressionContext context = mock(JoshLangParser.PowExpressionContext.class);
        context.left = mock(JoshLangParser.ExpressionContext.class);
        context.right = mock(JoshLangParser.ExpressionContext.class);

        EventHandlerAction leftAction = mockActionForExpression(context.left);
        EventHandlerAction rightAction = mockActionForExpression(context.right);

        Fragment result = visitor.visitPowExpression(context);
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
        JoshLangParser.ParenExpressionContext context = mock(JoshLangParser.ParenExpressionContext.class);
        ParseTree childExpressionNode = mock(ParseTree.class); // Typically an ExpressionContext
        Fragment expectedFragment = mock(Fragment.class);

        when(context.getChild(1)).thenReturn(childExpressionNode); // '(' is child 0, expr is child 1, ')' is child 2
        when(childExpressionNode.accept(parent)).thenReturn(expectedFragment);

        Fragment actualFragment = visitor.visitParenExpression(context);
        assertSame(expectedFragment, actualFragment);
    }

    @Test
    void testVisitLimitBoundExpression() {
        JoshLangParser.LimitBoundExpressionContext context = mock(JoshLangParser.LimitBoundExpressionContext.class);
        context.operand = mock(JoshLangParser.ExpressionContext.class);
        context.low = mock(JoshLangParser.ExpressionContext.class);
        context.high = mock(JoshLangParser.ExpressionContext.class);

        EventHandlerAction operandAction = mockActionForExpression(context.operand);
        EventHandlerAction lowAction = mockActionForExpression(context.low);
        EventHandlerAction highAction = mockActionForExpression(context.high);

        Fragment result = visitor.visitLimitBoundExpression(context);
        assertNotNull(result);
        assertTrue(result instanceof ActionFragment);
        EventHandlerAction action = result.getCurrentAction();
        assertNotNull(action);

        EventHandlerMachine mockMachine = mock(EventHandlerMachine.class);
        action.apply(mockMachine);

        verify(operandAction).apply(mockMachine);
        verify(lowAction).apply(mockMachine);
        verify(highAction).apply(mockMachine);
        verify(mockMachine).bound(true, true);
    }

    @Test
    void testVisitLimitMinExpression() {
        JoshLangParser.LimitMinExpressionContext context = mock(JoshLangParser.LimitMinExpressionContext.class);
        context.operand = mock(JoshLangParser.ExpressionContext.class);
        context.limit = mock(JoshLangParser.ExpressionContext.class);

        EventHandlerAction operandAction = mockActionForExpression(context.operand);
        EventHandlerAction limitAction = mockActionForExpression(context.limit);

        Fragment result = visitor.visitLimitMinExpression(context);
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
        JoshLangParser.LimitMaxExpressionContext context = mock(JoshLangParser.LimitMaxExpressionContext.class);
        context.operand = mock(JoshLangParser.ExpressionContext.class);
        context.limit = mock(JoshLangParser.ExpressionContext.class);

        EventHandlerAction operandAction = mockActionForExpression(context.operand);
        EventHandlerAction limitAction = mockActionForExpression(context.limit);

        Fragment result = visitor.visitLimitMaxExpression(context);
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
    void testVisitSingleParamFunctionCall_Abs() {
        JoshLangParser.SingleParamFunctionCallContext context = mock(JoshLangParser.SingleParamFunctionCallContext.class);
        context.operand = mock(JoshLangParser.ExpressionContext.class);
        context.name = mock(Token.class);
        when(context.name.getText()).thenReturn("abs");

        EventHandlerAction operandAction = mockActionForExpression(context.operand);

        Fragment result = visitor.visitSingleParamFunctionCall(context);
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
