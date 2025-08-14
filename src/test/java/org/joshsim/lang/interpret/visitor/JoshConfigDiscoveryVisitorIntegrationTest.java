package org.joshsim.lang.interpret.visitor;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
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

    Set<String> discovered = parseAndDiscover(script);
    assertTrue(discovered.contains("example.gridSize"));
    assertTrue(discovered.contains("example.stepCount"));
    assertTrue(discovered.contains("example.treeCount"));
    assertTrue(discovered.contains("example.initialHeight"));
  }

  private Set<String> parseAndDiscover(String script) {
    JoshLangLexer lexer = new JoshLangLexer(CharStreams.fromString(script));
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    JoshLangParser parser = new JoshLangParser(tokens);
    ParseTree tree = parser.program();

    JoshConfigDiscoveryVisitor visitor = new JoshConfigDiscoveryVisitor();
    return visitor.visit(tree);
  }
}

