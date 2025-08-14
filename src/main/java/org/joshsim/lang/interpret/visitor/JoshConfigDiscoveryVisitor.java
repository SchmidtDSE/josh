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

  /**
   * Creates a new config discovery visitor.
   */
  public JoshConfigDiscoveryVisitor() {
    super();
  }

  @Override
  public Set<String> visitConfigValue(JoshLangParser.ConfigValueContext ctx) {
    String variableName = ctx.name.getText();
    Set<String> result = new HashSet<>();
    result.add(variableName);
    
    // Continue visiting children to collect any nested config values
    Set<String> childResult = visitChildren(ctx);
    if (childResult != null) {
      result.addAll(childResult);
    }
    
    return result;
  }

  @Override
  protected Set<String> defaultResult() {
    return new HashSet<>();
  }

  @Override
  protected Set<String> aggregateResult(Set<String> aggregate, Set<String> nextResult) {
    if (aggregate == null) {
      aggregate = new HashSet<>();
    }
    if (nextResult != null) {
      aggregate.addAll(nextResult);
    }
    return aggregate;
  }
}