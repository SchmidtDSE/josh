package org.joshsim.lang.interpret.visitor.delegates;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.joshsim.engine.value.ValueResolver;
import org.joshsim.engine.value.converter.Conversion;
import org.joshsim.engine.value.converter.DirectConversion;
import org.joshsim.engine.value.converter.NoopConversion;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.engine.value.type.EngineValue;
import org.joshsim.engine.value.function.CompiledCallable;
import org.joshsim.lang.antlr.JoshLangParser;
import org.joshsim.lang.interpret.BridgeGetter;
import org.joshsim.lang.interpret.action.EventHandlerAction;
import org.joshsim.lang.interpret.fragment.ActionFragment;
import org.joshsim.lang.interpret.fragment.ConversionFragment;
import org.joshsim.lang.interpret.fragment.Fragment;
import org.joshsim.lang.interpret.machine.EventHandlerMachine;
import org.joshsim.lang.interpret.visitor.JoshParserToMachineVisitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JoshTypesUnitsVisitorTest {

    private DelegateToolbox toolbox;
    private JoshParserToMachineVisitor parent;
    private EngineValueFactory engineValueFactory;
    private BridgeGetter bridgeGetter;
    private EngineValue singleCount;
    private JoshTypesUnitsVisitor visitor;
    private EngineValue mockEngineValueForPosition;


    @BeforeEach
    void setUp() {
        toolbox = mock(DelegateToolbox.class);
        parent = mock(JoshParserToMachineVisitor.class);
        engineValueFactory = mock(EngineValueFactory.class);
        bridgeGetter = mock(BridgeGetter.class);
        singleCount = mock(EngineValue.class);
        mockEngineValueForPosition = mock(EngineValue.class); // For the build(anyString(), Units.of(""))

        when(toolbox.getParent()).thenReturn(parent);
        when(toolbox.getValueFactory()).thenReturn(engineValueFactory);
        when(toolbox.getBridgeGetter()).thenReturn(bridgeGetter);
        when(engineValueFactory.build(1, Units.of("count"))).thenReturn(singleCount);
        when(engineValueFactory.build(anyString(), eq(Units.of("")))).thenReturn(mockEngineValueForPosition);


        visitor = new JoshTypesUnitsVisitor(toolbox);
    }

    private EventHandlerAction mockActionForExpression(JoshLangParser.ExpressionContext exprCtx) {
        Fragment fragment = mock(Fragment.class);
        EventHandlerAction action = mock(EventHandlerAction.class);
        when(exprCtx.accept(parent)).thenReturn(fragment);
        when(fragment.getCurrentAction()).thenReturn(action);
        return action;
    }

    @Test
    void testVisitCast() {
        JoshLangParser.CastContext context = mock(JoshLangParser.CastContext.class);
        context.operand = mock(JoshLangParser.ExpressionContext.class);
        context.target = mock(Token.class);
        String targetUnitsStr = "meter";
        Units expectedUnits = Units.of(targetUnitsStr);

        EventHandlerAction operandAction = mockActionForExpression(context.operand);
        when(context.target.getText()).thenReturn(targetUnitsStr);

        Fragment result = visitor.visitCast(context);
        assertNotNull(result);
        assertTrue(result instanceof ActionFragment);
        EventHandlerAction action = result.getCurrentAction();
        assertNotNull(action);

        EventHandlerMachine mockMachine = mock(EventHandlerMachine.class);
        action.apply(mockMachine);

        verify(operandAction).apply(mockMachine);
        verify(mockMachine).cast(expectedUnits, false);
    }

    @Test
    void testVisitCastForce() {
        JoshLangParser.CastForceContext context = mock(JoshLangParser.CastForceContext.class);
        context.operand = mock(JoshLangParser.ExpressionContext.class);
        context.target = mock(Token.class);
        String targetUnitsStr = "second";
        Units expectedUnits = Units.of(targetUnitsStr);

        EventHandlerAction operandAction = mockActionForExpression(context.operand);
        when(context.target.getText()).thenReturn(targetUnitsStr);

        Fragment result = visitor.visitCastForce(context);
        assertNotNull(result);
        assertTrue(result instanceof ActionFragment);
        EventHandlerAction action = result.getCurrentAction();
        assertNotNull(action);

        EventHandlerMachine mockMachine = mock(EventHandlerMachine.class);
        action.apply(mockMachine);

        verify(operandAction).apply(mockMachine);
        verify(mockMachine).cast(expectedUnits, true);
    }

    @Test
    void testVisitNoopConversion() {
        JoshLangParser.NoopConversionContext context = mock(JoshLangParser.NoopConversionContext.class);
        ParseTree aliasNode = mock(ParseTree.class);
        String aliasName = "myUnit";
        Units expectedUnits = Units.of(aliasName);

        // K_PASS IDENTIFIER
        when(context.getChild(1)).thenReturn(aliasNode);
        when(aliasNode.getText()).thenReturn(aliasName);

        Fragment result = visitor.visitNoopConversion(context);
        assertNotNull(result);
        assertTrue(result instanceof ConversionFragment);
        ConversionFragment cf = (ConversionFragment) result;
        Conversion conversion = cf.getConversion();
        assertNotNull(conversion);
        assertTrue(conversion instanceof NoopConversion);
        assertEquals(expectedUnits, conversion.getSourceUnits()); // NoopConversion sets source and dest the same
        assertEquals(expectedUnits, conversion.getDestinationUnits());
    }

    @Test
    void testVisitActiveConversion() {
        JoshLangParser.ActiveConversionContext context = mock(JoshLangParser.ActiveConversionContext.class);
        ParseTree destUnitsNode = mock(ParseTree.class);
        JoshLangParser.ExpressionContext exprNode = mock(JoshLangParser.ExpressionContext.class); // Child 2 is expression for callable

        String destUnitsStr = "kilometer";
        Units expectedDestUnits = Units.of(destUnitsStr);
        EventHandlerAction callableAction = mockActionForExpression(exprNode);

        // IDENTIFIER K_TO expression
        when(context.getChild(0)).thenReturn(destUnitsNode);
        when(destUnitsNode.getText()).thenReturn(destUnitsStr);
        when(context.getChild(2)).thenReturn(exprNode); // This is the expression for the callable

        Fragment result = visitor.visitActiveConversion(context);
        assertNotNull(result);
        assertTrue(result instanceof ConversionFragment);
        ConversionFragment cf = (ConversionFragment) result;
        Conversion conversion = cf.getConversion();
        assertNotNull(conversion);
        assertTrue(conversion instanceof DirectConversion);
        assertEquals(expectedDestUnits, conversion.getDestinationUnits());
        assertNull(conversion.getSourceUnits()); // Source units not defined by this rule, set by parent UnitStanza

        CompiledCallable compiledCallable = ((DirectConversion)conversion).getConversionCallable();
        assertNotNull(compiledCallable);
        // To test compiledCallable, one would call its execute method,
        // which should involve bridgeGetter and the callableAction.
        // For this unit test, ensuring it's created is often sufficient.
    }

    @Test
    void testVisitCreateVariableExpression() {
        JoshLangParser.CreateVariableExpressionContext context = mock(JoshLangParser.CreateVariableExpressionContext.class);
        context.count = mock(JoshLangParser.ExpressionContext.class);
        context.target = mock(Token.class);
        String entityName = "myEntity";

        EventHandlerAction countAction = mockActionForExpression(context.count);
        when(context.target.getText()).thenReturn(entityName);

        Fragment result = visitor.visitCreateVariableExpression(context);
        assertNotNull(result);
        assertTrue(result instanceof ActionFragment);
        EventHandlerAction action = result.getCurrentAction();
        assertNotNull(action);

        EventHandlerMachine mockMachine = mock(EventHandlerMachine.class);
        action.apply(mockMachine);

        verify(countAction).apply(mockMachine);
        verify(mockMachine).createEntity(entityName);
    }

    @Test
    void testVisitAttrExpression() {
        JoshLangParser.AttrExpressionContext context = mock(JoshLangParser.AttrExpressionContext.class);
        JoshLangParser.ExpressionContext exprCtx = mock(JoshLangParser.ExpressionContext.class); // Child 0
        ParseTree attrNameNode = mock(ParseTree.class); // Child 2

        String attrName = "health";
        EventHandlerAction exprAction = mockActionForExpression(exprCtx);

        when(context.getChild(0)).thenReturn(exprCtx);
        when(context.getChild(2)).thenReturn(attrNameNode);
        when(attrNameNode.getText()).thenReturn(attrName);

        ValueResolver mockResolver = mock(ValueResolver.class);

        try (MockedConstruction<ValueResolver> mockedResolverConstruction =
                     mockConstruction(ValueResolver.class, (mock, constructionContext) -> {
                         // Assert constructor arguments if possible/needed
                         // For now, just ensure our mockResolver is used in pushAttribute
                         // This is tricky because the mock created by mockConstruction is what's used.
                     })) {

            Fragment result = visitor.visitAttrExpression(context);
            assertNotNull(result);
            assertTrue(result instanceof ActionFragment);
            EventHandlerAction action = result.getCurrentAction();
            assertNotNull(action);

            EventHandlerMachine mockMachine = mock(EventHandlerMachine.class);
            action.apply(mockMachine);

            assertEquals(1, mockedResolverConstruction.constructed().size());
            ValueResolver instantiatedResolver = mockedResolverConstruction.constructed().get(0);

            verify(exprAction).apply(mockMachine);
            verify(mockMachine).pushAttribute(instantiatedResolver);
            // To verify ValueResolver constructor:
            // assertEquals(engineValueFactory, constructionContext.arguments().get(0));
            // assertEquals(attrName, constructionContext.arguments().get(1));
            // This depends on argument order and type, may need specific constructor context.
        }
    }

    @Test
    void testVisitSpatialQuery() {
        JoshLangParser.SpatialQueryContext context = mock(JoshLangParser.SpatialQueryContext.class);
        context.target = mock(Token.class); // Assuming target is a simple token for the query string
        context.distance = mock(JoshLangParser.ExpressionContext.class);
        String targetQueryString = "nearest(agent)"; // Example query string

        EventHandlerAction distanceAction = mockActionForExpression(context.distance);
        // The visitor uses ctx.target.toString() which might be different from getText() if target is complex
        // For a simple Token, getText() is often what's intended if toString() isn't overridden meaningfully.
        // Let's assume ctx.target.getText() for clarity, or mock toString() if that's truly used.
        // The production code uses `ctx.target.getText()`, so this is correct.
        when(context.target.getText()).thenReturn(targetQueryString);

        try (MockedConstruction<ValueResolver> mockedResolverConstruction =
                     mockConstruction(ValueResolver.class)) {

            Fragment result = visitor.visitSpatialQuery(context);
            assertNotNull(result);
            assertTrue(result instanceof ActionFragment);
            EventHandlerAction action = result.getCurrentAction();
            assertNotNull(action);

            EventHandlerMachine mockMachine = mock(EventHandlerMachine.class);
            action.apply(mockMachine);

            assertEquals(1, mockedResolverConstruction.constructed().size());
            ValueResolver instantiatedResolver = mockedResolverConstruction.constructed().get(0);

            verify(distanceAction).apply(mockMachine);
            verify(mockMachine).executeSpatialQuery(instantiatedResolver);
            // Can verify ValueResolver constructor args here too if needed
        }
    }

    @Test
    void testVisitPosition() {
        JoshLangParser.PositionContext context = mock(JoshLangParser.PositionContext.class);
        JoshLangParser.ExpressionContext unitsExpr1 = mock(JoshLangParser.ExpressionContext.class); // child 0
        ParseTree typeNode1 = mock(ParseTree.class); // child 1
        JoshLangParser.ExpressionContext unitsExpr2 = mock(JoshLangParser.ExpressionContext.class); // child 3
        ParseTree typeNode2 = mock(ParseTree.class); // child 4

        String type1 = "cartesian";
        String type2 = "polar";

        EventHandlerAction unitsAction1 = mockActionForExpression(unitsExpr1);
        EventHandlerAction unitsAction2 = mockActionForExpression(unitsExpr2);

        when(context.getChild(0)).thenReturn(unitsExpr1);
        when(context.getChild(1)).thenReturn(typeNode1);
        when(typeNode1.getText()).thenReturn(type1);
        // child 2 is ','
        when(context.getChild(3)).thenReturn(unitsExpr2);
        when(context.getChild(4)).thenReturn(typeNode2);
        when(typeNode2.getText()).thenReturn(type2);

        Fragment result = visitor.visitPosition(context);
        assertNotNull(result);
        assertTrue(result instanceof ActionFragment);
        EventHandlerAction action = result.getCurrentAction();
        assertNotNull(action);

        EventHandlerMachine mockMachine = mock(EventHandlerMachine.class);
        action.apply(mockMachine);

        // Order of operations is important
        ArgumentCaptor<EventHandlerAction> machineActions = ArgumentCaptor.forClass(EventHandlerAction.class);
        // This captures individual pushes and makePosition. More complex to verify sequence this way.
        // Let's verify step-by-step.

        // A better way to verify sequence with Mockito is InOrder
        org.mockito.InOrder inOrder = inOrder(mockMachine, unitsAction1, unitsAction2);

        inOrder.verify(unitsAction1).apply(mockMachine);
        inOrder.verify(mockMachine).push(mockEngineValueForPosition); // From engineFactory.build(type1, Units.of(""))
        inOrder.verify(unitsAction2).apply(mockMachine);
        inOrder.verify(mockMachine).push(mockEngineValueForPosition); // From engineFactory.build(type2, Units.of(""))
        inOrder.verify(mockMachine).makePosition();

        verify(engineValueFactory).build(type1, Units.of(""));
        verify(engineValueFactory).build(type2, Units.of(""));
    }

    @Test
    void testVisitCreateSingleExpression() {
        JoshLangParser.CreateSingleExpressionContext context = mock(JoshLangParser.CreateSingleExpressionContext.class);
        context.target = mock(Token.class);
        String entityName = "anotherEntity";
        when(context.target.getText()).thenReturn(entityName);

        Fragment result = visitor.visitCreateSingleExpression(context);
        assertNotNull(result);
        assertTrue(result instanceof ActionFragment);
        EventHandlerAction action = result.getCurrentAction();
        assertNotNull(action);

        EventHandlerMachine mockMachine = mock(EventHandlerMachine.class);
        action.apply(mockMachine);

        org.mockito.InOrder inOrder = inOrder(mockMachine);
        inOrder.verify(mockMachine).push(singleCount);
        inOrder.verify(mockMachine).createEntity(entityName);
    }

    @Test
    void testVisitAssignment() {
        JoshLangParser.AssignmentContext context = mock(JoshLangParser.AssignmentContext.class);
        ParseTree identifierNode = mock(ParseTree.class); // child 1
        context.val = mock(JoshLangParser.ExpressionContext.class); // child 3 is val, child 2 is '='

        String identifierName = "myVar"; // A valid, non-reserved name
        EventHandlerAction valAction = mockActionForExpression(context.val);

        when(context.getChild(1)).thenReturn(identifierNode);
        when(identifierNode.getText()).thenReturn(identifierName);
        // Assuming grammar: K_LET IDENTIFIER K_ASSIGN expression
        // Child 0: K_LET, Child 1: IDENTIFIER, Child 2: K_ASSIGN, Child 3: expression
        // The visitor code uses: ctx.getChild(1).getText(), ctx.val.accept(parent)
        // So, this mocking aligns.

        Fragment result = visitor.visitAssignment(context);
        assertNotNull(result);
        assertTrue(result instanceof ActionFragment);
        EventHandlerAction action = result.getCurrentAction();
        assertNotNull(action);

        EventHandlerMachine mockMachine = mock(EventHandlerMachine.class);
        action.apply(mockMachine);

        verify(valAction).apply(mockMachine);
        verify(mockMachine).saveLocalVariable(identifierName);
        // Implicitly tests ReservedWordChecker.checkVariableDeclaration did not throw
    }
}
