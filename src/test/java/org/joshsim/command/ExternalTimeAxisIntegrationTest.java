/**
 * End-to-end integration tests for declared external temporal metadata.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FileInputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import org.joshsim.engine.value.engine.ValueSupportFactory;
import org.joshsim.precompute.BinaryGridSerializationStrategy;
import org.joshsim.precompute.DoublePrecomputedGrid;
import org.joshsim.precompute.GridSerializationStrategy;
import org.joshsim.precompute.TimeAxis;
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

        start unit year
          alias years
        end unit
        """.formatted(csvTarget));

    Path jshd = tempDir.resolve("temperature.jshd");
    PreprocessOptions preprocessOptions = PreprocessOptions.builder()
            .timeAxis(TimeAxisParams.of("count", "2024", "year", "3", "1", "", ""))
            .build();
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
    PreprocessOptions preprocessOptions = PreprocessOptions.builder()
            .timeAxis(TimeAxisParams.of("ISO", "2024-01-01", "", "3", "", "P1Y", ""))
            .build();
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
    // meta.year remains the legacy raw simulation timestep even when the ISO clock is enabled.
    assertEquals(new TreeSet<>(List.of(0L, 1L, 2L)), parsed.years);
    assertTrue(parsed.temperatures.stream().anyMatch(t -> t > 250.0));
  }

  @Test
  public void isoAxis_externalAtTimeAndMetadataReadCompressedLogicalMapping() throws Exception {
    Path outDir = Files.createDirectories(tempDir.resolve("iso_jshdz_run"));
    Path csvTarget = outDir.resolve("results_{replicate}.csv");
    Path script = tempDir.resolve("iso_jshdz.josh");
    String scriptTemplate = """
        start simulation Test
          grid.size = 16000 m
          grid.low = 36.73 degrees latitude, -119.52 degrees longitude
          grid.high = 35.80 degrees latitude, -117.98 degrees longitude
          grid.patch = "Default"

          time.type = "ISO"
          time.low = "2024-01-01"
          time.high = "2026-01-01"
          time.interval = "P1Y"

        %s

          exportFiles.patch = "file://%s"
        end simulation

        start patch Default
          export.time.step = meta.time
          export.year.step = meta.year
          export.axisLength.step = length of external temperature
          export.temperature.step = external temperature at time meta.time
        end patch
        """;
    Files.writeString(script, scriptTemplate.formatted("", csvTarget));

    Path jshdz = tempDir.resolve("temperature.jshdz");
    PreprocessOptions preprocessOptions = PreprocessOptions.builder()
            .timeAxis(TimeAxisParams.of("ISO", "2024-01-01", "", "3", "", "P1Y", ""))
            .build();
    PreprocessUtil.preprocess(script.toFile(), "Test", fixture().toString(), VARIABLE, "K",
        jshdz.toFile(), preprocessOptions, new OutputOptions());

    Files.writeString(script, scriptTemplate.formatted("""
          axisLength.constant = length of external temperature
          steps.high = axisLength - 1 count
        """, csvTarget));

    // GridSpec mappings use the logical external name, not a filename with a format extension.
    RunUtil.RunOptions runOptions = RunUtil.RunOptions.builder(script.toFile(), "Test")
        .replicates(1)
        .dataFiles(new String[] {"temperature=" + jshdz})
        .seed(Optional.of(42L))
        .build();
    RunUtil.RunResult result = RunUtil.run(runOptions, new OutputOptions());
    assertTrue(result.isSuccess(), "run should succeed: " + result.getMessage());

    Path csv = outDir.resolve("results_0.csv");
    assertTrue(Files.exists(csv));
    RunResult parsed = parseCsv(csv);
    assertTrue(parsed.temperatures.stream().anyMatch(t -> t > 250.0),
        "external ... at time must read compressed temporal data");
    List<Double> lengths = parseNumericColumn(csv, "axisLength");
    assertTrue(!lengths.isEmpty() && lengths.stream().allMatch(length -> length == 3.0),
        "metadata lookup must use the same compressed logical mapping");
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

  private static List<Double> parseNumericColumn(Path csv, String column) throws Exception {
    List<String> lines = Files.readAllLines(csv);
    String[] header = lines.get(0).split(",", -1);
    int columnIndex = -1;
    for (int i = 0; i < header.length; i++) {
      if (header[i].equals(column)) {
        columnIndex = i;
        break;
      }
    }
    assertTrue(columnIndex >= 0, "CSV must have " + column + " column");

    List<Double> values = new ArrayList<>();
    for (int i = 1; i < lines.size(); i++) {
      values.add(Double.parseDouble(lines.get(i).split(",", -1)[columnIndex]));
    }
    return values;
  }

  private record RunResult(TreeSet<Long> years, List<Double> temperatures) {}

  // --- Preprocessing validation edge cases ---

  /**
   * Writes a three-step script whose grid preprocessing can actually be built.
   *
   * <p>The {@code Default} patch has to be defined, not just named by {@code grid.patch}: without
   * it preprocessing fails while building the grid, and a test asserting on a rejected time axis
   * would pass on the wrong {@link IllegalArgumentException}.</p>
   *
   * @param name Base name for the script file and its unused export target.
   * @return Path to the written script.
   */
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

        start patch Default
          export.step.step = meta.stepCount
        end patch
        """.formatted(csvTarget.toString());
    Path scriptFile = tempDir.resolve(name + ".josh");
    Files.writeString(scriptFile, script);
    return scriptFile;
  }

  @Test
  public void forcedTimestepWithInstantWritesSingleSliceAxis() throws Exception {
    // The batch fan-out shape: preprocessBatch dispatches one job per timestep, so each job writes
    // a single slice and must declare an instant rather than a range.
    Path script = writeBasicScript("forced_instant");
    Path out = tempDir.resolve("forced_instant.jshd");
    PreprocessOptions opts = PreprocessOptions.builder()
            .timestep("1")
            .timeAxis(TimeAxisParams.of("count", "", "year", "", "", "", "2025"))
            .build();

    PreprocessUtil.preprocess(script.toFile(), "Test", fixture().toString(), VARIABLE, "K",
        out.toFile(), opts, new OutputOptions());

    TimeAxis axis = readTimeAxis(out);
    assertEquals(TimeAxis.Type.COUNT, axis.getType());
    assertEquals(TimeAxis.Kind.INSTANT, axis.getKind());
    assertEquals(1, axis.getCount());
    assertEquals("year", axis.getCountUnit());
    assertEquals(0, new BigDecimal("2025").compareTo(axis.getCountStart()));
  }

  @Test
  public void forcedTimestepWithRangeFails() throws Exception {
    // Declaring the full source range on a single-timestep job would silently mislabel the slice.
    Path script = writeBasicScript("forced_range");
    Path out = tempDir.resolve("forced_range.jshd");
    PreprocessOptions opts = PreprocessOptions.builder()
            .timestep("1")
            .timeAxis(TimeAxisParams.of("count", "2024", "year", "3", "1", "", ""))
            .build();

    assertThrows(IllegalArgumentException.class, () ->
        PreprocessUtil.preprocess(script.toFile(), "Test", fixture().toString(), VARIABLE, "K",
            out.toFile(), opts, new OutputOptions()));
  }

  private static TimeAxis readTimeAxis(Path jshd) throws Exception {
    GridSerializationStrategy deserializer = new BinaryGridSerializationStrategy(
        new ValueSupportFactory());
    DoublePrecomputedGrid grid;
    try (FileInputStream stream = new FileInputStream(jshd.toFile())) {
      grid = (DoublePrecomputedGrid) deserializer.deserialize(stream);
    }
    return grid.getTimeAxis().orElseThrow(
        () -> new AssertionError("preprocessed file carries no time axis"));
  }

  @Test
  public void countMismatchFails() throws Exception {
    Path script = writeBasicScript("count_mismatch");
    Path out = tempDir.resolve("mismatch.jshd");
    // Script has 3 output slices (0..2) but we declare count=5
    PreprocessOptions opts = PreprocessOptions.builder()
            .timeAxis(TimeAxisParams.of("count", "2024", "year", "5", "1", "", ""))
            .build();
    IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () ->
        PreprocessUtil.preprocess(script.toFile(), "Test", fixture().toString(), VARIABLE, "K",
            out.toFile(), opts, new OutputOptions()));
    assertTrue(thrown.getMessage().contains("--time-count must equal the number of output slices"),
        "unexpected failure: " + thrown.getMessage());
  }

  @Test
  public void isoWithTimeUnitFails() throws Exception {
    Path script = writeBasicScript("iso_unit_mismatch");
    Path out = tempDir.resolve("iso_unit.jshd");
    // ISO mode should not accept --time-unit
    PreprocessOptions opts = PreprocessOptions.builder()
            .timeAxis(TimeAxisParams.of("ISO", "2024-01-01", "year", "3", "", "P1Y", ""))
            .build();
    IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () ->
        PreprocessUtil.preprocess(script.toFile(), "Test", fixture().toString(), VARIABLE, "K",
            out.toFile(), opts, new OutputOptions()));
    assertTrue(thrown.getMessage().contains("ISO time metadata uses --time-interval"),
        "unexpected failure: " + thrown.getMessage());
  }

  @Test
  public void countWithTimeIntervalFails() throws Exception {
    Path script = writeBasicScript("count_interval_mismatch");
    Path out = tempDir.resolve("count_interval.jshd");
    // Count mode should not accept --time-interval
    PreprocessOptions opts = PreprocessOptions.builder()
            .timeAxis(TimeAxisParams.of("count", "2024", "year", "3", "1", "P1Y", ""))
            .build();
    IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () ->
        PreprocessUtil.preprocess(script.toFile(), "Test", fixture().toString(), VARIABLE, "K",
            out.toFile(), opts, new OutputOptions()));
    assertTrue(thrown.getMessage().contains("--time-interval is only valid for ISO time metadata"),
        "unexpected failure: " + thrown.getMessage());
  }

  @Test
  public void instantWithMultipleSlicesFails() throws Exception {
    Path script = writeBasicScript("instant_multi");
    Path out = tempDir.resolve("instant_multi.jshd");
    // Script has 3 output slices but instant requires exactly 1
    PreprocessOptions opts = PreprocessOptions.builder()
            .timeAxis(TimeAxisParams.of("count", "", "year", "", "", "", "2024"))
            .build();
    IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () ->
        PreprocessUtil.preprocess(script.toFile(), "Test", fixture().toString(), VARIABLE, "K",
            out.toFile(), opts, new OutputOptions()));
    assertTrue(
        thrown.getMessage().contains("A count time instant requires exactly one output slice"),
        "unexpected failure: " + thrown.getMessage());
  }

  @Test
  public void unsupportedTimeTypeFails() throws Exception {
    Path script = writeBasicScript("bad_type");
    Path out = tempDir.resolve("bad_type.jshd");
    PreprocessOptions opts = PreprocessOptions.builder()
            .timeAxis(TimeAxisParams.of("julian", "2024", "year", "3", "1", "", ""))
            .build();
    IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () ->
        PreprocessUtil.preprocess(script.toFile(), "Test", fixture().toString(), VARIABLE, "K",
            out.toFile(), opts, new OutputOptions()));
    assertTrue(thrown.getMessage().contains("Unsupported --time-type"),
        "unexpected failure: " + thrown.getMessage());
  }
}
