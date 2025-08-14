package org.joshsim.lang.interpret.visitor;

import java.util.HashSet;
import java.util.Set;
import org.joshsim.lang.antlr.JoshLangBaseVisitor;
import org.joshsim.lang.antlr.JoshLangParser;

/**
 * Visitor to discover configuration variables used in Josh scripts.
 *
 * <p>This visitor traverses the AST and collects all configuration variable
 * references found in config expressions. For example, "config example.testVar"
 * would add "example.testVar" to the discovered set.</p>
 */
public class JoshConfigDiscoveryVisitor extends JoshLangBaseVisitor<Set<String>> {
  private final Set<String> configVariables = new HashSet<>();

  /**
   * Creates a new config discovery visitor.
   */
  public JoshConfigDiscoveryVisitor() {
    super();
  }

  @Override
  public Set<String> visitConfigValue(JoshLangParser.ConfigValueContext ctx) {
    String variableName = ctx.name.getText();
    configVariables.add(variableName);
    return configVariables;
  }

  @Override
  protected Set<String> defaultResult() {
    return configVariables;
  }

  @Override
  protected Set<String> aggregateResult(Set<String> aggregate, Set<String> nextResult) {
    return aggregate; // configVariables is modified in place
  }

  /**
   * Gets the discovered configuration variables.
   *
   * @return Set of configuration variable names found in the script
   */
  public Set<String> getDiscoveredVariables() {
    return new HashSet<>(configVariables);
  }
}