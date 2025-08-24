
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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import org.apache.sis.referencing.CRS;
import org.joshsim.JoshSimCommander;
import org.joshsim.JoshSimFacade;
import org.joshsim.engine.geometry.EngineGeometryFactory;
import org.joshsim.engine.geometry.grid.GridGeometryFactory;
import org.joshsim.geo.geometry.EarthGeometryFactory;
import org.joshsim.lang.interpret.JoshProgram;
import org.joshsim.lang.io.InputGetterStrategy;
import org.joshsim.lang.io.InputOutputLayer;
import org.joshsim.lang.io.JvmInputOutputLayerBuilder;
import org.joshsim.lang.io.JvmMappedInputGetter;
import org.joshsim.lang.io.JvmWorkingDirInputGetter;
import org.joshsim.util.MinioOptions;
import org.joshsim.util.OutputOptions;
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

  @Option(names = "--replicate", description = "Replicate number", defaultValue = "0")
  private int replicateNumber;

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
      description = "Specify external data files to include (format: filename=path)",
      split = ","
  )
  private String[] dataFiles = new String[0];

  @Override
  public Integer call() {
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

    // Create appropriate InputGetterStrategy based on --data option
    InputGetterStrategy inputStrategy;
    if (dataFiles.length == 0) {
      inputStrategy = new JvmWorkingDirInputGetter();
    } else {
      Map<String, String> fileMapping = parseDataFiles(dataFiles);
      inputStrategy = new JvmMappedInputGetter(fileMapping);
    }

    // Create InputOutputLayer with the chosen strategy
    InputOutputLayer inputOutputLayer = new JvmInputOutputLayerBuilder()
        .withReplicate(replicateNumber)
        .withInputStrategy(inputStrategy)
        .build();

    JoshSimCommander.ProgramInitResult initResult = JoshSimCommander.getJoshProgram(
        geometryFactory,
        file,
        output,
        inputOutputLayer
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
    JoshSimFacade.runSimulation(
        geometryFactory,
        program,
        simulation,
        (step) -> output.printInfo(String.format("Completed step %d.", step)),
        serialPatches,
        replicateNumber,
        favorBigDecimal
    );

    if (minioOptions.isMinioOutput()) {
      return saveToMinio("run", file);
    }

    return 0;
  }

  private Integer saveToMinio(String subDirectories, File file) {
    boolean successful = JoshSimCommander.saveToMinio(subDirectories, file, minioOptions, output);
    return successful ? 0 : MINIO_ERROR_CODE;
  }

  /**
   * Parse the data files option into a mapping from filename to path.
   *
   * @param dataFiles Array of data file specifications in format "filename=path".
   * @return Map from filename to path.
   * @throws IllegalArgumentException if any data file specification is invalid.
   */
  private Map<String, String> parseDataFiles(String[] dataFiles) {
    Map<String, String> mapping = new HashMap<>();
    for (String dataFile : dataFiles) {
      String[] parts = dataFile.split("=", 2);
      if (parts.length != 2) {
        throw new IllegalArgumentException("Invalid data file format: " + dataFile
            + ". Expected format: filename=path");
      }
      mapping.put(parts[0].trim(), parts[1].trim());
    }
    return mapping;
  }
}
