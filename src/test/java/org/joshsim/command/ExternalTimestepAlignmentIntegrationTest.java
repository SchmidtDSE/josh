/**
 * Integration tests for external-data timestep alignment under non-zero steps.low.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import org.joshsim.engine.value.engine.ValueSupportFactory;
import org.joshsim.precompute.BinaryGridSerializationStrategy;
import org.joshsim.precompute.DataGridLayer;
import org.joshsim.precompute.DoublePrecomputedGrid;
import org.joshsim.util.OutputOptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;


/**
 * End-to-end coverage that the simulation timestep axis is decoupled from the data source's
 * 0-based record axis, so a calendar {@code steps.low} (e.g. 2024) lines preprocessed external
 * data up with the grid key, {@code meta.year}, and the organism-side {@code external} read.
 *
 * <p>Backed by the committed {@code maxtemp_tulare_annual.nc} fixture (variable
 * {@code Maximum_air_temperature_at_2m}, {@code calendar_year} 2024..2053). The fixture is read
 * ordinally (record i = the i-th simulation step), so the same records must surface whether the
 * run is keyed at 0.. or 2024.. - only the reported year shifts.</p>
 */
public class ExternalTimestepAlignmentIntegrationTest {

  private static final String VARIABLE = "Maximum_air_temperature_at_2m";

  @TempDir
  Path tempDir;

  /** Build a sim that exports meta.year and the external temperature over a steps window. */
  private Path writeScript(String name, long stepsLow, long stepsHigh, Path csvTarget)
      throws Exception {
    String script = """
        start simulation Test
          grid.size = 16000 m
          grid.low = 36.73 degrees latitude, -119.52 degrees longitude
          grid.high = 35.80 degrees latitude, -117.98 degrees longitude
          grid.patch = "Default"
          steps.low = %d count
          steps.high = %d count
          exportFiles.patch = "file://%s"
        end simulation

        start patch Default
          export.year.step = meta.year
          export.temperature.step = external temperature
        end patch
        """.formatted(stepsLow, stepsHigh, csvTarget.toString());
    Path scriptFile = tempDir.resolve(name + ".josh");
    Files.writeString(scriptFile, script);
    return scriptFile;
  }

  private Path fixture() throws Exception {
    return Path.of(getClass().getResource("/netcdf/maxtemp_tulare_annual.nc").toURI());
  }

  private DoublePrecomputedGrid preprocessGrid(
      long stepsLow, long stepsHigh, PreprocessUtil.PreprocessOptions options) throws Exception {
    Path script = writeScript("pre_" + stepsLow, stepsLow, stepsHigh,
        tempDir.resolve("unused.csv"));
    Path out = tempDir.resolve("grid_" + stepsLow + ".jshd");
    PreprocessUtil.preprocess(script.toFile(), "Test", fixture().toString(), VARIABLE, "K",
        out.toFile(), options, new OutputOptions());
    try (FileInputStream in = new FileInputStream(out.toFile())) {
      DataGridLayer layer = new BinaryGridSerializationStrategy(new ValueSupportFactory())
          .deserialize(in);
      return (DoublePrecomputedGrid) layer;
    }
  }

  private static double sumNonZero(DoublePrecomputedGrid grid, long timestep) {
    double total = 0;
    for (long x = grid.getMinX(); x <= grid.getMaxX(); x++) {
      for (long y = grid.getMinY(); y <= grid.getMaxY(); y++) {
        total += Math.abs(grid.getAt(x, y, timestep).getAsDouble());
      }
    }
    return total;
  }

  /**
   * Preprocessing the same source at steps.low=0 and steps.low=2024 must store the SAME records,
   * just keyed at different (steps.low-relative) timesteps - record i lands at grid key
   * steps.low + i in both.
   */
  @Test
  public void preprocessAlignsSourceRecordsToStepsLow() throws Exception {
    PreprocessUtil.PreprocessOptions opts = new PreprocessUtil.PreprocessOptions();
    DoublePrecomputedGrid base = preprocessGrid(0, 2, opts);
    DoublePrecomputedGrid calendar = preprocessGrid(2024, 2026, opts);

    assertEquals(0, base.getMinTimestep(), "baseline grid keyed from 0");
    assertEquals(2024, calendar.getMinTimestep(), "calendar grid keyed from steps.low");
    assertEquals(2026, calendar.getMaxTimestep());

    // Sanity: the fixture actually populated data (guards against a bad extent silently passing).
    assertTrue(sumNonZero(base, 0) > 0, "baseline step 0 should have real data");
    assertTrue(sumNonZero(calendar, 2024) > 0, "calendar step 2024 should have real data");

    // Record i (0..2) must be identical whether keyed at i or at 2024 + i.
    for (int i = 0; i <= 2; i++) {
      for (long x = base.getMinX(); x <= base.getMaxX(); x++) {
        for (long y = base.getMinY(); y <= base.getMaxY(); y++) {
          assertEquals(
              base.getAt(x, y, i).getAsDouble(),
              calendar.getAt(x, y, 2024 + i).getAsDouble(),
              1e-9,
              "record " + i + " at (" + x + "," + y + ") must match across steps.low offsets");
        }
      }
    }
  }

  /**
   * A calendar steps.low run reads real external data (not zeros, no crash) and reports calendar
   * years via meta.year alone - and the temperature values are identical to a steps.low=0 run of
   * the same records, confirming no data regression while the year column shifts.
   */
  @Test
  public void calendarStepsLowRunReadsRealDataAndShiftsYearOnly() throws Exception {
    RunResult calendar = runWindow(2024, 2026);
    RunResult baseline = runWindow(0, 2);

    // New behavior: meta.year alone yields calendar years, and external data is real - the bug
    // this fixes produced an all-zero grid, so require physical Kelvin values to be present.
    // (Edge patches outside the fixture's footprint are legitimately zero in both runs.)
    assertEquals(new TreeSet<>(List.of(2024L, 2025L, 2026L)), calendar.years,
        "calendar run year column should read 2024..2026 from meta.year directly");
    assertTrue(calendar.temperatures.stream().anyMatch(t -> t > 250.0),
        "calendar run external temperatures should include real Kelvin values, not all zeros");

    // No regression: baseline still works and reads the same data, only the year label differs.
    assertEquals(new TreeSet<>(List.of(0L, 1L, 2L)), baseline.years,
        "baseline run year column should read 0..2");
    assertEquals(baseline.temperatures, calendar.temperatures,
        "the same source records must be read in both runs; only the year column shifts");
  }

  /**
   * The forced single-timestep path (--timestep) keeps its legacy absolute index: forcing
   * timestep 2 reads source record 2, matching the steps.low=0 preprocessing's record 2.
   */
  @Test
  public void forcedTimestepStillReadsAbsoluteSourceIndex() throws Exception {
    final DoublePrecomputedGrid base = preprocessGrid(0, 2, new PreprocessUtil.PreprocessOptions());
    PreprocessUtil.PreprocessOptions forced = new PreprocessUtil.PreprocessOptions(
        "EPSG:4326", "lon", "lat", "calendar_year", "2", null, false, false);
    DoublePrecomputedGrid forcedGrid = preprocessGrid(2, 2, forced);

    assertEquals(2, forcedGrid.getMinTimestep());
    assertEquals(2, forcedGrid.getMaxTimestep());
    assertTrue(sumNonZero(forcedGrid, 2) > 0, "forced timestep should still read real data");
    for (long x = base.getMinX(); x <= base.getMaxX(); x++) {
      for (long y = base.getMinY(); y <= base.getMaxY(); y++) {
        assertEquals(
            base.getAt(x, y, 2).getAsDouble(),
            forcedGrid.getAt(x, y, 2).getAsDouble(),
            1e-9,
            "forced --timestep 2 must read the same record as record 2 of a 0-based run");
      }
    }
  }

  /** Preprocess + run a window, returning the observed year set and temperature values. */
  private RunResult runWindow(long stepsLow, long stepsHigh) throws Exception {
    preprocessGrid(stepsLow, stepsHigh, new PreprocessUtil.PreprocessOptions());
    Path jshd = tempDir.resolve("grid_" + stepsLow + ".jshd"); // written by preprocessGrid
    assertTrue(Files.exists(jshd));

    Path outDir = Files.createDirectories(tempDir.resolve("run_" + stepsLow));
    Path csvTarget = outDir.resolve("results_{replicate}.csv");
    Path script = writeScript("run_" + stepsLow, stepsLow, stepsHigh, csvTarget);

    RunUtil.RunOptions options = RunUtil.RunOptions.builder(script.toFile(), "Test")
        .replicates(1)
        .dataFiles(new String[] {"temperature.jshd=" + jshd})
        .seed(Optional.of(42L))
        .build();
    RunUtil.RunResult result = RunUtil.run(options, new OutputOptions());
    assertTrue(result.isSuccess(),
        "run with steps.low=" + stepsLow + " should succeed: " + result.getMessage());

    Path csv = outDir.resolve("results_0.csv");
    assertTrue(Files.exists(csv), "expected output CSV at " + csv);
    return parseCsv(csv);
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
    assertFalse(yearCol < 0 || tempCol < 0, "CSV must have year and temperature columns");

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
}
