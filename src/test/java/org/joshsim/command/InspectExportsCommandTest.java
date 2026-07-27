package org.joshsim.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

/**
 * Unit tests for the {@link InspectExportsCommand} class.
 *
 * <p>These tests cover the output format selection, which previously inverted: passing
 * {@code --json} emitted plain text because picocli sets a matched boolean flag to the opposite of
 * its default.</p>
 */
public class InspectExportsCommandTest {

  private static final String SIMULATION =
      "start simulation Main\n"
      + "  grid.size = 1000 m\n"
      + "  grid.low = 33.7 degrees latitude, -115.4 degrees longitude\n"
      + "  grid.high = 33.8 degrees latitude, -115.5 degrees longitude\n"
      + "  steps.low = 0 count\n"
      + "  steps.high = 1 count\n"
      + "  exportFiles.patch = \"file:///tmp/inspect_exports_test.csv\"\n"
      + "end simulation\n"
      + "\n"
      + "start patch Default\n"
      + "  value.init = 1 count\n"
      + "end patch\n";

  private ByteArrayOutputStream outContent;
  private ByteArrayOutputStream errContent;
  private PrintStream originalOut;
  private PrintStream originalErr;

  @TempDir
  Path tempDir;

  /**
   * Sets up output stream capture for both stdout and stderr.
   */
  @BeforeEach
  public void setUp() {
    outContent = new ByteArrayOutputStream();
    errContent = new ByteArrayOutputStream();
    originalOut = System.out;
    originalErr = System.err;
    System.setOut(new PrintStream(outContent));
    System.setErr(new PrintStream(errContent));
  }

  /**
   * Restores the original stdout and stderr streams.
   */
  @AfterEach
  public void tearDown() {
    System.setOut(originalOut);
    System.setErr(originalErr);
  }

  @Test
  public void testJsonIsTheDefault() throws IOException {
    Path script = writeScript();

    int exitCode = new CommandLine(new InspectExportsCommand()).execute(script.toString(), "Main");

    assertEquals(0, exitCode);
    String output = outContent.toString();
    assertTrue(output.contains("\"exportFiles\""), output);
    assertTrue(output.contains("inspect_exports_test.csv"), output);
  }

  @Test
  public void testJsonFlagIsNotInverted() throws IOException {
    // Regression: `--json` used to emit plain text.
    Path script = writeScript();

    int exitCode = new CommandLine(new InspectExportsCommand())
        .execute(script.toString(), "Main", "--json");

    assertEquals(0, exitCode);
    String output = outContent.toString();
    assertTrue(output.contains("\"exportFiles\""), output);
    assertFalse(output.contains("Export Files:"), output);
  }

  @Test
  public void testNoJsonSelectsPlainText() throws IOException {
    Path script = writeScript();

    int exitCode = new CommandLine(new InspectExportsCommand())
        .execute(script.toString(), "Main", "--no-json");

    assertEquals(0, exitCode);
    String output = outContent.toString();
    assertTrue(output.contains("Export Files:"), output);
    assertFalse(output.contains("\"exportFiles\""), output);
  }

  @Test
  public void testConflictingFormatFlagsRejected() throws IOException {
    Path script = writeScript();

    int exitCode = new CommandLine(new InspectExportsCommand())
        .execute(script.toString(), "Main", "--json", "--no-json");

    assertEquals(6, exitCode);
    assertTrue(errContent.toString().contains("mutually exclusive"), errContent.toString());
  }

  @Test
  public void testMissingSimulationStillReturnsItsOwnCode() throws IOException {
    // The usage code must not collide with the pre-existing "simulation not found" code.
    Path script = writeScript();

    int exitCode = new CommandLine(new InspectExportsCommand())
        .execute(script.toString(), "NoSuchSimulation");

    assertEquals(4, exitCode);
    assertTrue(errContent.toString().contains("Could not find simulation"), errContent.toString());
  }

  private Path writeScript() throws IOException {
    Path script = tempDir.resolve("main.josh");
    Files.writeString(script, SIMULATION, StandardCharsets.UTF_8);
    return script;
  }
}
