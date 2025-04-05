/**
 * Structures for interpreting a Josh source.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret;

import org.joshsim.lang.interpret.fragment.Fragment;
import org.joshsim.lang.parse.ParseResult;


/**
 * Strategy to interpret a Josh program into a single JoshProgram.
 */
public class JoshInterpreter {

  /**
   * Interpret a Josh source into a JoshProgram.
   *
   * @param parseResult The result of parsing to interpret.
   * @param simulationName The name of the simulation to be executed.
   * @return Parsed simulations.
   */
  public JoshProgram interpret(ParseResult parseResult, String simulationName) {
    if (parseResult.hasErrors()) {
      throw new RuntimeException("Cannot interpret program with parse errors.");
    }

    BridgeGetter bridgeGetter = new FutureBridgeGetter();

    JoshParserToMachineVisitor visitor = new JoshParserToMachineVisitor(bridgeGetter);
    Fragment fragment = visitor.visit(parseResult.getProgram().orElseThrow());
    
    JoshProgram program = fragment.getProgram();
    bridgeGetter.setProgram(program);
    bridgeGetter.setSimulationName(simulationName);

    return program;
  }

}
