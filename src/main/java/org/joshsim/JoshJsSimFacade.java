/**
 * Facade which makes exports available to JS clients.
 *
 * @license BSD-3-Clause
 */

package org.joshsim;

import org.joshsim.engine.geometry.EngineGeometryFactory;
import org.joshsim.engine.geometry.grid.GridGeometryFactory;
import org.joshsim.lang.interpret.JoshInterpreter;
import org.joshsim.lang.interpret.JoshProgram;
import org.joshsim.lang.parse.JoshParser;
import org.joshsim.lang.parse.ParseResult;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSExport;


/**
 * Facade which offers access to JS clients.
 */
public class JoshJsSimFacade {

  /**
   * Validates and interprets the provided code using the JoshParser and JoshInterpreter.
   *
   * @param code The source code to validate and interpret.
   * @return A string containing parsing or interpretation error messages, or an empty string if
   *     there are no errors.
   */
  @JSExport
  public static String validate(String code) {
    JoshParser parser = new JoshParser();
    ParseResult result = parser.parse(code);
    if (result.hasErrors()) {
      return result.getErrors().get(0).toString();
    }

    JoshInterpreter interpreter = new JoshInterpreter();
    try {
      interpreter.interpret(result, new GridGeometryFactory());
    } catch (Exception e) {
      return e.getMessage();
    }

    return "";
  }

  @JSExport
  public static void runSimulation(String code, String simulationName) {
    ParseResult result = JoshSimFacade.parse(code);
    if (result.hasErrors()) {
      throw new RuntimeException("Failed on: " + result.getErrors().getFirst().toString());
    }

    EngineGeometryFactory geometryFactory = new GridGeometryFactory();

    JoshProgram program = JoshSimFacade.interpret(geometryFactory, result);

    JoshSimFacade.runSimulation(
        geometryFactory,
        program,
        simulationName,
        (x) -> JoshJsSimFacade.reportStepComplete((int) x),
        true
    );
  }

  /**
   * Required entrypoint for wasm.
   *
   * @param args ignored arguments
   */
  public static void main(String[] args) {}

  @JSBody(params = { "count" }, script = "reportStepComplete(count)")
  private static native void reportStepComplete(int count);

}
