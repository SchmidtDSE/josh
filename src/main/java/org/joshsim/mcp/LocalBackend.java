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
import org.joshsim.command.PreprocessUtil;
import org.joshsim.command.RunUtil;
import org.joshsim.engine.config.ConfigDiscoverabilityOutputFormatter;
import org.joshsim.engine.config.DiscoveredConfigVar;
import org.joshsim.engine.geometry.grid.GridGeometryFactory;
import org.joshsim.lang.antlr.JoshLangLexer;
import org.joshsim.lang.antlr.JoshLangParser;
import org.joshsim.lang.interpret.visitor.JoshConfigDiscoveryVisitor;
import org.joshsim.util.OutputOptions;


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
    // Build the minimal RunOptions the MCP run_simulation tool exposes today. Every other
    // RunCommand knob (replicateStart, csvPrecision, exportQueueSize, useFloat64, outputSteps,
    // dataFiles, customParameters) falls through to its CLI default inside RunUtil, so an
    // MCP-driven run behaves identically to `josh run <script> <simulation>` with the matching
    // flags — no divergent run logic lives here.
    //
    // Two MCP-specific defaults, deliberately not exposed as tool arguments:
    //   - Profiling stays disabled: evalDuration profiling is a developer feature with no MCP
    //     surface in v1.
    //   - MinIO uses a default MinioOptions instance, which resolves credentials from the
    //     environment (and config file) exactly as the CLI does when no --minio-* flags are
    //     passed. This keeps secrets out of tool arguments (and therefore out of LLM chat
    //     history) while still letting models that export to MinIO work over MCP.
    RunUtil.RunOptions options = RunUtil.RunOptions.builder(script.toFile(), simulation)
        .replicates(replicates)
        .serialPatches(serialPatches)
        .seed(seed)
        .build();

    try {
      RunUtil.RunResult result = RunUtil.run(options, output);
      return new RunSimulationResult(
          result.isSuccess(), result.getMessage(), result.getLastStep());
    } catch (Exception e) {
      return new RunSimulationResult(false, "Simulation failed: " + e.getMessage(), 0);
    }
  }

}
