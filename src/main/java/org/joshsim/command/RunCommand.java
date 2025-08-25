
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
import org.joshsim.pipeline.job.config.JobVariationParser;
import org.joshsim.pipeline.job.config.TemplateStringRenderer;
import org.joshsim.util.MinioOptions;
import org.joshsim.util.OutputOptions;
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

  @Override
  public Integer call() {
    // Validate replicates parameter
    if (replicates < 1) {
      output.printError("Number of replicates must be at least 1");
      return 1;
    }
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

    // Create job configuration using JobVariationParser
    JoshJobBuilder jobBuilder = new JoshJobBuilder().setReplicates(replicates);
    JobVariationParser parser = new JobVariationParser();
    JoshJob job = parser.parseDataFiles(jobBuilder, dataFiles).build();

    // Create appropriate InputGetterStrategy based on job configuration
    InputGetterStrategy inputStrategy;
    if (job.getFilePaths().isEmpty()) {
      inputStrategy = new JvmWorkingDirInputGetter();
    } else {
      inputStrategy = new JvmMappedInputGetter(job.getFilePaths());
    }

    // Create template renderer for initialization phase (using replicate 0)
    TemplateStringRenderer initTemplateRenderer = new TemplateStringRenderer(job, 0);

    // Create InputOutputLayer with the chosen strategy (using first replicate for initialization)
    InputOutputLayer initInputOutputLayer = new JvmInputOutputLayerBuilder()
        .withReplicate(0)
        .withInputStrategy(inputStrategy)
        .withTemplateRenderer(initTemplateRenderer)
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

    boolean favorBigDecimal = !useFloat64;

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
        job.getReplicates()
    );

    // Set up JVM compatibility layer
    CompatibilityLayerKeeper.set(new JvmCompatibilityLayer());

    // Set up EngineValueFactory
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

    // Execute simulation for each replicate
    for (int currentReplicate = 0; currentReplicate < job.getReplicates(); currentReplicate++) {
      // Reset progress tracking for each new replicate (except first)
      if (currentReplicate > 0) {
        progressCalculator.resetForNextReplicate(currentReplicate + 1);
      }

      // Create TemplateStringRenderer for this replicate
      TemplateStringRenderer templateRenderer = new TemplateStringRenderer(job, currentReplicate);

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
            .build();
      } else {
        inputOutputLayer = new JvmInputOutputLayerBuilder()
            .withReplicate(currentReplicate)
            .withInputStrategy(inputStrategy)
            .withTemplateRenderer(templateRenderer)
            .build();
      }

      final int replicateNum = currentReplicate;
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
          serialPatches
      );

      // Report replicate completion (except for the last replicate)
      if (currentReplicate < job.getReplicates() - 1) {
        ProgressUpdate completion = progressCalculator.updateReplicateCompleted(
            currentReplicate + 1);
        output.printInfo(completion.getMessage());
      }
    }

    if (minioOptions.isMinioOutput()) {
      return saveToMinio("run", file);
    }

    return 0;
  }

  private Integer saveToMinio(String subDirectories, File file) {
    boolean successful = JoshSimCommander.saveToMinio(subDirectories, file, minioOptions, output);
    return successful ? 0 : MINIO_ERROR_CODE;
  }

}
