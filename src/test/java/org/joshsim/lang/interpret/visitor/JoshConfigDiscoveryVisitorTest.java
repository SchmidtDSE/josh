package org.joshsim.lang.interpret.visitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.joshsim.lang.antlr.JoshLangLexer;
import org.joshsim.lang.antlr.JoshLangParser;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for JoshConfigDiscoveryVisitor.
 */
public class JoshConfigDiscoveryVisitorTest {

  @Test
  public void testDiscoverSingleConfigVariable() {
    String script = """
        start simulation Test
          grid.size = config example.gridSize
        end simulation
        """;

    Set<String> discovered = parseAndDiscover(script);
    assertEquals(1, discovered.size());
    assertTrue(discovered.contains("example.gridSize"));
  }

  @Test
  public void testDiscoverMultipleConfigVariables() {
    String script = """
        start simulation Test
          grid.size = config example.gridSize
          steps.high = config params.stepCount
        end simulation
        
        start patch Default
          Tree.init = create config example.treeCount of Tree
        end patch
        """;

    Set<String> discovered = parseAndDiscover(script);
    assertEquals(3, discovered.size());
    assertTrue(discovered.contains("example.gridSize"));
    assertTrue(discovered.contains("params.stepCount"));
    assertTrue(discovered.contains("example.treeCount"));
  }

  @Test
  public void testNoConfigVariables() {
    String script = """
        start simulation Test
          grid.size = 1000 m
          steps.high = 10 count
        end simulation
        """;

    Set<String> discovered = parseAndDiscover(script);
    assertTrue(discovered.isEmpty());
  }

  @Test
  public void testConfigVariableInExpression() {
    String script = """
        start simulation Test
          grid.size = config example.gridSize + 100 m
        end simulation
        """;

    Set<String> discovered = parseAndDiscover(script);
    assertEquals(1, discovered.size());
    assertTrue(discovered.contains("example.gridSize"));
  }

  @Test
  public void testDuplicateConfigVariables() {
    String script = """
        start simulation Test
          grid.size = config example.testVar
          steps.high = config example.testVar
        end simulation
        """;

    Set<String> discovered = parseAndDiscover(script);
    assertEquals(1, discovered.size());
    assertTrue(discovered.contains("example.testVar"));
  }

  @Test
  public void testConfigVariableInOrganismStanza() {
    String script = """
        start simulation Test
          grid.size = 1000 m
          steps.high = 10 count
        end simulation
        
        start organism Tree
          height.init = config tree.initialHeight
          height.step = prior.height + config tree.growthRate
        end organism
        """;

    Set<String> discovered = parseAndDiscover(script);
    assertEquals(2, discovered.size());
    assertTrue(discovered.contains("tree.initialHeight"));
    assertTrue(discovered.contains("tree.growthRate"));
  }

  @Test
  public void testConfigVariableWithComplexIdentifier() {
    String script = """
        start simulation Test
          grid.size = config environment.parameters.gridSize
          steps.high = config simulation.settings.maxSteps
        end simulation
        """;

    Set<String> discovered = parseAndDiscover(script);
    assertEquals(2, discovered.size());
    assertTrue(discovered.contains("environment.parameters.gridSize"));
    assertTrue(discovered.contains("simulation.settings.maxSteps"));
  }

  private Set<String> parseAndDiscover(String script) {
    JoshLangLexer lexer = new JoshLangLexer(CharStreams.fromString(script));
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    JoshLangParser parser = new JoshLangParser(tokens);
    ParseTree tree = parser.program();

    JoshConfigDiscoveryVisitor visitor = new JoshConfigDiscoveryVisitor();
    visitor.visit(tree);
    return visitor.getDiscoveredVariables();
  }
}