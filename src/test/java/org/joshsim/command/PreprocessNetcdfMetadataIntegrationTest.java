/**
 * End-to-end preprocessing tests for NetCDF files carrying minimal CF metadata.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.joshsim.engine.value.engine.ValueSupportFactory;
import org.joshsim.precompute.BinaryGridSerializationStrategy;
import org.joshsim.precompute.DoublePrecomputedGrid;
import org.joshsim.precompute.GridSerializationStrategy;
import org.joshsim.precompute.XzGridSerializationStrategy;
import org.joshsim.util.OutputOptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;
import ucar.ma2.ArrayDouble;
import ucar.ma2.ArrayFloat;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.write.NetcdfFormatWriter;

/**
 * Verifies that preprocessing tolerates NetCDF files which omit optional CF metadata.
 *
 * <p>Rasters produced by tools other than the Josh fixture generator routinely leave out the
 * variable {@code units} attribute or have no time dimension at all, so these paths are covered
 * here alongside the fully annotated fixtures used elsewhere.</p>
 */
public class PreprocessNetcdfMetadataIntegrationTest {

  private static final String VARIABLE = "temperature";

  @TempDir
  Path tempDir;

  @Test
  public void variableWithoutUnitsAttributeIsPreprocessed() throws Exception {
    Path data = tempDir.resolve("no_units.nc");
    writeNetcdf(data, true, false);
    Path script = writeScript("no_units.josh");
    Path output = tempDir.resolve("no_units.jshdz");

    PreprocessUtil.PreprocessOptions options = new PreprocessUtil.PreprocessOptions(
        "EPSG:4326", "lon", "lat", "time", "", null, false, false,
        "count", "2015", "year", "2", "1", "", "");
    PreprocessUtil.preprocess(script.toFile(), "Preprocess", data.toString(), VARIABLE,
        "celsius", output.toFile(), options, new OutputOptions());

    // Each timestep must carry that timestep's slice of the source raster rather than a fill.
    assertValuesWithin(output, 0, 10.0, 13.0);
    assertValuesWithin(output, 1, 20.0, 23.0);
  }

  @Test
  public void noTimeDimensionPreprocessesTimelessRaster() throws Exception {
    Path data = tempDir.resolve("no_time.nc");
    writeNetcdf(data, false, true);
    Path script = writeScript("no_time.josh");
    Path output = tempDir.resolve("no_time.jshdz");

    // An empty time name is what --no-time-dim resolves to.
    PreprocessUtil.PreprocessOptions options = new PreprocessUtil.PreprocessOptions(
        "EPSG:4326", "lon", "lat", "", "", null, false, false);
    PreprocessUtil.preprocess(script.toFile(), "Preprocess", data.toString(), VARIABLE,
        "celsius", output.toFile(), options, new OutputOptions());

    // With no time dimension every timestep reads the single available slice.
    assertValuesWithin(output, 0, 10.0, 13.0);
    assertValuesWithin(output, 1, 10.0, 13.0);
  }

  @Test
  public void noTimeDimFlagPreprocessesTimelessRasterThroughCli() throws Exception {
    Path data = tempDir.resolve("cli_no_time.nc");
    writeNetcdf(data, false, true);
    Path script = writeScript("cli_no_time.josh");
    Path output = tempDir.resolve("cli_no_time.jshdz");

    int exitCode = new CommandLine(new PreprocessCommand()).execute(
        script.toString(), "Preprocess", data.toString(), VARIABLE, "celsius",
        output.toString(), "--x-coord", "lon", "--y-coord", "lat", "--no-time-dim");

    assertEquals(0, exitCode, "--no-time-dim should preprocess a raster with no time dimension");
    assertValuesWithin(output, 0, 10.0, 13.0);
  }

  @Test
  public void timelessRasterWithoutFlagStillReportsTheMissingDimension() throws Exception {
    Path data = tempDir.resolve("cli_needs_flag.nc");
    writeNetcdf(data, false, true);
    Path script = writeScript("cli_needs_flag.josh");
    Path output = tempDir.resolve("cli_needs_flag.jshdz");

    // Without the flag the defaulted dimension name is still required to exist, so a typo in
    // --time-dim keeps failing loudly rather than silently reading a single slice.
    int exitCode = new CommandLine(new PreprocessCommand()).execute(
        script.toString(), "Preprocess", data.toString(), VARIABLE, "celsius",
        output.toString(), "--x-coord", "lon", "--y-coord", "lat");

    assertNotEquals(0, exitCode, "a missing time dimension should be reported, not assumed");
  }

  /**
   * Asserts that the populated cells at a timestep hold values from that slice of the source.
   *
   * <p>Cells whose centers fall outside the raster's coverage keep the grid default of zero, so
   * only populated cells are range-checked. Requiring at least one populated cell is what
   * distinguishes real interpolated data from an all-default grid.</p>
   *
   * @param output The preprocessed jshdz file to read back.
   * @param timestep The timestep to check.
   * @param minExpected Lowest value present in the source slice.
   * @param maxExpected Highest value present in the source slice.
   */
  private static void assertValuesWithin(Path output, long timestep, double minExpected,
      double maxExpected) throws Exception {
    ValueSupportFactory valueFactory = new ValueSupportFactory();
    GridSerializationStrategy deserializer = new XzGridSerializationStrategy(
        new BinaryGridSerializationStrategy(valueFactory));

    DoublePrecomputedGrid grid;
    try (FileInputStream stream = new FileInputStream(output.toFile())) {
      grid = (DoublePrecomputedGrid) deserializer.deserialize(stream);
    }

    int populated = 0;
    for (long x = grid.getMinX(); x <= grid.getMaxX(); x++) {
      for (long y = grid.getMinY(); y <= grid.getMaxY(); y++) {
        double value = grid.getAt(x, y, timestep).getAsDecimal().doubleValue();
        if (value == 0.0) {
          continue;
        }
        assertTrue(value >= minExpected && value <= maxExpected,
            String.format("value %f at (%d, %d, step %d) should come from the source slice",
                value, x, y, timestep));
        populated++;
      }
    }
    assertTrue(populated > 0,
        "step " + timestep + " should read values from the source raster, not only defaults");
  }

  private Path writeScript(String name) throws Exception {
    Path script = tempDir.resolve(name);
    Files.writeString(script, """
        start simulation Preprocess
          grid.size = 30 m
          grid.low = 33.901 degrees latitude, -116.001 degrees longitude
          grid.high = 33.9 degrees latitude, -116.0 degrees longitude
          steps.low = 0 count
          steps.high = 1 count
        end simulation

        start patch Default
        end patch
        """);
    return script;
  }

  /**
   * Writes a small NetCDF raster covering the script's grid extent.
   *
   * @param path Destination file.
   * @param withTime Whether to give the data variable a leading time dimension.
   * @param withUnits Whether to annotate the data variable with a {@code units} attribute.
   */
  private static void writeNetcdf(Path path, boolean withTime, boolean withUnits)
      throws Exception {
    NetcdfFormatWriter.Builder builder =
        NetcdfFormatWriter.createNewNetcdf3(path.toString());
    Dimension timeDim = withTime ? builder.addDimension("time", 2) : null;
    Dimension latDim = builder.addDimension("lat", 2);
    Dimension lonDim = builder.addDimension("lon", 2);

    if (withTime) {
      builder.addVariable("time", DataType.DOUBLE, List.of(timeDim));
    }
    builder.addVariable("lat", DataType.DOUBLE, List.of(latDim));
    builder.addVariable("lon", DataType.DOUBLE, List.of(lonDim));

    List<Dimension> dataDims = withTime
        ? List.of(timeDim, latDim, lonDim)
        : List.of(latDim, lonDim);
    var dataVar = builder.addVariable(VARIABLE, DataType.FLOAT, dataDims);
    if (withUnits) {
      dataVar.addAttribute(new Attribute("units", "celsius"));
    }

    try (NetcdfFormatWriter writer = builder.build()) {
      if (withTime) {
        ArrayDouble.D1 times = new ArrayDouble.D1(2);
        times.set(0, 2015);
        times.set(1, 2016);
        writer.write(writer.findVariable("time"), times);
      }

      ArrayDouble.D1 lats = new ArrayDouble.D1(2);
      lats.set(0, 33.901);
      lats.set(1, 33.9);
      writer.write(writer.findVariable("lat"), lats);

      ArrayDouble.D1 lons = new ArrayDouble.D1(2);
      lons.set(0, -116.001);
      lons.set(1, -116.0);
      writer.write(writer.findVariable("lon"), lons);

      if (withTime) {
        ArrayFloat.D3 values = new ArrayFloat.D3(2, 2, 2);
        for (int t = 0; t < 2; t++) {
          for (int y = 0; y < 2; y++) {
            for (int x = 0; x < 2; x++) {
              values.set(t, y, x, 10 + (t * 10) + (y * 2) + x);
            }
          }
        }
        writer.write(writer.findVariable(VARIABLE), values);
      } else {
        ArrayFloat.D2 values = new ArrayFloat.D2(2, 2);
        for (int y = 0; y < 2; y++) {
          for (int x = 0; x < 2; x++) {
            values.set(y, x, 10 + (y * 2) + x);
          }
        }
        writer.write(writer.findVariable(VARIABLE), values);
      }
    }
  }
}
