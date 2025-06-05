package org.joshsim.lang.interpret.visitor.delegates;

import org.antlr.v4.runtime.tree.ParseTree;
import org.joshsim.engine.doc.EntityType;
import org.joshsim.engine.entity.EntityBuilder;
import org.joshsim.engine.entity.EntityPrototype;
import org.joshsim.engine.entity.handler.EventHandlerGroup;
import org.joshsim.engine.entity.handler.EventHandlerGroupBuilder;
import org.joshsim.engine.entity.handler.EventKey;
import org.joshsim.engine.value.converter.Conversion;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.function.ValueFunction;
import org.joshsim.lang.antlr.JoshLangParser;
import org.joshsim.lang.interpret.ProgramBuilder;
import org.joshsim.lang.interpret.fragment.*;
import org.joshsim.lang.interpret.visitor.JoshParserToMachineVisitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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
        JoshLangParser.StateStanzaContext context = mock(JoshLangParser.StateStanzaContext.class);
        ParseTree stateNameNode = mock(ParseTree.class);
        ParseTree eventHandlerGroupNode = mock(ParseTree.class);
        Fragment groupFragment = mock(Fragment.class);
        EventHandlerGroupBuilder mockGroupBuilder = mock(EventHandlerGroupBuilder.class);
        String stateName = "myState";

        // "state" stateName eventHandlerGroup* "end"
        // Child 0: "state" token
        // Child 1: stateName (IDENTIFIER token) - but grammar shows it as part of stateName: name=IDENTIFIER
        // Let's assume grammar is `stateStanza: K_STATE name=IDENTIFIER eventHandlerGroup* K_END;`
        // Then `ctx.name.getText()` would be stateName.
        // If it is `stateStanza: K_STATE IDENTIFIER eventHandlerGroup* K_END;`
        // Then `ctx.getChild(1).getText()`
        // The provided code uses `ctx.getChild(2).getText()`, which implies a structure like
        // `K_STATE SOMETHING IDENTIFIER ...` - let's stick to the prompt's structure.
        // The original code for JoshStanzaVisitor is: `String stateName = ctx.getChild(2).getText();`
        // And loop from `i = 3` to `ctx.getChildCount() - 2`.
        // This means: child 0=K_STATE, child 1=???, child 2=stateName, child 3...n-2 = groups, child n-1=K_END

        when(context.getChild(2)).thenReturn(stateNameNode);
        when(stateNameNode.getText()).thenReturn(stateName);
        when(context.getChildCount()).thenReturn(5); // state, ?, stateName, group, end
        when(context.getChild(3)).thenReturn(eventHandlerGroupNode);
        when(eventHandlerGroupNode.accept(parent)).thenReturn(groupFragment);
        when(groupFragment.getEventHandlerGroup()).thenReturn(mockGroupBuilder);

        Fragment result = visitor.visitStateStanza(context);

        assertNotNull(result);
        assertTrue(result instanceof StateFragment);
        StateFragment stateFragment = (StateFragment) result;

        assertEquals(1, stateFragment.getEventHandlerGroupBuilders().size());
        assertSame(mockGroupBuilder, stateFragment.getEventHandlerGroupBuilders().get(0));
        verify(mockGroupBuilder).setState(stateName);
    }

    @Test
    void testVisitEntityStanza_SuccessfulPath() {
        JoshLangParser.EntityStanzaContext context = mock(JoshLangParser.EntityStanzaContext.class);
        ParseTree entityTypeNode = mock(ParseTree.class); // e.g., "agent"
        ParseTree entityIdentifierNode = mock(ParseTree.class); // e.g., "myAgent"
        ParseTree entityBodyNode = mock(ParseTree.class); // Contains event handler groups
        ParseTree closeEntityTypeNode = mock(ParseTree.class); // e.g., "agent"

        String entityTypeStr = "agent";
        String entityIdentifierStr = "myAgent";

        // Structure: K_ENTITY entityType IDENTIFIER entityBody K_END closeEntityType
        // Child 0: K_ENTITY
        // Child 1: entityType (e.g. "agent")
        // Child 2: IDENTIFIER (name)
        // Child 3: entityBody (contains list of event handler groups)
        // Child 4: K_END
        // Child 5: closeEntityType (e.g. "agent")
        when(context.getChild(1)).thenReturn(entityTypeNode);
        when(entityTypeNode.getText()).thenReturn(entityTypeStr);
        when(context.getChild(2)).thenReturn(entityIdentifierNode);
        when(entityIdentifierNode.getText()).thenReturn(entityIdentifierStr);
        when(context.getChild(3)).thenReturn(entityBodyNode); // This is the one that returns groups
        when(context.getChild(5)).thenReturn(closeEntityTypeNode);
        when(closeEntityTypeNode.getText()).thenReturn(entityTypeStr); // Matching type for success
        when(context.getChildCount()).thenReturn(6);


        Fragment bodyFragment = mock(Fragment.class); // Fragment from visiting entityBodyNode
        EventHandlerGroupBuilder mockGroupBuilder = mock(EventHandlerGroupBuilder.class);
        EventKey mockEventKey = mock(EventKey.class);
        EventHandlerGroup mockGroup = mock(EventHandlerGroup.class);

        when(entityBodyNode.accept(parent)).thenReturn(bodyFragment);
        when(bodyFragment.getEventHandlerGroups()).thenReturn(Collections.singletonList(mockGroupBuilder));
        when(mockGroupBuilder.buildKey(anyString(), any(EntityType.class))).thenReturn(mockEventKey);
        when(mockGroupBuilder.build(any(EventKey.class))).thenReturn(mockGroup);

        EntityBuilder mockEntityBuilder = mock(EntityBuilder.class);
        when(mockEntityBuilder.build(anyString(), any(EntityType.class))).thenReturn(mock(EntityPrototype.class));


        try (MockedConstruction<EntityBuilder> mockedEntityBuilderConstruction =
                     mockConstruction(EntityBuilder.class, (mock, constructionContext) -> {
                         when(mock.build(anyString(), any(EntityType.class))).thenReturn(mock(EntityPrototype.class));
                         // Stub addEventHandlerGroup if needed, or verify on the captured mock
                     })) {

            Fragment result = visitor.visitEntityStanza(context);

            assertNotNull(result);
            assertTrue(result instanceof EntityFragment);
            EntityFragment entityFragment = (EntityFragment) result;
            EntityPrototype prototype = entityFragment.getEntityPrototype();
            assertNotNull(prototype);

            // Verify interactions on the constructed EntityBuilder
            assertEquals(1, mockedEntityBuilderConstruction.constructed().size());
            EntityBuilder instantiatedEntityBuilder = mockedEntityBuilderConstruction.constructed().get(0);

            verify(instantiatedEntityBuilder).build(entityIdentifierStr, EntityType.AGENT);
            verify(instantiatedEntityBuilder).addEventHandlerGroup(mockGroup);
            verify(mockGroupBuilder).buildKey(entityIdentifierStr, EntityType.AGENT);
            verify(mockGroupBuilder).build(mockEventKey);

        }
    }

    @Test
    void testVisitEntityStanza_ThrowsOnMismatch() {
        JoshLangParser.EntityStanzaContext context = mock(JoshLangParser.EntityStanzaContext.class);
        ParseTree entityTypeNode = mock(ParseTree.class);
        ParseTree entityIdentifierNode = mock(ParseTree.class); // Needed for getEntityType
        ParseTree closeEntityTypeNode = mock(ParseTree.class);

        when(context.getChild(1)).thenReturn(entityTypeNode);
        when(entityTypeNode.getText()).thenReturn("agent");
        when(context.getChild(2)).thenReturn(entityIdentifierNode); // For getEntityType
        when(entityIdentifierNode.getText()).thenReturn("someAgent");
        when(context.getChild(5)).thenReturn(closeEntityTypeNode); // Assuming 6 children for mismatch test too
        when(closeEntityTypeNode.getText()).thenReturn("behavior"); // Mismatch
        when(context.getChildCount()).thenReturn(6);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            visitor.visitEntityStanza(context);
        });
        assertTrue(exception.getMessage().contains("Entity close type 'behavior' does not match open type 'agent'"));
    }


    @Test
    void testVisitUnitStanza() {
        JoshLangParser.UnitStanzaContext context = mock(JoshLangParser.UnitStanzaContext.class);
        ParseTree sourceUnitNameNode = mock(ParseTree.class);
        ParseTree conversionRuleNode = mock(ParseTree.class); // Represents a conversion rule line
        Fragment ruleFragment = mock(Fragment.class); // Fragment from visiting conversionRuleNode
        Conversion incompleteConversion = mock(Conversion.class); // Incomplete conv from ruleFragment

        String sourceUnitName = "meter";
        Units destinationUnits = Units.of("kilometer");
        ValueFunction conversionCallable = mock(ValueFunction.class);

        // K_UNITS IDENTIFIER conversionRule* K_END
        // Child 0: K_UNITS
        // Child 1: IDENTIFIER (source unit name)
        // Child 2 to n-2: conversionRule
        // Child n-1: K_END
        // Original code: String sourceUnitName = ctx.getChild(2).getText(); loop i=3 to count-2
        // This implies structure: K_UNITS ? IDENTIFIER ...
        // Let's follow the prompt: ctx.getChild(2).getText() for sourceUnitName
        when(context.getChild(2)).thenReturn(sourceUnitNameNode);
        when(sourceUnitNameNode.getText()).thenReturn(sourceUnitName);
        when(context.getChildCount()).thenReturn(5); // units, ?, meter, rule, end
        when(context.getChild(3)).thenReturn(conversionRuleNode);

        when(conversionRuleNode.accept(parent)).thenReturn(ruleFragment);
        when(ruleFragment.getConversion()).thenReturn(incompleteConversion);
        when(incompleteConversion.getDestinationUnits()).thenReturn(destinationUnits);
        when(incompleteConversion.getConversionCallable()).thenReturn(conversionCallable);

        Fragment result = visitor.visitUnitStanza(context);

        assertNotNull(result);
        assertTrue(result instanceof ConversionsFragment);
        ConversionsFragment conversionsFragment = (ConversionsFragment) result;
        List<Conversion> conversions = conversionsFragment.getConversions();
        assertEquals(1, conversions.size());
        Conversion actualConversion = conversions.get(0);

        assertEquals(Units.of(sourceUnitName), actualConversion.getSourceUnits());
        assertEquals(destinationUnits, actualConversion.getDestinationUnits());
        assertSame(conversionCallable, actualConversion.getConversionCallable());
    }


    @Test
    void testVisitConfigStatement_ThrowsException() {
        JoshLangParser.ConfigStatementContext context = mock(JoshLangParser.ConfigStatementContext.class);
        assertThrows(RuntimeException.class, () -> {
            visitor.visitConfigStatement(context);
        });
    }

    @Test
    void testVisitImportStatement_ThrowsException() {
        JoshLangParser.ImportStatementContext context = mock(JoshLangParser.ImportStatementContext.class);
        assertThrows(RuntimeException.class, () -> {
            visitor.visitImportStatement(context);
        });
    }

    @Test
    void testVisitProgram() {
        JoshLangParser.ProgramContext context = mock(JoshLangParser.ProgramContext.class);
        ParseTree childStatementNode = mock(ParseTree.class);
        Fragment childFragment = mock(Fragment.class); // Fragment from visiting childStatementNode

        when(context.getChildCount()).thenReturn(1); // One statement/stanza in the program
        when(context.getChild(0)).thenReturn(childStatementNode);
        when(childStatementNode.accept(parent)).thenReturn(childFragment);

        ProgramBuilder finalProgramBuilderMock = mock(ProgramBuilder.class); // Final builder to be in fragment


        try (MockedConstruction<ProgramBuilder> mockedBuilderConstruction =
                     mockConstruction(ProgramBuilder.class, (mock, constructionContext) -> {
                         // 'mock' is the ProgramBuilder instance created by 'new ProgramBuilder()'
                         // No specific stubbing needed on 'mock' for this test,
                         // as we'll verify add() on it and then check the ProgramFragment contains it.
                     })) {

            Fragment result = visitor.visitProgram(context);

            assertNotNull(result);
            assertTrue(result instanceof ProgramFragment);
            ProgramFragment programFragment = (ProgramFragment) result;

            assertEquals(1, mockedBuilderConstruction.constructed().size());
            ProgramBuilder instantiatedBuilder = mockedBuilderConstruction.constructed().get(0);

            assertSame(instantiatedBuilder, programFragment.getProgramBuilder());
            verify(instantiatedBuilder).add(childFragment);

        }
    }
}
