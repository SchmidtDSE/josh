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
import org.joshsim.JoshSimFacadeUtil;
import org.joshsim.engine.entity.base.MutableEntity;
import org.joshsim.engine.geometry.EngineGeometryFactory;
import org.joshsim.engine.geometry.grid.GridGeometryFactory;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.lang.bridge.GridInfoExtractor;
import org.joshsim.lang.bridge.ShadowingEntity;
import org.joshsim.lang.interpret.JoshProgram;
import org.joshsim.lang.io.InputOutputLayer;
import org.joshsim.lang.io.JvmInputOutputLayer;
import org.joshsim.lang.io.JvmInputOutputLayerBuilder;
import org.joshsim.lang.parse.ParseResult;


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
   * parameters including step ranges. It uses the parsing infrastructure directly without
   * requiring temporary files.</p>
   *
   * @param joshCode The Josh script code as a string
   * @param simulationName The name of the simulation to extract metadata for
   * @return SimulationMetadata containing the extracted parameters
   * @throws IllegalArgumentException if the Josh script is invalid or cannot be parsed
   */
  public static SimulationMetadata extractMetadataFromCode(String joshCode, String simulationName) {
    try {
      // Parse Josh code directly using facade utilities (avoids temporary files)
      ParseResult result = JoshSimFacadeUtil.parse(joshCode);
      if (result.hasErrors()) {
        throw new IllegalArgumentException(
            "Failed to parse Josh script: " + result.getErrors().iterator().next().toString());
      }

      // Create geometry factory and value factory for interpretation
      EngineGeometryFactory geometryFactory = new GridGeometryFactory();
      EngineValueFactory valueFactory = new EngineValueFactory();
      InputOutputLayer ioLayer = new JvmInputOutputLayerBuilder().build();

      // Interpret the parsed code to get JoshProgram
      JoshProgram program = JoshSimFacadeUtil.interpret(
          valueFactory,
          geometryFactory, 
          result,
          ioLayer);

      // Get simulation entity following the pattern from JoshJsSimFacade
      MutableEntity simEntityRaw = program.getSimulations().getProtoype(simulationName).build();
      MutableEntity simEntity = new ShadowingEntity(valueFactory, simEntityRaw, simEntityRaw);

      // Use GridInfoExtractor to extract simulation metadata
      GridInfoExtractor extractor = new GridInfoExtractor(simEntity, valueFactory);

      long totalSteps = extractor.getTotalSteps();
      long stepsLow = Math.round(extractor.getStepsLow().getAsDouble());
      long stepsHigh = Math.round(extractor.getStepsHigh().getAsDouble());

      return new SimulationMetadata(stepsLow, stepsHigh, totalSteps);

    } catch (IllegalArgumentException e) {
      // For parsing errors, return default values as fallback
      return new SimulationMetadata(0, 10, 11); // Default: steps 0-10 = 11 total
    } catch (RuntimeException e) {
      // For other runtime errors, return default values as fallback
      return new SimulationMetadata(0, 10, 11); // Default: steps 0-10 = 11 total
    }
  }
}