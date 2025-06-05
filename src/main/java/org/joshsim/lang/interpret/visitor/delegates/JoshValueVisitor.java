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

  public Fragment visitUnitsValue(JoshLangParser.UnitsValueContext ctx) {
    EngineValue value = parseUnitsValue(ctx);
    EventHandlerAction action = (machine) -> machine.push(value);
    return new ActionFragment(action);
  }

  public Fragment visitString(JoshLangParser.StringContext ctx) {
    String string = ctx.getText();
    EngineValue value = engineValueFactory.build(string, Units.of(""));
    EventHandlerAction action = (machine) -> machine.push(value);
    return new ActionFragment(action);
  }

  public Fragment visitBool(JoshLangParser.BoolContext ctx) {
    boolean bool = ctx.getChild(0).getText().equals("true");
    EngineValue value = engineValueFactory.build(bool, Units.of(""));
    EventHandlerAction action = (machine) -> machine.push(value);
    return new ActionFragment(action);
  }

  public Fragment visitAllExpression(JoshLangParser.AllExpressionContext ctx) {
    EventHandlerAction action = (machine) -> machine.push(allString);
    return new ActionFragment(action);
  }

  public Fragment visitExternalValue(JoshLangParser.ExternalValueContext ctx) {
    String name = ctx.name.getText();
    EventHandlerAction action = (machine) -> {
      long stepCount = machine.getStepCount();
      machine.pushExternal(name, stepCount);
      return machine;
    };
    return new ActionFragment(action);
  }

  public Fragment visitExternalValueAtTime(JoshLangParser.ExternalValueAtTimeContext ctx) {
    String name = ctx.getChild(1).getText();
    EventHandlerAction action = (machine) -> {
      return machine;
    };
    return new ActionFragment(action);
  }

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
