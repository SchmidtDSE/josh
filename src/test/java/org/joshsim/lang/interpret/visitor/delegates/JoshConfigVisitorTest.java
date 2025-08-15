package org.joshsim.lang.interpret.visitor.delegates;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.joshsim.lang.antlr.JoshLangParser.ConfigValueContext;
import org.joshsim.lang.antlr.JoshLangParser.ConfigValueWithDefaultContext;
import org.joshsim.lang.antlr.JoshLangParser.ExpressionContext;
import org.joshsim.lang.antlr.JoshLangParser.IdentifierContext;
import org.joshsim.lang.interpret.action.EventHandlerAction;
import org.joshsim.lang.interpret.fragment.josh.ActionFragment;
import org.joshsim.lang.interpret.fragment.josh.JoshFragment;
import org.joshsim.lang.interpret.machine.EventHandlerMachine;
import org.joshsim.lang.interpret.visitor.JoshParserToMachineVisitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JoshConfigVisitorTest {

  private DelegateToolbox toolbox;
  private JoshConfigVisitor visitor;

  @BeforeEach
  void setUp() {
    toolbox = mock(DelegateToolbox.class);
    visitor = new JoshConfigVisitor(toolbox);
  }

  @Test
  void testVisitConfigValue() {
    // Mock
    ConfigValueContext context = mock(ConfigValueContext.class);
    IdentifierContext nameContext = mock(IdentifierContext.class);

    // Mock the identifier() method to return nameContext
    context.name = nameContext;
    when(nameContext.getText()).thenReturn("config.testVar");

    // Test
    JoshFragment result = visitor.visitConfigValue(context);

    // Validate
    assertNotNull(result);
    assertTrue(result instanceof ActionFragment);

    EventHandlerAction action = result.getCurrentAction();
    assertNotNull(action);

    EventHandlerMachine mockMachine = mock(EventHandlerMachine.class);
    org.mockito.Mockito.doNothing().when(mockMachine).pushConfig("config.testVar");

    action.apply(mockMachine);

    verify(mockMachine).pushConfig("config.testVar");
  }

  @Test
  void testVisitConfigValueWithDefault() {
    // Mock
    ConfigValueWithDefaultContext context = mock(ConfigValueWithDefaultContext.class);
    IdentifierContext nameContext = mock(IdentifierContext.class);
    ExpressionContext defaultContext = mock(ExpressionContext.class);

    // Mock the context
    context.name = nameContext;
    context.defaultValue = defaultContext;
    when(nameContext.getText()).thenReturn("config.testVar");

    final JoshParserToMachineVisitor parentVisitor = mock(JoshParserToMachineVisitor.class);
    when(toolbox.getParent()).thenReturn(parentVisitor);

    final JoshFragment defaultFragment = mock(JoshFragment.class);
    when(parentVisitor.visit(defaultContext)).thenReturn(defaultFragment);

    final EventHandlerAction defaultAction = mock(EventHandlerAction.class);
    when(defaultFragment.getCurrentAction()).thenReturn(defaultAction);

    // Test
    JoshFragment result = visitor.visitConfigValueWithDefault(context);

    // Validate
    assertNotNull(result);
    assertTrue(result instanceof ActionFragment);

    EventHandlerAction action = result.getCurrentAction();
    assertNotNull(action);

    EventHandlerMachine mockMachine = mock(EventHandlerMachine.class);
    org.mockito.Mockito.doNothing().when(mockMachine).pushConfigWithDefault("config.testVar");

    action.apply(mockMachine);

    verify(defaultAction).apply(mockMachine);
    verify(mockMachine).pushConfigWithDefault("config.testVar");
  }
}
