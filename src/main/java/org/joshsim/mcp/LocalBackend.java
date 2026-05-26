/**
 * Local in-JVM backend for the Josh MCP server.
 *
 * <p>Implements {@link Backend} by delegating to the existing Josh facades:
 * {@code JoshSimCommander.getJoshProgram}, the {@code JoshConfigDiscoveryVisitor},
 * {@code PreprocessUtil.preprocess}, and {@code JoshSimFacadeUtil.runSimulation}.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.mcp;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.joshsim.JoshSimCommander;
import org.joshsim.JoshSimFacadeUtil;
import org.joshsim.command.PreprocessUtil;
import org.joshsim.compat.CompatibilityLayerKeeper;
import org.joshsim.compat.JvmCompatibilityLayer;
import org.joshsim.engine.config.ConfigDiscoverabilityOutputFormatter;
import org.joshsim.engine.config.DiscoveredConfigVar;
import org.joshsim.engine.geometry.grid.GridGeometryFactory;
import org.joshsim.engine.value.engine.ValueSupportFactory;
import org.joshsim.lang.antlr.JoshLangLexer;
import org.joshsim.lang.antlr.JoshLangParser;
import org.joshsim.lang.interpret.JoshProgram;
import org.joshsim.lang.interpret.visitor.JoshConfigDiscoveryVisitor;
import org.joshsim.lang.io.InputGetterStrategy;
import org.joshsim.lang.io.InputOutputLayer;
import org.joshsim.lang.io.JvmInputOutputLayerBuilder;
import org.joshsim.lang.io.JvmWorkingDirInputGetter;
import org.joshsim.precompute.JshdExternalGetter;
import org.joshsim.precompute.JshdzExternalGetter;
import org.joshsim.precompute.MultiFormatExternalGetter;
import org.joshsim.util.OutputOptions;
import org.joshsim.util.SharedRandom;


/**
 * In-process backend that runs Josh operations in the MCP server JVM.
 *
 * <p>This is the only backend in Phase 1. It calls the same facades used by the CLI commands
 * without going through the CLI command objects themselves, so it can route output to stderr
 * rather than stdout.</p>
 */
public class LocalBackend implements Backend {

  private final OutputOptions output;

  /**
   * Constructs a LocalBackend using the given output options.
   *
   * @param output output options; should be a {@link StderrOutputOptions} for MCP use
   */
  public LocalBackend(OutputOptions output) {
    this.output = output;
  }

  @Override
  public ValidateResult validate(Path script) {
    File file = script.toFile();
    JoshSimCommander.ProgramInitResult result = JoshSimCommander.getJoshProgram(
        new GridGeometryFactory(),
        file,
        output
    );
    if (result.getFailureStep().isPresent()) {
      return new ValidateResult(false,
          "Validation failed at step: " + result.getFailureStep().get());
    }
    return new ValidateResult(true, "Validated Josh script: " + script);
  }

  @Override
  public DiscoverConfigResult discoverConfig(Path script) {
    File file = script.toFile();

    // First validate the file
    JoshSimCommander.ProgramInitResult initResult = JoshSimCommander.getJoshProgram(
        new GridGeometryFactory(),
        file,
        output
    );
    if (initResult.getFailureStep().isPresent()) {
      return new DiscoverConfigResult(false,
          "Script validation failed at step: " + initResult.getFailureStep().get());
    }

    // Parse file for config discovery
    try {
      String fileContent = Files.readString(script);
      JoshLangLexer lexer = new JoshLangLexer(CharStreams.fromString(fileContent));
      CommonTokenStream tokens = new CommonTokenStream(lexer);
      JoshLangParser parser = new JoshLangParser(tokens);
      ParseTree tree = parser.program();

      JoshConfigDiscoveryVisitor visitor = new JoshConfigDiscoveryVisitor();
      Set<DiscoveredConfigVar> configVariables = visitor.visit(tree);

      String formattedOutput = ConfigDiscoverabilityOutputFormatter.format(configVariables);
      if (formattedOutput.isEmpty()) {
        return new DiscoverConfigResult(true, "[No variables found]");
      }
      return new DiscoverConfigResult(true, formattedOutput);
    } catch (IOException e) {
      return new DiscoverConfigResult(false, "Error reading file: " + e.getMessage());
    } catch (Exception e) {
      return new DiscoverConfigResult(false,
          "Error discovering config variables: " + e.getMessage());
    }
  }

  @Override
  public PreprocessResult preprocess(
      Path script,
      String simulation,
      Path dataFile,
      String variable,
      String unitsStr,
      Path outputFile,
      Optional<PreprocessOptions> options
  ) {
    PreprocessUtil.PreprocessOptions utilOptions;
    if (options.isPresent()) {
      PreprocessOptions o = options.get();
      utilOptions = new PreprocessUtil.PreprocessOptions(
          o.getCrsCode(),
          o.getHorizCoordName(),
          o.getVertCoordName(),
          o.getTimeName(),
          o.getTimestep(),
          o.getDefaultValue(),
          o.isParallel(),
          o.isAmend()
      );
    } else {
      utilOptions = new PreprocessUtil.PreprocessOptions();
    }

    try {
      PreprocessUtil.preprocess(
          script.toFile(),
          simulation,
          dataFile.toString(),
          variable,
          unitsStr,
          outputFile.toFile(),
          utilOptions,
          output
      );
      return new PreprocessResult(true,
          "Successfully preprocessed data to " + outputFile);
    } catch (IllegalArgumentException e) {
      return new PreprocessResult(false, e.getMessage());
    } catch (Exception e) {
      return new PreprocessResult(false, "Preprocessing failed: " + e.getMessage());
    }
  }

  @Override
  public RunSimulationResult runSimulation(
      Path script,
      String simulation,
      int replicates,
      boolean serialPatches,
      Optional<Long> seed
  ) {
    File file = script.toFile();

    // Initialize shared random per-call (per forward-compatibility discipline)
    if (seed.isPresent()) {
      SharedRandom.initialize(seed.get());
    } else {
      SharedRandom.initialize(Optional.empty());
    }

    // Set up JVM compatibility layer
    JvmCompatibilityLayer compatLayer = new JvmCompatibilityLayer();
    CompatibilityLayerKeeper.set(compatLayer);

    ValueSupportFactory valueFactory = new ValueSupportFactory();
    GridGeometryFactory geometryFactory = new GridGeometryFactory();

    InputGetterStrategy inputStrategy = new JvmWorkingDirInputGetter();
    InputOutputLayer inputOutputLayer = new JvmInputOutputLayerBuilder()
        .withReplicate(0)
        .withInputStrategy(inputStrategy)
        .build();

    JoshSimCommander.ProgramInitResult initResult = JoshSimCommander.getJoshProgram(
        valueFactory,
        geometryFactory,
        file,
        output,
        inputOutputLayer
    );

    if (initResult.getFailureStep().isPresent()) {
      SharedRandom.clear();
      return new RunSimulationResult(false,
          "Script validation failed at step: " + initResult.getFailureStep().get(), 0);
    }

    JoshProgram program = initResult.getProgram().orElseThrow();
    if (!program.getSimulations().hasPrototype(simulation)) {
      SharedRandom.clear();
      return new RunSimulationResult(false,
          "Could not find simulation: " + simulation, 0);
    }

    // Force serial execution when seeded for determinism
    boolean effectiveSerial = serialPatches || seed.isPresent();

    long[] stepCounter = {0};

    try {
      for (int rep = 0; rep < replicates; rep++) {
        InputOutputLayer repLayer = new JvmInputOutputLayerBuilder()
            .withReplicate(rep)
            .withInputStrategy(inputStrategy)
            .withAppendMode(rep > 0)
            .build();

        MultiFormatExternalGetter externalGetter = new MultiFormatExternalGetter(
            new JshdExternalGetter(inputStrategy, valueFactory),
            new JshdzExternalGetter(inputStrategy, valueFactory)
        );

        JoshSimFacadeUtil.runSimulation(
            valueFactory,
            geometryFactory,
            repLayer,
            externalGetter,
            program,
            simulation,
            (step) -> stepCounter[0] = step,
            effectiveSerial,
            Optional.empty()
        );
      }
    } catch (Exception e) {
      SharedRandom.clear();
      return new RunSimulationResult(false,
          "Simulation failed: " + e.getMessage(), stepCounter[0]);
    }

    SharedRandom.clear();
    return new RunSimulationResult(true,
        "Simulation '" + simulation + "' completed: "
            + replicates + " replicate(s), last step " + stepCounter[0],
        stepCounter[0]);
  }

}
