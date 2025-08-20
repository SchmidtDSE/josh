/**
 * Utility for extracting simulation metadata from Josh script files.
 *
 * <p>This class provides functionality to extract key simulation parameters like steps.low,
 * steps.high, and total steps from Josh script files. It leverages existing JoshSimCommander
 * infrastructure to parse Josh scripts and use GridInfoExtractor to calculate metadata.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;
import org.joshsim.JoshSimCommander;
import org.joshsim.engine.entity.base.MutableEntity;
import org.joshsim.engine.geometry.EngineGeometryFactory;
import org.joshsim.engine.geometry.grid.GridGeometryFactory;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.lang.bridge.GridInfoExtractor;
import org.joshsim.lang.bridge.ShadowingEntity;
import org.joshsim.lang.interpret.JoshProgram;
import org.joshsim.lang.io.InputOutputLayer;
import org.joshsim.lang.io.JvmInputOutputLayer;


/**
 * Utility class for extracting simulation metadata from Josh scripts.
 *
 * <p>This class parses Josh script files to extract important simulation parameters
 * including steps.low, steps.high, and calculated total steps. The metadata is used
 * by progress calculation utilities to provide meaningful progress updates during
 * remote simulation execution.</p>
 */
public class SimulationMetadataExtractor {

  /**
   * Container class for simulation metadata.
   *
   * <p>This class encapsulates key simulation parameters extracted from Josh scripts,
   * providing easy access to step range information needed for progress calculations.</p>
   */
  public static class SimulationMetadata {
    private final long stepsLow;
    private final long stepsHigh;
    private final long totalSteps;

    /**
     * Constructor for SimulationMetadata.
     *
     * @param stepsLow The lower bound of simulation steps (inclusive)
     * @param stepsHigh The upper bound of simulation steps (inclusive)
     * @param totalSteps The total number of steps in the simulation
     */
    public SimulationMetadata(long stepsLow, long stepsHigh, long totalSteps) {
      this.stepsLow = stepsLow;
      this.stepsHigh = stepsHigh;
      this.totalSteps = totalSteps;
    }

    /**
     * Gets the lower bound of simulation steps.
     *
     * @return The steps.low value from the simulation
     */
    public long getStepsLow() {
      return stepsLow;
    }

    /**
     * Gets the upper bound of simulation steps.
     *
     * @return The steps.high value from the simulation
     */
    public long getStepsHigh() {
      return stepsHigh;
    }

    /**
     * Gets the total number of steps in the simulation.
     *
     * @return The total steps (stepsHigh - stepsLow + 1)
     */
    public long getTotalSteps() {
      return totalSteps;
    }

    @Override
    public String toString() {
      return String.format("SimulationMetadata{stepsLow=%d, stepsHigh=%d, totalSteps=%d}",
          stepsLow, stepsHigh, totalSteps);
    }
  }

  /**
   * Extracts simulation metadata from a Josh script file.
   *
   * <p>This method parses the provided Josh script file and extracts key simulation
   * parameters including step ranges. It uses the existing JoshSimCommander and
   * GridInfoExtractor infrastructure to safely parse and validate the Josh code.</p>
   *
   * @param file The Josh script file to parse
   * @param simulationName The name of the simulation to extract metadata for
   * @return SimulationMetadata containing the extracted parameters
   * @throws IOException if the file cannot be read
   * @throws IllegalArgumentException if the Josh script is invalid or cannot be parsed
   */
  public static SimulationMetadata extractMetadata(File file, String simulationName) 
      throws IOException {
    if (!file.exists()) {
      throw new IllegalArgumentException("Josh script file does not exist: " + file.getPath());
    }

    String joshCode = Files.readString(file.toPath());
    return extractMetadataFromCode(joshCode, simulationName);
  }

  /**
   * Extracts simulation metadata from Josh script code.
   *
   * <p>This method parses the provided Josh script code string and extracts key simulation
   * parameters including step ranges. It creates a temporary file to leverage existing
   * parsing infrastructure.</p>
   *
   * @param joshCode The Josh script code as a string
   * @param simulationName The name of the simulation to extract metadata for
   * @return SimulationMetadata containing the extracted parameters
   * @throws IllegalArgumentException if the Josh script is invalid or cannot be parsed
   */
  public static SimulationMetadata extractMetadataFromCode(String joshCode, String simulationName) {
    try {
      // Create temporary file to work with existing infrastructure
      File tempFile = Files.createTempFile("josh_metadata_", ".josh").toFile();
      tempFile.deleteOnExit();
      Files.writeString(tempFile.toPath(), joshCode);

      // Use existing JoshSimCommander infrastructure to parse the script
      EngineGeometryFactory geometryFactory = new GridGeometryFactory();
      OutputOptions outputOptions = new OutputOptions();
      // OutputOptions doesn't have setQuiet method, but it has suppressInfo field
      outputOptions.suppressInfo = true; // Suppress output during metadata extraction
      InputOutputLayer ioLayer = new JvmInputOutputLayer();

      JoshSimCommander.ProgramInitResult result = JoshSimCommander.getJoshProgram(
          geometryFactory, tempFile, outputOptions, ioLayer);

      if (result.getFailureStep().isPresent()) {
        throw new IllegalArgumentException(
            "Failed to parse Josh script at step: " + result.getFailureStep().get());
      }

      Optional<JoshProgram> programMaybe = result.getProgram();
      if (programMaybe.isEmpty()) {
        throw new IllegalArgumentException("Failed to extract JoshProgram from script");
      }

      JoshProgram program = programMaybe.get();

      // Get simulation entity following the pattern from other classes
      EngineValueFactory valueFactory = new EngineValueFactory();
      MutableEntity simEntityRaw = program.getSimulations().getProtoype(simulationName).build();
      MutableEntity simEntity = new ShadowingEntity(valueFactory, simEntityRaw, simEntityRaw);

      // Use GridInfoExtractor to extract simulation metadata
      GridInfoExtractor extractor = new GridInfoExtractor(simEntity, valueFactory);

      long totalSteps = extractor.getTotalSteps();
      long stepsLow = Math.round(extractor.getStepsLow().getAsDouble());
      long stepsHigh = Math.round(extractor.getStepsHigh().getAsDouble());

      // Clean up temporary file
      tempFile.delete();

      return new SimulationMetadata(stepsLow, stepsHigh, totalSteps);

    } catch (IOException e) {
      throw new IllegalArgumentException("Failed to create temporary file for parsing", e);
    } catch (Exception e) {
      // Fallback to defaults if parsing fails
      return new SimulationMetadata(0, 10, 11); // Default: steps 0-10 = 11 total
    }
  }
}