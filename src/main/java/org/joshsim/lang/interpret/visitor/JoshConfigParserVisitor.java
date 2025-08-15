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
import org.joshsim.lang.interpret.fragment.jshc.JshcCommentFragment;
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

    // Process each config line using visitor pattern
    for (JoshConfigParser.ConfigLineContext lineContext : ctx.configLine()) {
      JshcFragment lineFragment = lineContext.accept(this);
      ConfigBuilder lineBuilder = lineFragment.getConfigBuilder();
      builder = builder.combine(lineBuilder);
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
    JshcFragment identifierFragment = ctx.identifier().accept(this);
    // Units field holds the text in identifier fragment
    String variableName = identifierFragment.getUnits();
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
    JshcFragment numberFragment = ctx.number().accept(this);
    String numberText = numberFragment.getNumber().toString();
    // The identifier (units) may be omitted if the value has no units
    String unitsText = ctx.identifier() != null ? ctx.identifier().accept(this).getUnits() : "";

    try {
      // Parse the number
      BigDecimal number = numberFragment.getNumber();

      // Create units object
      Units units = unitsText.isEmpty() ? Units.EMPTY : Units.of(unitsText);

      // Create EngineValue
      EngineValue engineValue = valueFactory.parseNumber(numberText, units);

      return new JshcValueFragment(number, unitsText, engineValue);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Invalid number format: " + numberText, e);
    }
  }

  /**
   * Visits a comment context and returns a comment fragment.
   *
   * @param ctx The ANTLR comment context
   * @return A JshcCommentFragment for the comment
   */
  @Override
  public JshcFragment visitComment(JoshConfigParser.CommentContext ctx) {
    // Comments are ignored - return comment fragment with empty builder
    return new JshcCommentFragment();
  }

  /**
   * Visits an empty line context and returns a comment fragment.
   *
   * @param ctx The ANTLR empty line context
   * @return A JshcCommentFragment for the empty line
   */
  @Override
  public JshcFragment visitEmptyLine(JoshConfigParser.EmptyLineContext ctx) {
    // Empty lines are ignored - return comment fragment with empty builder
    return new JshcCommentFragment();
  }

  /**
   * Visits an identifier context and returns a value fragment containing the text.
   *
   * @param ctx The ANTLR identifier context
   * @return A JshcValueFragment containing the identifier text
   */
  @Override
  public JshcFragment visitIdentifier(JoshConfigParser.IdentifierContext ctx) {
    String text = ctx.IDENTIFIER_().getText();
    return new JshcValueFragment(null, text, null);
  }

  /**
   * Visits a number context and returns a value fragment containing the number.
   *
   * @param ctx The ANTLR number context
   * @return A JshcValueFragment containing the number
   */
  @Override
  public JshcFragment visitNumber(JoshConfigParser.NumberContext ctx) {
    String numberText = ctx.NUMBER_().getText();
    try {
      BigDecimal number = new BigDecimal(numberText);
      return new JshcValueFragment(number, "", null);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Invalid number format: " + numberText, e);
    }
  }
}
