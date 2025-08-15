package org.joshsim.lang.interpret.visitor;

import java.util.HashSet;
import java.util.Set;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.RuleNode;
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
  public Set<DiscoveredConfigVar> visitConfigValueWithDefault(
      JoshLangParser.ConfigValueWithDefaultContext ctx) {
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
   * <p>This method handles different types of parse tree contexts and builds
   * a human-readable string representation of the default value, handling common
   * cases like numbers with units, strings, and simple expressions.</p>
   *
   * @param defaultValueContext The parse tree context for the default value expression
   * @return A string representation of the default value
   */
  private String extractDefaultValueString(ParseTree defaultValueContext) {
    if (defaultValueContext == null) {
      return "";
    }

    // Handle specific expression types
    if (defaultValueContext instanceof JoshLangParser.SimpleExpressionContext) {
      JoshLangParser.SimpleExpressionContext simpleExpr = 
          (JoshLangParser.SimpleExpressionContext) defaultValueContext;
      return extractSimpleExpressionString(simpleExpr);
    } else if (defaultValueContext instanceof JoshLangParser.SimpleNumberContext) {
      JoshLangParser.SimpleNumberContext simpleNumber = 
          (JoshLangParser.SimpleNumberContext) defaultValueContext;
      return extractSimpleNumberString(simpleNumber);
    } else if (defaultValueContext instanceof JoshLangParser.SimpleStringContext) {
      JoshLangParser.SimpleStringContext simpleString = 
          (JoshLangParser.SimpleStringContext) defaultValueContext;
      return extractSimpleStringString(simpleString);
    } else {
      // For complex expressions not handled by specific methods,
      // fall back to getting the full text
      return defaultValueContext.getText();
    }
  }

  /**
   * Extracts string representation from a simple expression context.
   */
  private String extractSimpleExpressionString(JoshLangParser.SimpleExpressionContext ctx) {
    return extractUnitsValueString(ctx.unitsValue());
  }

  /**
   * Extracts string representation from a simple number context.
   */
  private String extractSimpleNumberString(JoshLangParser.SimpleNumberContext ctx) {
    return ctx.number().getText();
  }

  /**
   * Extracts string representation from a simple string context.
   */
  private String extractSimpleStringString(JoshLangParser.SimpleStringContext ctx) {
    return ctx.string().STR_().getText();
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
  protected Set<DiscoveredConfigVar> aggregateResult(
      Set<DiscoveredConfigVar> aggregate, Set<DiscoveredConfigVar> nextResult) {
    if (aggregate == null) {
      aggregate = new HashSet<>();
    }
    if (nextResult != null) {
      aggregate.addAll(nextResult);
    }
    return aggregate;
  }
}
