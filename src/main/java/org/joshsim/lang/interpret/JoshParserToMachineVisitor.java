/**
 * Visitor for Josh sources that parses to an interpreter runtime builder.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret;

import org.joshsim.engine.value.EngineValue;
import org.joshsim.lang.antlr.JoshLangBaseVisitor;
import org.joshsim.lang.antlr.JoshLangParser;


public class JoshParserToMachineVisitor extends JoshLangBaseVisitor<InterpreterMachineBuilder> {

  @Override
  public InterpreterMachineBuilder visitIdentifier(JoshLangParser.IdentifierContext ctx) {
    String identifierName = ctx.getText();
    InterpreterAction action = (machine) -> machine.pushIdentifier(identifierName);
    return new SingleActionMachineScaffold(action);
  }

  @Override
  public InterpreterMachineBuilder visitNumber(JoshLangParser.NumberContext ctx) {
    double number = Double.parseDouble(ctx.getText());
    InterpreterAction action = (machine) -> machine.pushNumber(numberText);
    return new SingleActionMachineScaffold(action);
  }

  @Override
  public InterpreterMachineBuilder visitUnitsValue(JoshLangParser.UnitsValueContext ctx) {
    String numberText = Double.parseDouble(ctx.getChild(0).getText());
    String unitsText = ctx.getChild(1).getText();
    EngineValue value = new EngineValue(numberText, new Units(unitsText));
    InterpreterAction action = (machine) -> machine.pushValue(value);
    return new SingleActionMachineScaffold(action);
  }

  @Override
  public InterpreterMachineBuilder visitMapParam(JoshLangParser.MapParamContext ctx) {
    InterpterAction operandAction = ctx.operand.accept(this).getAction();
    InterpterAction fromLowAction = ctx.fromlow.accept(this).getAction();
    InterpterAction fromHighAction = ctx.fromhigh.accept(this).getAction();
    InterpterAction toLowAction = ctx.tolow.accept(this).getAction();
    InterpterAction toHighAction = ctx.tohigh.accept(this).getAction();
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
