package org.joshsim.lang.interpret.visitor.delegates;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.engine.value.type.EngineValue;
import org.joshsim.lang.antlr.JoshLangParser.ExpressionContext;
import org.joshsim.lang.antlr.JoshLangParser.NormalSampleContext;
import org.joshsim.lang.antlr.JoshLangParser.SampleParamContext;
import org.joshsim.lang.antlr.JoshLangParser.SampleParamReplacementContext;
import org.joshsim.lang.antlr.JoshLangParser.SampleSimpleContext;
import org.joshsim.lang.antlr.JoshLangParser.SliceContext;
import org.joshsim.lang.antlr.JoshLangParser.UniformSampleContext;
import org.joshsim.lang.interpret.action.EventHandlerAction;
import org.joshsim.lang.interpret.fragment.josh.ActionFragment;
import org.joshsim.lang.interpret.fragment.josh.JoshFragment;
import org.joshsim.lang.interpret.machine.EventHandlerMachine;
import org.joshsim.lang.interpret.visitor.JoshParserToMachineVisitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JoshDistributionVisitorTest {

  private DelegateToolbox toolbox;
  private JoshParserToMachineVisitor parent;
  private EngineValueFactory valueFactory;
  private JoshDistributionVisitor visitor;
  private EngineValue singleCount;

  @BeforeEach
  void setUp() {
    toolbox = mock(DelegateToolbox.class);
    parent = mock(JoshParserToMachineVisitor.class);
    valueFactory = mock(EngineValueFactory.class);
    singleCount = mock(EngineValue.class);

    when(toolbox.getParent()).thenReturn(parent);
    when(toolbox.getValueFactory()).thenReturn(valueFactory);
    when(valueFactory.build(1, Units.of("count"))).thenReturn(singleCount);

    visitor = new JoshDistributionVisitor(toolbox);
  }

  @Test
  void testVisitSlice() {
    // Mock
    SliceContext context = mock(SliceContext.class);
    context.subject = mock(SliceContext.class);
    context.selection = mock(SliceContext.class);

    JoshFragment subjectFragment = mock(JoshFragment.class);
    JoshFragment selectionFragment = mock(JoshFragment.class);
    EventHandlerAction subjectAction = mock(EventHandlerAction.class);
    EventHandlerAction selectionAction = mock(EventHandlerAction.class);

    when(context.subject.accept(parent)).thenReturn(subjectFragment);
    when(context.selection.accept(parent)).thenReturn(selectionFragment);
    when(subjectFragment.getCurrentAction()).thenReturn(subjectAction);
    when(selectionFragment.getCurrentAction()).thenReturn(selectionAction);

    // Test
    JoshFragment result = visitor.visitSlice(context);

    // Validate
    assertNotNull(result);
    assertTrue(result instanceof ActionFragment);

    EventHandlerAction action = result.getCurrentAction();
    assertNotNull(action);

    EventHandlerMachine mockMachine = mock(EventHandlerMachine.class);
    when(mockMachine.slice()).thenReturn(mockMachine);

    action.apply(mockMachine);

    verify(subjectAction).apply(mockMachine);
    verify(selectionAction).apply(mockMachine);
    verify(mockMachine).slice();
  }

  @Test
  void testVisitSampleSimple() {
    // Mock
    SampleSimpleContext context = mock(SampleSimpleContext.class);
    context.target = mock(SampleSimpleContext.class);

    JoshFragment targetFragment = mock(JoshFragment.class);
    EventHandlerAction targetAction = mock(EventHandlerAction.class);

    when(context.target.accept(parent)).thenReturn(targetFragment);
    when(targetFragment.getCurrentAction()).thenReturn(targetAction);

    // Test
    JoshFragment result = visitor.visitSampleSimple(context);

    // Validate
    assertNotNull(result);
    assertTrue(result instanceof ActionFragment);

    EventHandlerAction action = result.getCurrentAction();
    assertNotNull(action);

    EventHandlerMachine mockMachine = mock(EventHandlerMachine.class);

    action.apply(mockMachine);

    verify(targetAction).apply(mockMachine);
    verify(mockMachine).push(singleCount);
    verify(mockMachine).sample(true);
  }

  @Test
  void testVisitSampleParam() {
    // Mock
    SampleParamContext context = mock(SampleParamContext.class);
    context.count = mock(SampleParamContext.class);
    context.target = mock(SampleParamContext.class);

    JoshFragment countFragment = mock(JoshFragment.class);
    JoshFragment targetFragment = mock(JoshFragment.class);
    EventHandlerAction countAction = mock(EventHandlerAction.class);
    EventHandlerAction targetAction = mock(EventHandlerAction.class);

    when(context.count.accept(parent)).thenReturn(countFragment);
    when(context.target.accept(parent)).thenReturn(targetFragment);
    when(countFragment.getCurrentAction()).thenReturn(countAction);
    when(targetFragment.getCurrentAction()).thenReturn(targetAction);

    // Test
    JoshFragment result = visitor.visitSampleParam(context);

    // Validate
    assertNotNull(result);
    assertTrue(result instanceof ActionFragment);

    EventHandlerAction action = result.getCurrentAction();
    assertNotNull(action);

    EventHandlerMachine mockMachine = mock(EventHandlerMachine.class);

    action.apply(mockMachine);

    verify(countAction).apply(mockMachine);
    verify(targetAction).apply(mockMachine);
    verify(mockMachine).sample(true);
  }

  @Test
  void testVisitSampleParamReplacement() {
    // Mock
    SampleParamReplacementContext context = mock(SampleParamReplacementContext.class);
    context.count = mock(SampleParamReplacementContext.class);
    context.target = mock(SampleParamReplacementContext.class);
    context.replace = mock(org.antlr.v4.runtime.Token.class);

    JoshFragment countFragment = mock(JoshFragment.class);
    JoshFragment targetFragment = mock(JoshFragment.class);
    EventHandlerAction countAction = mock(EventHandlerAction.class);
    EventHandlerAction targetAction = mock(EventHandlerAction.class);

    when(context.count.accept(parent)).thenReturn(countFragment);
    when(context.target.accept(parent)).thenReturn(targetFragment);
    when(countFragment.getCurrentAction()).thenReturn(countAction);
    when(targetFragment.getCurrentAction()).thenReturn(targetAction);
    when(context.replace.getText()).thenReturn("with");

    // Test
    JoshFragment result = visitor.visitSampleParamReplacement(context);

    // Validate
    assertNotNull(result);
    assertTrue(result instanceof ActionFragment);

    EventHandlerAction action = result.getCurrentAction();
    assertNotNull(action);

    EventHandlerMachine mockMachine = mock(EventHandlerMachine.class);

    action.apply(mockMachine);

    verify(countAction).apply(mockMachine);
    verify(targetAction).apply(mockMachine);
    verify(mockMachine).sample(true);
  }

  @Test
  void testVisitUniformSample() {
    // Mock
    UniformSampleContext context = mock(UniformSampleContext.class);
    context.low = mock(ExpressionContext.class);
    context.high = mock(ExpressionContext.class);

    JoshFragment lowFragment = mock(JoshFragment.class);
    JoshFragment highFragment = mock(JoshFragment.class);
    EventHandlerAction lowAction = mock(EventHandlerAction.class);
    EventHandlerAction highAction = mock(EventHandlerAction.class);

    when(context.low.accept(parent)).thenReturn(lowFragment);
    when(context.high.accept(parent)).thenReturn(highFragment);
    when(lowFragment.getCurrentAction()).thenReturn(lowAction);
    when(highFragment.getCurrentAction()).thenReturn(highAction);

    // Test
    JoshFragment result = visitor.visitUniformSample(context);

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
    NormalSampleContext context = mock(NormalSampleContext.class);
    context.mean = mock(ExpressionContext.class);
    context.stdev = mock(ExpressionContext.class);

    JoshFragment meanFragment = mock(JoshFragment.class);
    JoshFragment stdevFragment = mock(JoshFragment.class);
    EventHandlerAction meanAction = mock(EventHandlerAction.class);
    EventHandlerAction stdevAction = mock(EventHandlerAction.class);

    when(context.mean.accept(parent)).thenReturn(meanFragment);
    when(context.stdev.accept(parent)).thenReturn(stdevFragment);
    when(meanFragment.getCurrentAction()).thenReturn(meanAction);
    when(stdevFragment.getCurrentAction()).thenReturn(stdevAction);

    // Test
    JoshFragment result = visitor.visitNormalSample(context);

    // Validate
    assertNotNull(result);
    assertTrue(result instanceof ActionFragment);

    EventHandlerAction action = result.getCurrentAction();
    assertNotNull(action);

    EventHandlerMachine mockMachine = mock(EventHandlerMachine.class);

    action.apply(mockMachine);

    verify(meanAction).apply(mockMachine);
    verify(stdevAction).apply(mockMachine);
    verify(mockMachine).randNorm();
  }
}
