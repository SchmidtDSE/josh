package org.joshsim.lang.interpret.visitor.delegates;

import org.antlr.v4.runtime.Token;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JoshDistributionVisitorTest {

    private DelegateToolbox toolbox;
    private JoshParserToMachineVisitor parent;
    private JoshDistributionVisitor visitor;
    private EngineValue mockSingleCount;

    @BeforeEach
    void setUp() {
        toolbox = mock(DelegateToolbox.class);
        parent = mock(JoshParserToMachineVisitor.class);
        EngineValueFactory mockValueFactory = mock(EngineValueFactory.class);
        mockSingleCount = mock(EngineValue.class);

        when(toolbox.getParent()).thenReturn(parent);
        when(toolbox.getValueFactory()).thenReturn(mockValueFactory);
        when(mockValueFactory.build(1, Units.of("count"))).thenReturn(mockSingleCount);

        visitor = new JoshDistributionVisitor(toolbox);
    }

    @Test
    void testVisitSlice() {
        // Mock
        JoshLangParser.SliceContext context = mock(JoshLangParser.SliceContext.class);
        context.subject = mock(JoshLangParser.ExpressionContext.class);
        context.selection = mock(JoshLangParser.ExpressionContext.class);

        Fragment subjectFragment = mock(Fragment.class);
        Fragment selectionFragment = mock(Fragment.class);
        EventHandlerAction subjectAction = mock(EventHandlerAction.class);
        EventHandlerAction selectionAction = mock(EventHandlerAction.class);

        when(context.subject.accept(parent)).thenReturn(subjectFragment);
        when(context.selection.accept(parent)).thenReturn(selectionFragment);
        when(subjectFragment.getCurrentAction()).thenReturn(subjectAction);
        when(selectionFragment.getCurrentAction()).thenReturn(selectionAction);

        // Test
        Fragment result = visitor.visitSlice(context);

        // Validate
        assertNotNull(result);
        assertTrue(result instanceof ActionFragment);

        EventHandlerAction action = result.getCurrentAction();
        assertNotNull(action);

        EventHandlerMachine mockMachine = mock(EventHandlerMachine.class);
        action.apply(mockMachine);

        verify(subjectAction).apply(mockMachine);
        verify(selectionAction).apply(mockMachine);
        verify(mockMachine).slice();
    }

    @Test
    void testVisitSampleSimple() {
        // Mock
        JoshLangParser.SampleSimpleContext context = mock(JoshLangParser.SampleSimpleContext.class);
        context.target = mock(JoshLangParser.ExpressionContext.class);

        Fragment targetFragment = mock(Fragment.class);
        EventHandlerAction targetAction = mock(EventHandlerAction.class);

        when(context.target.accept(parent)).thenReturn(targetFragment);
        when(targetFragment.getCurrentAction()).thenReturn(targetAction);

        // Test
        Fragment result = visitor.visitSampleSimple(context);

        // Validate
        assertNotNull(result);
        assertTrue(result instanceof ActionFragment);

        EventHandlerAction action = result.getCurrentAction();
        assertNotNull(action);

        EventHandlerMachine mockMachine = mock(EventHandlerMachine.class);
        action.apply(mockMachine);

        verify(targetAction).apply(mockMachine);
        verify(mockMachine).push(mockSingleCount); // Verifies push of the singleCount from constructor
        verify(mockMachine).sample(false); // sampleSimple implies without replacement
    }

    @Test
    void testVisitSampleParam() {
        // Mock
        JoshLangParser.SampleParamContext context = mock(JoshLangParser.SampleParamContext.class);
        context.count = mock(JoshLangParser.ExpressionContext.class);
        context.target = mock(JoshLangParser.ExpressionContext.class);

        Fragment countFragment = mock(Fragment.class);
        Fragment targetFragment = mock(Fragment.class);
        EventHandlerAction countAction = mock(EventHandlerAction.class);
        EventHandlerAction targetAction = mock(EventHandlerAction.class);

        when(context.count.accept(parent)).thenReturn(countFragment);
        when(context.target.accept(parent)).thenReturn(targetFragment);
        when(countFragment.getCurrentAction()).thenReturn(countAction);
        when(targetFragment.getCurrentAction()).thenReturn(targetAction);

        // Test
        Fragment result = visitor.visitSampleParam(context);

        // Validate
        assertNotNull(result);
        assertTrue(result instanceof ActionFragment);

        EventHandlerAction action = result.getCurrentAction();
        assertNotNull(action);

        EventHandlerMachine mockMachine = mock(EventHandlerMachine.class);
        action.apply(mockMachine);

        verify(countAction).apply(mockMachine);
        verify(targetAction).apply(mockMachine);
        verify(mockMachine).sample(false); // sampleParam implies without replacement by default
    }

    @Test
    void testVisitSampleParamReplacement_withReplacement() {
        // Mock
        JoshLangParser.SampleParamReplacementContext context = mock(JoshLangParser.SampleParamReplacementContext.class);
        context.count = mock(JoshLangParser.ExpressionContext.class);
        context.target = mock(JoshLangParser.ExpressionContext.class);
        context.replace = mock(Token.class);

        Fragment countFragment = mock(Fragment.class);
        Fragment targetFragment = mock(Fragment.class);
        EventHandlerAction countAction = mock(EventHandlerAction.class);
        EventHandlerAction targetAction = mock(EventHandlerAction.class);

        when(context.count.accept(parent)).thenReturn(countFragment);
        when(context.target.accept(parent)).thenReturn(targetFragment);
        when(countFragment.getCurrentAction()).thenReturn(countAction);
        when(targetFragment.getCurrentAction()).thenReturn(targetAction);
        when(context.replace.getText()).thenReturn("with");


        // Test
        Fragment result = visitor.visitSampleParamReplacement(context);

        // Validate
        assertNotNull(result);
        assertTrue(result instanceof ActionFragment);

        EventHandlerAction action = result.getCurrentAction();
        assertNotNull(action);

        EventHandlerMachine mockMachine = mock(EventHandlerMachine.class);
        action.apply(mockMachine);

        verify(countAction).apply(mockMachine);
        verify(targetAction).apply(mockMachine);
        verify(mockMachine).sample(true); // "with" replacement
    }

    @Test
    void testVisitSampleParamReplacement_withoutReplacement() {
        // Mock
        JoshLangParser.SampleParamReplacementContext context = mock(JoshLangParser.SampleParamReplacementContext.class);
        context.count = mock(JoshLangParser.ExpressionContext.class);
        context.target = mock(JoshLangParser.ExpressionContext.class);
        context.replace = mock(Token.class);

        Fragment countFragment = mock(Fragment.class);
        Fragment targetFragment = mock(Fragment.class);
        EventHandlerAction countAction = mock(EventHandlerAction.class);
        EventHandlerAction targetAction = mock(EventHandlerAction.class);

        when(context.count.accept(parent)).thenReturn(countFragment);
        when(context.target.accept(parent)).thenReturn(targetFragment);
        when(countFragment.getCurrentAction()).thenReturn(countAction);
        when(targetFragment.getCurrentAction()).thenReturn(targetAction);
        when(context.replace.getText()).thenReturn("without");


        // Test
        Fragment result = visitor.visitSampleParamReplacement(context);

        // Validate
        assertNotNull(result);
        assertTrue(result instanceof ActionFragment);

        EventHandlerAction action = result.getCurrentAction();
        assertNotNull(action);

        EventHandlerMachine mockMachine = mock(EventHandlerMachine.class);
        action.apply(mockMachine);

        verify(countAction).apply(mockMachine);
        verify(targetAction).apply(mockMachine);
        verify(mockMachine).sample(false); // "without" replacement
    }


    @Test
    void testVisitUniformSample() {
        // Mock
        JoshLangParser.UniformSampleContext context = mock(JoshLangParser.UniformSampleContext.class);
        context.low = mock(JoshLangParser.ExpressionContext.class);
        context.high = mock(JoshLangParser.ExpressionContext.class);

        Fragment lowFragment = mock(Fragment.class);
        Fragment highFragment = mock(Fragment.class);
        EventHandlerAction lowAction = mock(EventHandlerAction.class);
        EventHandlerAction highAction = mock(EventHandlerAction.class);

        when(context.low.accept(parent)).thenReturn(lowFragment);
        when(context.high.accept(parent)).thenReturn(highFragment);
        when(lowFragment.getCurrentAction()).thenReturn(lowAction);
        when(highFragment.getCurrentAction()).thenReturn(highAction);

        // Test
        Fragment result = visitor.visitUniformSample(context);

        // Validate
        assertNotNull(result);
        assertTrue(result instanceof ActionFragment);

        EventHandlerAction action = result.getCurrentAction();
        assertNotNull(action);

        EventHandlerMachine mockMachine = mock(EventHandlerMachine.class);
        action.apply(mockMachine);

        verify(lowAction).apply(mockMachine);
        verify(highAction).apply(mockMachine);
        verify(mockMachine).randUniform();
    }

    @Test
    void testVisitNormalSample() {
        // Mock
        JoshLangParser.NormalSampleContext context = mock(JoshLangParser.NormalSampleContext.class);
        context.mean = mock(JoshLangParser.ExpressionContext.class);
        context.stdev = mock(JoshLangParser.ExpressionContext.class);

        Fragment meanFragment = mock(Fragment.class);
        Fragment stdevFragment = mock(Fragment.class);
        EventHandlerAction meanAction = mock(EventHandlerAction.class);
        EventHandlerAction stdevAction = mock(EventHandlerAction.class);

        when(context.mean.accept(parent)).thenReturn(meanFragment);
        when(context.stdev.accept(parent)).thenReturn(stdevFragment);
        when(meanFragment.getCurrentAction()).thenReturn(meanAction);
        when(stdevFragment.getCurrentAction()).thenReturn(stdevAction);

        // Test
        Fragment result = visitor.visitNormalSample(context);

        // Validate
        assertNotNull(result);
        assertTrue(result instanceof ActionFragment);

        EventHandlerAction action = result.getCurrentAction();
        assertNotNull(action);

        EventHandlerMachine mockMachine = mock(EventHandlerMachine.class);
        action.apply(mockMachine);

        verify(meanAction).apply(mockMachine);
        verify(stdevAction).apply(mockMachine);
        verify(mockMachine).randNormal();
    }
}
