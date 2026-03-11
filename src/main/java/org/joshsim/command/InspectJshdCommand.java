/**
 * Command which inspects values in jshd format files at specific coordinates.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.command;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.Callable;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.engine.value.type.EngineValue;
import org.joshsim.geo.external.readers.JshdExternalDataReader;
import org.joshsim.precompute.DoublePrecomputedGrid;
import org.joshsim.util.OutputOptions;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;


/**
 * Command line interface handler for inspecting JSHD file values at specific coordinates.
 *
 * <p>This class implements the 'inspectJshd' command which allows users to examine values stored
 * in JSHD binary files at specific grid coordinates and time points. This functionality is useful
 * for debugging preprocessed data files and validating that data has been correctly stored.
 *
 * <p>The command supports two modes:
 * <ul>
 *   <li>Single-value mode: inspect a value at specific coordinates (default)</li>
 *   <li>CSV export mode: dump the entire grid across all timesteps to a CSV file (--to-csv)</li>
 * </ul>
 *
 * <p>In single-value mode, the command takes the following parameters:
 * <ul>
 *   <li>jshd file path - Path to the JSHD file to inspect</li>
 *   <li>variable name - Name of the variable to read (typically "data" for JSHD files)</li>
 *   <li>time step - The time point to inspect</li>
 *   <li>x coordinate - X coordinate in grid space</li>
 *   <li>y coordinate - Y coordinate in grid space</li>
 * </ul>
 *
 * <p>In CSV export mode, only the jshd file path and variable name are required. The CSV output
 * includes all grid cells across all timesteps with columns: x, y, timestep, value. Grid metadata
 * (bounds, dimensions, units) is printed to stdout.
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

  @Parameters(index = "2", description = "Time step to inspect (ignored with --to-csv)",
      defaultValue = "")
  private String timestep;

  @Parameters(index = "3", description = "X coordinate in grid space (ignored with --to-csv)",
      defaultValue = "")
  private String xcoordinate;

  @Parameters(index = "4", description = "Y coordinate in grid space (ignored with --to-csv)",
      defaultValue = "")
  private String ycoordinate;

  @Option(
      names = "--to-csv",
      description = "Export entire grid to CSV file (all timesteps, all locations)",
      defaultValue = ""
  )
  private String csvOutputPath;

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

    if (csvOutputPath != null && !csvOutputPath.isEmpty()) {
      return exportToCsv();
    } else {
      return inspectSingleValue();
    }
  }

  private Integer exportToCsv() {
    EngineValueFactory valueFactory = new EngineValueFactory();
    try (JshdExternalDataReader reader = new JshdExternalDataReader(valueFactory)) {
      reader.open(jshdFile.getAbsolutePath());

      if (!reader.getVariableNames().contains(variable)) {
        output.printError("Variable '" + variable + "' not found in JSHD file. "
            + "Available variables: " + reader.getVariableNames());
        return 6;
      }

      DoublePrecomputedGrid grid = reader.getGrid();

      // Print metadata as JSON to stdout
      StringBuilder json = new StringBuilder();
      json.append("{\n");

      long minX = grid.getMinX();
      json.append("  \"minX\": ").append(minX).append(",\n");

      long maxX = grid.getMaxX();
      json.append("  \"maxX\": ").append(maxX).append(",\n");

      long minY = grid.getMinY();
      json.append("  \"minY\": ").append(minY).append(",\n");

      long maxY = grid.getMaxY();
      json.append("  \"maxY\": ").append(maxY).append(",\n");

      long minTimestep = grid.getMinTimestep();
      json.append("  \"minTimestep\": ").append(minTimestep).append(",\n");

      long maxTimestep = grid.getMaxTimestep();
      json.append("  \"maxTimestep\": ").append(maxTimestep).append(",\n");

      json.append("  \"width\": ").append(grid.getWidth()).append(",\n");
      json.append("  \"height\": ").append(grid.getHeight()).append(",\n");

      String units = grid.getUnits().toString();
      json.append("  \"units\": \"").append(units).append("\",\n");
      json.append("  \"csv\": \"").append(new File(csvOutputPath).getAbsolutePath()).append("\"\n");
      json.append("}");

      // Write CSV
      File csvFile = new File(csvOutputPath);
      try (PrintWriter writer = new PrintWriter(new FileWriter(csvFile))) {
        writer.println("x,y,timestep,value");

        for (long t = minTimestep; t <= maxTimestep; t++) {
          for (long y = minY; y <= maxY; y++) {
            for (long x = minX; x <= maxX; x++) {
              EngineValue val = grid.getAt(x, y, t);
              writer.printf("%d,%d,%d,%s%n", x, y, t, val.getAsDecimal().toString());
            }
          }
        }
      }

      output.printInfo(json.toString());
      return 0;

    } catch (IOException e) {
      output.printError("Error writing CSV file: " + e.getMessage());
      return 9;
    } catch (Exception e) {
      output.printError("Error inspecting JSHD file: " + e.getMessage());
      return 8;
    }
  }

  private Integer inspectSingleValue() {
    // Parse coordinates and timestep
    if (timestep == null || timestep.isEmpty()) {
      output.printError("Time step is required when not using --to-csv.");
      return 3;
    }

    if (xcoordinate == null || xcoordinate.isEmpty()) {
      output.printError("X coordinate is required when not using --to-csv.");
      return 4;
    }

    if (ycoordinate == null || ycoordinate.isEmpty()) {
      output.printError("Y coordinate is required when not using --to-csv.");
      return 5;
    }

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
