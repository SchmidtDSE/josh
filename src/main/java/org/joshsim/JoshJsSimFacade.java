/**
 * Facade which makes exports available to JS clients.
 *
 * @license BSD-3-Clause
 */

package org.joshsim;

import java.util.HashMap;
import java.util.Map;
import org.joshsim.cloud.VirtualFileSystemWireDeserializer;
import org.joshsim.compat.CompatibilityLayerKeeper;
import org.joshsim.compat.CompatibleStringJoiner;
import org.joshsim.compat.EmulatedCompatibilityLayer;
import org.joshsim.engine.entity.base.MutableEntity;
import org.joshsim.engine.geometry.EngineGeometryFactory;
import org.joshsim.engine.geometry.grid.GridGeometryFactory;
import org.joshsim.engine.value.type.EngineValue;
import org.joshsim.lang.bridge.GridInfoExtractor;
import org.joshsim.lang.bridge.ShadowingEntity;
import org.joshsim.lang.interpret.JoshInterpreter;
import org.joshsim.lang.interpret.JoshProgram;
import org.joshsim.lang.io.InputOutputLayer;
import org.joshsim.lang.io.MapToMemoryStringConverter;
import org.joshsim.lang.io.SandboxInputOutputLayer;
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
    setupForWasm(true);

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
      interpreter.interpret(result, new GridGeometryFactory(), getInputOutputLayer());
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
    setupForWasm(true);

    ParseResult result = JoshSimFacadeUtil.parse(code);
    if (result.hasErrors()) {
      throw new RuntimeException("Failed on: " + result.getErrors().iterator().next().toString());
    }

    EngineGeometryFactory geometryFactory = new GridGeometryFactory();

    JoshProgram program = JoshSimFacadeUtil.interpret(
        geometryFactory,
        result,
        getInputOutputLayer()
    );

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
   * @param externalData The serialization of the virtual file system to use in this simulation.
   * @param favorBigDecimal Flag indicating if numbers should be backed by BigDecimal or double if
   *     not specified. True if BigDecimal and false otherwise.
   */
  @JSExport
  public static void runSimulation(String code, String simulationName, String externalData,
        boolean favorBigDecimal) {
    try {
      runSimulationUnsafe(code, simulationName, externalData, favorBigDecimal);
    } catch (Exception e) {
      reportError(e.toString());
    }
  }

  /**
   * Generates metadata for a specific simulation defined in the provided source code.
   *
   * <p>This method parses and interprets the given source code, extracts the simulation
   * information, and generates a metadata map containing details such as coordinate reference
   * systems, time ranges, and patch name. The metadata is then converted to the memory-passing
   * string format.</p>
   *
   * @param code The Josh source code containing simulation definitions.
   * @param simulationName The name of the simulation to extract metadata for.
   * @return A string representation of the simulation metadata in memory-passing format.
   * @throws RuntimeException If the source code has parsing errors or if the specified
   *      simulation does not exist.
   */
  @JSExport
  public static String getSimulationMetadata(String code, String simulationName) {
    setupForWasm(true);

    ParseResult result = JoshSimFacadeUtil.parse(code);
    if (result.hasErrors()) {
      throw new RuntimeException("Failed on: " + result.getErrors().iterator().next().toString());
    }

    EngineGeometryFactory geometryFactory = new GridGeometryFactory();

    JoshProgram program = JoshSimFacadeUtil.interpret(
        geometryFactory,
        result,
        getInputOutputLayer()
    );
    program.getSimulations().getProtoype(simulationName);

    MutableEntity simEntityRaw = program.getSimulations().getProtoype(simulationName).build();
    MutableEntity simEntity = new ShadowingEntity(simEntityRaw, simEntityRaw);
    GridInfoExtractor extractor = new GridInfoExtractor(
        simEntity,
        CompatibilityLayerKeeper.get().getEngineValueFactory()
    );

    Map<String, String> outputRecord = new HashMap<>();
    outputRecord.put("name", simulationName);
    outputRecord.put("inputCrs", extractor.getInputCrs());
    outputRecord.put("targetCrs", extractor.getTargetCrs());
    outputRecord.put("startStr", extractor.getStartStr());
    outputRecord.put("endStr", extractor.getEndStr());
    outputRecord.put("patchName", extractor.getPatchName());

    EngineValue size = extractor.getSize();
    outputRecord.put("sizeStr", String.format(
        "%s %s",
        size.getAsString(),
        size.getUnits().toString()
    ));

    return MapToMemoryStringConverter.convert("simulationMetadata", outputRecord);
  }

  /**
   * Configures the system for execution within a WebAssembly (Wasm) environment.
   *
   * <p>This method sets the platform-specific compatibility layer to an instance of
   * EmulatedCompatibilityLaye}, which provides the necessary abstractions to enable simulations to
   * run within the WebAssembly virtual machine.</p>
   *
   * @param favorBigDecimal Flag indicating if numbers should be backed by BigDecimal or double if
   *     not specified. True if BigDecimal and false otherwise.
   */
  private static void setupForWasm(boolean favorBigDecimal) {
    CompatibilityLayerKeeper.set(new EmulatedCompatibilityLayer(favorBigDecimal));
  }

  /**
   * Interpret and run a Josh simulation without error handling.
   *
   * <p>Parses the given Josh script, interprets it, and runs the specified simulation. This method
   * performs various steps including syntax checking, program interpretation, and simulation
   * execution. It uses a callback mechanism to signal completion of each simulation step.</p>
   *
   * @param code The Josh source code to parse, interpret, and run as a simulation.
   * @param simulationName The name of the simulation to be executed as defined in the parsed
   *     program.
   * @param externalData The serialization of the virtual file system to use within this simulation.
   * @param favorBigDecimal Flag indicating if numbers should be backed by BigDecimal or double if
   *     not specified. True if BigDecimal and false otherwise.
   * @throws RuntimeException If parsing the code results in errors.
   */
  private static void runSimulationUnsafe(String code, String simulationName, String externalData,
        boolean favorBigDecimal) {
    setupForWasm(favorBigDecimal);

    ParseResult result = JoshSimFacadeUtil.parse(code);
    if (result.hasErrors()) {
      throw new RuntimeException("Failed on: " + result.getErrors().iterator().next().toString());
    }

    EngineGeometryFactory geometryFactory = new GridGeometryFactory();
    InputOutputLayer inputOutputLayer = getInputOutputLayer(externalData);

    JoshProgram program = JoshSimFacadeUtil.interpret(geometryFactory, result, inputOutputLayer);

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
   * Required entrypoint for wasm.
   *
   * @param args ignored arguments
   */
  public static void main(String[] args) {}

  /**
   * Get the input / output layer for the browser sandbox without a filesystem.
   *
   * @return Sandboxed input / output layer with an empty virtual file system.
   */
  private static InputOutputLayer getInputOutputLayer() {
    return new SandboxInputOutputLayer(
        new HashMap<>(),
        JoshJsSimFacade::reportData
    );
  }

  /**
   * Get the input / output layer for the browser sandbox.
   *
   * @param externalData The string serialization of the virtual file system to use within the input
   *     output layer.
   * @return Sandboxed input / output layer.
   */
  private static InputOutputLayer getInputOutputLayer(String externalData) {
    return new SandboxInputOutputLayer(
        VirtualFileSystemWireDeserializer.load(externalData),
        JoshJsSimFacade::reportData
    );
  }

  @JSBody(params = { "count" }, script = "reportStepComplete(count)")
  private static native void reportStepComplete(int count);

  @JSBody(params = { "payload" }, script = "reportData(payload)")
  private static native void reportData(String payload);

  @JSBody(params = { "message" }, script = "reportError(message)")
  private static native void reportError(String payload);

}
