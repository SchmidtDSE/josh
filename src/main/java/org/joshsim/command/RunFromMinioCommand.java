/**
 * Command for running a simulation from MinIO-staged inputs.
 *
 * <p>This command downloads simulation inputs (Josh code, external data) from MinIO,
 * validates that all exports target MinIO, and runs the simulation with results written
 * directly to MinIO. Designed for use by K8s batch pods or any machine with MinIO access.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.command;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import org.joshsim.JoshSimCommander;
import org.joshsim.JoshSimFacadeUtil;
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
import org.joshsim.lang.io.ExportTarget;
import org.joshsim.lang.io.ExportTargetParser;
import org.joshsim.lang.io.InputGetterStrategy;
import org.joshsim.lang.io.InputOutputLayer;
import org.joshsim.lang.io.JvmInputOutputLayerBuilder;
import org.joshsim.lang.io.JvmMappedInputGetter;
import org.joshsim.pipeline.job.JoshJob;
import org.joshsim.pipeline.job.JoshJobBuilder;
import org.joshsim.pipeline.job.config.TemplateStringRenderer;
import org.joshsim.util.MinioHandler;
import org.joshsim.util.MinioOptions;
import org.joshsim.util.OutputOptions;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;


/**
 * Runs a simulation from MinIO-staged inputs.
 *
 * <p>Downloads inputs from a MinIO staging path, validates export paths use the minio://
 * protocol, and executes the simulation. Results are written directly to MinIO via the
 * existing minio:// export system. The JOB_COMPLETION_INDEX environment variable determines
 * the replicate number for indexed K8s Job execution.</p>
 */
@Command(
    name = "runFromMinio",
    description = "Run a simulation from MinIO-staged inputs"
)
public class RunFromMinioCommand implements Callable<Integer> {

  private static final int MINIO_ERROR_CODE = 100;
  private static final int VALIDATION_ERROR_CODE = 101;
  private static final int UNKNOWN_ERROR_CODE = 404;
  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Option(
      names = "--job-id",
      description = "Job ID (UUID prefix in MinIO under batch-jobs/<job-id>/inputs/)",
      required = true
  )
  private String jobId;

  @Mixin
  private MinioOptions minioOptions = new MinioOptions();

  @Mixin
  private OutputOptions output = new OutputOptions();

  @Override
  public Integer call() {
    try {
      // Determine replicate number from K8s indexed job env var
      int replicate = getReplicateNumber();
      output.printInfo("Running replicate " + replicate + " for job " + jobId);

      // Download inputs from MinIO
      MinioHandler minio = new MinioHandler(minioOptions, output);
      File tempDir = Files.createTempDirectory("josh-minio-" + jobId).toFile();

      // Download and parse metadata
      File metadataFile = new File(tempDir, "metadata.json");
      minio.downloadFile("batch-jobs/" + jobId + "/inputs/metadata.json", metadataFile);
      JsonNode metadata = MAPPER.readTree(metadataFile);

      final String simulation = metadata.get("simulation").asText();
      boolean useFloat64 = metadata.has("useFloat64") && metadata.get("useFloat64").asBoolean();

      // Download Josh code
      File codeFile = new File(tempDir, "code.josh");
      minio.downloadFile("batch-jobs/" + jobId + "/inputs/code.josh", codeFile);

      // Download external data files
      Map<String, String> fileMappings = new HashMap<>();
      if (metadata.has("externalFiles")) {
        for (JsonNode fileNode : metadata.get("externalFiles")) {
          String filename = fileNode.asText();
          File localFile = new File(tempDir, filename);
          minio.downloadFile(
              "batch-jobs/" + jobId + "/inputs/" + filename,
              localFile
          );
          fileMappings.put(filename, localFile.getAbsolutePath());
        }
      }

      // Parse Josh program
      InputGetterStrategy inputStrategy = fileMappings.isEmpty()
          ? new org.joshsim.lang.io.JvmWorkingDirInputGetter()
          : new JvmMappedInputGetter(fileMappings);

      JoshJob job = new JoshJobBuilder()
          .setReplicates(1)
          .build();
      TemplateStringRenderer templateRenderer = new TemplateStringRenderer(job, replicate);

      boolean favorBigDecimal = !useFloat64;
      ValueSupportFactory valueFactory = new ValueSupportFactory(favorBigDecimal);
      EngineGeometryFactory geometryFactory = new GridGeometryFactory();

      InputOutputLayer initLayer = new JvmInputOutputLayerBuilder()
          .withReplicate(replicate)
          .withInputStrategy(inputStrategy)
          .withTemplateRenderer(templateRenderer)
          .withMinioOptions(minioOptions)
          .build();

      JoshSimCommander.ProgramInitResult initResult = JoshSimCommander.getJoshProgram(
          valueFactory,
          geometryFactory,
          codeFile,
          output,
          initLayer
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

      JoshProgram program = initResult.getProgram().orElseThrow();
      if (!program.getSimulations().hasPrototype(simulation)) {
        output.printError("Could not find simulation: " + simulation);
        return 4;
      }

      // Validate export paths use minio:// protocol
      Integer validationResult = validateExportPaths(program, simulation, valueFactory);
      if (validationResult != null) {
        return validationResult;
      }

      // Build execution InputOutputLayer with Earth-space if applicable
      MutableEntity simEntityRaw = program.getSimulations().getProtoype(simulation).build();
      MutableEntity simEntity = new ShadowingEntity(valueFactory, simEntityRaw, simEntityRaw);
      GridInfoExtractor extractor = new GridInfoExtractor(simEntity, valueFactory);
      boolean hasDegrees = extractor.getStartStr().contains("degree");

      EngineValue sizeValue = extractor.getSize();
      String sizeStr = sizeValue.getUnits().toString();
      boolean sizeMeters = sizeStr.equals("m") || sizeStr.equals("meter")
          || sizeStr.equals("meters");

      InputOutputLayer execLayer;
      if (hasDegrees && sizeMeters) {
        PatchBuilderExtentsBuilder extentsBuilder = new PatchBuilderExtentsBuilder();
        ExtentsUtil.addExtents(extentsBuilder, extractor.getStartStr(), true, valueFactory);
        ExtentsUtil.addExtents(extentsBuilder, extractor.getEndStr(), false, valueFactory);
        BigDecimal sizePrimitive = sizeValue.getAsDecimal();
        execLayer = new JvmInputOutputLayerBuilder()
            .withReplicate(replicate)
            .withEarthSpace(extentsBuilder.build(), sizePrimitive)
            .withInputStrategy(inputStrategy)
            .withTemplateRenderer(templateRenderer)
            .withMinioOptions(minioOptions)
            .build();
      } else {
        execLayer = new JvmInputOutputLayerBuilder()
            .withReplicate(replicate)
            .withInputStrategy(inputStrategy)
            .withTemplateRenderer(templateRenderer)
            .withMinioOptions(minioOptions)
            .build();
      }

      // Run simulation
      output.printInfo("Starting simulation " + simulation + " (replicate " + replicate + ")");
      JoshSimFacadeUtil.runSimulation(
          valueFactory,
          geometryFactory,
          execLayer,
          program,
          simulation,
          (step) -> output.printInfo("  Step " + step + " complete"),
          false,
          Optional.empty()
      );

      output.printInfo("Replicate " + replicate + " complete for job " + jobId);
      return 0;

    } catch (Exception e) {
      output.printError("runFromMinio failed: " + e.getMessage());
      return MINIO_ERROR_CODE;
    }
  }

  /**
   * Gets the replicate number from the JOB_COMPLETION_INDEX environment variable.
   *
   * @return The replicate number (0-based), defaulting to 0 if env var is not set
   */
  int getReplicateNumber() {
    String indexStr = System.getenv("JOB_COMPLETION_INDEX");
    if (indexStr == null || indexStr.isEmpty()) {
      return 0;
    }
    return Integer.parseInt(indexStr);
  }

  /**
   * Validates that all configured export paths use the minio:// protocol.
   *
   * @param program The parsed Josh program
   * @param simulation The simulation name
   * @param valueFactory The value support factory
   * @return null if valid, error code if invalid
   */
  private Integer validateExportPaths(JoshProgram program, String simulation,
      ValueSupportFactory valueFactory) {
    MutableEntity simEntityRaw = program.getSimulations().getProtoype(simulation).build();
    MutableEntity simEntity = new ShadowingEntity(valueFactory, simEntityRaw, simEntityRaw);
    simEntity.startSubstep("constant");

    String[] exportKeys = {"exportFiles.patch", "exportFiles.meta", "exportFiles.entity"};
    for (String key : exportKeys) {
      Optional<EngineValue> export = simEntity.getAttributeValue(key);
      if (export.isPresent()) {
        String rawPath = export.get().getAsString();
        ExportTarget target = ExportTargetParser.parse(rawPath);
        if (!"minio".equals(target.getProtocol())) {
          output.printError(
              "Export path for " + key + " must use minio:// protocol for MinIO-based execution. "
              + "Found: " + rawPath + ". "
              + "Update your Josh script to use minio:// URIs, e.g.: "
              + "\"minio://bucket/path/to/output.csv\""
          );
          simEntityRaw.endSubstep();
          return VALIDATION_ERROR_CODE;
        }
      }
    }

    simEntityRaw.endSubstep();
    return null;
  }
}
