/**
 * Structures for interpreting a Josh source.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret;

import org.joshsim.lang.parse.ParseResult;


/**
 * Strategy to interpret a Josh program into a single JoshProgram.
 */
public class JoshInterpreter {

  /**
   * Interpret a Josh source into a JoshProgram.
   *
   * @param parseResult The result of parsing to interpret.
   * @return Parsed simulations.
   */
  public JoshProgram interpret(ParseResult parseResult) {
    if (parseResult.hasErrors()) {
      throw new RuntimeException("Cannot interpret program with parse errors: " + parseResult.getErrors());
    }

    JoshParserToMachineVisitor visitor = new JoshParserToMachineVisitor();
    Fragment fragment = visitor.visit(parseResult.getProgram()
        .orElseThrow(() -> new RuntimeException("Program context missing from parse result")));
    
    return fragment.getProgram();
  }

}
