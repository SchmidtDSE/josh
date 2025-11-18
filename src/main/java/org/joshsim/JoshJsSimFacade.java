/**
 * Facade which makes exports available to JS clients.
 *
 * @license BSD-3-Clause
 */

package org.joshsim;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.joshsim.cloud.VirtualFileSystemWireDeserializer;
import org.joshsim.compat.CompatibilityLayerKeeper;
import org.joshsim.compat.CompatibleStringJoiner;
import org.joshsim.compat.EmulatedCompatibilityLayer;
import org.joshsim.engine.config.ConfigDiscoverabilityOutputFormatter;
import org.joshsim.engine.config.DiscoveredConfigVar;
import org.joshsim.engine.entity.base.MutableEntity;
import org.joshsim.engine.geometry.EngineGeometryFactory;
import org.joshsim.engine.geometry.grid.GridGeometryFactory;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.engine.value.type.EngineValue;
import org.joshsim.lang.antlr.JoshLangLexer;
import org.joshsim.lang.antlr.JoshLangParser;
import org.joshsim.lang.bridge.GridInfoExtractor;
import org.joshsim.lang.bridge.ShadowingEntity;
import org.joshsim.lang.interpret.JoshInterpreter;
import org.joshsim.lang.interpret.JoshProgram;
import org.joshsim.lang.interpret.visitor.JoshConfigDiscoveryVisitor;
import org.joshsim.lang.io.InputOutputLayer;
import org.joshsim.lang.io.SandboxInputOutputLayer;
import org.joshsim.lang.parse.JoshParser;
import org.joshsim.lang.parse.ParseError;
import org.joshsim.lang.parse.ParseResult;
import org.joshsim.util.OutputStepsParser;
import org.joshsim.wire.NamedMap;
import org.joshsim.wire.WireConverter;
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
      EngineValueFactory valueFactory = new EngineValueFactory();
      EngineGeometryFactory geometryFactory = new GridGeometryFactory();
      interpreter.interpret(result, valueFactory, geometryFactory, getInputOutputLayer());
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

    JoshProgram program = JoshSimFacadeUtil.interpret(
        new EngineValueFactory(),
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
   * @param outputSteps Comma-separated string of step numbers to export (e.g., "5,7,8,9,20").
   *     If empty or null, all steps are exported.
   */
  @JSExport
  public static void runSimulation(String code, String simulationName, String externalData,
        boolean favorBigDecimal, String outputSteps) {
    try {
      runSimulationUnsafe(code, simulationName, externalData, favorBigDecimal, outputSteps);
    } catch (Exception e) {
      reportError(e.toString());
    }
  }

  /**
   * Interpret and run a Josh simulation.
   *
   * <p>Backward compatibility method that delegates to the new method with empty outputSteps.
   * This maintains compatibility with existing JavaScript callers that don't specify
   * outputSteps.</p>
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
    runSimulation(code, simulationName, externalData, favorBigDecimal, "");
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
    setupForWasm();

    ParseResult result = JoshSimFacadeUtil.parse(code);
    if (result.hasErrors()) {
      throw new RuntimeException("Failed on: " + result.getErrors().iterator().next().toString());
    }

    EngineGeometryFactory geometryFactory = new GridGeometryFactory();

    EngineValueFactory engineValueFactory = new EngineValueFactory();
    JoshProgram program = JoshSimFacadeUtil.interpret(
        engineValueFactory,
        geometryFactory,
        result,
        getInputOutputLayer()
    );
    program.getSimulations().getProtoype(simulationName);

    MutableEntity simEntityRaw = program.getSimulations().getProtoype(simulationName).build();
    MutableEntity simEntity = new ShadowingEntity(engineValueFactory, simEntityRaw, simEntityRaw);
    GridInfoExtractor extractor = new GridInfoExtractor(simEntity, engineValueFactory);

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

    outputRecord.put("totalSteps", String.valueOf(extractor.getTotalSteps()));

    // Add stepsLow for frontend progress normalization
    EngineValue stepsLow = extractor.getStepsLow();
    outputRecord.put("stepsLow", String.valueOf(Math.round(stepsLow.getAsDouble())));

    NamedMap namedMap = new NamedMap("simulationMetadata", outputRecord);
    return WireConverter.serializeToString(namedMap);
  }

  /**
   * Discover configuration variables used in the provided Josh script.
   *
   * <p>Parses the given Josh source code and identifies all configuration variables
   * referenced using 'config' expressions. For example, "config example.testVar"
   * would be discovered as "example.testVar". Variables with default values are
   * shown with the default in parentheses, such as "example.testVar(5m)".</p>
   *
   * @param code The Josh source code to analyze for configuration variable usage.
   * @return A line-separated string of all discovered configuration variable names,
   *     with defaults shown in parentheses when present, or an empty string if no
   *     configuration variables are found.
   * @throws RuntimeException If parsing the code results in errors.
   */
  @JSExport
  public static String discoverConfigVariables(String code) {
    setupForWasm();

    JoshParser parser = new JoshParser();
    ParseResult result = parser.parse(code);
    if (result.hasErrors()) {
      String firstError = result.getErrors().iterator().next().toString();
      throw new RuntimeException("Failed on: " + firstError);
    }

    // Parse the AST to discover config variables
    JoshLangLexer lexer = new JoshLangLexer(CharStreams.fromString(code));
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    JoshLangParser joshParser = new JoshLangParser(tokens);
    ParseTree tree = joshParser.program();

    JoshConfigDiscoveryVisitor visitor = new JoshConfigDiscoveryVisitor();
    Set<DiscoveredConfigVar> configVariables = visitor.visit(tree);

    return ConfigDiscoverabilityOutputFormatter.format(configVariables);
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
   * @param outputSteps Comma-separated string of step numbers to export (e.g., "5,7,8,9,20").
   *     If empty or null, all steps are exported.
   * @throws RuntimeException If parsing the code results in errors.
   */
  private static void runSimulationUnsafe(String code, String simulationName, String externalData,
        boolean favorBigDecimal, String outputSteps) {
    setupForWasm();

    ParseResult result = JoshSimFacadeUtil.parse(code);
    if (result.hasErrors()) {
      throw new RuntimeException("Failed on: " + result.getErrors().iterator().next().toString());
    }

    EngineValueFactory valueFactory = new EngineValueFactory(favorBigDecimal);
    EngineGeometryFactory geometryFactory = new GridGeometryFactory();
    InputOutputLayer inputOutputLayer = getInputOutputLayer(externalData);

    JoshProgram program = JoshSimFacadeUtil.interpret(
        valueFactory,
        geometryFactory,
        result,
        inputOutputLayer
    );

    Optional<Set<Integer>> parsedOutputSteps = parseOutputSteps(outputSteps);

    JoshSimFacadeUtil.runSimulation(
        valueFactory,
        geometryFactory,
        inputOutputLayer,
        program,
        simulationName,
        (x) -> JoshJsSimFacade.reportStepComplete((int) x),
        true,
        parsedOutputSteps,
        Optional.empty()
    );
  }

  /**
   * Interpret and run a Josh simulation without error handling.
   *
   * <p>Backward compatibility method that delegates to the new method with empty outputSteps.
   * This maintains compatibility with existing internal callers that don't specify outputSteps.</p>
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
    runSimulationUnsafe(code, simulationName, externalData, favorBigDecimal, "");
  }

  /**
   * Parses the output-steps parameter using the OutputStepsParser utility.
   *
   * @param outputSteps Comma-separated string of step numbers to export
   * @return Optional containing the set of steps to export, or empty if all steps should be
   *     exported
   * @throws RuntimeException if the output-steps format is invalid
   */
  private static Optional<Set<Integer>> parseOutputSteps(String outputSteps) {
    return OutputStepsParser.parseForWasmOrRemote(outputSteps);
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
