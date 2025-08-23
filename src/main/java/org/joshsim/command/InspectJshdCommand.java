/**
 * Command which inspects values in jshd format files at specific coordinates.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.command;

import java.io.File;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.Callable;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.engine.value.type.EngineValue;
import org.joshsim.geo.external.readers.JshdExternalDataReader;
import org.joshsim.util.OutputOptions;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Parameters;


/**
 * Command line interface handler for inspecting JSHD file values at specific coordinates.
 *
 * <p>This class implements the 'inspectJshd' command which allows users to examine values stored
 * in JSHD binary files at specific grid coordinates and time points. This functionality is useful
 * for debugging preprocessed data files and validating that data has been correctly stored.
 *
 * <p>The command takes the following parameters:
 * <ul>
 *   <li>jshd file path - Path to the JSHD file to inspect</li>
 *   <li>variable name - Name of the variable to read (typically "data" for JSHD files)</li>
 *   <li>time step - The time point to inspect</li>
 *   <li>x coordinate - X coordinate in grid space</li>
 *   <li>y coordinate - Y coordinate in grid space</li>
 * </ul>
 *
 * <p>The command outputs the value found at the specified location along with its units,
 * or an appropriate error message if the value cannot be found or the inputs are invalid.
 *
 * @see org.joshsim.geo.external.readers.JshdExternalDataReader
 * @see org.joshsim.precompute.DoublePrecomputedGrid
 */
@Command(
    name = "inspectJshd",
    description = "Inspect values in JSHD files at specific coordinates"
)
public class InspectJshdCommand implements Callable<Integer> {

  @Parameters(index = "0", description = "Path to JSHD file to inspect")
  private File jshdFile;

  @Parameters(index = "1", description = "Name of the variable to inspect")
  private String variable;

  @Parameters(index = "2", description = "Time step to inspect")
  private String timestep;

  @Parameters(index = "3", description = "X coordinate (grid space)")
  private String xcoordinate;

  @Parameters(index = "4", description = "Y coordinate (grid space)")
  private String ycoordinate;

  @Mixin
  private OutputOptions output = new OutputOptions();

  @Override
  public Integer call() {
    // Validate file exists
    if (!jshdFile.exists()) {
      output.printError("Could not find JSHD file: " + jshdFile.getAbsolutePath());
      return 1;
    }

    // Validate file is a JSHD file
    if (!jshdFile.getName().toLowerCase().endsWith(".jshd")) {
      output.printError("File is not a JSHD file: " + jshdFile.getName());
      return 2;
    }

    // Parse coordinates and timestep
    long timePoint;
    try {
      timePoint = Long.parseLong(timestep);
    } catch (NumberFormatException e) {
      output.printError("Invalid time step: " + timestep + ". Must be a valid integer.");
      return 3;
    }

    BigDecimal x;
    try {
      x = new BigDecimal(xcoordinate);
    } catch (NumberFormatException e) {
      output.printError("Invalid X coordinate: " + xcoordinate + ". Must be a valid number.");
      return 4;
    }

    BigDecimal y;
    try {
      y = new BigDecimal(ycoordinate);
    } catch (NumberFormatException e) {
      output.printError("Invalid Y coordinate: " + ycoordinate + ". Must be a valid number.");
      return 5;
    }

    // Create JSHD reader and attempt to read the value
    EngineValueFactory valueFactory = new EngineValueFactory();
    try (JshdExternalDataReader reader = new JshdExternalDataReader(valueFactory)) {
      reader.open(jshdFile.getAbsolutePath());
      
      // Validate variable name exists
      if (!reader.getVariableNames().contains(variable)) {
        output.printError("Variable '" + variable + "' not found in JSHD file. "
            + "Available variables: " + reader.getVariableNames());
        return 6;
      }

      // Read the value at the specified coordinates and timestep
      Optional<EngineValue> value = reader.readValueAt(variable, x, y, (int) timePoint);
      
      if (value.isPresent()) {
        EngineValue engineValue = value.get();
        String unitsStr = engineValue.getUnits().toString();
        
        // Print the value with units
        output.printInfo(String.format("Value at (%s, %s, %d): %s %s",
            xcoordinate, ycoordinate, timePoint,
            engineValue.getAsDecimal().toString(), unitsStr));
        return 0;
      } else {
        output.printError(String.format("No value found at coordinates (%s, %s) "
            + "for timestep %d in variable '%s'",
            xcoordinate, ycoordinate, timePoint, variable));
        return 7;
      }

    } catch (Exception e) {
      output.printError("Error inspecting JSHD file: " + e.getMessage());
      return 8;
    }
  }
}