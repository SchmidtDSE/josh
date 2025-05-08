
package org.joshsim.command;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.Callable;
import org.apache.sis.referencing.CRS;
import org.joshsim.JoshSimCommander;
import org.joshsim.engine.entity.base.MutableEntity;
import org.joshsim.engine.geometry.EngineGeometryFactory;
import org.joshsim.engine.geometry.PatchSet;
import org.joshsim.engine.geometry.grid.GridGeometryFactory;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.geo.external.ExternalGeoMapper;
import org.joshsim.geo.external.ExternalGeoMapperBuilder;
import org.joshsim.geo.external.GridExternalCoordinateTransformer;
import org.joshsim.geo.external.NearestNeighborInterpolationStrategy;
import org.joshsim.geo.geometry.EarthGeometryFactory;
import org.joshsim.lang.bridge.EngineBridge;
import org.joshsim.lang.bridge.GridFromSimFactory;
import org.joshsim.lang.bridge.GridInfoExtractor;
import org.joshsim.lang.bridge.QueryCacheEngineBridge;
import org.joshsim.lang.bridge.ShadowingEntity;
import org.joshsim.lang.interpret.JoshProgram;
import org.joshsim.precompute.BinaryGridSerializationStrategy;
import org.joshsim.precompute.PrecomputedGrid;
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
 * @see org.joshsim.precompute.PrecomputedGrid
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

    EngineBridge bridge = new QueryCacheEngineBridge(
        engineGeometryFactory,
        simEntity,
        program.getConverter(),
        program.getPrototypes()
    );
    GridFromSimFactory gridFactory = new GridFromSimFactory(bridge);
    PatchSet patchSet = gridFactory.build(simEntity);

    // Initialize an external geo mapper
    ExternalGeoMapperBuilder geoMapperBuilder = new ExternalGeoMapperBuilder();
    geoMapperBuilder.addCrsCode(crsCode);
    geoMapperBuilder.addInterpolationStrategy(new NearestNeighborInterpolationStrategy());
    geoMapperBuilder.addCoordinateTransformer(new GridExternalCoordinateTransformer());
    geoMapperBuilder.addDimensions(horizCoordName, vertCoordName, timeName);
    ExternalGeoMapper mapper = geoMapperBuilder.build();

    // Create grid from streaming data
    GridInfoExtractor extractor = new GridInfoExtractor(simEntity, new EngineValueFactory());
    String startStr = extractor.getStartStr();
    String endStr = extractor.getEndStr();

    PrecomputedGrid grid = StreamToPrecomputedGridUtil.streamToGrid(
        EngineValueFactory.getDefault(),
        (timestep) -> {
          try {
            return mapper.streamVariableTimeStepToPatches(
                dataFile,
                variable,
                (int) timestep,
                patchSet
            );
          } catch (IOException e) {
            throw new RuntimeException("Failed to stream on patches: " + e);
          }
        },
        gridFactory.buildExtents(startStr, endStr),
        bridge.getStartTimestep(),
        bridge.getEndTimestep(),
        new Units(unitsStr)
    );

    // Serialize to binary file
    BinaryGridSerializationStrategy serializer = new BinaryGridSerializationStrategy();
    try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
      serializer.serialize(grid, outputStream);
    } catch (FileNotFoundException e) {
      throw new RuntimeException("File not found: " + e);
    } catch (IOException e) {
      throw new RuntimeException("Error in IO on file: " + e);
    }

    output.printInfo("Successfully preprocessed data to " + outputFile);
    return 0;
  }
}
