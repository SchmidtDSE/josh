/**
 * Command which inspects values in jshd format files at specific coordinates.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.command;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.Callable;
import org.joshsim.engine.value.engine.ValueSupportFactory;
import org.joshsim.engine.value.type.EngineValue;
import org.joshsim.precompute.BinaryGridSerializationStrategy;
import org.joshsim.precompute.DoublePrecomputedGrid;
import org.joshsim.precompute.GridSerializationStrategy;
import org.joshsim.precompute.XzGridSerializationStrategy;
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
 * @see org.joshsim.precompute.XzGridSerializationStrategy
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

    // Validate file is a JSHD or JSHDZ file
    String lowerName = jshdFile.getName().toLowerCase();
    if (!lowerName.endsWith(".jshd") && !lowerName.endsWith(".jshdz")) {
      output.printError("File is not a JSHD or JSHDZ file: " + jshdFile.getName());
      return 2;
    }

    if (csvOutputPath != null && !csvOutputPath.isEmpty()) {
      return exportToCsv();
    } else {
      return inspectSingleValue();
    }
  }

  private DoublePrecomputedGrid loadGrid() throws IOException {
    ValueSupportFactory valueFactory = new ValueSupportFactory();
    boolean compressed = jshdFile.getName().toLowerCase().endsWith(".jshdz");
    GridSerializationStrategy strategy = compressed
        ? new XzGridSerializationStrategy(new BinaryGridSerializationStrategy(valueFactory))
        : new BinaryGridSerializationStrategy(valueFactory);
    try (FileInputStream fis = new FileInputStream(jshdFile)) {
      return (DoublePrecomputedGrid) strategy.deserialize(fis);
    }
  }

  private static final String JSHD_VARIABLE = "data";

  private Integer exportToCsv() {
    try {
      DoublePrecomputedGrid grid = loadGrid();

      if (!variable.equals(JSHD_VARIABLE)) {
        output.printError("Variable '" + variable + "' not found in JSHD/JSHDZ file. "
            + "Available variables: [" + JSHD_VARIABLE + "]");
        return 6;
      }

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

    try {
      DoublePrecomputedGrid grid = loadGrid();

      if (!variable.equals(JSHD_VARIABLE)) {
        output.printError("Variable '" + variable + "' not found in JSHD/JSHDZ file. "
            + "Available variables: [" + JSHD_VARIABLE + "]");
        return 6;
      }

      long gridX = x.longValue();
      long gridY = y.longValue();
      long gridTimeStep = timePoint + grid.getMinTimestep();

      boolean outOfBounds = gridX < grid.getMinX() || gridX > grid.getMaxX()
          || gridY < grid.getMinY() || gridY > grid.getMaxY()
          || gridTimeStep < grid.getMinTimestep() || gridTimeStep > grid.getMaxTimestep();

      if (outOfBounds) {
        output.printError(String.format("No value found at coordinates (%s, %s) "
            + "for timestep %d in variable '%s'",
            xcoordinate, ycoordinate, timePoint, variable));
        return 7;
      }

      EngineValue engineValue = grid.getAt(gridX, gridY, gridTimeStep);
      String unitsStr = engineValue.getUnits().toString();

      output.printInfo(String.format("Value at (%s, %s, %d): %s %s",
          xcoordinate, ycoordinate, timePoint,
          engineValue.getAsDecimal().toString(), unitsStr));
      return 0;

    } catch (Exception e) {
      output.printError("Error inspecting JSHD/JSHDZ file: " + e.getMessage());
      return 8;
    }
  }
}
