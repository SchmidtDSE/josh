package org.joshsim.lang.interpret.visitor.delegates;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import org.joshsim.engine.entity.base.EntityBuilder;
import org.joshsim.engine.entity.handler.EventHandlerGroupBuilder;
import org.joshsim.engine.entity.prototype.EntityPrototype;
import org.joshsim.engine.entity.type.EntityType;
import org.joshsim.engine.value.converter.Conversion;
import org.joshsim.engine.value.converter.DirectConversion;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.type.EngineValue;
import org.joshsim.lang.antlr.JoshLangParser.ConfigStatementContext;
import org.joshsim.lang.antlr.JoshLangParser.EntityStanzaContext;
import org.joshsim.lang.antlr.JoshLangParser.ImportStatementContext;
import org.joshsim.lang.antlr.JoshLangParser.ProgramContext;
import org.joshsim.lang.antlr.JoshLangParser.StateStanzaContext;
import org.joshsim.lang.antlr.JoshLangParser.UnitStanzaContext;
import org.joshsim.lang.interpret.fragment.ConversionsFragment;
import org.joshsim.lang.interpret.fragment.EntityFragment;
import org.joshsim.lang.interpret.fragment.Fragment;
import org.joshsim.lang.interpret.fragment.ProgramFragment;
import org.joshsim.lang.interpret.fragment.StateFragment;
import org.joshsim.lang.interpret.visitor.JoshParserToMachineVisitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.antlr.v4.runtime.tree.ParseTree;

class JoshStanzaVisitorTest {

  private DelegateToolbox toolbox;
  private JoshParserToMachineVisitor parent;
  private JoshStanzaVisitor visitor;

  @BeforeEach
  void setUp() {
    toolbox = mock(DelegateToolbox.class);
    parent = mock(JoshParserToMachineVisitor.class);

    when(toolbox.getParent()).thenReturn(parent);

    visitor = new JoshStanzaVisitor(toolbox);
  }

  @Test
  void testVisitStateStanza() {
    // Mock
    StateStanzaContext context = mock(StateStanzaContext.class);
    ParseTree stateNameNode = mock(ParseTree.class);
    ParseTree handlerGroupNode = mock(ParseTree.class);
    Fragment handlerGroupFragment = mock(Fragment.class);
    EventHandlerGroupBuilder groupBuilder = mock(EventHandlerGroupBuilder.class);

    when(context.getChildCount()).thenReturn(6); // state + name + 1 handler group + 3 other nodes
    when(context.getChild(2)).thenReturn(stateNameNode);
    when(context.getChild(3)).thenReturn(handlerGroupNode);
    when(stateNameNode.getText()).thenReturn("testState");
    when(handlerGroupNode.accept(parent)).thenReturn(handlerGroupFragment);
    when(handlerGroupFragment.getEventHandlerGroup()).thenReturn(groupBuilder);

    // Test
    Fragment result = visitor.visitStateStanza(context);

    // Validate
    assertNotNull(result);
    assertTrue(result instanceof StateFragment);

    verify(groupBuilder).setState("testState");
  }

  @Test
  void testVisitEntityStanza() {
    // Mock
    EntityStanzaContext context = mock(EntityStanzaContext.class);
    ParseTree entityTypeNode = mock(ParseTree.class);
    ParseTree identifierNode = mock(ParseTree.class);
    ParseTree closeEntityTypeNode = mock(ParseTree.class);
    ParseTree childNode = mock(ParseTree.class);
    Fragment childFragment = mock(Fragment.class);
    EventHandlerGroupBuilder groupBuilder = mock(EventHandlerGroupBuilder.class);
    List<EventHandlerGroupBuilder> groupBuilders = new ArrayList<>();
    groupBuilders.add(groupBuilder);

    when(context.getChildCount()).thenReturn(6); // entity + name + 1 inner + 3 other nodes
    when(context.getChild(1)).thenReturn(entityTypeNode);
    when(context.getChild(2)).thenReturn(identifierNode);
    when(context.getChild(3)).thenReturn(childNode);
    when(context.getChild(5)).thenReturn(closeEntityTypeNode);
    when(entityTypeNode.getText()).thenReturn("agent");
    when(identifierNode.getText()).thenReturn("testAgent");
    when(closeEntityTypeNode.getText()).thenReturn("agent");
    when(childNode.accept(parent)).thenReturn(childFragment);
    when(childFragment.getEventHandlerGroups()).thenReturn(groupBuilders);

    // Test
    Fragment result = visitor.visitEntityStanza(context);

    // Validate
    assertNotNull(result);
    assertTrue(result instanceof EntityFragment);
  }

  @Test
  void testVisitUnitStanza() {
    // Mock
    UnitStanzaContext context = mock(UnitStanzaContext.class);
    ParseTree unitNameNode = mock(ParseTree.class);
    ParseTree conversionNode = mock(ParseTree.class);
    Fragment conversionFragment = mock(Fragment.class);
    Conversion incompleteConversion = mock(Conversion.class);
    Units destinationUnits = Units.of("destUnit");

    when(context.getChildCount()).thenReturn(6); // unit + name + 1 conversion + 3 other nodes
    when(context.getChild(2)).thenReturn(unitNameNode);
    when(context.getChild(3)).thenReturn(conversionNode);
    when(unitNameNode.getText()).thenReturn("sourceUnit");
    when(conversionNode.accept(parent)).thenReturn(conversionFragment);
    when(conversionFragment.getConversion()).thenReturn(incompleteConversion);
    when(incompleteConversion.getDestinationUnits()).thenReturn(destinationUnits);
    when(incompleteConversion.getConversionCallable()).thenReturn(value -> mock(EngineValue.class));

    // Test
    Fragment result = visitor.visitUnitStanza(context);

    // Validate
    assertNotNull(result);
    assertTrue(result instanceof ConversionsFragment);

    Iterable<Conversion> conversions = result.getConversions();
    assertNotNull(conversions);
  }

  @Test
  void testVisitConfigStatement() {
    // Mock
    ConfigStatementContext context = mock(ConfigStatementContext.class);

    // Test and Validate
    assertThrows(RuntimeException.class, () -> visitor.visitConfigStatement(context));
  }

  @Test
  void testVisitImportStatement() {
    // Mock
    ImportStatementContext context = mock(ImportStatementContext.class);

    // Test and Validate
    assertThrows(RuntimeException.class, () -> visitor.visitImportStatement(context));
  }

  @Test
  void testVisitProgram() {
    // Mock
    ProgramContext context = mock(ProgramContext.class);
    ParseTree childNode = mock(ParseTree.class);
    Fragment childFragment = mock(Fragment.class);

    when(context.getChildCount()).thenReturn(1);
    when(context.getChild(0)).thenReturn(childNode);
    when(childNode.accept(parent)).thenReturn(childFragment);

    // Test
    Fragment result = visitor.visitProgram(context);

    // Validate
    assertNotNull(result);
    assertTrue(result instanceof ProgramFragment);
  }
}
