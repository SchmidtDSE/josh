
package org.joshsim.lang.interpret.visitor.delegates;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.antlr.v4.runtime.Token;
import org.joshsim.lang.antlr.JoshLangParser.ExternalValueAtTimeContext;
import org.joshsim.lang.antlr.JoshLangParser.ExternalValueContext;
import org.joshsim.lang.antlr.JoshLangParser.IdentifierContext;
import org.joshsim.lang.interpret.action.EventHandlerAction;
import org.joshsim.lang.interpret.fragment.josh.ActionFragment;
import org.joshsim.lang.interpret.fragment.josh.JoshFragment;
import org.joshsim.lang.interpret.machine.EventHandlerMachine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JoshExternalVisitorTest {

  private DelegateToolbox toolbox;
  private JoshExternalVisitor visitor;

  @BeforeEach
  void setUp() {
    toolbox = mock(DelegateToolbox.class);
    visitor = new JoshExternalVisitor(toolbox);
  }

  @Test
  void testVisitExternalValue() {
    // Mock
    ExternalValueContext context = mock(ExternalValueContext.class);
    IdentifierContext nameContext = mock(IdentifierContext.class);

    // Mock the identifier() method to return nameContext
    context.name = nameContext;
    when(nameContext.getText()).thenReturn("externalVar");

    // Test
    JoshFragment result = visitor.visitExternalValue(context);

    // Validate
    assertNotNull(result);
    assertTrue(result instanceof ActionFragment);

    EventHandlerAction action = result.getCurrentAction();
    assertNotNull(action);

    EventHandlerMachine mockMachine = mock(EventHandlerMachine.class);
    when(mockMachine.getStepCount()).thenReturn(42L);
    org.mockito.Mockito.doNothing().when(mockMachine).pushExternal("externalVar", 42L);

    action.apply(mockMachine);

    verify(mockMachine).getStepCount();
    verify(mockMachine).pushExternal("externalVar", 42L);
  }

  @Test
  void testVisitExternalValueAtTime() {
    // Mock
    ExternalValueAtTimeContext context = mock(ExternalValueAtTimeContext.class);
    IdentifierContext nameContext = mock(IdentifierContext.class);
    Token stepNode = mock(Token.class);

    context.name = nameContext;
    when(nameContext.getText()).thenReturn("externalVar");
    context.step = stepNode;
    when(stepNode.getText()).thenReturn("123");

    // Test
    JoshFragment result = visitor.visitExternalValueAtTime(context);

    // Validate
    assertNotNull(result);
    assertTrue(result instanceof ActionFragment);

    EventHandlerAction action = result.getCurrentAction();
    assertNotNull(action);

    EventHandlerMachine mockMachine = mock(EventHandlerMachine.class);
    org.mockito.Mockito.doNothing().when(mockMachine).pushExternal("externalVar", 123L);

    action.apply(mockMachine);

    verify(mockMachine).pushExternal("externalVar", 123L);
  }
}
