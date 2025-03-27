/**
 * Visitor for Josh sources that parses to an interpreter runtime builder.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret;

import org.joshsim.engine.value.EngineValue;
import org.joshsim.engine.value.EngineValueFactory;
import org.joshsim.engine.value.Units;
import org.joshsim.lang.antlr.JoshLangBaseVisitor;
import org.joshsim.lang.antlr.JoshLangParser;

import java.math.BigDecimal;


public class JoshParserToMachineVisitor extends JoshLangBaseVisitor<InterpreterMachineScaffold> {

  private final EngineValueFactory engineValueFactory;

  public JoshParserToMachineVisitor() {
    super();
    engineValueFactory = new EngineValueFactory();
  }

  public InterpreterMachineScaffold visitIdentifier(JoshLangParser.IdentifierContext ctx) {
    String identifierName = ctx.getText();
    InterpreterAction action = (machine) -> machine.pushIdentifier(identifierName);
    return new SingleActionMachineScaffold(action);
  }

  public InterpreterMachineScaffold visitNumber(JoshLangParser.NumberContext ctx) {
    double number = Double.parseDouble(ctx.getText());
    InterpreterAction action = (machine) -> machine.pushNumber(number);
    return new SingleActionMachineScaffold(action);
  }

  public InterpreterMachineScaffold visitUnitsValue(JoshLangParser.UnitsValueContext ctx) {
    BigDecimal number = BigDecimal.valueOf(Double.parseDouble(ctx.getChild(0).getText()));
    String unitsText = ctx.getChild(1).getText();
    EngineValue value = engineValueFactory.build(number, new Units(unitsText));
    InterpreterAction action = (machine) -> machine.pushValue(value);
    return new SingleActionMachineScaffold(action);
  }

  public InterpreterMachineScaffold visitMapParam(JoshLangParser.MapParamContext ctx) {
    InterpreterAction operandAction = ctx.operand.accept(this).getCurrentAction();
    InterpreterAction fromLowAction = ctx.fromlow.accept(this).getCurrentAction();
    InterpreterAction fromHighAction = ctx.fromhigh.accept(this).getCurrentAction();
    InterpreterAction toLowAction = ctx.tolow.accept(this).getCurrentAction();
    InterpreterAction toHighAction = ctx.tohigh.accept(this).getCurrentAction();
    String method = ctx.method.getText();

    InterpreterAction action = (machine) -> {
      operandAction.apply(machine);
      fromLowAction.apply(machine);
      fromHighAction.apply(machine);
      toLowAction.apply(machine);
      toHighAction.apply(machine);
      machine.applyMap(method);
      return machine;
    };

    return new SingleActionMachineScaffold(action);
  }


  
}
