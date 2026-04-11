/**
 * Command for running a single-timestep preprocess job from MinIO-staged inputs.
 *
 * <p>Downloads a data file and Josh script from MinIO, preprocesses a single timestep
 * determined by JOB_COMPLETION_INDEX, and uploads the resulting .jshd file back to MinIO.
 * Designed for use by K8s batch pods or any machine with MinIO access.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.command;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.stream.Stream;
import org.apache.sis.referencing.CRS;
import org.joshsim.JoshSimCommander;
import org.joshsim.engine.config.JshcConfigGetter;
import org.joshsim.engine.entity.base.GeoKey;
import org.joshsim.engine.entity.base.MutableEntity;
import org.joshsim.engine.geometry.EngineGeometryFactory;
import org.joshsim.engine.geometry.PatchBuilderExtents;
import org.joshsim.engine.geometry.PatchSet;
import org.joshsim.engine.geometry.grid.GridGeometryFactory;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.ValueSupportFactory;
import org.joshsim.engine.value.type.EngineValue;
import org.joshsim.geo.external.ExternalGeoMapper;
import org.joshsim.geo.external.ExternalGeoMapperBuilder;
import org.joshsim.geo.external.NearestNeighborInterpolationStrategy;
import org.joshsim.geo.external.NoopExternalCoordinateTransformer;
import org.joshsim.geo.geometry.EarthGeometryFactory;
import org.joshsim.lang.bridge.EngineBridge;
import org.joshsim.lang.bridge.GridFromSimFactory;
import org.joshsim.lang.bridge.GridInfoExtractor;
import org.joshsim.lang.bridge.QueryCacheEngineBridge;
import org.joshsim.lang.bridge.ShadowingEntity;
import org.joshsim.lang.interpret.JoshProgram;
import org.joshsim.lang.io.InputGetterStrategy;
import org.joshsim.lang.io.JvmInputOutputLayerBuilder;
import org.joshsim.precompute.BinaryGridSerializationStrategy;
import org.joshsim.precompute.DataGridLayer;
import org.joshsim.precompute.ExtentsTransformer;
import org.joshsim.precompute.JshdExternalGetter;
import org.joshsim.precompute.PatchKeyConverter;
import org.joshsim.precompute.StreamToPrecomputedGridUtil;
import org.joshsim.util.MinioHandler;
import org.joshsim.util.MinioOptions;
import org.joshsim.util.OutputOptions;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;


/**
 * Preprocesses a single timestep from MinIO-staged inputs.
 *
 * <p>Downloads a data file and Josh script from MinIO, runs preprocessing for one timestep
 * (determined by JOB_COMPLETION_INDEX + stepsLow from metadata), and uploads the resulting
 * .jshd file back to MinIO. Enables distributed preprocessing across K8s indexed jobs or
 * manual parallelism on multiple machines.</p>
 */
@Command(
    name = "preprocessFromMinio",
    description = "Preprocess a single timestep from MinIO-staged inputs"
)
public class PreprocessFromMinioCommand implements Callable<Integer> {

  private static final int MINIO_ERROR_CODE = 100;
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
      int timestepIndex = getTimestepIndex();
      output.printInfo("Preprocessing timestep index " + timestepIndex + " for job " + jobId);

      // Download inputs from MinIO
      MinioHandler minio = new MinioHandler(minioOptions, output);
      File tempDir = Files.createTempDirectory("josh-preprocess-" + jobId).toFile();

      // Download and parse metadata
      File metadataFile = new File(tempDir, "metadata.json");
      minio.downloadFile("batch-jobs/" + jobId + "/inputs/metadata.json", metadataFile);
      JsonNode metadata = MAPPER.readTree(metadataFile);

      final String simulation = metadata.get("simulation").asText();
      final String dataFileName = metadata.get("dataFile").asText();
      final String variable = metadata.get("variable").asText();
      final String unitsStr = metadata.get("units").asText();
      final String crsCode = metadata.has("crs") ? metadata.get("crs").asText() : "EPSG:4326";
      final String horizCoord = metadata.has("xCoord") ? metadata.get("xCoord").asText() : "lon";
      final String vertCoord = metadata.has("yCoord") ? metadata.get("yCoord").asText() : "lat";
      final String timeDim = metadata.has("timeDim") ? metadata.get("timeDim").asText()
          : "calendar_year";
      long stepsLow = metadata.has("stepsLow") ? metadata.get("stepsLow").asLong() : 0;

      Optional<Double> defaultValue = Optional.empty();
      if (metadata.has("defaultValue") && !metadata.get("defaultValue").isNull()) {
        defaultValue = Optional.of(metadata.get("defaultValue").asDouble());
      }

      // Compute the actual timestep to process
      long timestep = stepsLow + timestepIndex;
      output.printInfo("Processing timestep " + timestep + " (index " + timestepIndex
          + ", stepsLow " + stepsLow + ")");

      // Download Josh script
      File scriptFile = new File(tempDir, "code.josh");
      minio.downloadFile("batch-jobs/" + jobId + "/inputs/code.josh", scriptFile);

      // Download data file
      File dataFile = new File(tempDir, dataFileName);
      minio.downloadFile("batch-jobs/" + jobId + "/inputs/" + dataFileName, dataFile);

      // Parse Josh program
      JoshSimCommander.ProgramInitResult initResult = JoshSimCommander.getJoshProgram(
          new GridGeometryFactory(), scriptFile, output
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

      // Run preprocessing for this single timestep
      File outputJshd = new File(tempDir, "partial-" + timestepIndex + ".jshd");
      int result = runPreprocessTimestep(
          program, simulation, dataFile.getAbsolutePath(), variable, unitsStr,
          crsCode, horizCoord, vertCoord, timeDim, timestep, defaultValue, outputJshd
      );
      if (result != 0) {
        return result;
      }

      // Upload result to MinIO
      String outputPath = "batch-jobs/" + jobId + "/results/partial-" + timestepIndex + ".jshd";
      boolean uploaded = minio.uploadFile(outputJshd, outputPath);
      if (!uploaded) {
        output.printError("Failed to upload result to MinIO");
        return MINIO_ERROR_CODE;
      }

      output.printInfo("Timestep " + timestep + " complete for job " + jobId);
      return 0;

    } catch (Exception e) {
      output.printError("preprocessFromMinio failed: " + e.getMessage());
      return MINIO_ERROR_CODE;
    }
  }

  /**
   * Gets the timestep index from the JOB_COMPLETION_INDEX environment variable.
   *
   * @return The timestep index (0-based), defaulting to 0 if env var is not set
   */
  int getTimestepIndex() {
    String indexStr = System.getenv("JOB_COMPLETION_INDEX");
    if (indexStr == null || indexStr.isEmpty()) {
      return 0;
    }
    return Integer.parseInt(indexStr);
  }

  /**
   * Runs preprocessing for a single timestep, mirroring PreprocessCommand logic.
   */
  private int runPreprocessTimestep(JoshProgram program, String simulation, String dataFilePath,
      String variable, String unitsStr, String crsCode, String horizCoord, String vertCoord,
      String timeDim, long timestep, Optional<Double> defaultValue, File outputFile) {

    ValueSupportFactory valueFactory = new ValueSupportFactory();

    // Build geo mapper for single timestep
    ExternalGeoMapperBuilder geoMapperBuilder = new ExternalGeoMapperBuilder(valueFactory);
    geoMapperBuilder.addCrsCode(crsCode);
    geoMapperBuilder.addInterpolationStrategy(new NearestNeighborInterpolationStrategy());
    geoMapperBuilder.addCoordinateTransformer(new NoopExternalCoordinateTransformer());
    geoMapperBuilder.addDimensions(horizCoord, vertCoord, timeDim);
    geoMapperBuilder.forceTimestep(timestep);

    final ExternalGeoMapper mapper = geoMapperBuilder.build();
    mapper.setUseParallelProcessing(true);

    // Build simulation entity and grid
    MutableEntity simEntityRaw = program.getSimulations().getProtoype(simulation).build();
    MutableEntity simEntity = new ShadowingEntity(valueFactory, simEntityRaw, simEntityRaw);
    GridInfoExtractor extractor = new GridInfoExtractor(simEntity, valueFactory);
    EngineValue size = extractor.getSize();

    CoordinateReferenceSystem crs;
    try {
      crs = CRS.forCode(crsCode);
    } catch (Exception e) {
      output.printError("Failed to read CRS code: " + e);
      return 1;
    }
    EngineGeometryFactory geometryFactory = new EarthGeometryFactory(crs);

    InputGetterStrategy inputStrategy = new JvmInputOutputLayerBuilder().build().getInputStrategy();
    EngineBridge bridge = new QueryCacheEngineBridge(
        valueFactory, geometryFactory, simEntity, program.getConverter(),
        program.getPrototypes(),
        new JshdExternalGetter(inputStrategy, valueFactory),
        new JshcConfigGetter(inputStrategy, valueFactory)
    );
    GridFromSimFactory gridFactory = new GridFromSimFactory(bridge);
    PatchSet patchSet = gridFactory.build(simEntity, crsCode);

    String startStr = extractor.getStartStr();
    String endStr = extractor.getEndStr();
    PatchBuilderExtents extents = gridFactory.buildExtents(startStr, endStr);
    PatchKeyConverter patchKeyConverter = new PatchKeyConverter(extents, size.getAsDecimal());

    // Stream data for single timestep
    DataGridLayer grid = StreamToPrecomputedGridUtil.streamToGrid(
        valueFactory,
        (ts) -> {
          Stream<Map.Entry<GeoKey, EngineValue>> geoStream;
          try {
            geoStream = mapper.streamVariableTimeStepToPatches(
                dataFilePath, variable, (int) ts, patchSet
            );
          } catch (IOException e) {
            throw new RuntimeException("Failed to stream patches: " + e);
          }
          return geoStream.map(
              (entry) -> patchKeyConverter.convert(
                  entry.getKey(), entry.getValue().getAsDecimal()
              )
          );
        },
        ExtentsTransformer.transformToGrid(extents, size.getAsDecimal()),
        timestep,
        timestep,
        Units.of(unitsStr),
        defaultValue
    );

    // Serialize to binary file
    BinaryGridSerializationStrategy serializer = new BinaryGridSerializationStrategy(valueFactory);
    try (FileOutputStream fos = new FileOutputStream(outputFile)) {
      serializer.serialize(grid, fos);
      fos.flush();
    } catch (IOException e) {
      output.printError("Failed to write .jshd: " + e.getMessage());
      return 5;
    }

    output.printInfo("Preprocessed timestep " + timestep + " to " + outputFile.getName());
    return 0;
  }
}
