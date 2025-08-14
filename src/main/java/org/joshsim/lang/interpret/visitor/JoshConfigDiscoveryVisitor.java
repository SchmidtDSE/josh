package org.joshsim.lang.interpret.visitor;

import java.util.HashSet;
import java.util.Set;
import org.antlr.v4.runtime.tree.ParseTree;
import org.joshsim.engine.config.DiscoveredConfigVar;
import org.joshsim.lang.antlr.JoshLangBaseVisitor;
import org.joshsim.lang.antlr.JoshLangParser;

/**
 * Visitor to discover configuration variables used in Josh scripts.
 *
 * <p>This visitor traverses the AST and collects all configuration variable
 * references found in config expressions. For example, "config example.testVar"
 * would add "example.testVar" to the discovered set, while "config example.testVar else 5 m"
 * would add "example.testVar(5 m)" to show the default value.</p>
 */
public class JoshConfigDiscoveryVisitor extends JoshLangBaseVisitor<Set<DiscoveredConfigVar>> {

  /**
   * Creates a new config discovery visitor.
   */
  public JoshConfigDiscoveryVisitor() {
    super();
  }

  @Override
  public Set<DiscoveredConfigVar> visitConfigValue(JoshLangParser.ConfigValueContext ctx) {
    String variableName = ctx.name.getText();
    DiscoveredConfigVar discovered = new DiscoveredConfigVar(variableName);
    Set<DiscoveredConfigVar> result = new HashSet<>();
    result.add(discovered);

    // Continue visiting children to collect any nested config values
    Set<DiscoveredConfigVar> childResult = visitChildren(ctx);
    if (childResult != null) {
      result.addAll(childResult);
    }

    return result;
  }

  @Override
  public Set<DiscoveredConfigVar> visitConfigValueWithDefault(JoshLangParser.ConfigValueWithDefaultContext ctx) {
    String variableName = ctx.name.getText();
    String defaultValueString = extractDefaultValueString(ctx.defaultValue);
    DiscoveredConfigVar discovered = new DiscoveredConfigVar(variableName, defaultValueString);
    Set<DiscoveredConfigVar> result = new HashSet<>();
    result.add(discovered);

    // Continue visiting children to collect any nested config values
    Set<DiscoveredConfigVar> childResult = visitChildren(ctx);
    if (childResult != null) {
      result.addAll(childResult);
    }

    return result;
  }

  /**
   * Extracts a readable string representation from a default value expression.
   *
   * <p>This method traverses the parse tree to build a human-readable string
   * representation of the default value, handling common cases like numbers
   * with units, strings, and simple expressions.</p>
   *
   * @param defaultValueContext The parse tree context for the default value expression
   * @return A string representation of the default value
   */
  private String extractDefaultValueString(ParseTree defaultValueContext) {
    if (defaultValueContext == null) {
      return "";
    }

    // Handle simple cases based on context type
    if (defaultValueContext instanceof JoshLangParser.SimpleExpressionContext) {
      JoshLangParser.SimpleExpressionContext simpleCtx = (JoshLangParser.SimpleExpressionContext) defaultValueContext;
      return extractUnitsValueString(simpleCtx.unitsValue());
    } else if (defaultValueContext instanceof JoshLangParser.SimpleNumberContext) {
      JoshLangParser.SimpleNumberContext numberCtx = (JoshLangParser.SimpleNumberContext) defaultValueContext;
      return numberCtx.number().getText();
    } else if (defaultValueContext instanceof JoshLangParser.SimpleStringContext) {
      JoshLangParser.SimpleStringContext stringCtx = (JoshLangParser.SimpleStringContext) defaultValueContext;
      return stringCtx.string().STR_().getText();
    }

    // For complex expressions, fall back to getting the full text
    return defaultValueContext.getText();
  }

  /**
   * Extracts a string representation from a units value (number + optional units).
   *
   * @param unitsValueContext The parse tree context for the units value
   * @return A string representation like "5 m" or "10"
   */
  private String extractUnitsValueString(JoshLangParser.UnitsValueContext unitsValueContext) {
    if (unitsValueContext == null) {
      return "";
    }

    StringBuilder result = new StringBuilder();
    result.append(unitsValueContext.number().getText());

    // Check for units - could be identifier or PERCENT_
    if (unitsValueContext.identifier() != null) {
      result.append(" ").append(unitsValueContext.identifier().getText());
    } else if (unitsValueContext.PERCENT_() != null) {
      result.append(unitsValueContext.PERCENT_().getText());
    }

    return result.toString();
  }

  @Override
  protected Set<DiscoveredConfigVar> defaultResult() {
    return new HashSet<>();
  }

  @Override
  protected Set<DiscoveredConfigVar> aggregateResult(Set<DiscoveredConfigVar> aggregate, Set<DiscoveredConfigVar> nextResult) {
    if (aggregate == null) {
      aggregate = new HashSet<>();
    }
    if (nextResult != null) {
      aggregate.addAll(nextResult);
    }
    return aggregate;
  }
}
