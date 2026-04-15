/**
 * Utility class for preprocessing external data into jshd format.
 *
 * <p>Extracts the core preprocessing logic from {@link PreprocessCommand} so it can be
 * called from both the CLI command and the {@code /preprocessBatch} server handler.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.command;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import org.joshsim.geo.external.ExternalDataReader;
import org.joshsim.geo.external.ExternalDataReaderFactory;
import org.joshsim.geo.external.ExternalGeoMapper;
import org.joshsim.geo.external.ExternalGeoMapperBuilder;
import org.joshsim.geo.external.ExternalSpatialDimensions;
import org.joshsim.geo.external.NearestNeighborInterpolationStrategy;
import org.joshsim.geo.external.NoopExternalCoordinateTransformer;
import org.joshsim.geo.external.readers.JshdExternalDataReader;
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
import org.joshsim.precompute.GridCombiner;
import org.joshsim.precompute.JshdExternalGetter;
import org.joshsim.precompute.PatchKeyConverter;
import org.joshsim.precompute.StreamToPrecomputedGridUtil;
import org.joshsim.util.OutputOptions;
import org.opengis.referencing.crs.CoordinateReferenceSystem;


/**
 * Shared preprocessing logic for converting external data to jshd format.
 *
 * <p>This utility extracts the core preprocessing pipeline from {@link PreprocessCommand}
 * so it can be reused by the {@code /preprocessBatch} server handler. The CLI command
 * delegates to {@link #preprocess} and maps exceptions to exit codes.</p>
 */
public class PreprocessUtil {

  /**
   * Options for preprocessing that correspond to optional CLI flags.
   *
   * <p>Immutable data class holding CRS, coordinate names, timestep filtering,
   * parallel processing, amend mode, and default value parameters.</p>
   */
  public static class PreprocessOptions {
    private final String crsCode;
    private final String horizCoordName;
    private final String vertCoordName;
    private final String timeName;
    private final String timestep;
    private final String defaultValue;
    private final boolean parallel;
    private final boolean amend;

    /**
     * Constructs PreprocessOptions with all parameters.
     *
     * @param crsCode Coordinate reference system code (e.g., {@code EPSG:4326}).
     * @param horizCoordName Name of the horizontal coordinate dimension.
     * @param vertCoordName Name of the vertical coordinate dimension.
     * @param timeName Name of the time dimension.
     * @param timestep Single timestep to process, or empty string for all.
     * @param defaultValue Default fill value for grid spaces, or null.
     * @param parallel Whether to enable parallel patch processing.
     * @param amend Whether to amend an existing output file.
     */
    public PreprocessOptions(String crsCode, String horizCoordName, String vertCoordName,
        String timeName, String timestep, String defaultValue, boolean parallel, boolean amend) {
      this.crsCode = crsCode;
      this.horizCoordName = horizCoordName;
      this.vertCoordName = vertCoordName;
      this.timeName = timeName;
      this.timestep = timestep != null ? timestep : "";
      this.defaultValue = defaultValue;
      this.parallel = parallel;
      this.amend = amend;
    }

    /**
     * Constructs PreprocessOptions with default values.
     */
    public PreprocessOptions() {
      this("EPSG:4326", "lon", "lat", "calendar_year", "", null, false, false);
    }

    public String getCrsCode() {
      return crsCode;
    }

    public String getHorizCoordName() {
      return horizCoordName;
    }

    public String getVertCoordName() {
      return vertCoordName;
    }

    public String getTimeName() {
      return timeName;
    }

    public String getTimestep() {
      return timestep;
    }

    public String getDefaultValue() {
      return defaultValue;
    }

    public boolean isParallel() {
      return parallel;
    }

    public boolean isAmend() {
      return amend;
    }
  }

  private PreprocessUtil() {
    // Static utility class
  }

  /**
   * Preprocesses external data into jshd binary format.
   *
   * <p>Parses the Josh script, extracts grid metadata from the named simulation,
   * maps external geospatial data onto the simulation grid, and serializes the
   * result to a binary jshd file. Optionally amends an existing jshd file.</p>
   *
   * @param scriptFile Path to the Josh script file.
   * @param simulation Name of the simulation to extract grid info from.
   * @param dataFile Path to the input data file (NetCDF, GeoTIFF, jshd).
   * @param variable Variable name or band number to extract.
   * @param unitsStr Units of the data for simulation use.
   * @param outputFile Path where the preprocessed jshd file should be written.
   * @param options Preprocessing options (CRS, coordinates, timestep, etc.).
   * @param output Output options for logging messages.
   * @throws IllegalArgumentException If the script has parse errors, the simulation
   *     is not found, the default value is invalid, or units are unsupported.
   * @throws IOException If reading or writing files fails.
   * @throws Exception If CRS lookup or other processing fails.
   */
  public static void preprocess(File scriptFile, String simulation, String dataFile,
      String variable, String unitsStr, File outputFile, PreprocessOptions options,
      OutputOptions output) throws Exception {
    // Parse program
    JoshSimCommander.ProgramInitResult initResult = JoshSimCommander.getJoshProgram(
        new GridGeometryFactory(),
        scriptFile,
        output
    );

    if (initResult.getFailureStep().isPresent()) {
      throw new IllegalArgumentException(
          "Failed to load Josh script at step: " + initResult.getFailureStep().get()
      );
    }

    JoshProgram program = initResult.getProgram().orElseThrow();
    if (!program.getSimulations().hasPrototype(simulation)) {
      throw new IllegalArgumentException("Could not find simulation: " + simulation);
    }

    // Initialize an external geo mapper
    ValueSupportFactory valueFactory = new ValueSupportFactory();
    ExternalGeoMapperBuilder geoMapperBuilder = new ExternalGeoMapperBuilder(valueFactory);
    geoMapperBuilder.addCrsCode(options.getCrsCode());
    geoMapperBuilder.addInterpolationStrategy(new NearestNeighborInterpolationStrategy());
    geoMapperBuilder.addCoordinateTransformer(new NoopExternalCoordinateTransformer());

    Optional<Long> forcedTimestep = options.getTimestep().isBlank()
        ? Optional.empty()
        : Optional.of(Long.parseLong(options.getTimestep()));

    geoMapperBuilder.addDimensions(
        options.getHorizCoordName(),
        options.getVertCoordName(),
        options.getTimeName()
    );
    if (forcedTimestep.isPresent()) {
      geoMapperBuilder.forceTimestep(forcedTimestep.get());
    }

    final ExternalGeoMapper mapper = geoMapperBuilder.build();
    mapper.setUseParallelProcessing(options.isParallel());

    // If using JSHD input file, validate grid compatibility
    if (dataFile.toLowerCase().endsWith(".jshd")) {
      validateJshdGridCompatibility(valueFactory, dataFile, options.getCrsCode(), output);
    }

    // Get metadata
    MutableEntity simEntityRaw = program.getSimulations().getProtoype(simulation).build();
    MutableEntity simEntity = new ShadowingEntity(valueFactory, simEntityRaw, simEntityRaw);

    // Create grid from streaming data
    GridInfoExtractor extractor = new GridInfoExtractor(simEntity, valueFactory);
    String startStr = extractor.getStartStr();
    String endStr = extractor.getEndStr();
    EngineValue size = extractor.getSize();

    // Build bridge
    CoordinateReferenceSystem crs;
    try {
      crs = CRS.forCode(options.getCrsCode());
    } catch (Exception e) {
      throw new IllegalArgumentException("Failed to read CRS code: " + e.getMessage(), e);
    }
    EngineGeometryFactory geometryFactory = new EarthGeometryFactory(crs);

    InputGetterStrategy inputStrategy = new JvmInputOutputLayerBuilder().build()
        .getInputStrategy();
    EngineBridge bridge = new QueryCacheEngineBridge(
        valueFactory,
        geometryFactory,
        simEntity,
        program.getConverter(),
        program.getPrototypes(),
        new JshdExternalGetter(inputStrategy, valueFactory),
        new JshcConfigGetter(inputStrategy, valueFactory)
    );
    GridFromSimFactory gridFactory = new GridFromSimFactory(bridge);
    PatchSet patchSet = gridFactory.build(simEntity, options.getCrsCode());

    // Check and parse grid
    if (!unitsSupported(size.getUnits().toString())) {
      throw new IllegalArgumentException("Unsupported units for grid size: " + size.getUnits());
    }
    PatchBuilderExtents extents = gridFactory.buildExtents(startStr, endStr);
    PatchKeyConverter patchKeyConverter = new PatchKeyConverter(
        extents,
        size.getAsDecimal()
    );

    long startTimestep = forcedTimestep.orElse(bridge.getStartTimestep());
    long endTimestep = forcedTimestep.orElse(bridge.getEndTimestep());

    // Parse default value if provided, but only use it for non-amend operations
    Optional<Double> parsedDefaultValue = Optional.empty();
    if (options.getDefaultValue() != null && !options.getDefaultValue().trim().isEmpty()
        && !options.isAmend()) {
      try {
        parsedDefaultValue = Optional.of(Double.parseDouble(options.getDefaultValue()));
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException(
            "Invalid default value: " + options.getDefaultValue() + ". Must be a valid number."
        );
      }
    }

    DataGridLayer grid = StreamToPrecomputedGridUtil.streamToGrid(
        valueFactory,
        (timestepVal) -> {
          Stream<Map.Entry<GeoKey, EngineValue>> geoStream;
          try {
            geoStream = mapper.streamVariableTimeStepToPatches(
                dataFile,
                variable,
                (int) timestepVal,
                patchSet
            );
          } catch (IOException e) {
            throw new RuntimeException("Failed to stream on patches: " + e);
          }
          return geoStream.map(
              (entry) -> patchKeyConverter.convert(
                  entry.getKey(),
                  entry.getValue().getAsDecimal()
              )
          );
        },
        ExtentsTransformer.transformToGrid(extents, size.getAsDecimal()),
        startTimestep,
        endTimestep,
        Units.of(unitsStr),
        parsedDefaultValue
    );

    // If amending, combine with existing grid
    DataGridLayer finalGrid = grid;
    if (options.isAmend() && outputFile.exists()) {
      BinaryGridSerializationStrategy deserializer = new BinaryGridSerializationStrategy(
          valueFactory
      );
      try (FileInputStream inputStream = new FileInputStream(outputFile)) {
        DataGridLayer existingGrid = deserializer.deserialize(inputStream);
        GridCombiner combiner = new GridCombiner(valueFactory, geometryFactory);
        finalGrid = combiner.combine(existingGrid, grid);
      } catch (IOException e) {
        throw new IOException("Error reading existing grid file: " + e.getMessage(), e);
      }
    }

    // Serialize to binary file
    BinaryGridSerializationStrategy serializer = new BinaryGridSerializationStrategy(
        valueFactory
    );
    try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
      serializer.serialize(finalGrid, outputStream);
      outputStream.flush();
    } catch (FileNotFoundException e) {
      throw new IOException("Output file not found: " + e.getMessage(), e);
    } catch (IOException e) {
      throw new IOException("Error writing output file: " + e.getMessage(), e);
    }

    output.printInfo("Successfully preprocessed data to " + outputFile);
  }

  private static boolean unitsSupported(String unitsStr) {
    return unitsStr.equals("m") || unitsStr.equals("meter") || unitsStr.equals("meters");
  }

  /**
   * Validates that a JSHD file has compatible grid dimensions for preprocessing.
   *
   * @param valueFactory Factory for creating EngineValue objects.
   * @param jshdFilePath Path to the JSHD file to validate.
   * @param crsCode Coordinate reference system code.
   * @param output Output options for logging.
   * @throws IOException If validation fails or file cannot be read.
   */
  private static void validateJshdGridCompatibility(
      ValueSupportFactory valueFactory,
      String jshdFilePath,
      String crsCode,
      OutputOptions output) throws IOException {

    try (ExternalDataReader jshdReader = ExternalDataReaderFactory.createReader(
        valueFactory, jshdFilePath)) {

      jshdReader.open(jshdFilePath);
      jshdReader.setCrsCode(crsCode);

      ExternalSpatialDimensions dimensions = jshdReader.getSpatialDimensions();
      BigDecimal[] bounds = dimensions.getBounds();

      if (bounds[0] == null || bounds[1] == null || bounds[2] == null || bounds[3] == null) {
        throw new IOException("JSHD file has invalid spatial bounds");
      }

      List<String> variables = jshdReader.getVariableNames();
      if (variables.isEmpty()) {
        throw new IOException("JSHD file contains no readable variables");
      }

      if (jshdReader instanceof JshdExternalDataReader) {
        JshdExternalDataReader jshdSpecificReader = (JshdExternalDataReader) jshdReader;

        BigDecimal minX = jshdSpecificReader.getMinX();
        BigDecimal maxX = jshdSpecificReader.getMaxX();
        BigDecimal minY = jshdSpecificReader.getMinY();
        BigDecimal maxY = jshdSpecificReader.getMaxY();

        if (minX == null || maxX == null || minY == null || maxY == null) {
          throw new IOException("JSHD file has invalid grid coordinates");
        }

        if (minX.compareTo(maxX) >= 0 || minY.compareTo(maxY) >= 0) {
          throw new IOException("JSHD file has invalid grid bounds: min must be less than max");
        }
      }

      output.printInfo("JSHD grid compatibility validation passed");

    } catch (Exception e) {
      if (e instanceof IOException) {
        throw (IOException) e;
      }
      throw new IOException("Failed to validate JSHD grid compatibility: " + e.getMessage(), e);
    }
  }
}
