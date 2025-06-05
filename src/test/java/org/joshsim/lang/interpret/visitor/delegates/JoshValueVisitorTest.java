package org.joshsim.lang.interpret.visitor.delegates;

import org.antlr.v4.runtime.tree.ParseTree;
import org.joshsim.engine.value.ValueResolver;
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
import org.mockito.MockedConstruction;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JoshValueVisitorTest {

    private DelegateToolbox toolbox;
    private JoshParserToMachineVisitor parent; // Not directly used by JoshValueVisitor methods, but part of toolbox
    private EngineValueFactory engineValueFactory;
    private EngineValue allString;
    private JoshValueVisitor visitor;

    @BeforeEach
    void setUp() {
        toolbox = mock(DelegateToolbox.class);
        parent = mock(JoshParserToMachineVisitor.class);
        engineValueFactory = mock(EngineValueFactory.class);
        allString = mock(EngineValue.class);

        when(toolbox.getParent()).thenReturn(parent);
        when(toolbox.getValueFactory()).thenReturn(engineValueFactory);
        when(engineValueFactory.build("all", Units.of(""))).thenReturn(allString);

        visitor = new JoshValueVisitor(toolbox);
    }

    @Test
    void testVisitIdentifier() {
        JoshLangParser.IdentifierContext context = mock(JoshLangParser.IdentifierContext.class);
        String identifierName = "myVariable";
        when(context.getText()).thenReturn(identifierName);

        try (MockedConstruction<ValueResolver> mockedResolverConstruction =
                     mockConstruction(ValueResolver.class)) {

            Fragment result = visitor.visitIdentifier(context);
            assertNotNull(result);
            assertTrue(result instanceof ActionFragment);
            EventHandlerAction action = result.getCurrentAction();
            assertNotNull(action);

            EventHandlerMachine mockMachine = mock(EventHandlerMachine.class);
            action.apply(mockMachine);

            assertEquals(1, mockedResolverConstruction.constructed().size());
            ValueResolver instantiatedResolver = mockedResolverConstruction.constructed().get(0);
            // Verify constructor arguments on ValueResolver
            // In ValueResolver constructor: ValueResolver(EngineValueFactory factory, String identifier)
            // constructionContext.arguments() would give [engineValueFactory, identifierName]
            // This can be verified if mockConstruction is set up with an Answer to check args.
            // For now, verifying the push is key.
            verify(mockMachine).push(instantiatedResolver);
        }
    }

    @Test
    void testVisitNumber() {
        JoshLangParser.NumberContext context = mock(JoshLangParser.NumberContext.class);
        ParseTree numberNode = mock(ParseTree.class);
        String numberStr = "123.45";
        EngineValue mockNumericValue = mock(EngineValue.class);

        when(context.getChild(0)).thenReturn(numberNode);
        when(numberNode.getText()).thenReturn(numberStr);
        when(engineValueFactory.parseNumber(numberStr, Units.of("count"))).thenReturn(mockNumericValue);

        Fragment result = visitor.visitNumber(context);
        assertNotNull(result);
        assertTrue(result instanceof ActionFragment);
        EventHandlerAction action = result.getCurrentAction();
        assertNotNull(action);

        EventHandlerMachine mockMachine = mock(EventHandlerMachine.class);
        action.apply(mockMachine);

        verify(mockMachine).push(mockNumericValue);
    }

    @Test
    void testVisitUnitsValue_IntegerWithUnits() {
        JoshLangParser.UnitsValueContext context = mock(JoshLangParser.UnitsValueContext.class);
        ParseTree numberNode = mock(ParseTree.class); // Child 0: NUMBER
        ParseTree unitsNode = mock(ParseTree.class);  // Child 1: IDENTIFIER (for units)

        String numberStr = "5";
        String unitsStr = "km";
        EngineValue mockUnitsValue = mock(EngineValue.class);

        when(context.getChild(0)).thenReturn(numberNode);
        when(numberNode.getText()).thenReturn(numberStr);
        when(context.getChild(1)).thenReturn(unitsNode);
        when(unitsNode.getText()).thenReturn(unitsStr);

        // Logic from parseUnitsValue for integer with units:
        when(engineValueFactory.build(5L, Units.of("km"))).thenReturn(mockUnitsValue);

        Fragment result = visitor.visitUnitsValue(context);
        assertNotNull(result);
        assertTrue(result instanceof ActionFragment);
        EventHandlerAction action = result.getCurrentAction();
        assertNotNull(action);

        EventHandlerMachine mockMachine = mock(EventHandlerMachine.class);
        action.apply(mockMachine);

        verify(mockMachine).push(mockUnitsValue);
    }

    @Test
    void testVisitUnitsValue_Percentage() {
        JoshLangParser.UnitsValueContext context = mock(JoshLangParser.UnitsValueContext.class);
        ParseTree numberNode = mock(ParseTree.class);
        ParseTree unitsNode = mock(ParseTree.class);

        String numberStr = "25"; // 25 percent
        String unitsStr = "percent";
        EngineValue mockPercentageValue = mock(EngineValue.class);

        when(context.getChild(0)).thenReturn(numberNode);
        when(numberNode.getText()).thenReturn(numberStr);
        when(context.getChild(1)).thenReturn(unitsNode);
        when(unitsNode.getText()).thenReturn(unitsStr);

        // Logic from parseUnitsValue for percentage:
        when(engineValueFactory.buildForNumber(0.25, Units.of("count"))).thenReturn(mockPercentageValue);

        Fragment result = visitor.visitUnitsValue(context);
        assertNotNull(result);
        assertTrue(result instanceof ActionFragment);
        EventHandlerAction action = result.getCurrentAction();
        assertNotNull(action);

        EventHandlerMachine mockMachine = mock(EventHandlerMachine.class);
        action.apply(mockMachine);

        verify(mockMachine).push(mockPercentageValue);
    }


    @Test
    void testVisitString() {
        JoshLangParser.StringContext context = mock(JoshLangParser.StringContext.class);
        String rawString = "\"hello world\""; // String with quotes
        String actualString = "hello world";   // String without quotes
        EngineValue mockStringValue = mock(EngineValue.class);

        when(context.getText()).thenReturn(rawString);
        when(engineValueFactory.build(actualString, Units.of(""))).thenReturn(mockStringValue);

        Fragment result = visitor.visitString(context);
        assertNotNull(result);
        assertTrue(result instanceof ActionFragment);
        EventHandlerAction action = result.getCurrentAction();
        assertNotNull(action);

        EventHandlerMachine mockMachine = mock(EventHandlerMachine.class);
        action.apply(mockMachine);

        verify(mockMachine).push(mockStringValue);
    }

    @Test
    void testVisitBool_True() {
        JoshLangParser.BoolContext context = mock(JoshLangParser.BoolContext.class);
        ParseTree boolNode = mock(ParseTree.class);
        EngineValue mockBoolValue = mock(EngineValue.class);

        when(context.getChild(0)).thenReturn(boolNode);
        when(boolNode.getText()).thenReturn("true");
        when(engineValueFactory.build(true, Units.of(""))).thenReturn(mockBoolValue);

        Fragment result = visitor.visitBool(context);
        assertNotNull(result);
        assertTrue(result instanceof ActionFragment);
        EventHandlerAction action = result.getCurrentAction();
        assertNotNull(action);

        EventHandlerMachine mockMachine = mock(EventHandlerMachine.class);
        action.apply(mockMachine);

        verify(mockMachine).push(mockBoolValue);
    }

    @Test
    void testVisitAllExpression() {
        JoshLangParser.AllExpressionContext context = mock(JoshLangParser.AllExpressionContext.class);
        // allString is already mocked and configured in setUp

        Fragment result = visitor.visitAllExpression(context);
        assertNotNull(result);
        assertTrue(result instanceof ActionFragment);
        EventHandlerAction action = result.getCurrentAction();
        assertNotNull(action);

        EventHandlerMachine mockMachine = mock(EventHandlerMachine.class);
        action.apply(mockMachine);

        verify(mockMachine).push(allString);
    }

    @Test
    void testVisitExternalValue() {
        JoshLangParser.ExternalValueContext context = mock(JoshLangParser.ExternalValueContext.class);
        String externalName = "env_temp";
        long stepCount = 15L;

        // Assuming ctx.name is a Token for the IDENTIFIER
        org.antlr.v4.runtime.Token nameToken = mock(org.antlr.v4.runtime.Token.class);
        when(context.name).thenReturn(nameToken);
        when(nameToken.getText()).thenReturn(externalName);

        Fragment result = visitor.visitExternalValue(context);
        assertNotNull(result);
        assertTrue(result instanceof ActionFragment);
        EventHandlerAction action = result.getCurrentAction();
        assertNotNull(action);

        EventHandlerMachine mockMachine = mock(EventHandlerMachine.class);
        when(mockMachine.getStepCount()).thenReturn(stepCount); // Mock machine's response

        action.apply(mockMachine);

        verify(mockMachine).getStepCount();
        verify(mockMachine).pushExternal(externalName, stepCount);
    }

    @Test
    void testVisitExternalValueAtTime() {
        JoshLangParser.ExternalValueAtTimeContext context = mock(JoshLangParser.ExternalValueAtTimeContext.class);
        ParseTree nameNode = mock(ParseTree.class);
        String externalName = "temp_at_time";
        // Assuming grammar: K_EXTERNAL_TIME IDENTIFIER expression
        // Child 0: K_EXTERNAL_TIME, Child 1: IDENTIFIER, Child 2: expression (for time)
        // The visitor code: String name = ctx.getChild(1).getText();
        // Action is (machine) -> {}

        when(context.getChild(1)).thenReturn(nameNode);
        when(nameNode.getText()).thenReturn(externalName);
        // Expression for time (child 2) is not used in the current empty action.

        Fragment result = visitor.visitExternalValueAtTime(context);
        assertNotNull(result);
        assertTrue(result instanceof ActionFragment);
        EventHandlerAction action = result.getCurrentAction();
        assertNotNull(action);

        EventHandlerMachine mockMachine = mock(EventHandlerMachine.class);
        EventHandlerMachine returnedMachine = action.apply(mockMachine); // Apply the action

        assertSame(mockMachine, returnedMachine); // Action is (machine) -> { return machine; } (or just machine -> {})
        verifyNoMoreInteractions(mockMachine); // Verify no push, pop, or specific method calls
                                               // (other than what might be default like toString if logging)
    }
}
