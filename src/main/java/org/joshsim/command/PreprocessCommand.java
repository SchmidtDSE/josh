/**
 * Command which converts external data to a jshd format.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.command;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.stream.Stream;
import org.apache.sis.referencing.CRS;
import org.joshsim.JoshSimCommander;
import org.joshsim.engine.entity.base.GeoKey;
import org.joshsim.engine.entity.base.MutableEntity;
import org.joshsim.engine.geometry.EngineGeometryFactory;
import org.joshsim.engine.geometry.PatchBuilderExtents;
import org.joshsim.engine.geometry.PatchSet;
import org.joshsim.engine.geometry.grid.GridGeometryFactory;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueFactory;
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
import org.joshsim.lang.io.JvmInputOutputLayer;
import org.joshsim.precompute.BinaryGridSerializationStrategy;
import org.joshsim.precompute.DataGridLayer;
import org.joshsim.precompute.ExtentsTransformer;
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

    // Get metadata
    CoordinateReferenceSystem crs;
    try {
      crs = CRS.forCode(crsCode);
    } catch (Exception e) {
      System.out.println("Failed to read CRS code due to: " + e);
      return 1;
    }
    EngineGeometryFactory engineGeometryFactory = new EarthGeometryFactory(crs);
    MutableEntity simEntityRaw = program.getSimulations().getProtoype(simulation).build();
    MutableEntity simEntity = new ShadowingEntity(simEntityRaw, simEntityRaw);

    // Initialize an external geo mapper
    ExternalGeoMapperBuilder geoMapperBuilder = new ExternalGeoMapperBuilder();
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
    
    ExternalGeoMapper mapper = geoMapperBuilder.build();

    // Create grid from streaming data
    GridInfoExtractor extractor = new GridInfoExtractor(simEntity, EngineValueFactory.getDefault());
    String startStr = extractor.getStartStr();
    String endStr = extractor.getEndStr();
    EngineValue size = extractor.getSize();

    // Build bridge
    EngineBridge bridge = new QueryCacheEngineBridge(
        engineGeometryFactory,
        simEntity,
        program.getConverter(),
        program.getPrototypes(),
        new JshdExternalGetter(new JvmInputOutputLayer().getInputStrategy())
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

    DataGridLayer grid = StreamToPrecomputedGridUtil.streamToGrid(
        EngineValueFactory.getDefault(),
        (timestep) -> {
          System.out.println("Preprocessing: " + timestep);
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
        Units.of(unitsStr)
    );

    // Serialize to binary file
    BinaryGridSerializationStrategy serializer = new BinaryGridSerializationStrategy(
        EngineValueFactory.getDefault()
    );
    try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
      serializer.serialize(grid, outputStream);
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
}
