/**
 * ANTLR visitor for parsing Josh configuration files (.jshc format).
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret.visitor;

import java.math.BigDecimal;
import org.joshsim.engine.config.ConfigBuilder;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.engine.value.type.EngineValue;
import org.joshsim.lang.antlr.JoshConfigBaseVisitor;
import org.joshsim.lang.antlr.JoshConfigParser;
import org.joshsim.lang.interpret.fragment.jshc.JshcConfigBuilderFragment;
import org.joshsim.lang.interpret.fragment.jshc.JshcFragment;
import org.joshsim.lang.interpret.fragment.jshc.JshcValueFragment;

/**
 * ANTLR visitor for parsing .jshc configuration files.
 *
 * <p>This visitor processes ANTLR parse trees generated from .jshc files
 * and produces JshcFragment instances containing the parsed configuration data.</p>
 */
public class JoshConfigParserVisitor extends JoshConfigBaseVisitor<JshcFragment> {

  private final EngineValueFactory valueFactory;

  /**
   * Creates a new visitor with the specified value factory.
   *
   * @param valueFactory The factory to use for creating EngineValues
   */
  public JoshConfigParserVisitor(EngineValueFactory valueFactory) {
    this.valueFactory = valueFactory;
  }

  /**
   * Visits the root config context and processes all configuration lines.
   *
   * @param ctx The ANTLR config context
   * @return A JshcFragment containing the complete configuration
   */
  @Override
  public JshcFragment visitConfig(JoshConfigParser.ConfigContext ctx) {
    ConfigBuilder builder = new ConfigBuilder();

    // Process each config line
    for (JoshConfigParser.ConfigLineContext lineContext : ctx.configLine()) {
      if (lineContext.assignment() != null) {
        // Process assignment and add to main builder
        String variableName = lineContext.assignment().ID().getText();
        JshcFragment valueFragment = lineContext.assignment().value().accept(this);
        builder.addValue(variableName, valueFragment.getEngineValue());
      }
      // Comments and empty lines are automatically ignored by the grammar
    }

    return new JshcConfigBuilderFragment(builder);
  }

  /**
   * Visits an assignment context and creates a config fragment.
   *
   * @param ctx The ANTLR assignment context
   * @return A JshcFragment containing the assignment
   */
  @Override
  public JshcFragment visitAssignment(JoshConfigParser.AssignmentContext ctx) {
    String variableName = ctx.ID().getText();
    JshcFragment valueFragment = ctx.value().accept(this);

    // Add the value to a new config builder
    ConfigBuilder builder = new ConfigBuilder();
    builder.addValue(variableName, valueFragment.getEngineValue());

    return new JshcConfigBuilderFragment(builder);
  }

  /**
   * Visits a value context and parses the number and units.
   *
   * @param ctx The ANTLR value context
   * @return A JshcFragment containing the parsed value
   */
  @Override
  public JshcFragment visitValue(JoshConfigParser.ValueContext ctx) {
    String numberText = ctx.NUMBER().getText();
    String unitsText = "";

    if (ctx.ID() != null) {
      unitsText = ctx.ID().getText();
    }

    try {
      // Parse the number
      BigDecimal number = new BigDecimal(numberText);

      // Create units object
      Units units = unitsText.isEmpty() ? Units.EMPTY : Units.of(unitsText);

      // Create EngineValue
      EngineValue engineValue = valueFactory.parseNumber(numberText, units);

      return new JshcValueFragment(number, unitsText, engineValue);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Invalid number format: " + numberText, e);
    } catch (Exception e) {
      String errorMsg = "Invalid value or units: " + numberText + " " + unitsText;
      throw new IllegalArgumentException(errorMsg, e);
    }
  }

  /**
   * Visits a comment context and returns an empty config builder.
   *
   * @param ctx The ANTLR comment context
   * @return An empty JshcFragment
   */
  @Override
  public JshcFragment visitComment(JoshConfigParser.CommentContext ctx) {
    // Comments are ignored - return empty builder
    return new JshcConfigBuilderFragment(new ConfigBuilder());
  }

  /**
   * Visits an empty line context and returns an empty config builder.
   *
   * @param ctx The ANTLR empty line context
   * @return An empty JshcFragment
   */
  @Override
  public JshcFragment visitEmptyLine(JoshConfigParser.EmptyLineContext ctx) {
    // Empty lines are ignored - return empty builder
    return new JshcConfigBuilderFragment(new ConfigBuilder());
  }
}
