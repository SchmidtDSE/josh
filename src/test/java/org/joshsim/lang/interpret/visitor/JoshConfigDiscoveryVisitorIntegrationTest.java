package org.joshsim.lang.interpret.visitor;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
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
 * Integration tests for JoshConfigDiscoveryVisitor using example files.
 */
public class JoshConfigDiscoveryVisitorIntegrationTest {

  @Test
  public void testDiscoveryWithConfigExample() throws IOException {
    // This test will need to be updated once config_example.josh is created
    // For now, test with a simple inline example
    String script = """
        start simulation ConfigExample
          grid.size = config example.gridSize
          steps.low = 0 count
          steps.high = config example.stepCount
        end simulation

        start patch Default
          Tree.init = create config example.treeCount of Tree
        end patch

        start organism Tree
          height.init = config example.initialHeight
          height.step = prior.height + 0.1 meters
        end organism
        """;

    Set<DiscoveredConfigVar> discovered = parseAndDiscover(script);
    assertTrue(containsVar(discovered, "example.gridSize", Optional.empty()));
    assertTrue(containsVar(discovered, "example.stepCount", Optional.empty()));
    assertTrue(containsVar(discovered, "example.treeCount", Optional.empty()));
    assertTrue(containsVar(discovered, "example.initialHeight", Optional.empty()));
  }

  /**
   * Helper method to check if a set contains a specific DiscoveredConfigVar.
   */
  private boolean containsVar(Set<DiscoveredConfigVar> vars, String name, Optional<String> defaultValue) {
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
