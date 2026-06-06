
/**
 * Command line interface handler for running Josh simulations.
 *
 * <p>This class implements the 'run' command which executes a specified simulation from a Josh
 * script file. It processes patches either serially or in parallel using grid-based coordinate
 * space.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.command;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import org.joshsim.JoshSimCommander;
import org.joshsim.lang.io.MapSerializeStrategy;
import org.joshsim.util.MinioOptions;
import org.joshsim.util.OutputOptions;
import org.joshsim.util.OutputStepsParser;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;


/**
 * Command handler for executing Josh simulations.
 *
 * <p>Processes command line arguments to run a specified simulation from a Josh script file.
 * Supports configuration of the coordinate reference system and parallel/serial patch processing.
 * Can optionally save results to Minio storage. The actual run pipeline lives in {@link RunUtil};
 * this class only translates picocli fields into {@link RunUtil.RunOptions} and maps the
 * {@link RunUtil.RunResult} to a process exit code.</p>
 */
@Command(
    name = "run",
    description = "Run a simulation file"
)
public class RunCommand implements Callable<Integer> {
  private static final int UNKNOWN_ERROR_CODE = 404;

  @Parameters(index = "0", description = "Path to file to validate")
  private File file;

  @Parameters(index = "1", description = "Simulation to run")
  private String simulation;

  @Option(names = "--replicates", description = "Number of replicates to run", defaultValue = "1")
  private int replicates = 1;

  @Option(
      names = "--replicate-index",
      description = "Run a single replicate at this index (mutually exclusive with --replicates)."
          + " Used by K8s indexed Jobs where each pod runs one replicate."
  )
  private Integer replicateIndex;

  @Option(
      names = "--replicate-start",
      description = "Starting replicate index (default: 0). Combined with --replicates this "
          + "selects the half-open range [start, start+count). Mutually exclusive with "
          + "--replicate-index.",
      defaultValue = "0"
  )
  private int replicateStart = 0;

  @Option(
      names = "--use-float-64",
      description = "Use double instead of BigDecimal, offering speed but lower precision.",
      defaultValue = "false"
  )
  private boolean useFloat64;

  @Mixin
  private OutputOptions output = new OutputOptions();

  @Mixin
  private MinioOptions minioOptions = new MinioOptions();

  @Option(
      names = "--serial-patches",
      description = "Run patches in serial instead of parallel",
      defaultValue = "false"
  )
  private boolean serialPatches;

  @Option(
      names = "--data",
      description = "Specify external data files to include (format: filename=path;filename2=path2)"
  )
  private String[] dataFiles = new String[0];

  @Option(
      names = "--custom-tag",
      description = "Custom template parameters (format: name=value). Can be specified "
                  + "multiple times."
  )
  private String[] customTags = new String[0];

  @Option(
      names = "--output-steps",
      description = "Comma-separated list of time steps to export (e.g., 5,7,8,9,20). "
                  + "If not specified, all steps are exported."
  )
  private String outputSteps = "";

  @Option(
      names = "--export-queue-size",
      description = "Maximum number of records to buffer before applying backpressure "
                  + "(default: 1000000)",
      defaultValue = "1000000"
  )
  private int exportQueueSize = 1000000;

  @Option(
      names = "--seed",
      description = "Random seed for reproducible simulations. If specified, all random "
                  + "operations will use this seed to produce deterministic results."
  )
  private Long seed = null;

  @Option(
      names = "--enable-profiler",
      description = "Enable evalDuration profiling to capture attribute resolution timing.",
      defaultValue = "false"
  )
  private boolean enableProfiler;

  @Option(
      names = "--csv-precision",
      description = "Maximum decimal places for numeric values in CSV output. "
                  + "Use -1 for unlimited precision (default: 10).",
      defaultValue = "10"
  )
  private int csvPrecision = MapSerializeStrategy.DEFAULT_MAX_DECIMAL_PLACES;

  /**
   * Parses custom parameter command-line options.
   *
   * @return Map of custom parameter names to values
   * @throws IllegalArgumentException if any custom tag is malformed or uses reserved names
   */
  private Map<String, String> parseCustomParameters() {
    Map<String, String> customParameters = new HashMap<>();
    for (String customTag : customTags) {
      int equalsIndex = customTag.indexOf('=');
      if (equalsIndex <= 0 || equalsIndex == customTag.length() - 1) {
        throw new IllegalArgumentException("Invalid custom-tag format: " + customTag
            + ". Expected format: name=value");
      }
      String name = customTag.substring(0, equalsIndex).trim();
      String value = customTag.substring(equalsIndex + 1);

      // Validate name doesn't conflict with reserved templates
      if ("replicate".equals(name) || "step".equals(name) || "variable".equals(name)) {
        throw new IllegalArgumentException("Custom parameter name '" + name
            + "' conflicts with reserved template variable");
      }

      customParameters.put(name, value);
    }
    return customParameters;
  }

  /**
   * Parses the output-steps command line option using the OutputStepsParser utility.
   *
   * @return Optional containing the set of steps to export, or empty if all steps should
   *     be exported
   * @throws IllegalArgumentException if the output-steps format is invalid
   */
  private Optional<Set<Integer>> parseOutputSteps() {
    return OutputStepsParser.parseForCli(outputSteps);
  }

  @Override
  public Integer call() {
    // Validate replicates parameter
    if (replicateIndex != null && replicates > 1) {
      output.printError("--replicate-index and --replicates are mutually exclusive");
      return 1;
    }
    if (replicateIndex != null && replicateIndex < 0) {
      output.printError("--replicate-index must be >= 0");
      return 1;
    }
    if (replicateIndex != null && replicateStart != 0) {
      output.printError("--replicate-index and --replicate-start are mutually exclusive");
      return 1;
    }
    if (replicateStart < 0) {
      output.printError("--replicate-start must be >= 0");
      return 1;
    }
    if (replicates < 1) {
      output.printError("Number of replicates must be at least 1");
      return 1;
    }

    // Parse CLI-shaped inputs up front for fail-fast validation.
    Map<String, String> customParameters = parseCustomParameters();
    Optional<Set<Integer>> parsedOutputSteps = parseOutputSteps();

    RunUtil.RunOptions options = RunUtil.RunOptions.builder(file, simulation)
        .replicates(replicates)
        .replicateIndex(replicateIndex)
        .replicateStart(replicateStart)
        .serialPatches(serialPatches)
        .seed(Optional.ofNullable(seed))
        .useFloat64(useFloat64)
        .enableProfiler(enableProfiler)
        .csvPrecision(csvPrecision)
        .exportQueueSize(exportQueueSize)
        .outputSteps(parsedOutputSteps)
        .dataFiles(dataFiles)
        .customParameters(customParameters)
        .minioOptions(minioOptions)
        .build();

    RunUtil.RunResult result = RunUtil.run(options, output);

    if (result.getInitFailureStep().isPresent()) {
      JoshSimCommander.CommanderStepEnum failStep = result.getInitFailureStep().get();
      return switch (failStep) {
        case LOAD -> 1;
        case READ -> 2;
        case PARSE -> 3;
        default -> UNKNOWN_ERROR_CODE;
      };
    }
    if (result.isSimulationNotFound()) {
      return 4;
    }

    return 0;
  }

}
