/**
 * Shared simulation-run logic extracted from {@link RunCommand}.
 *
 * <p>Holds the full "run a simulation" pipeline — random-seed setup, grid-search job expansion,
 * program initialization, Earth-space detection, and the per-replicate execution loop — so that
 * both the {@code run} CLI command and the MCP {@code run_simulation} tool drive identical logic
 * rather than maintaining divergent reimplementations.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.command;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.joshsim.JoshSimCommander;
import org.joshsim.JoshSimFacadeUtil;
import org.joshsim.compat.CompatibilityLayerKeeper;
import org.joshsim.compat.JvmCompatibilityLayer;
import org.joshsim.engine.entity.base.MutableEntity;
import org.joshsim.engine.geometry.EngineGeometryFactory;
import org.joshsim.engine.geometry.ExtentsUtil;
import org.joshsim.engine.geometry.PatchBuilderExtentsBuilder;
import org.joshsim.engine.geometry.grid.GridGeometryFactory;
import org.joshsim.engine.value.engine.ValueSupportFactory;
import org.joshsim.engine.value.type.EngineValue;
import org.joshsim.lang.bridge.GridInfoExtractor;
import org.joshsim.lang.bridge.ShadowingEntity;
import org.joshsim.lang.interpret.JoshProgram;
import org.joshsim.lang.interpret.RecursiveValueResolverFactory;
import org.joshsim.lang.interpret.TimedRecursiveValueResolverFactory;
import org.joshsim.lang.interpret.ValueResolverFactory;
import org.joshsim.lang.io.InputGetterStrategy;
import org.joshsim.lang.io.InputOutputLayer;
import org.joshsim.lang.io.JvmInputOutputLayerBuilder;
import org.joshsim.lang.io.JvmMappedInputGetter;
import org.joshsim.lang.io.JvmWorkingDirInputGetter;
import org.joshsim.lang.io.MapSerializeStrategy;
import org.joshsim.pipeline.job.JoshJob;
import org.joshsim.pipeline.job.JoshJobBuilder;
import org.joshsim.pipeline.job.config.JobVariationParser;
import org.joshsim.pipeline.job.config.TemplateStringRenderer;
import org.joshsim.precompute.JshdExternalGetter;
import org.joshsim.precompute.JshdzExternalGetter;
import org.joshsim.precompute.MultiFormatExternalGetter;
import org.joshsim.util.MinioOptions;
import org.joshsim.util.OutputOptions;
import org.joshsim.util.ProgressCalculator;
import org.joshsim.util.ProgressUpdate;
import org.joshsim.util.SharedRandom;
import org.joshsim.util.SimulationMetadata;
import org.joshsim.util.SimulationMetadataExtractor;

/**
 * Static utility that runs a Josh simulation end-to-end.
 *
 * <p>This is the single source of truth for how a simulation is run in-JVM. {@link RunCommand}
 * builds a {@link RunOptions} from its picocli fields and maps the {@link RunResult} to a process
 * exit code; the MCP {@code LocalBackend} builds a {@link RunOptions} and maps the result to an
 * MCP tool result. Neither reimplements the run loop.</p>
 */
public final class RunUtil {

  private RunUtil() {
    // Static utility class
  }

  /**
   * Immutable configuration for a simulation run.
   *
   * <p>Mirrors the knobs exposed by the {@code run} CLI command. Construct via
   * {@link #builder(File, String)}; every field other than the script file and simulation name
   * has a default matching the CLI's default, so a caller that sets nothing extra behaves exactly
   * like {@code josh run <script> <simulation>} with no flags.</p>
   */
  public static final class RunOptions {
    private final File scriptFile;
    private final String simulation;
    private final int replicates;
    private final Integer replicateIndex;
    private final int replicateStart;
    private final boolean serialPatches;
    private final Optional<Long> seed;
    private final boolean useFloat64;
    private final boolean enableProfiler;
    private final int csvPrecision;
    private final int exportQueueSize;
    private final Optional<Set<Integer>> outputSteps;
    private final String[] dataFiles;
    private final Map<String, String> customParameters;
    private final MinioOptions minioOptions;

    private RunOptions(Builder builder) {
      this.scriptFile = builder.scriptFile;
      this.simulation = builder.simulation;
      this.replicates = builder.replicates;
      this.replicateIndex = builder.replicateIndex;
      this.replicateStart = builder.replicateStart;
      this.serialPatches = builder.serialPatches;
      this.seed = builder.seed;
      this.useFloat64 = builder.useFloat64;
      this.enableProfiler = builder.enableProfiler;
      this.csvPrecision = builder.csvPrecision;
      this.exportQueueSize = builder.exportQueueSize;
      this.outputSteps = builder.outputSteps;
      this.dataFiles = builder.dataFiles;
      this.customParameters = builder.customParameters;
      this.minioOptions = builder.minioOptions;
    }

    /**
     * Creates a builder for the required script file and simulation name.
     *
     * @param scriptFile the {@code .josh} script file to run
     * @param simulation the name of the simulation block to run
     * @return a new builder seeded with CLI-matching defaults
     */
    public static Builder builder(File scriptFile, String simulation) {
      return new Builder(scriptFile, simulation);
    }

    /**
     * Builder for {@link RunOptions}. Defaults match the {@code run} CLI command's option defaults.
     */
    public static final class Builder {
      private final File scriptFile;
      private final String simulation;
      private int replicates = 1;
      private Integer replicateIndex = null;
      private int replicateStart = 0;
      private boolean serialPatches = false;
      private Optional<Long> seed = Optional.empty();
      private boolean useFloat64 = false;
      private boolean enableProfiler = false;
      private int csvPrecision = MapSerializeStrategy.DEFAULT_MAX_DECIMAL_PLACES;
      private int exportQueueSize = 1000000;
      private Optional<Set<Integer>> outputSteps = Optional.empty();
      private String[] dataFiles = new String[0];
      private Map<String, String> customParameters = new HashMap<>();
      private MinioOptions minioOptions = new MinioOptions();

      private Builder(File scriptFile, String simulation) {
        this.scriptFile = scriptFile;
        this.simulation = simulation;
      }

      /**
       * Sets the number of replicates to run (default 1).
       *
       * @param value replicate count
       * @return this builder
       */
      public Builder replicates(int value) {
        this.replicates = value;
        return this;
      }

      /**
       * Sets a single replicate index to run, or {@code null} to run the full range (default null).
       *
       * @param value replicate index, or null
       * @return this builder
       */
      public Builder replicateIndex(Integer value) {
        this.replicateIndex = value;
        return this;
      }

      /**
       * Sets the starting replicate index for the half-open range {@code [start, start+count)}
       * (default 0).
       *
       * @param value starting replicate index
       * @return this builder
       */
      public Builder replicateStart(int value) {
        this.replicateStart = value;
        return this;
      }

      /**
       * Sets whether to process patches serially rather than in parallel (default false). A
       * present {@code seed} forces serial processing regardless of this value.
       *
       * @param value true to process patches serially
       * @return this builder
       */
      public Builder serialPatches(boolean value) {
        this.serialPatches = value;
        return this;
      }

      /**
       * Sets the random seed for reproducible runs (default empty).
       *
       * @param value the seed, or empty for a non-deterministic run
       * @return this builder
       */
      public Builder seed(Optional<Long> value) {
        this.seed = value;
        return this;
      }

      /**
       * Sets whether to use {@code double} instead of {@code BigDecimal} for decimals
       * (default false).
       *
       * @param value true to favor double precision
       * @return this builder
       */
      public Builder useFloat64(boolean value) {
        this.useFloat64 = value;
        return this;
      }

      /**
       * Sets whether to enable evalDuration profiling (default false).
       *
       * @param value true to enable the profiler
       * @return this builder
       */
      public Builder enableProfiler(boolean value) {
        this.enableProfiler = value;
        return this;
      }

      /**
       * Sets the maximum decimal places for numeric CSV output, or -1 for unlimited
       * (default {@link MapSerializeStrategy#DEFAULT_MAX_DECIMAL_PLACES}).
       *
       * @param value maximum decimal places
       * @return this builder
       */
      public Builder csvPrecision(int value) {
        this.csvPrecision = value;
        return this;
      }

      /**
       * Sets the export queue capacity before backpressure is applied (default 1000000).
       *
       * @param value queue capacity
       * @return this builder
       */
      public Builder exportQueueSize(int value) {
        this.exportQueueSize = value;
        return this;
      }

      /**
       * Sets the specific time steps to export, or empty to export all steps (default empty).
       *
       * @param value set of steps to export, or empty
       * @return this builder
       */
      public Builder outputSteps(Optional<Set<Integer>> value) {
        this.outputSteps = value;
        return this;
      }

      /**
       * Sets the external data file specifications for grid search (default none).
       *
       * @param value data file specifications in {@code name=path} form
       * @return this builder
       */
      public Builder dataFiles(String[] value) {
        this.dataFiles = value;
        return this;
      }

      /**
       * Sets custom template parameters applied to each job (default none).
       *
       * @param value map of custom parameter names to values
       * @return this builder
       */
      public Builder customParameters(Map<String, String> value) {
        this.customParameters = value;
        return this;
      }

      /**
       * Sets the MinIO options used to build the input/output layer (default a fresh instance,
       * which resolves credentials from CLI flags, config file, then environment).
       *
       * @param value the MinIO options
       * @return this builder
       */
      public Builder minioOptions(MinioOptions value) {
        this.minioOptions = value;
        return this;
      }

      /**
       * Builds the immutable {@link RunOptions}.
       *
       * @return a new {@link RunOptions}
       */
      public RunOptions build() {
        return new RunOptions(this);
      }
    }
  }

  /**
   * Outcome of a simulation run.
   *
   * <p>Carries enough structured detail for the CLI to derive an exit code and for the MCP tool to
   * build a result message, without either caller needing to interpret run internals.</p>
   */
  public static final class RunResult {
    private final boolean success;
    private final String message;
    private final long lastStep;
    private final int totalReplicatesRun;
    private final Optional<JoshSimCommander.CommanderStepEnum> initFailureStep;
    private final boolean simulationNotFound;

    private RunResult(boolean success, String message, long lastStep, int totalReplicatesRun,
        Optional<JoshSimCommander.CommanderStepEnum> initFailureStep, boolean simulationNotFound) {
      this.success = success;
      this.message = message;
      this.lastStep = lastStep;
      this.totalReplicatesRun = totalReplicatesRun;
      this.initFailureStep = initFailureStep;
      this.simulationNotFound = simulationNotFound;
    }

    private static RunResult success(String message, long lastStep, int totalReplicatesRun) {
      return new RunResult(true, message, lastStep, totalReplicatesRun, Optional.empty(), false);
    }

    private static RunResult initFailure(JoshSimCommander.CommanderStepEnum step) {
      return new RunResult(false, "Script validation failed at step: " + step, 0, 0,
          Optional.of(step), false);
    }

    private static RunResult simulationNotFound(String simulation) {
      return new RunResult(false, "Could not find simulation: " + simulation, 0, 0,
          Optional.empty(), true);
    }

    public boolean isSuccess() {
      return success;
    }

    public String getMessage() {
      return message;
    }

    public long getLastStep() {
      return lastStep;
    }

    public int getTotalReplicatesRun() {
      return totalReplicatesRun;
    }

    /**
     * Returns the program-initialization step that failed, if initialization failed.
     *
     * @return the failing {@link JoshSimCommander.CommanderStepEnum}, or empty if init succeeded
     */
    public Optional<JoshSimCommander.CommanderStepEnum> getInitFailureStep() {
      return initFailureStep;
    }

    /**
     * Indicates whether the run failed because the named simulation was not found in the script.
     *
     * @return true if the simulation name did not match any prototype in the program
     */
    public boolean isSimulationNotFound() {
      return simulationNotFound;
    }
  }

  /**
   * Runs a simulation end-to-end according to the given options.
   *
   * <p>Diagnostic and progress messages are written via {@code output}; the caller controls where
   * they land (the CLI routes them to stdout, the MCP server to stderr). The returned
   * {@link RunResult} reports success/failure structurally. Genuinely unexpected exceptions from
   * the simulation engine are allowed to propagate; the random-number generator state is always
   * cleared on exit.</p>
   *
   * @param opts   the run configuration
   * @param output the output sink for diagnostic and progress messages
   * @return a {@link RunResult} describing the outcome
   */
  public static RunResult run(RunOptions opts, OutputOptions output) {
    boolean serialPatches = opts.serialPatches;

    // Initialize shared random with seed for reproducibility.
    if (opts.seed.isPresent()) {
      SharedRandom.initialize(opts.seed.get());
      output.printInfo("Using random seed: " + opts.seed.get());

      // Force serial execution when seeded for deterministic results. Parallel execution would
      // cause non-deterministic random call ordering.
      if (!serialPatches) {
        output.printInfo("Note: Forcing serial patch execution for reproducibility with --seed");
        serialPatches = true;
      }
    } else {
      SharedRandom.initialize(Optional.empty());
    }

    try {
      return runInternal(opts, output, serialPatches);
    } finally {
      // Clean up shared random regardless of how the run terminated.
      SharedRandom.clear();
    }
  }

  private static RunResult runInternal(RunOptions opts, OutputOptions output,
      boolean serialPatches) {
    final EngineGeometryFactory geometryFactory = new GridGeometryFactory();

    // When --replicate-index is set, run exactly one replicate at that index.
    int effectiveReplicates = opts.replicateIndex != null ? 1 : opts.replicates;

    List<JoshJob> jobs = buildJobs(opts, effectiveReplicates);
    reportPlannedRun(opts, jobs, effectiveReplicates, output);

    // Initialize the program once (using the first job) to validate it before running.
    ValueSupportFactory valueFactory = createValueFactory(opts);
    JoshSimCommander.ProgramInitResult initResult =
        initializeProgram(opts, valueFactory, geometryFactory, jobs.get(0), output);
    if (initResult.getFailureStep().isPresent()) {
      return RunResult.initFailure(initResult.getFailureStep().get());
    }
    output.printInfo("Validated Josh code at " + opts.scriptFile);

    JoshProgram program = initResult.getProgram().orElseThrow();
    if (!program.getSimulations().hasPrototype(opts.simulation)) {
      output.printError("Could not find simulation: " + opts.simulation);
      return RunResult.simulationNotFound(opts.simulation);
    }

    configureCompatibilityLayer(opts);
    ProgressCalculator progressCalculator = createProgressCalculator(opts, jobs, output);
    GridSpatialInfo spatialInfo = extractSpatialInfo(program, opts.simulation, valueFactory);

    ExecutionSummary summary = executeJobs(opts, jobs, valueFactory, geometryFactory, program,
        spatialInfo, progressCalculator, serialPatches, output);

    reportRunCompletion(jobs, effectiveReplicates, summary, output);

    String message = "Simulation '" + opts.simulation + "' completed: "
        + summary.totalReplicates() + " replicate(s), last step " + summary.lastStep();
    return RunResult.success(message, summary.lastStep(), summary.totalReplicates());
  }

  /**
   * Expands the run into one job per grid-search combination of data files.
   */
  private static List<JoshJob> buildJobs(RunOptions opts, int effectiveReplicates) {
    JoshJobBuilder templateJobBuilder = new JoshJobBuilder()
        .setReplicates(effectiveReplicates)
        .setCustomParameters(opts.customParameters);
    JobVariationParser parser = new JobVariationParser();
    List<JoshJobBuilder> jobBuilders = parser.parseDataFiles(templateJobBuilder, opts.dataFiles);
    return jobBuilders.stream()
        .map(JoshJobBuilder::build)
        .toList();
  }

  /**
   * Prints what the run is about to do (grid-search shape and total simulation count).
   */
  private static void reportPlannedRun(RunOptions opts, List<JoshJob> jobs,
      int effectiveReplicates, OutputOptions output) {
    if (opts.replicateIndex != null) {
      output.printInfo("Running replicate index " + opts.replicateIndex
          + " across " + jobs.size() + " job combination(s)");
    } else {
      output.printInfo("Grid search will execute " + jobs.size() + " job combination(s) "
          + "with " + opts.replicates + " replicate(s) each");
    }
    output.printInfo("Total simulations to run: " + (jobs.size() * effectiveReplicates));
  }

  /**
   * Creates the value factory, wiring in the profiler-enabled resolver factory when requested.
   */
  private static ValueSupportFactory createValueFactory(RunOptions opts) {
    boolean favorBigDecimal = !opts.useFloat64;
    return new ValueSupportFactory(favorBigDecimal, buildValueResolverFactory(opts.enableProfiler));
  }

  /**
   * Chooses the input strategy for a job: working-directory lookup, or a mapped getter when the
   * job supplies explicit file paths.
   */
  private static InputGetterStrategy createInputStrategy(JoshJob job) {
    if (job.getFilePaths().isEmpty()) {
      return new JvmWorkingDirInputGetter();
    }
    return new JvmMappedInputGetter(job.getFilePaths());
  }

  /**
   * Parses and validates the program using the first job's configuration for the initial
   * input/output layer.
   */
  private static JoshSimCommander.ProgramInitResult initializeProgram(RunOptions opts,
      ValueSupportFactory valueFactory, EngineGeometryFactory geometryFactory, JoshJob firstJob,
      OutputOptions output) {
    InputGetterStrategy inputStrategy = createInputStrategy(firstJob);
    TemplateStringRenderer initTemplateRenderer = new TemplateStringRenderer(firstJob, 0);
    InputOutputLayer initInputOutputLayer = new JvmInputOutputLayerBuilder()
        .withReplicate(0)
        .withInputStrategy(inputStrategy)
        .withTemplateRenderer(initTemplateRenderer)
        .withMinioOptions(opts.minioOptions)
        .withMaxDecimalPlaces(opts.csvPrecision)
        .build();

    return JoshSimCommander.getJoshProgram(
        valueFactory,
        geometryFactory,
        opts.scriptFile,
        output,
        initInputOutputLayer
    );
  }

  /**
   * Installs the JVM compatibility layer with the configured export queue capacity.
   */
  private static void configureCompatibilityLayer(RunOptions opts) {
    JvmCompatibilityLayer compatLayer = new JvmCompatibilityLayer();
    compatLayer.setExportQueueCapacity(opts.exportQueueSize);
    CompatibilityLayerKeeper.set(compatLayer);
  }

  /**
   * Builds the progress calculator, falling back to default metadata if extraction fails.
   */
  private static ProgressCalculator createProgressCalculator(RunOptions opts, List<JoshJob> jobs,
      OutputOptions output) {
    SimulationMetadata metadata;
    try {
      metadata = SimulationMetadataExtractor.extractMetadata(opts.scriptFile, opts.simulation);
    } catch (Exception e) {
      // Use default metadata if extraction fails.
      metadata = new SimulationMetadata(0, 10, 11);
      output.printInfo("Using default metadata for progress tracking: " + e.getMessage());
    }
    return new ProgressCalculator(
        metadata.getTotalSteps(),
        jobs.size() * opts.replicates // Total simulations = jobs × replicates
    );
  }

  /**
   * Extracts grid information needed to detect Earth-space simulations (degrees + metric size).
   */
  private static GridSpatialInfo extractSpatialInfo(JoshProgram program, String simulation,
      ValueSupportFactory valueFactory) {
    MutableEntity simEntityRaw = program.getSimulations().getProtoype(simulation).build();
    MutableEntity simEntity = new ShadowingEntity(valueFactory, simEntityRaw, simEntityRaw);
    GridInfoExtractor extractor = new GridInfoExtractor(simEntity, valueFactory);
    boolean hasDegrees = extractor.getStartStr().contains("degree");

    EngineValue sizeValueRaw = extractor.getSize();
    String sizeStr = sizeValueRaw.getUnits().toString();
    boolean sizeMeters = sizeStr.equals("m") || sizeStr.equals("meter") || sizeStr.equals("meters");
    return new GridSpatialInfo(hasDegrees, sizeMeters, sizeValueRaw, extractor);
  }

  /**
   * Runs every replicate of every job, reporting progress, and returns the aggregate outcome.
   */
  private static ExecutionSummary executeJobs(RunOptions opts, List<JoshJob> jobs,
      ValueSupportFactory valueFactory, EngineGeometryFactory geometryFactory, JoshProgram program,
      GridSpatialInfo spatialInfo, ProgressCalculator progressCalculator, boolean serialPatches,
      OutputOptions output) {
    int totalReplicateCount = 0;
    long[] lastStep = {0};
    InputGetterStrategy inputStrategy = createInputStrategy(jobs.get(0));

    for (int jobIndex = 0; jobIndex < jobs.size(); jobIndex++) {
      JoshJob currentJob = jobs.get(jobIndex);
      output.printInfo("Executing job combination " + (jobIndex + 1) + "/" + jobs.size());

      if (!currentJob.getFilePaths().isEmpty()) {
        inputStrategy = new JvmMappedInputGetter(currentJob.getFilePaths());
      }

      int startReplicate = opts.replicateIndex != null ? opts.replicateIndex : opts.replicateStart;
      int endReplicate = opts.replicateIndex != null
          ? opts.replicateIndex + 1 : opts.replicateStart + currentJob.getReplicates();

      for (int currentReplicate = startReplicate; currentReplicate < endReplicate;
           currentReplicate++) {
        int effectiveReplicate = opts.replicateIndex != null ? opts.replicateIndex
            : currentReplicate;
        totalReplicateCount++;

        if (totalReplicateCount > 1) {
          progressCalculator.resetForNextReplicate(totalReplicateCount);
        }

        runReplicate(currentJob, effectiveReplicate, totalReplicateCount > 1,
            valueFactory, geometryFactory, inputStrategy, spatialInfo, program, opts.simulation,
            progressCalculator, opts.outputSteps, serialPatches, opts.minioOptions,
            opts.csvPrecision, output, lastStep);

        ProgressUpdate completion = progressCalculator.updateReplicateCompleted(
            totalReplicateCount);
        output.printInfo(completion.getMessage());
      }

      if (jobIndex < jobs.size() - 1) {
        output.printInfo("Completed job combination " + (jobIndex + 1) + "/" + jobs.size());
      }
    }

    return new ExecutionSummary(totalReplicateCount, lastStep[0]);
  }

  /**
   * Prints the final success summary.
   */
  private static void reportRunCompletion(List<JoshJob> jobs, int effectiveReplicates,
      ExecutionSummary summary, OutputOptions output) {
    output.printInfo("");
    output.printInfo("✓ All simulations completed successfully!");
    output.printInfo("  Total replicates run: " + summary.totalReplicates());
    output.printInfo("  Job combinations: " + jobs.size());
    output.printInfo("  Replicates per job: " + effectiveReplicates);
  }

  /**
   * Builds the appropriate ValueResolverFactory based on whether profiling is enabled.
   *
   * @param enableProfiler True to use timed resolution for evalDuration support, false otherwise.
   * @return A ValueResolverFactory configured for the requested profiling mode.
   */
  private static ValueResolverFactory buildValueResolverFactory(boolean enableProfiler) {
    if (enableProfiler) {
      return new TimedRecursiveValueResolverFactory();
    } else {
      return new RecursiveValueResolverFactory();
    }
  }

  private static void runReplicate(JoshJob currentJob, int currentReplicate, boolean appendMode,
      ValueSupportFactory valueFactory, EngineGeometryFactory geometryFactory,
      InputGetterStrategy inputStrategy, GridSpatialInfo spatialInfo, JoshProgram program,
      String simulation, ProgressCalculator progressCalculator,
      Optional<Set<Integer>> parsedOutputSteps, boolean serialPatches, MinioOptions minioOptions,
      int csvPrecision, OutputOptions output, long[] lastStep) {
    TemplateStringRenderer templateRenderer = new TemplateStringRenderer(currentJob,
        currentReplicate);

    InputOutputLayer inputOutputLayer = buildReplicateLayer(currentReplicate, appendMode,
        valueFactory, inputStrategy, spatialInfo, templateRenderer, minioOptions, csvPrecision);

    MultiFormatExternalGetter externalGetter = new MultiFormatExternalGetter(
        new JshdExternalGetter(inputStrategy, valueFactory),
        new JshdzExternalGetter(inputStrategy, valueFactory)
    );

    JoshSimFacadeUtil.runSimulation(
        valueFactory,
        geometryFactory,
        inputOutputLayer,
        externalGetter,
        program,
        simulation,
        (step) -> {
          lastStep[0] = step;
          ProgressUpdate update = progressCalculator.updateStep(step);
          if (update.shouldReport()) {
            output.printInfo(update.getMessage());
          }
        },
        serialPatches,
        parsedOutputSteps
    );
  }

  /**
   * Builds the input/output layer for a single replicate, adding Earth-space extents when the
   * simulation grid is defined in degrees with a metric cell size.
   */
  private static InputOutputLayer buildReplicateLayer(int currentReplicate, boolean appendMode,
      ValueSupportFactory valueFactory, InputGetterStrategy inputStrategy,
      GridSpatialInfo spatialInfo, TemplateStringRenderer templateRenderer,
      MinioOptions minioOptions, int csvPrecision) {
    JvmInputOutputLayerBuilder builder = new JvmInputOutputLayerBuilder()
        .withReplicate(currentReplicate)
        .withInputStrategy(inputStrategy)
        .withTemplateRenderer(templateRenderer)
        .withMinioOptions(minioOptions)
        .withAppendMode(appendMode)
        .withMaxDecimalPlaces(csvPrecision);

    if (spatialInfo.hasDegrees() && spatialInfo.sizeMeters()) {
      GridInfoExtractor extractor = spatialInfo.extractor();
      PatchBuilderExtentsBuilder extentsBuilder = new PatchBuilderExtentsBuilder();
      ExtentsUtil.addExtents(extentsBuilder, extractor.getStartStr(), true, valueFactory);
      ExtentsUtil.addExtents(extentsBuilder, extractor.getEndStr(), false, valueFactory);
      builder.withEarthSpace(extentsBuilder.build(), spatialInfo.sizeValueRaw().getAsDecimal());
    }

    return builder.build();
  }

  /**
   * Grid information used to decide whether a simulation runs in Earth space.
   *
   * @param hasDegrees   whether the grid is defined in degrees
   * @param sizeMeters   whether the grid cell size is expressed in meters
   * @param sizeValueRaw the raw grid cell size value
   * @param extractor    the extractor providing grid start/end strings
   */
  private record GridSpatialInfo(boolean hasDegrees, boolean sizeMeters, EngineValue sizeValueRaw,
      GridInfoExtractor extractor) {}

  /**
   * Aggregate outcome of executing all jobs and replicates.
   *
   * @param totalReplicates the number of replicates actually run
   * @param lastStep        the last completed step of the final replicate
   */
  private record ExecutionSummary(int totalReplicates, long lastStep) {}

}
