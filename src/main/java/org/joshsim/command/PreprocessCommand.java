
package org.joshsim.command;

import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.Callable;
import org.joshsim.JoshSimCommander;
import org.joshsim.engine.geometry.grid.GridGeometryFactory;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.geo.external.ExternalDataReader;
import org.joshsim.geo.external.ExternalDataReaderFactory;
import org.joshsim.lang.bridge.GridFromSimFactory;
import org.joshsim.lang.bridge.EngineBridge;
import org.joshsim.lang.interpret.JoshProgram;
import org.joshsim.precompute.BinaryGridSerializationStrategy;
import org.joshsim.precompute.PrecomputedGrid;
import org.joshsim.precompute.StreamToPrecomputedGridUtil;
import org.joshsim.util.OutputOptions;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Parameters;

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
  private File dataFile;

  @Parameters(index = "3", description = "Path where preprocessed jshd file should be written")
  private File outputFile;

  @Mixin
  private OutputOptions output = new OutputOptions();

  @Override
  public Integer call() {
    // First validate and parse the Josh script
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

    try {
      // Create grid factory to get simulation metadata
      EngineBridge bridge = new EngineBridge(program.getConverter(), program.getPrototypes());
      GridFromSimFactory gridFactory = new GridFromSimFactory(bridge);
      
      // Create external data reader
      ExternalDataReader reader = ExternalDataReaderFactory.createReader(dataFile.getPath());
      
      // Create grid from streaming data
      PrecomputedGrid grid = StreamToPrecomputedGridUtil.streamToGrid(
          EngineValueFactory.getDefault(),
          reader::getValueStream,
          gridFactory.build(program.getSimulations().getPrototype(simulation)).getExtents(),
          0,  // minTimestep
          1,  // maxTimestep - only preprocessing one timestep
          reader.getUnits()
      );

      // Serialize to binary file
      BinaryGridSerializationStrategy serializer = new BinaryGridSerializationStrategy();
      try (FileOutputStream fos = new FileOutputStream(outputFile)) {
        serializer.serialize(grid, fos);
      }

      output.printInfo("Successfully preprocessed data to " + outputFile);
      return 0;

    } catch (Exception e) {
      output.printError("Error during preprocessing: " + e.getMessage());
      return 5;
    }
  }
}
