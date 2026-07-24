/**
 * End-to-end integration tests for declared external temporal metadata.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import org.joshsim.util.OutputOptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;


/** Verifies preprocessing + run paths for both count and ISO declared time axes. */
public class ExternalTimeAxisIntegrationTest {

  private static final String VARIABLE = "Maximum_air_temperature_at_2m";

  @TempDir
  Path tempDir;

  private Path fixture() throws Exception {
    return Path.of(getClass().getResource("/netcdf/maxtemp_tulare_annual.nc").toURI());
  }

  @Test
  public void countAxis_externalAtYearReadsExpectedSlices() throws Exception {
    Path outDir = Files.createDirectories(tempDir.resolve("count_run"));
    Path csvTarget = outDir.resolve("results_{replicate}.csv");
    Path script = tempDir.resolve("count.josh");
    Files.writeString(script, """
        start simulation Test
          grid.size = 16000 m
          grid.low = 36.73 degrees latitude, -119.52 degrees longitude
          grid.high = 35.80 degrees latitude, -117.98 degrees longitude
          grid.patch = \"Default\"

          steps.low = 2024 count
          steps.high = 2026 count

          exportFiles.patch = \"file://%s\"
        end simulation

        start patch Default
          export.year.step = meta.year
          export.temperature.step = external temperature at year meta.year
        end patch
        """.formatted(csvTarget));

    Path jshd = tempDir.resolve("temperature.jshd");
    PreprocessUtil.PreprocessOptions preprocessOptions = new PreprocessUtil.PreprocessOptions(
        "EPSG:4326", "lon", "lat", "calendar_year", "", null, false, false,
        "count", "2024", "year", "3", "1", "", "");
    PreprocessUtil.preprocess(script.toFile(), "Test", fixture().toString(), VARIABLE, "K",
        jshd.toFile(), preprocessOptions, new OutputOptions());

    RunUtil.RunOptions runOptions = RunUtil.RunOptions.builder(script.toFile(), "Test")
        .replicates(1)
        .dataFiles(new String[] {"temperature.jshd=" + jshd})
        .seed(Optional.of(42L))
        .build();
    RunUtil.RunResult result = RunUtil.run(runOptions, new OutputOptions());
    assertTrue(result.isSuccess(), "run should succeed: " + result.getMessage());

    Path csv = outDir.resolve("results_0.csv");
    assertTrue(Files.exists(csv));
    RunResult parsed = parseCsv(csv);
    assertEquals(new TreeSet<>(List.of(2024L, 2025L, 2026L)), parsed.years);
    assertTrue(parsed.temperatures.stream().anyMatch(t -> t > 250.0));
  }

  @Test
  public void isoAxis_metaTimeReadsExpectedSlices() throws Exception {
    Path outDir = Files.createDirectories(tempDir.resolve("iso_run"));
    Path csvTarget = outDir.resolve("results_{replicate}.csv");
    Path script = tempDir.resolve("iso.josh");
    Files.writeString(script, """
        start simulation Test
          grid.size = 16000 m
          grid.low = 36.73 degrees latitude, -119.52 degrees longitude
          grid.high = 35.80 degrees latitude, -117.98 degrees longitude
          grid.patch = \"Default\"

          time.type = \"ISO\"
          time.low = \"2024-01-01\"
          time.high = \"2026-01-01\"
          time.interval = \"P1Y\"

          exportFiles.patch = \"file://%s\"
        end simulation

        start patch Default
          export.time.step = meta.time
          export.year.step = meta.year
          export.temperature.step = external temperature at time meta.time
        end patch
        """.formatted(csvTarget));

    Path jshd = tempDir.resolve("temperature.jshd");
    PreprocessUtil.PreprocessOptions preprocessOptions = new PreprocessUtil.PreprocessOptions(
        "EPSG:4326", "lon", "lat", "calendar_year", "", null, false, false,
        "ISO", "2024-01-01", "", "3", "", "P1Y", "");
    PreprocessUtil.preprocess(script.toFile(), "Test", fixture().toString(), VARIABLE, "K",
        jshd.toFile(), preprocessOptions, new OutputOptions());

    RunUtil.RunOptions runOptions = RunUtil.RunOptions.builder(script.toFile(), "Test")
        .replicates(1)
        .dataFiles(new String[] {"temperature.jshd=" + jshd})
        .seed(Optional.of(42L))
        .build();
    RunUtil.RunResult result = RunUtil.run(runOptions, new OutputOptions());
    assertTrue(result.isSuccess(), "run should succeed: " + result.getMessage());

    Path csv = outDir.resolve("results_0.csv");
    assertTrue(Files.exists(csv));
    RunResult parsed = parseCsv(csv);
    assertEquals(new TreeSet<>(List.of(2024L, 2025L, 2026L)), parsed.years);
    assertTrue(parsed.temperatures.stream().anyMatch(t -> t > 250.0));
  }

  private static RunResult parseCsv(Path csv) throws Exception {
    List<String> lines = Files.readAllLines(csv);
    String[] header = lines.get(0).split(",", -1);
    int yearCol = -1;
    int tempCol = -1;
    for (int i = 0; i < header.length; i++) {
      if (header[i].equals("year")) {
        yearCol = i;
      } else if (header[i].equals("temperature")) {
        tempCol = i;
      }
    }
    assertTrue(yearCol >= 0 && tempCol >= 0, "CSV must have year and temperature columns");

    TreeSet<Long> years = new TreeSet<>();
    List<Double> temperatures = new ArrayList<>();
    for (int i = 1; i < lines.size(); i++) {
      String[] row = lines.get(i).split(",", -1);
      years.add((long) Double.parseDouble(row[yearCol]));
      temperatures.add(Double.parseDouble(row[tempCol]));
    }
    temperatures.sort(Double::compareTo);
    return new RunResult(years, temperatures);
  }

  private record RunResult(TreeSet<Long> years, List<Double> temperatures) {}

  // --- Preprocessing validation edge cases ---

  private Path writeBasicScript(String name) throws Exception {
    Path csvTarget = tempDir.resolve(name + "_unused.csv");
    String script = """
        start simulation Test
          grid.size = 16000 m
          grid.low = 36.73 degrees latitude, -119.52 degrees longitude
          grid.high = 35.80 degrees latitude, -117.98 degrees longitude
          grid.patch = "Default"
          steps.low = 0 count
          steps.high = 2 count
          exportFiles.patch = "file://%s"
        end simulation
        """.formatted(csvTarget.toString());
    Path scriptFile = tempDir.resolve(name + ".josh");
    Files.writeString(scriptFile, script);
    return scriptFile;
  }

  @Test
  public void countMismatchFails() throws Exception {
    Path script = writeBasicScript("count_mismatch");
    Path out = tempDir.resolve("mismatch.jshd");
    // Script has 3 output slices (0..2) but we declare count=5
    PreprocessUtil.PreprocessOptions opts = new PreprocessUtil.PreprocessOptions(
        "EPSG:4326", "lon", "lat", "calendar_year", "", null, false, false,
        "count", "2024", "year", "5", "1", "", "");
    assertThrows(IllegalArgumentException.class, () ->
        PreprocessUtil.preprocess(script.toFile(), "Test", fixture().toString(), VARIABLE, "K",
            out.toFile(), opts, new OutputOptions()));
  }

  @Test
  public void isoWithTimeUnitFails() throws Exception {
    Path script = writeBasicScript("iso_unit_mismatch");
    Path out = tempDir.resolve("iso_unit.jshd");
    // ISO mode should not accept --time-unit
    PreprocessUtil.PreprocessOptions opts = new PreprocessUtil.PreprocessOptions(
        "EPSG:4326", "lon", "lat", "calendar_year", "", null, false, false,
        "ISO", "2024-01-01", "year", "3", "", "P1Y", "");
    assertThrows(IllegalArgumentException.class, () ->
        PreprocessUtil.preprocess(script.toFile(), "Test", fixture().toString(), VARIABLE, "K",
            out.toFile(), opts, new OutputOptions()));
  }

  @Test
  public void countWithTimeIntervalFails() throws Exception {
    Path script = writeBasicScript("count_interval_mismatch");
    Path out = tempDir.resolve("count_interval.jshd");
    // Count mode should not accept --time-interval
    PreprocessUtil.PreprocessOptions opts = new PreprocessUtil.PreprocessOptions(
        "EPSG:4326", "lon", "lat", "calendar_year", "", null, false, false,
        "count", "2024", "year", "3", "1", "P1Y", "");
    assertThrows(IllegalArgumentException.class, () ->
        PreprocessUtil.preprocess(script.toFile(), "Test", fixture().toString(), VARIABLE, "K",
            out.toFile(), opts, new OutputOptions()));
  }

  @Test
  public void instantWithMultipleSlicesFails() throws Exception {
    Path script = writeBasicScript("instant_multi");
    Path out = tempDir.resolve("instant_multi.jshd");
    // Script has 3 output slices but instant requires exactly 1
    PreprocessUtil.PreprocessOptions opts = new PreprocessUtil.PreprocessOptions(
        "EPSG:4326", "lon", "lat", "calendar_year", "", null, false, false,
        "count", "", "year", "", "", "", "2024");
    assertThrows(IllegalArgumentException.class, () ->
        PreprocessUtil.preprocess(script.toFile(), "Test", fixture().toString(), VARIABLE, "K",
            out.toFile(), opts, new OutputOptions()));
  }

  @Test
  public void unsupportedTimeTypeFails() throws Exception {
    Path script = writeBasicScript("bad_type");
    Path out = tempDir.resolve("bad_type.jshd");
    PreprocessUtil.PreprocessOptions opts = new PreprocessUtil.PreprocessOptions(
        "EPSG:4326", "lon", "lat", "calendar_year", "", null, false, false,
        "julian", "2024", "year", "3", "1", "", "");
    assertThrows(IllegalArgumentException.class, () ->
        PreprocessUtil.preprocess(script.toFile(), "Test", fixture().toString(), VARIABLE, "K",
            out.toFile(), opts, new OutputOptions()));
  }
}
