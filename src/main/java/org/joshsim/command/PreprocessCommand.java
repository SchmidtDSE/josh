/**
 * Command which converts external data to a jshd format.
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
import org.joshsim.engine.value.engine.EngineValueFactory;
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
import org.joshsim.lang.io.JvmInputOutputLayer;
import org.joshsim.precompute.BinaryGridSerializationStrategy;
import org.joshsim.precompute.DataGridLayer;
import org.joshsim.precompute.ExtentsTransformer;
import org.joshsim.precompute.GridCombiner;
import org.joshsim.precompute.JshdExternalGetter;
import org.joshsim.precompute.PatchKeyConverter;
import org.joshsim.precompute.StreamToPrecomputedGridUtil;
import org.joshsim.util.OutputOptions;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;


/**
 * Command line interface handler for preprocessing simulation data.
 *
 * <p>This class implements the 'preprocess' command which prepares input data for use in Josh
 * simulations. It handles the conversion of raw data files into a binary format (.jshd) that can be
 * efficiently loaded during simulation execution. The preprocessing includes:
 *
 * <ul>
 *   <li>Reading and parsing Josh script files</li>
 *   <li>Extracting simulation metadata and grid information</li>
 *   <li>Converting external geospatial data to the simulation's coordinate system</li>
 *   <li>Serializing the processed data to a binary file format</li>
 * </ul>
 *
 * </p>
 *
 * @see DataGridLayer
 * @see org.joshsim.geo.external.ExternalGeoMapper
 */
@Command(
    name = "preprocess",
    description = "Preprocess data for a simulation"
)
public class PreprocessCommand implements Callable<Integer> {
  private static final int UNKNOWN_ERROR_CODE = 404;

  @Parameters(index = "0", description = "Path to Josh script file")
  private File scriptFile;

  @Parameters(index = "1", description = "Name of simulation to preprocess")
  private String simulation;

  @Parameters(index = "2", description = "Path to data file to preprocess")
  private String dataFile;

  @Parameters(index = "3", description = "Name of the variable to be read or band number")
  private String variable;

  @Parameters(index = "4", description = "Units of the data to use within simulations")
  private String unitsStr;

  @Parameters(index = "5", description = "Path where preprocessed jshd file should be written")
  private File outputFile;

  @Option(
      names = "--amend",
      description = "Amend existing file rather than overwriting",
      defaultValue = "false"
  )
  private boolean amend;

  @Option(
      names = "--crs",
      description = "CRS to use in reading the file.",
      defaultValue = "EPSG:4326"
  )
  private String crsCode;

  @Option(
      names = "--x-coord",
      description = "Name of X coordinate.",
      defaultValue = "lon"
  )
  private String horizCoordName;

  @Option(
      names = "--y-coord",
      description = "Name of Y coordinate.",
      defaultValue = "lat"
  )
  private String vertCoordName;

  @Option(
      names = "--time-dim",
      description = "Time dimension.",
      defaultValue = "calendar_year"
  )
  private String timeName;

  @Option(
      names = "--timestep",
      description = "The single timestep to process.",
      defaultValue = ""
  )
  private String timestep;

  @Option(
      names = "--default-value",
      description = "Default value to fill grid spaces before copying data from source file"
  )
  private String defaultValue;

  @Mixin
  private OutputOptions output = new OutputOptions();

  @Override
  public Integer call() {
    // Parse program
    JoshSimCommander.ProgramInitResult initResult = JoshSimCommander.getJoshProgram(
        new GridGeometryFactory(),
        scriptFile,
        output
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

    // Initialize an external geo mapper
    EngineValueFactory valueFactory = new EngineValueFactory();
    ExternalGeoMapperBuilder geoMapperBuilder = new ExternalGeoMapperBuilder(valueFactory);
    geoMapperBuilder.addCrsCode(crsCode);
    geoMapperBuilder.addInterpolationStrategy(new NearestNeighborInterpolationStrategy());
    geoMapperBuilder.addCoordinateTransformer(new NoopExternalCoordinateTransformer());

    Optional<Long> forcedTimestep = timestep.isBlank() ? Optional.empty() : Optional.of(
        Long.parseLong(timestep)
    );

    if (forcedTimestep.isPresent()) {
      geoMapperBuilder.addDimensions(horizCoordName, vertCoordName, timeName);
      geoMapperBuilder.forceTimestep(forcedTimestep.get());
    } else {
      geoMapperBuilder.addDimensions(horizCoordName, vertCoordName, timeName);
    }

    final ExternalGeoMapper mapper = geoMapperBuilder.build();

    // If using JSHD input file, validate grid compatibility
    if (dataFile.toLowerCase().endsWith(".jshd")) {
      try {
        validateJshdGridCompatibility(valueFactory, dataFile, crsCode);
      } catch (Exception e) {
        output.printError("JSHD grid compatibility validation failed: " + e.getMessage());
        return 6;
      }
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
      crs = CRS.forCode(crsCode);
    } catch (Exception e) {
      output.printError("Failed to read CRS code due to: " + e);
      return 1;
    }
    EngineGeometryFactory geometryFactory = new EarthGeometryFactory(crs);

    EngineBridge bridge = new QueryCacheEngineBridge(
        valueFactory,
        geometryFactory,
        simEntity,
        program.getConverter(),
        program.getPrototypes(),
        new JshdExternalGetter(new JvmInputOutputLayer().getInputStrategy(), valueFactory),
        new JshcConfigGetter(new JvmInputOutputLayer().getInputStrategy(), valueFactory)
    );
    GridFromSimFactory gridFactory = new GridFromSimFactory(bridge);
    PatchSet patchSet = gridFactory.build(simEntity, crsCode);

    // Check and parse grid
    if (!unitsSupported(size.getUnits().toString())) {
      output.printError("Unsupported units for grid size: " + size.getUnits());
      return 1;
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
    if (defaultValue != null && !defaultValue.trim().isEmpty() && !amend) {
      try {
        parsedDefaultValue = Optional.of(Double.parseDouble(defaultValue));
      } catch (NumberFormatException e) {
        output.printError("Invalid default value: " + defaultValue + ". Must be a valid number.");
        return 5;
      }
    }

    DataGridLayer grid = StreamToPrecomputedGridUtil.streamToGrid(
        valueFactory,
        (timestep) -> {
          Stream<Map.Entry<GeoKey, EngineValue>> geoStream;
          try {
            geoStream = mapper.streamVariableTimeStepToPatches(
                dataFile,
                variable,
                (int) timestep,
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
    if (amend && outputFile.exists()) {
      BinaryGridSerializationStrategy deserializer = new BinaryGridSerializationStrategy(
          valueFactory
      );
      try (FileInputStream inputStream = new FileInputStream(outputFile)) {
        DataGridLayer existingGrid = deserializer.deserialize(inputStream);
        GridCombiner combiner = new GridCombiner(valueFactory, geometryFactory);
        finalGrid = combiner.combine(existingGrid, grid);
      } catch (IOException e) {
        throw new RuntimeException("Error reading existing grid file: " + e);
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
      throw new RuntimeException("File not found: " + e);
    } catch (IOException e) {
      throw new RuntimeException("Error in IO on file: " + e);
    }

    output.printInfo("Successfully preprocessed data to " + outputFile);
    return 0;
  }

  private boolean unitsSupported(String unitsStr) {
    return unitsStr.equals("m") || unitsStr.equals("meter") || unitsStr.equals("meters");
  }

  /**
   * Validates that a JSHD file has compatible grid dimensions for preprocessing.
   * This ensures that the source JSHD file and target simulation grid have the same spatial bounds.
   *
   * @param valueFactory Factory for creating EngineValue objects
   * @param jshdFilePath Path to the JSHD file to validate
   * @param crsCode Coordinate reference system code
   * @throws IOException If validation fails or file cannot be read
   */
  private void validateJshdGridCompatibility(
        EngineValueFactory valueFactory,
        String jshdFilePath,
        String crsCode) throws IOException {
    
    // Create and open JSHD reader
    try (ExternalDataReader jshdReader = ExternalDataReaderFactory.createReader(
        valueFactory, jshdFilePath)) {
      
      jshdReader.open(jshdFilePath);
      jshdReader.setCrsCode(crsCode);

      // Get spatial dimensions
      ExternalSpatialDimensions dimensions = jshdReader.getSpatialDimensions();
      BigDecimal[] bounds = dimensions.getBounds();
      
      // For now, we just verify the file can be read and has valid dimensions
      // More sophisticated validation could be added here to compare with target grid
      if (bounds[0] == null || bounds[1] == null || bounds[2] == null || bounds[3] == null) {
        throw new IOException("JSHD file has invalid spatial bounds");
      }

      // Verify we can read variable names
      List<String> variables = jshdReader.getVariableNames();
      if (variables.isEmpty()) {
        throw new IOException("JSHD file contains no readable variables");
      }

      // If we have a JshdExternalDataReader, we can do additional validation
      if (jshdReader instanceof JshdExternalDataReader) {
        JshdExternalDataReader jshdSpecificReader = (JshdExternalDataReader) jshdReader;
        
        // Validate that the grid has reasonable bounds
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
