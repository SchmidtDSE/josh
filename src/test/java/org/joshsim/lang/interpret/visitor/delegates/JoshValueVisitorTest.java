package org.joshsim.lang.interpret.visitor.delegates;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.engine.value.type.EngineValue;
import org.joshsim.lang.antlr.JoshLangParser.AllExpressionContext;
import org.joshsim.lang.antlr.JoshLangParser.BoolContext;

import org.joshsim.lang.antlr.JoshLangParser.IdentifierContext;
import org.joshsim.lang.antlr.JoshLangParser.NumberContext;
import org.joshsim.lang.antlr.JoshLangParser.StringContext;
import org.joshsim.lang.antlr.JoshLangParser.UnitsValueContext;
import org.joshsim.lang.interpret.ValueResolver;
import org.joshsim.lang.interpret.action.EventHandlerAction;
import org.joshsim.lang.interpret.fragment.ActionFragment;
import org.joshsim.lang.interpret.fragment.Fragment;
import org.joshsim.lang.interpret.machine.EventHandlerMachine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JoshValueVisitorTest {

  private DelegateToolbox toolbox;
  private EngineValueFactory valueFactory;
  private JoshValueVisitor visitor;
  private EngineValue mockValue;
  private EngineValue allString;

  @BeforeEach
  void setUp() {
    toolbox = mock(DelegateToolbox.class);
    valueFactory = mock(EngineValueFactory.class);
    mockValue = mock(EngineValue.class);
    allString = mock(EngineValue.class);

    when(toolbox.getValueFactory()).thenReturn(valueFactory);
    when(valueFactory.build("all", Units.of(""))).thenReturn(allString);

    visitor = new JoshValueVisitor(toolbox);
  }

  @Test
  void testVisitIdentifier() {
    // Mock
    IdentifierContext context = mock(IdentifierContext.class);
    when(context.getText()).thenReturn("testVar");

    // Test
    Fragment result = visitor.visitIdentifier(context);

    // Validate
    assertNotNull(result);
    assertTrue(result instanceof ActionFragment);

    EventHandlerAction action = result.getCurrentAction();
    assertNotNull(action);

    EventHandlerMachine mockMachine = mock(EventHandlerMachine.class);

    action.apply(mockMachine);

    // Verify that push was called with a ValueResolver
    verify(mockMachine).push(org.mockito.ArgumentMatchers.any(ValueResolver.class));
  }

  @Test
  void testVisitNumber() {
    // Mock
    NumberContext context = mock(NumberContext.class);
    ParseTree child = mock(ParseTree.class);
    when(context.getChild(0)).thenReturn(child);
    when(child.getText()).thenReturn("42");
    when(valueFactory.parseNumber("42", Units.of("count"))).thenReturn(mockValue);

    // Test
    Fragment result = visitor.visitNumber(context);

    // Validate
    assertNotNull(result);
    assertTrue(result instanceof ActionFragment);

    EventHandlerAction action = result.getCurrentAction();
    assertNotNull(action);

    EventHandlerMachine mockMachine = mock(EventHandlerMachine.class);

    action.apply(mockMachine);

    verify(mockMachine).push(mockValue);
  }

  @Test
  void testVisitUnitsValue() {
    // Mock
    UnitsValueContext context = mock(UnitsValueContext.class);
    ParseTree numberChild = mock(ParseTree.class);
    ParseTree unitsChild = mock(ParseTree.class);
    when(context.getChild(0)).thenReturn(numberChild);
    when(context.getChild(1)).thenReturn(unitsChild);
    when(numberChild.getText()).thenReturn("5");
    when(unitsChild.getText()).thenReturn("meters");
    when(valueFactory.build(5L, Units.of("meters"))).thenReturn(mockValue);

    // Test
    Fragment result = visitor.visitUnitsValue(context);

    // Validate
    assertNotNull(result);
    assertTrue(result instanceof ActionFragment);

    EventHandlerAction action = result.getCurrentAction();
    assertNotNull(action);

    EventHandlerMachine mockMachine = mock(EventHandlerMachine.class);

    action.apply(mockMachine);

    verify(mockMachine).push(mockValue);
  }

  @Test
  void testVisitString() {
    // Mock
    StringContext context = mock(StringContext.class);
    when(context.getText()).thenReturn("\"test string\"");
    when(valueFactory.build("\"test string\"", Units.of(""))).thenReturn(mockValue);

    // Test
    Fragment result = visitor.visitString(context);

    // Validate
    assertNotNull(result);
    assertTrue(result instanceof ActionFragment);

    EventHandlerAction action = result.getCurrentAction();
    assertNotNull(action);

    EventHandlerMachine mockMachine = mock(EventHandlerMachine.class);

    action.apply(mockMachine);

    verify(mockMachine).push(mockValue);
  }

  @Test
  void testVisitBool() {
    // Mock
    BoolContext context = mock(BoolContext.class);
    ParseTree child = mock(ParseTree.class);
    when(context.getChild(0)).thenReturn(child);
    when(child.getText()).thenReturn("true");
    when(valueFactory.build(true, Units.of(""))).thenReturn(mockValue);

    // Test
    Fragment result = visitor.visitBool(context);

    // Validate
    assertNotNull(result);
    assertTrue(result instanceof ActionFragment);

    EventHandlerAction action = result.getCurrentAction();
    assertNotNull(action);

    EventHandlerMachine mockMachine = mock(EventHandlerMachine.class);

    action.apply(mockMachine);

    verify(mockMachine).push(mockValue);
  }

  @Test
  void testVisitAllExpression() {
    // Mock
    AllExpressionContext context = mock(AllExpressionContext.class);

    // Test
    Fragment result = visitor.visitAllExpression(context);

    // Validate
    assertNotNull(result);
    assertTrue(result instanceof ActionFragment);

    EventHandlerAction action = result.getCurrentAction();
    assertNotNull(action);

    EventHandlerMachine mockMachine = mock(EventHandlerMachine.class);

    action.apply(mockMachine);

    verify(mockMachine).push(allString);
  }

  
}
