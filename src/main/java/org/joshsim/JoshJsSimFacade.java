/**
 * Facade which makes exports available to JS clients.
 *
 * @license BSD-3-Clause
 */

package org.joshsim;

import org.joshsim.compat.CompatibilityLayerKeeper;
import org.joshsim.compat.CompatibleStringJoiner;
import org.joshsim.compat.EmulatedCompatibilityLayer;
import org.joshsim.engine.geometry.EngineGeometryFactory;
import org.joshsim.engine.geometry.grid.GridGeometryFactory;
import org.joshsim.lang.interpret.JoshInterpreter;
import org.joshsim.lang.interpret.JoshProgram;
import org.joshsim.lang.io.InputOutputLayer;
import org.joshsim.lang.io.WasmInputOutputLayer;
import org.joshsim.lang.parse.JoshParser;
import org.joshsim.lang.parse.ParseError;
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
    setupForWasm();

    JoshParser parser = new JoshParser();
    ParseResult result = parser.parse(code);
    if (result.hasErrors()) {
      ParseError first = result.getErrors().get(0);
      String lineMessage = String.format(
          "On line %d: %s",
          first.getLine(),
          first.getMessage()
      );
      return lineMessage;
    }

    JoshInterpreter interpreter = new JoshInterpreter();
    try {
      interpreter.interpret(result, new GridGeometryFactory());
    } catch (Exception e) {
      return e.getMessage();
    }

    return "";
  }

  /**
   * Get a list of simulations found within the given code.
   *
   * @param code The Josh source code to parse and extract simulations.
   * @throws RuntimeException If parsing the code results in errors.
   */
  @JSExport
  public static String getSimulations(String code) {
    setupForWasm();

    ParseResult result = JoshSimFacadeUtil.parse(code);
    if (result.hasErrors()) {
      throw new RuntimeException("Failed on: " + result.getErrors().iterator().next().toString());
    }

    EngineGeometryFactory geometryFactory = new GridGeometryFactory();

    JoshProgram program = JoshSimFacadeUtil.interpret(geometryFactory, result);

    CompatibleStringJoiner stringJoiner = CompatibilityLayerKeeper.get().createStringJoiner(",");
    for (String name : program.getSimulations().getSimulations()) {
      stringJoiner.add(name);
    }

    return stringJoiner.toString();
  }

  /**
   * Interpret and run a Josh simulation.
   *
   * <p>Parses the given Josh script, interprets it, and runs the specified simulation. This method
   * performs various steps including syntax checking, program interpretation, and simulation
   * execution. It uses a callback mechanism to signal completion of each simulation step.</p>
   *
   * @param code The Josh source code to parse, interpret, and run as a simulation.
   * @param simulationName The name of the simulation to be executed as defined in the parsed
   *     program.
   * @throws RuntimeException If parsing the code results in errors.
   */
  @JSExport
  public static void runSimulation(String code, String simulationName) {
    setupForWasm();

    ParseResult result = JoshSimFacadeUtil.parse(code);
    if (result.hasErrors()) {
      throw new RuntimeException("Failed on: " + result.getErrors().iterator().next().toString());
    }

    EngineGeometryFactory geometryFactory = new GridGeometryFactory();
    InputOutputLayer inputOutputLayer = new WasmInputOutputLayer();

    JoshProgram program = JoshSimFacadeUtil.interpret(geometryFactory, result);

    JoshSimFacadeUtil.runSimulation(
        geometryFactory,
        inputOutputLayer,
        program,
        simulationName,
        (x) -> JoshJsSimFacade.reportStepComplete((int) x),
        true
    );
  }

  /**
   * Configures the system for execution within a WebAssembly (Wasm) environment.
   *
   * <p>This method sets the platform-specific compatibility layer to an instance of
   * EmulatedCompatibilityLaye}, which provides the necessary abstractions to enable simulations to
   * run within the WebAssembly virtual machine.</p>
   */
  private static void setupForWasm() {
    CompatibilityLayerKeeper.set(new EmulatedCompatibilityLayer());
  }

  /**
   * Required entrypoint for wasm.
   *
   * @param args ignored arguments
   */
  public static void main(String[] args) {}

  @JSBody(params = { "count" }, script = "reportStepComplete(count)")
  private static native void reportStepComplete(int count);

  @JSBody(params = { "payload" }, script = "reportData(payload)")
  private static native void reportData(String payload);

}
