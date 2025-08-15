package org.joshsim.lang.interpret.visitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.Set;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.joshsim.engine.config.DiscoveredConfigVar;
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

    Set<DiscoveredConfigVar> discovered = parseAndDiscover(script);
    assertEquals(1, discovered.size());
    assertTrue(containsVar(discovered, "example.gridSize", Optional.empty()));
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

    Set<DiscoveredConfigVar> discovered = parseAndDiscover(script);
    assertEquals(3, discovered.size());
    assertTrue(containsVar(discovered, "example.gridSize", Optional.empty()));
    assertTrue(containsVar(discovered, "params.stepCount", Optional.empty()));
    assertTrue(containsVar(discovered, "example.treeCount", Optional.empty()));
  }

  @Test
  public void testNoConfigVariables() {
    String script = """
        start simulation Test
          grid.size = 1000 m
          steps.high = 10 count
        end simulation
        """;

    Set<DiscoveredConfigVar> discovered = parseAndDiscover(script);
    assertTrue(discovered.isEmpty());
  }

  @Test
  public void testConfigVariableInExpression() {
    String script = """
        start simulation Test
          grid.size = config example.gridSize + 100 m
        end simulation
        """;

    Set<DiscoveredConfigVar> discovered = parseAndDiscover(script);
    assertEquals(1, discovered.size());
    assertTrue(containsVar(discovered, "example.gridSize", Optional.empty()));
  }

  @Test
  public void testDuplicateConfigVariables() {
    String script = """
        start simulation Test
          grid.size = config example.testVar
          steps.high = config example.testVar
        end simulation
        """;

    Set<DiscoveredConfigVar> discovered = parseAndDiscover(script);
    assertEquals(1, discovered.size());
    assertTrue(containsVar(discovered, "example.testVar", Optional.empty()));
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

    Set<DiscoveredConfigVar> discovered = parseAndDiscover(script);
    assertEquals(2, discovered.size());
    assertTrue(containsVar(discovered, "tree.initialHeight", Optional.empty()));
    assertTrue(containsVar(discovered, "tree.growthRate", Optional.empty()));
  }

  @Test
  public void testConfigVariableWithComplexIdentifier() {
    String script = """
        start simulation Test
          grid.size = config environment.parameters.gridSize
          steps.high = config simulation.settings.maxSteps
        end simulation
        """;

    Set<DiscoveredConfigVar> discovered = parseAndDiscover(script);
    assertEquals(2, discovered.size());
    assertTrue(containsVar(discovered, "environment.parameters.gridSize", Optional.empty()));
    assertTrue(containsVar(discovered, "simulation.settings.maxSteps", Optional.empty()));
  }

  // NEW TESTS FOR CONFIG WITH DEFAULTS

  @Test
  public void testConfigVariableWithSimpleDefault() {
    String script = """
        start simulation Test
          grid.size = config example.gridSize else 1000 m
        end simulation
        """;

    Set<DiscoveredConfigVar> discovered = parseAndDiscover(script);
    assertEquals(1, discovered.size());
    assertTrue(containsVar(discovered, "example.gridSize", Optional.of("1000 m")));
  }

  @Test
  public void testConfigVariableWithNumberDefault() {
    String script = """
        start simulation Test
          steps.high = config params.stepCount else 10
        end simulation
        """;

    Set<DiscoveredConfigVar> discovered = parseAndDiscover(script);
    assertEquals(1, discovered.size());
    assertTrue(containsVar(discovered, "params.stepCount", Optional.of("10")));
  }

  @Test
  public void testConfigVariablesWithMixedDefaults() {
    String script = """
        start simulation Test
          grid.size = config example.gridSize else 1000 m
          steps.high = config params.stepCount
          organism.count = config example.treeCount else 50 count
        end simulation
        """;

    Set<DiscoveredConfigVar> discovered = parseAndDiscover(script);
    assertEquals(3, discovered.size());
    assertTrue(containsVar(discovered, "example.gridSize", Optional.of("1000 m")));
    assertTrue(containsVar(discovered, "params.stepCount", Optional.empty()));
    assertTrue(containsVar(discovered, "example.treeCount", Optional.of("50 count")));
  }

  @Test
  public void testConfigVariableWithComplexDefault() {
    String script = """
        start simulation Test
          value = config test.complexVar else 10.5 meters
        end simulation
        """;

    Set<DiscoveredConfigVar> discovered = parseAndDiscover(script);
    assertEquals(1, discovered.size());
    assertTrue(containsVar(discovered, "test.complexVar", Optional.of("10.5 meters")));
  }

  @Test
  public void testConfigVariableWithPercentDefault() {
    String script = """
        start simulation Test
          rate = config params.growthRate else 5%
        end simulation
        """;

    Set<DiscoveredConfigVar> discovered = parseAndDiscover(script);
    assertEquals(1, discovered.size());
    assertTrue(containsVar(discovered, "params.growthRate", Optional.of("5%")));
  }

  @Test
  public void testDuplicateConfigVariablesWithAndWithoutDefaults() {
    String script = """
        start simulation Test
          grid.size = config example.testVar else 500 m
          steps.high = config example.testVar
        end simulation
        """;

    // Should contain both versions since they have different default values
    Set<DiscoveredConfigVar> discovered = parseAndDiscover(script);
    assertEquals(2, discovered.size());
    assertTrue(containsVar(discovered, "example.testVar", Optional.of("500 m")));
    assertTrue(containsVar(discovered, "example.testVar", Optional.empty()));
  }

  /**
   * Helper method to check if a set contains a specific DiscoveredConfigVar.
   */
  private boolean containsVar(Set<DiscoveredConfigVar> vars, String name,
      Optional<String> defaultValue) {
    return vars.stream().anyMatch(var ->
        var.getName().equals(name) && var.getDefaultValue().equals(defaultValue));
  }

  private Set<DiscoveredConfigVar> parseAndDiscover(String script) {
    JoshLangLexer lexer = new JoshLangLexer(CharStreams.fromString(script));
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    JoshLangParser parser = new JoshLangParser(tokens);
    ParseTree tree = parser.program();

    JoshConfigDiscoveryVisitor visitor = new JoshConfigDiscoveryVisitor();
    return visitor.visit(tree);
  }
}
