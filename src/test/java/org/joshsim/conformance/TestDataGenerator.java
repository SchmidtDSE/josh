/**
 * Generates synthetic test data files for Phase 4 conformance testing.
 *
 * <p>This utility generates GeoTIFF and NetCDF test files with predictable,
 * well-documented values for testing Josh's geospatial data handling capabilities.
 *
 * <p>Generated files:
 * <ul>
 *   <li>GeoTIFF files (spatial/):
 *     <ul>
 *       <li>grid_10x10_sequential.tiff - Sequential values 0-99</li>
 *       <li>grid_10x10_constant.tiff - Constant value 42</li>
 *       <li>grid_10x10_checkerboard.tiff - Alternating 0/1 pattern</li>
 *     </ul>
 *   </li>
 *   <li>NetCDF files (temporal/):
 *     <ul>
 *       <li>temperature_2020-2024.nc - Seasonal temperature pattern</li>
 *       <li>precipitation_2020-2024.nc - Seasonal precipitation pattern</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.conformance;

import java.io.File;
import java.util.Arrays;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.coverage.CoverageFactoryFinder;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import ucar.ma2.ArrayFloat;
import ucar.ma2.DataType;
import ucar.ma2.Index;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;
import ucar.nc2.write.NetcdfFormatWriter;

/**
 * Generates synthetic test data files for conformance testing.
 *
 * <p>This class creates small, predictable test data files in GeoTIFF and NetCDF
 * formats. All generated files are designed to be deterministic and well-documented
 * to facilitate automated testing of Josh's geospatial data handling.
 *
 * <p>Usage:
 * <pre>
 * // Standalone execution
 * java org.joshsim.conformance.TestDataGenerator
 *
 * // Via Gradle
 * ./gradlew generateTestData
 * </pre>
 */
public class TestDataGenerator {

  /** Grid size for all test data (10x10 = 100 cells). */
  private static final int GRID_SIZE = 10;

  /** Minimum X coordinate in meters (UTM). */
  private static final double MIN_X = 0.0;

  /** Maximum X coordinate in meters (UTM). */
  private static final double MAX_X = 100.0;

  /** Minimum Y coordinate in meters (UTM). */
  private static final double MIN_Y = 0.0;

  /** Maximum Y coordinate in meters (UTM). */
  private static final double MAX_Y = 100.0;

  /** EPSG code for UTM Zone 10N. */
  private static final String EPSG_CODE = "EPSG:32610";

  /** Start year for time series data. */
  private static final int START_YEAR = 2020;

  /** End year for time series data (inclusive). */
  private static final int END_YEAR = 2024;

  /** Number of months per year for time series. */
  private static final int MONTHS_PER_YEAR = 12;

  /** Base temperature in Celsius for seasonal pattern. */
  private static final float BASE_TEMPERATURE = 20.0f;

  /** Temperature amplitude for seasonal variation. */
  private static final float TEMPERATURE_AMPLITUDE = 10.0f;

  /** Base precipitation in mm for seasonal pattern. */
  private static final float BASE_PRECIPITATION = 100.0f;

  /** Precipitation amplitude for seasonal variation. */
  private static final float PRECIPITATION_AMPLITUDE = 100.0f;

  /** Base directory for test data output. */
  private static final String BASE_DIR = "josh-tests/test-data";

  /**
   * Main entry point for standalone execution.
   *
   * <p>Generates all test data files and prints status messages to stdout.
   *
   * @param args Command line arguments (not used)
   */
  public static void main(String[] args) {
    try {
      System.out.println("Generating test data files...");
      System.out.println();

      generateAllTestData();

      System.out.println();
      System.out.println("Test data generation complete!");
      System.out.println("Files created in " + BASE_DIR + "/");
    } catch (Exception e) {
      System.err.println("Error generating test data: " + e.getMessage());
      e.printStackTrace();
      System.exit(1);
    }
  }

  /**
   * Generates all test data files (both GeoTIFF and NetCDF).
   *
   * @throws Exception If there is an error during file generation
   */
  public static void generateAllTestData() throws Exception {
    generateGeoTiffFiles();
    generateNetCdfFiles();
  }

  /**
   * Generates all GeoTIFF test files.
   *
   * <p>Creates three GeoTIFF files with different data patterns:
   * <ul>
   *   <li>Sequential: values from 0 to 99</li>
   *   <li>Constant: all cells = 42</li>
   *   <li>Checkerboard: alternating 0 and 1</li>
   * </ul>
   *
   * @throws Exception If there is an error during file generation
   */
  public static void generateGeoTiffFiles() throws Exception {
    String spatialDir = BASE_DIR + "/spatial";
    new File(spatialDir).mkdirs();

    System.out.println("Generating GeoTIFF files...");

    // Sequential: 0, 1, 2, ..., 99
    float[][] sequentialData = new float[GRID_SIZE][GRID_SIZE];
    for (int row = 0; row < GRID_SIZE; row++) {
      for (int col = 0; col < GRID_SIZE; col++) {
        sequentialData[row][col] = row * GRID_SIZE + col;
      }
    }
    String sequentialFile = spatialDir + "/grid_10x10_sequential.tiff";
    writeGeoTiff(sequentialData, sequentialFile);
    System.out.println("  Created: " + sequentialFile);

    // Constant: all cells = 42
    float[][] constantData = new float[GRID_SIZE][GRID_SIZE];
    for (int row = 0; row < GRID_SIZE; row++) {
      Arrays.fill(constantData[row], 42.0f);
    }
    String constantFile = spatialDir + "/grid_10x10_constant.tiff";
    writeGeoTiff(constantData, constantFile);
    System.out.println("  Created: " + constantFile);

    // Checkerboard: alternating 0 and 1
    float[][] checkerboardData = new float[GRID_SIZE][GRID_SIZE];
    for (int row = 0; row < GRID_SIZE; row++) {
      for (int col = 0; col < GRID_SIZE; col++) {
        checkerboardData[row][col] = (row + col) % 2;
      }
    }
    String checkerboardFile = spatialDir + "/grid_10x10_checkerboard.tiff";
    writeGeoTiff(checkerboardData, checkerboardFile);
    System.out.println("  Created: " + checkerboardFile);
  }

  /**
   * Writes a 2D float array to a GeoTIFF file.
   *
   * <p>Creates a GeoTIFF file with the following specifications:
   * <ul>
   *   <li>Coordinate system: EPSG:32610 (UTM Zone 10N)</li>
   *   <li>Extent: 0m,0m to 100m,100m</li>
   *   <li>Resolution: 10m per cell</li>
   *   <li>Data type: float32</li>
   * </ul>
   *
   * @param data The 2D array of float values to write
   * @param filename The output file path
   * @throws Exception If there is an error writing the file
   */
  private static void writeGeoTiff(float[][] data, String filename) throws Exception {
    // Define coordinate reference system (UTM Zone 10N)
    CoordinateReferenceSystem crs = CRS.decode(EPSG_CODE);

    // Define geographic extent
    ReferencedEnvelope envelope = new ReferencedEnvelope(
        MIN_X, MAX_X, MIN_Y, MAX_Y, crs
    );

    // Create GridCoverage2D
    GridCoverageFactory factory = CoverageFactoryFinder.getGridCoverageFactory(null);
    GridCoverage2D coverage = factory.create("TestData", data, envelope);

    // Write to GeoTIFF
    File outputFile = new File(filename);
    GeoTiffWriter writer = new GeoTiffWriter(outputFile);
    try {
      writer.write(coverage, null);
    } finally {
      writer.dispose();
    }
  }

  /**
   * Generates all NetCDF test files.
   *
   * <p>Creates two NetCDF files with time series data:
   * <ul>
   *   <li>Temperature: Seasonal pattern with 10-30°C range</li>
   *   <li>Precipitation: Seasonal pattern with 0-200mm range</li>
   * </ul>
   *
   * <p>Both files contain monthly data from 2020 to 2024 (60 time steps)
   * on a 10x10 spatial grid matching the GeoTIFF specifications.
   *
   * @throws Exception If there is an error during file generation
   */
  public static void generateNetCdfFiles() throws Exception {
    String temporalDir = BASE_DIR + "/temporal";
    new File(temporalDir).mkdirs();

    System.out.println("Generating NetCDF files...");

    String tempFile = temporalDir + "/temperature_2020-2024.nc";
    writeNetCdf(tempFile, "temperature", "degrees_C",
        "Air Temperature", true);
    System.out.println("  Created: " + tempFile);

    String precipFile = temporalDir + "/precipitation_2020-2024.nc";
    writeNetCdf(precipFile, "precipitation", "mm",
        "Total Precipitation", false);
    System.out.println("  Created: " + precipFile);
  }

  /**
   * Writes a NetCDF file with time series data.
   *
   * <p>Creates a CF-1.6 compliant NetCDF file with:
   * <ul>
   *   <li>Time dimension: Monthly from 2020 to 2024 (60 steps)</li>
   *   <li>Spatial dimensions: 10x10 grid matching GeoTIFF extent</li>
   *   <li>Coordinate reference system: EPSG:32610</li>
   *   <li>Data values: Seasonal pattern based on variable type</li>
   * </ul>
   *
   * @param filename The output file path
   * @param varName The variable name (e.g., "temperature")
   * @param units The units string (e.g., "degrees_C")
   * @param longName The long descriptive name for the variable
   * @param isTemperature True for temperature pattern, false for precipitation
   * @throws Exception If there is an error writing the file
   */
  private static void writeNetCdf(String filename, String varName, String units,
                                   String longName, boolean isTemperature)
      throws Exception {
    File outputFile = new File(filename);

    // Calculate number of time steps
    int numYears = END_YEAR - START_YEAR + 1;
    int numTimeSteps = numYears * MONTHS_PER_YEAR;

    // Create NetCDF file
    NetcdfFormatWriter.Builder builder = NetcdfFormatWriter.createNewNetcdf3(
        outputFile.getAbsolutePath()
    );

    // Add dimensions
    final Dimension timeDim = builder.addDimension("time", numTimeSteps);
    final Dimension latDim = builder.addDimension("y", GRID_SIZE);
    final Dimension lonDim = builder.addDimension("x", GRID_SIZE);

    // Add coordinate variables
    Variable.Builder<?> timevarbuilder = builder.addVariable("time", DataType.FLOAT, "time");
    timevarbuilder.addAttribute(new Attribute("units", "months since 2020-01-01 00:00:00"));
    timevarbuilder.addAttribute(new Attribute("calendar", "gregorian"));
    timevarbuilder.addAttribute(new Attribute("long_name", "time"));

    Variable.Builder<?> xvarbuilder = builder.addVariable("x", DataType.FLOAT, "x");
    xvarbuilder.addAttribute(new Attribute("units", "m"));
    xvarbuilder.addAttribute(new Attribute("long_name", "x coordinate (UTM)"));
    xvarbuilder.addAttribute(new Attribute("standard_name", "projection_x_coordinate"));

    Variable.Builder<?> yvarbuilder = builder.addVariable("y", DataType.FLOAT, "y");
    yvarbuilder.addAttribute(new Attribute("units", "m"));
    yvarbuilder.addAttribute(new Attribute("long_name", "y coordinate (UTM)"));
    yvarbuilder.addAttribute(new Attribute("standard_name", "projection_y_coordinate"));

    // Add CRS variable
    Variable.Builder<?> crsVar = builder.addVariable("crs", DataType.INT, "");
    crsVar.addAttribute(new Attribute("grid_mapping_name", "transverse_mercator"));
    crsVar.addAttribute(new Attribute("longitude_of_central_meridian", -123.0));
    crsVar.addAttribute(new Attribute("latitude_of_projection_origin", 0.0));
    crsVar.addAttribute(new Attribute("scale_factor_at_central_meridian", 0.9996));
    crsVar.addAttribute(new Attribute("false_easting", 500000.0));
    crsVar.addAttribute(new Attribute("false_northing", 0.0));
    crsVar.addAttribute(new Attribute("spatial_ref", EPSG_CODE));

    // Add data variable
    Variable.Builder<?> dataVar = builder.addVariable(varName, DataType.FLOAT,
        Arrays.asList(timeDim, latDim, lonDim));
    dataVar.addAttribute(new Attribute("units", units));
    dataVar.addAttribute(new Attribute("long_name", longName));
    dataVar.addAttribute(new Attribute("grid_mapping", "crs"));
    if (isTemperature) {
      dataVar.addAttribute(new Attribute("standard_name", "air_temperature"));
    } else {
      dataVar.addAttribute(new Attribute("standard_name", "precipitation_amount"));
    }

    // Add global attributes
    builder.addAttribute(new Attribute("Conventions", "CF-1.6"));
    builder.addAttribute(new Attribute("title", "Josh Test Data - " + longName));
    builder.addAttribute(new Attribute("institution", "Josh Simulation Engine"));
    builder.addAttribute(new Attribute("source", "TestDataGenerator"));
    builder.addAttribute(new Attribute("history", "Created for Phase 4 conformance testing"));

    // Build and write file
    try (NetcdfFormatWriter writer = builder.build()) {
      // Write time values (months since 2020-01-01)
      ArrayFloat.D1 timeData = new ArrayFloat.D1(numTimeSteps);
      for (int t = 0; t < numTimeSteps; t++) {
        timeData.set(t, t);
      }
      writer.write(writer.findVariable("time"), timeData);

      // Write x coordinates (cell centers)
      ArrayFloat.D1 xcoordinates = new ArrayFloat.D1(GRID_SIZE);
      for (int i = 0; i < GRID_SIZE; i++) {
        float cellCenter = (float) (MIN_X + (i + 0.5) * (MAX_X - MIN_X) / GRID_SIZE);
        xcoordinates.set(i, cellCenter);
      }
      writer.write(writer.findVariable("x"), xcoordinates);

      // Write y coordinates (cell centers)
      ArrayFloat.D1 ycoordinates = new ArrayFloat.D1(GRID_SIZE);
      for (int i = 0; i < GRID_SIZE; i++) {
        float cellCenter = (float) (MIN_Y + (i + 0.5) * (MAX_Y - MIN_Y) / GRID_SIZE);
        ycoordinates.set(i, cellCenter);
      }
      writer.write(writer.findVariable("y"), ycoordinates);

      // Write CRS value
      ucar.ma2.ArrayInt.D0 crsData = new ucar.ma2.ArrayInt.D0(false);
      crsData.set(0);
      writer.write(writer.findVariable("crs"), crsData);

      // Generate and write data values with seasonal pattern
      ArrayFloat.D3 data = new ArrayFloat.D3(numTimeSteps, GRID_SIZE, GRID_SIZE);
      for (int t = 0; t < numTimeSteps; t++) {
        // Calculate month (0-11) for seasonal pattern
        int month = t % MONTHS_PER_YEAR;
        // Seasonal variation: peaks in summer (month 6), minimum in winter (month 0/12)
        float seasonalFactor = (float) Math.sin((month - 3) * Math.PI / 6.0);

        for (int y = 0; y < GRID_SIZE; y++) {
          for (int x = 0; x < GRID_SIZE; x++) {
            float value;
            if (isTemperature) {
              // Temperature: 10-30°C seasonal pattern
              value = BASE_TEMPERATURE + TEMPERATURE_AMPLITUDE * seasonalFactor;
            } else {
              // Precipitation: 0-200mm seasonal pattern (inverse of temperature)
              value = BASE_PRECIPITATION + PRECIPITATION_AMPLITUDE * (-seasonalFactor);
            }
            data.set(t, y, x, value);
          }
        }
      }
      writer.write(writer.findVariable(varName), data);
    }
  }
}
