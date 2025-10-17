
/**
 * Command line interface handler for running Josh simulations.
 *
 * <p>This class implements the 'run' command which executes a specified simulation from a Josh
 * script file. It supports both grid-based and earth-based coordinate reference systems, and can
 * process patches either serially or in parallel.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.command;

import java.io.File;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import org.apache.sis.referencing.CRS;
import org.joshsim.JoshSimCommander;
import org.joshsim.JoshSimFacadeUtil;
import org.joshsim.compat.CompatibilityLayerKeeper;
import org.joshsim.compat.JvmCompatibilityLayer;
import org.joshsim.engine.entity.base.MutableEntity;
import org.joshsim.engine.geometry.EngineGeometryFactory;
import org.joshsim.engine.geometry.ExtentsUtil;
import org.joshsim.engine.geometry.PatchBuilderExtentsBuilder;
import org.joshsim.engine.geometry.grid.GridGeometryFactory;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.engine.value.type.EngineValue;
import org.joshsim.geo.geometry.EarthGeometryFactory;
import org.joshsim.lang.bridge.GridInfoExtractor;
import org.joshsim.lang.bridge.ShadowingEntity;
import org.joshsim.lang.interpret.JoshProgram;
import org.joshsim.lang.io.InputGetterStrategy;
import org.joshsim.lang.io.InputOutputLayer;
import org.joshsim.lang.io.JvmInputOutputLayerBuilder;
import org.joshsim.lang.io.JvmMappedInputGetter;
import org.joshsim.lang.io.JvmWorkingDirInputGetter;
import org.joshsim.pipeline.job.JoshJob;
import org.joshsim.pipeline.job.JoshJobBuilder;
import org.joshsim.pipeline.job.JoshJobFileInfo;
import org.joshsim.pipeline.job.config.JobVariationParser;
import org.joshsim.pipeline.job.config.TemplateStringRenderer;
import org.joshsim.util.MinioOptions;
import org.joshsim.util.OutputOptions;
import org.joshsim.util.OutputStepsParser;
import org.joshsim.util.ProgressCalculator;
import org.joshsim.util.ProgressUpdate;
import org.joshsim.util.SimulationMetadata;
import org.joshsim.util.SimulationMetadataExtractor;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.FactoryException;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;


/**
 * Command handler for executing Josh simulations.
 *
 * <p>Processes command line arguments to run a specified simulation from a Josh script file.
 * Supports configuration of the coordinate reference system and parallel/serial patch processing.
 * Can optionally save results to Minio storage.</p>
 */
@Command(
    name = "run",
    description = "Run a simulation file"
)
public class RunCommand implements Callable<Integer> {
  private static final int MINIO_ERROR_CODE = 100;
  private static final int UNKNOWN_ERROR_CODE = 404;

  @Parameters(index = "0", description = "Path to file to validate")
  private File file;

  @Parameters(index = "1", description = "Simulation to run")
  private String simulation;

  @Option(names = "--crs", description = "Coordinate Reference System", defaultValue = "")
  private String crs;


  @Option(names = "--replicates", description = "Number of replicates to run", defaultValue = "1")
  private int replicates = 1;

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
      names = "--upload-source",
      description = "Upload source .josh file to MinIO after simulation completes",
      defaultValue = "false"
  )
  private boolean uploadSource = false;

  @Option(
      names = "--upload-config",
      description = "Upload config files (.jshc) to MinIO after simulation completes",
      defaultValue = "false"
  )
  private boolean uploadConfig = false;

  @Option(
      names = "--upload-data",
      description = "Upload data files (.jshd) to MinIO after simulation completes",
      defaultValue = "false"
  )
  private boolean uploadData = false;

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
    if (replicates < 1) {
      output.printError("Number of replicates must be at least 1");
      return 1;
    }

    // Parse output steps early for fail-fast validation
    final Optional<Set<Integer>> parsedOutputSteps = parseOutputSteps();
    EngineGeometryFactory geometryFactory;
    if (crs.isEmpty()) {
      geometryFactory = new GridGeometryFactory();
    } else {
      CoordinateReferenceSystem crsRealized;
      try {
        crsRealized = CRS.forCode(crs);
      } catch (FactoryException e) {
        throw new RuntimeException(e);
      }
      geometryFactory = new EarthGeometryFactory(crsRealized);
    }

    // Parse custom parameters from command line
    Map<String, String> customParameters = parseCustomParameters();

    // Create job configurations using JobVariationParser for grid search
    JoshJobBuilder templateJobBuilder = new JoshJobBuilder()
        .setReplicates(replicates)
        .setCustomParameters(customParameters);
    JobVariationParser parser = new JobVariationParser();
    List<JoshJobBuilder> jobBuilders = parser.parseDataFiles(templateJobBuilder, dataFiles);

    // Build all job instances
    List<JoshJob> jobs = jobBuilders.stream()
        .map(JoshJobBuilder::build)
        .toList();

    // Report grid search information
    output.printInfo("Grid search will execute " + jobs.size() + " job combination(s) "
        + "with " + replicates + " replicate(s) each");
    output.printInfo("Total simulations to run: " + (jobs.size() * replicates));

    // Use first job for initialization (all jobs should have compatible structure)
    JoshJob firstJob = jobs.get(0);

    // Create appropriate InputGetterStrategy based on first job configuration
    InputGetterStrategy inputStrategy;
    if (firstJob.getFilePaths().isEmpty()) {
      inputStrategy = new JvmWorkingDirInputGetter();
    } else {
      inputStrategy = new JvmMappedInputGetter(firstJob.getFilePaths());
    }

    // Create template renderer for initialization phase (using first job, replicate 0)
    TemplateStringRenderer initTemplateRenderer = new TemplateStringRenderer(firstJob, 0);

    // Create InputOutputLayer with the chosen strategy (using first replicate for initialization)
    InputOutputLayer initInputOutputLayer = new JvmInputOutputLayerBuilder()
        .withReplicate(0)
        .withInputStrategy(inputStrategy)
        .withTemplateRenderer(initTemplateRenderer)
        .withMinioOptions(minioOptions)
        .build();

    JoshSimCommander.ProgramInitResult initResult = JoshSimCommander.getJoshProgram(
        geometryFactory,
        file,
        output,
        initInputOutputLayer
    );

    if (initResult.getFailureStep().isPresent()) {
      JoshSimCommander.CommanderStepEnum failStep = initResult.getFailureStep().get();
      return switch (failStep) {
        case LOAD -> 1;
        case READ -> 2;
        case PARSE -> 3;
        default -> UNKNOWN_ERROR_CODE;
      };
    }

    output.printInfo("Validated Josh code at " + file);

    JoshProgram program = initResult.getProgram().orElseThrow();
    if (!program.getSimulations().hasPrototype(simulation)) {
      output.printError("Could not find simulation: " + simulation);
      return 4;
    }

    // Extract simulation metadata for progress tracking
    SimulationMetadata metadata;
    try {
      metadata = SimulationMetadataExtractor.extractMetadata(file, simulation);
    } catch (Exception e) {
      // Use default metadata if extraction fails
      metadata = new SimulationMetadata(0, 10, 11);
      output.printInfo("Using default metadata for progress tracking: " + e.getMessage());
    }

    ProgressCalculator progressCalculator = new ProgressCalculator(
        metadata.getTotalSteps(),
        jobs.size() * replicates // Total simulations = jobs × replicates
    );

    // Set up JVM compatibility layer
    JvmCompatibilityLayer compatLayer = new JvmCompatibilityLayer();
    compatLayer.setExportQueueCapacity(exportQueueSize);
    CompatibilityLayerKeeper.set(compatLayer);

    // Set up EngineValueFactory
    boolean favorBigDecimal = !useFloat64;
    EngineValueFactory valueFactory = new EngineValueFactory(favorBigDecimal);

    // Extract grid information for Earth-space detection (similar to JoshSimFacade)
    MutableEntity simEntityRaw = program.getSimulations().getProtoype(simulation).build();
    MutableEntity simEntity = new ShadowingEntity(valueFactory, simEntityRaw, simEntityRaw);
    GridInfoExtractor extractor = new GridInfoExtractor(simEntity, valueFactory);
    boolean hasDegrees = extractor.getStartStr().contains("degree");

    EngineValue sizeValueRaw = extractor.getSize();
    Units sizeUnits = sizeValueRaw.getUnits();
    String sizeStr = sizeUnits.toString();
    boolean sizeMeterAbbreviated = sizeStr.equals("m");
    boolean sizeMetersFull = sizeStr.equals("meter") || sizeStr.equals("meters");
    boolean sizeMeters = sizeMetersFull || sizeMeterAbbreviated;

    // Execute simulation for each job combination and replicate
    int totalSimulationCount = 0;

    for (int jobIndex = 0; jobIndex < jobs.size(); jobIndex++) {
      JoshJob currentJob = jobs.get(jobIndex);
      output.printInfo("Executing job combination " + (jobIndex + 1) + "/" + jobs.size());

      // Update InputGetterStrategy for this job's file mappings
      if (!currentJob.getFilePaths().isEmpty()) {
        inputStrategy = new JvmMappedInputGetter(currentJob.getFilePaths());
      }

      for (int currentReplicate = 0; currentReplicate < currentJob.getReplicates();
           currentReplicate++) {
        totalSimulationCount++;

        // Reset progress tracking for each new simulation (except first)
        if (totalSimulationCount > 1) {
          progressCalculator.resetForNextReplicate(totalSimulationCount);
        }

        // Create TemplateStringRenderer for this job and replicate
        TemplateStringRenderer templateRenderer = new TemplateStringRenderer(currentJob,
            currentReplicate);

        // Create InputOutputLayer with template renderer (similar to JoshSimFacade logic)
        InputOutputLayer inputOutputLayer;
        if (hasDegrees && sizeMeters) {
          PatchBuilderExtentsBuilder extentsBuilder = new PatchBuilderExtentsBuilder();
          ExtentsUtil.addExtents(extentsBuilder, extractor.getStartStr(), true, valueFactory);
          ExtentsUtil.addExtents(extentsBuilder, extractor.getEndStr(), false, valueFactory);
          BigDecimal sizeValuePrimitive = sizeValueRaw.getAsDecimal();
          inputOutputLayer = new JvmInputOutputLayerBuilder()
              .withReplicate(currentReplicate)
              .withEarthSpace(extentsBuilder.build(), sizeValuePrimitive)
              .withInputStrategy(inputStrategy)
              .withTemplateRenderer(templateRenderer)
              .withMinioOptions(minioOptions)
              .build();
        } else {
          inputOutputLayer = new JvmInputOutputLayerBuilder()
              .withReplicate(currentReplicate)
              .withInputStrategy(inputStrategy)
              .withTemplateRenderer(templateRenderer)
              .withMinioOptions(minioOptions)
              .build();
        }

        JoshSimFacadeUtil.runSimulation(
            valueFactory,
            geometryFactory,
            inputOutputLayer,
            program,
            simulation,
            (step) -> {
              ProgressUpdate update = progressCalculator.updateStep(step);
              if (update.shouldReport()) {
                output.printInfo(update.getMessage());
              }
            },
            serialPatches,
            parsedOutputSteps
        );

        // Report replicate completion
        ProgressUpdate completion = progressCalculator.updateReplicateCompleted(
            totalSimulationCount);
        output.printInfo(completion.getMessage());
      }

      // Report job combination completion
      if (jobIndex < jobs.size() - 1) {
        output.printInfo("Completed job combination " + (jobIndex + 1) + "/" + jobs.size());
      }
    }

    // Report overall success
    output.printInfo("");
    output.printInfo("✓ All simulations completed successfully!");
    output.printInfo("  Total simulations run: " + totalSimulationCount);
    output.printInfo("  Job combinations: " + jobs.size());
    output.printInfo("  Replicates per job: " + replicates);

    if (minioOptions.isMinioOutput()) {
      // Upload the josh file if requested
      if (uploadSource) {
        Integer joshResult = saveToMinio("run", file);
        if (joshResult != 0) {
          return joshResult;
        }
      }

      // Upload config files if requested
      if (uploadConfig) {
        Integer configResult = uploadArtifacts(jobs, ".jshc", "run");
        if (configResult != 0) {
          return configResult;
        }
      }

      // Upload data files if requested
      if (uploadData) {
        Integer dataResult = uploadArtifacts(jobs, ".jshd", "run");
        if (dataResult != 0) {
          return dataResult;
        }
      }
    }

    return 0;
  }

  private Integer saveToMinio(String subDirectories, File file) {
    boolean successful = JoshSimCommander.saveToMinio(subDirectories, file, minioOptions, output);
    return successful ? 0 : MINIO_ERROR_CODE;
  }

  /**
   * Uploads artifact files with the given extension from all jobs.
   * If no files are found in jobs, scans the working directory for files with the extension.
   *
   * @param jobs List of jobs to extract files from
   * @param extension File extension to match (e.g., ".jshc", ".jshd")
   * @param subDirectories Subdirectory path in MinIO bucket
   * @return 0 if successful, error code otherwise
   */
  private Integer uploadArtifacts(List<JoshJob> jobs, String extension, String subDirectories) {
    // Collect unique file paths across all jobs
    Set<String> uniqueFilePaths = new HashSet<>();
    for (JoshJob job : jobs) {
      for (JoshJobFileInfo fileInfo : job.getFileInfos().values()) {
        if (fileInfo.getPath().endsWith(extension)) {
          uniqueFilePaths.add(fileInfo.getPath());
        }
      }
    }

    // If no files found in jobs (no --data flag used), scan working directory
    if (uniqueFilePaths.isEmpty()) {
      File workingDir = file.getParentFile();
      if (workingDir == null) {
        workingDir = new File(".");
      }

      File[] filesInDir = workingDir.listFiles((dir, name) -> name.endsWith(extension));
      if (filesInDir != null) {
        for (File f : filesInDir) {
          uniqueFilePaths.add(f.getPath());
        }
      }
    }

    // Upload each unique file
    for (String filePath : uniqueFilePaths) {
      File artifactFile = new File(filePath);
      if (artifactFile.exists()) {
        Integer result = saveToMinio(subDirectories, artifactFile);
        if (result != 0) {
          return result;
        }
      } else {
        output.printError("Artifact file not found: " + filePath);
      }
    }

    return 0;
  }

}
