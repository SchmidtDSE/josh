/**
 * Tests that preprocessing rejects unsupported grid size units before building the grid.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.command;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.joshsim.command.PreprocessUtil.PreprocessOptions;
import org.joshsim.util.OutputOptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Regression tests for grid size unit handling in {@link PreprocessUtil}.
 *
 * <p>An unsupported unit such as {@code km} used to be silently treated as meters by the
 * Earth-space patch builder, producing a grid orders of magnitude too fine that hung
 * preprocessing for hundreds of seconds. The unit guard must reject such units up front,
 * before any grid is built.</p>
 */
public class PreprocessUtilGridSizeUnitsTest {

  private static final String KM_SCRIPT = """
      start simulation TestSim
        grid.size = 16 km
        grid.low = 36.73 degrees latitude, -119.52 degrees longitude
        grid.high = 35.80 degrees latitude, -117.98 degrees longitude
        grid.patch = "Default"
        steps.low = 0 count
        steps.high = 1 count
        exportFiles.patch = "file:///tmp/test_out.csv"
      end simulation

      start patch Default
        export.count.step = 1 count
      end patch
      """;

  @TempDir
  Path tempDir;

  /**
   * A grid size in kilometers must be rejected quickly with a clear message rather than
   * hanging while an enormous grid is materialized.
   */
  @Test
  public void gridSizeInKilometersFailsFast() throws Exception {
    Path scriptFile = tempDir.resolve("km.josh");
    Files.writeString(scriptFile, KM_SCRIPT);
    File dataFile = tempDir.resolve("data.nc").toFile();
    File outputFile = tempDir.resolve("out.jshd").toFile();

    IllegalArgumentException ex = assertTimeoutPreemptively(Duration.ofSeconds(60), () ->
        assertThrows(IllegalArgumentException.class, () ->
            PreprocessUtil.preprocess(
                scriptFile.toFile(),
                "TestSim",
                dataFile.toString(),
                "data",
                "K",
                outputFile,
                new PreprocessOptions(),
                new OutputOptions()
            )
        )
    );

    assertTrue(ex.getMessage().contains("Unsupported units for grid size"),
        "Expected an unsupported-units error, got: " + ex.getMessage());
    assertTrue(ex.getMessage().contains("km"),
        "Error should name the offending unit, got: " + ex.getMessage());
  }
}
