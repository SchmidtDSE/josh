
package org.joshsim.lang.interpret.visitor.delegates;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.ValueSupportFactory;
import org.joshsim.engine.value.type.EngineValue;
import org.joshsim.lang.antlr.JoshLangParser.ExpressionContext;
import org.joshsim.lang.antlr.JoshLangParser.ExternalValueAtTimeContext;
import org.joshsim.lang.antlr.JoshLangParser.ExternalValueContext;
import org.joshsim.lang.antlr.JoshLangParser.IdentifierContext;
import org.joshsim.lang.interpret.action.EventHandlerAction;
import org.joshsim.lang.interpret.fragment.josh.ActionFragment;
import org.joshsim.lang.interpret.fragment.josh.JoshFragment;
import org.joshsim.lang.interpret.machine.EventHandlerMachine;
import org.joshsim.lang.interpret.visitor.JoshParserToMachineVisitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JoshExternalVisitorTest {

  private DelegateToolbox toolbox;
  private JoshParserToMachineVisitor parent;
  private ValueSupportFactory valueFactory;
  private JoshExternalVisitor visitor;

  @BeforeEach
  void setUp() {
    toolbox = mock(DelegateToolbox.class);
    parent = mock(JoshParserToMachineVisitor.class);
    valueFactory = mock(ValueSupportFactory.class);

    when(toolbox.getParent()).thenReturn(parent);
    when(toolbox.getValueFactory()).thenReturn(valueFactory);

    visitor = new JoshExternalVisitor(toolbox);
  }

  @Test
  void testVisitExternalValue() {
    // Mock
    ExternalValueContext context = mock(ExternalValueContext.class);
    IdentifierContext nameContext = mock(IdentifierContext.class);

    context.name = nameContext;
    when(nameContext.getText()).thenReturn("externalVar");

    // Test
    JoshFragment result = visitor.visitExternalValue(context);

    // Validate
    assertNotNull(result);
    assertTrue(result instanceof ActionFragment);

    EventHandlerAction action = result.getCurrentAction();
    assertNotNull(action);

    // Unadorned external reads resolve at the current timestep -- no attribute is consulted, so an
    // unrelated model attribute of the same name can never silently redirect it.
    EventHandlerMachine mockMachine = mock(EventHandlerMachine.class);
    EngineValue stepValue = mock(EngineValue.class);
    when(mockMachine.getCurrentTimestep()).thenReturn(42L);
    when(valueFactory.build(42L, Units.of("count"))).thenReturn(stepValue);

    action.apply(mockMachine);

    verify(mockMachine).push(stepValue);
    verify(mockMachine).pushExternalAtStep("externalVar");
  }

  @Test
  void testVisitExternalValueAtTime() {
    // Mock
    ExternalValueAtTimeContext context = mock(ExternalValueAtTimeContext.class);
    IdentifierContext nameContext = mock(IdentifierContext.class);

    context.name = nameContext;
    when(nameContext.getText()).thenReturn("externalVar");

    ExpressionContext stepContext = mock(ExpressionContext.class);
    JoshFragment stepFragment = mock(JoshFragment.class);
    EventHandlerAction stepAction = mock(EventHandlerAction.class);
    context.step = stepContext;
    when(stepContext.accept(parent)).thenReturn(stepFragment);
    when(stepFragment.getCurrentAction()).thenReturn(stepAction);

    // Test
    JoshFragment result = visitor.visitExternalValueAtTime(context);

    // Validate
    assertNotNull(result);
    assertTrue(result instanceof ActionFragment);

    EventHandlerAction action = result.getCurrentAction();
    assertNotNull(action);

    // The step expression (e.g. a literal, `prior`, or a model attribute) is evaluated first,
    // pushing its value; pushExternalAtStep then pops it to resolve the external read.
    EventHandlerMachine mockMachine = mock(EventHandlerMachine.class);
    action.apply(mockMachine);

    verify(stepAction).apply(mockMachine);
    verify(mockMachine).pushExternalAtStep("externalVar");
  }
}
