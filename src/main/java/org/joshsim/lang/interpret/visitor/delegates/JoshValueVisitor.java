/**
 * Delegate handling identifiers and literals parsing.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret.visitor.delegates;

import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.engine.value.type.EngineValue;
import org.joshsim.lang.antlr.JoshLangParser;
import org.joshsim.lang.interpret.ValueResolver;
import org.joshsim.lang.interpret.action.EventHandlerAction;
import org.joshsim.lang.interpret.fragment.ActionFragment;
import org.joshsim.lang.interpret.fragment.Fragment;


/**
 * Delegate which handles reading identifiers and literals from source like 10, "test", or 5 km.
 */
public class JoshValueVisitor implements JoshVisitorDelegate {

  private final EngineValueFactory engineValueFactory;
  private final EngineValue allString;

  /**
   * Constructs a new instance of the JoshValueVisitor class.
   *
   * @param toolbox The toolbox through which visitors can access supporting objects.
   */
  public JoshValueVisitor(DelegateToolbox toolbox) {
    engineValueFactory = toolbox.getValueFactory();
    allString = engineValueFactory.build("all", Units.of(""));
  }

  /**
   * Parse an identifier reference.
   *
   * <p>Parse a reference to a variable or other named entity in the program.</p>
   *
   * @param ctx The context from which to parse the identifier.
   * @return Fragment containing the identifier reference parsed.
   */
  public Fragment visitIdentifier(JoshLangParser.IdentifierContext ctx) {
    String identifierName = ctx.getText();
    ValueResolver resolver = new ValueResolver(engineValueFactory, identifierName);
    EventHandlerAction action = (machine) -> machine.push(resolver);
    return new ActionFragment(action);
  }

  /**
   * Parse a simple number.
   *
   * @param ctx The context from which to parse that number.
   * @return Fragment containing the number parsed.
   */
  public Fragment visitNumber(JoshLangParser.NumberContext ctx) {
    String numberStr = ctx.getChild(0).getText();
    EngineValue value = engineValueFactory.parseNumber(numberStr, Units.of("count"));
    EventHandlerAction action = (machine) -> machine.push(value);
    return new ActionFragment(action);
  }

  /**
   * Parse a value with units.
   *
   * <p>Parse a numeric value that has associated units, such as "5 km" or "10 percent".</p>
   *
   * @param ctx The context from which to parse the units value.
   * @return Fragment containing the units value parsed.
   */
  public Fragment visitUnitsValue(JoshLangParser.UnitsValueContext ctx) {
    EngineValue value = parseUnitsValue(ctx);
    EventHandlerAction action = (machine) -> machine.push(value);
    return new ActionFragment(action);
  }

  /**
   * Parse a string literal.
   *
   * <p>Parse a string literal enclosed in quotes.</p>
   *
   * @param ctx The context from which to parse the string.
   * @return Fragment containing the string literal parsed.
   */
  public Fragment visitString(JoshLangParser.StringContext ctx) {
    String string = ctx.getText();
    EngineValue value = engineValueFactory.build(string, Units.of(""));
    EventHandlerAction action = (machine) -> machine.push(value);
    return new ActionFragment(action);
  }

  /**
   * Parse a boolean literal.
   *
   * <p>Parse a boolean literal (true or false).</p>
   *
   * @param ctx The context from which to parse the boolean.
   * @return Fragment containing the boolean literal parsed.
   */
  public Fragment visitBool(JoshLangParser.BoolContext ctx) {
    boolean bool = ctx.getChild(0).getText().equals("true");
    EngineValue value = engineValueFactory.build(bool, Units.of(""));
    EventHandlerAction action = (machine) -> machine.push(value);
    return new ActionFragment(action);
  }

  /**
   * Parse an "all" expression.
   *
   * <p>Parse an expression that represents all entities or values in a collection.</p>
   *
   * @param ctx The context from which to parse the "all" expression.
   * @return Fragment containing the "all" expression parsed.
   */
  public Fragment visitAllExpression(JoshLangParser.AllExpressionContext ctx) {
    EventHandlerAction action = (machine) -> machine.push(allString);
    return new ActionFragment(action);
  }

  /**
   * Parse an external value reference.
   *
   * <p>Parse a reference to an external value at the current time step.</p>
   *
   * @param ctx The context from which to parse the external value reference.
   * @return Fragment containing the external value reference parsed.
   */
  public Fragment visitExternalValue(JoshLangParser.ExternalValueContext ctx) {
    String name = ctx.name.getText();
    EventHandlerAction action = (machine) -> {
      long stepCount = machine.getStepCount();
      machine.pushExternal(name, stepCount);
      return machine;
    };
    return new ActionFragment(action);
  }

  /**
   * Parse an external value reference at a specific time.
   *
   * <p>Parse a reference to an external value at a specified time step.</p>
   *
   * @param ctx The context from which to parse the external value at time reference.
   * @return Fragment containing the external value at time reference parsed.
   */
  public Fragment visitExternalValueAtTime(JoshLangParser.ExternalValueAtTimeContext ctx) {
    String name = ctx.name.getText();
    long step = Long.parseLong(ctx.step.getText());
    EventHandlerAction action = (machine) -> {
      machine.pushExternal(name, step);
      return machine;
    };
    return new ActionFragment(action);
  }

  /**
   * Parse a value with units into an EngineValue.
   *
   * <p>Helper method to parse a numeric value with units into an appropriate EngineValue,
   * handling special cases like percentages and different numeric formats.</p>
   *
   * @param ctx The context from which to parse the units value.
   * @return The parsed EngineValue with appropriate units.
   */
  private EngineValue parseUnitsValue(JoshLangParser.UnitsValueContext ctx) {
    String numberStr = ctx.getChild(0).getText();
    String unitsText = ctx.getChild(1).getText();
    boolean hasDecimal = numberStr.contains(".");
    boolean isPercent = unitsText.equals("percent") || unitsText.equals("%");

    if (unitsText.isBlank()) {
      unitsText = "count";
    }

    if (isPercent) {
      double percent = Double.parseDouble(numberStr);
      double converted = percent / 100;
      return engineValueFactory.buildForNumber(converted, Units.of("count"));
    } else if (hasDecimal) {
      return engineValueFactory.parseNumber(numberStr, Units.of(unitsText));
    } else {
      long number = Long.parseLong(numberStr);
      return engineValueFactory.build(number, Units.of(unitsText));
    }
  }

}
