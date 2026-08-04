package org.joshsim.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

/**
 * Unit tests for the {@link InspectExportsCommand} class.
 *
 * <p>These tests cover the output format selection, which previously inverted: passing
 * {@code --json} emitted plain text because picocli sets a matched boolean flag to the opposite of
 * its default.</p>
 */
public class InspectExportsCommandTest {

  private static final Path SCRIPT_PATH = Path.of("docs/src/guides/hello/hello.josh");
  private static final String SIMULATION_NAME = "Main";

  private ByteArrayOutputStream outContent;
  private ByteArrayOutputStream errContent;
  private PrintStream originalOut;
  private PrintStream originalErr;

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
  public void testJsonIsTheDefault() {
    int exitCode = new CommandLine(new InspectExportsCommand())
        .execute(SCRIPT_PATH.toString(), SIMULATION_NAME);

    assertEquals(0, exitCode);
    String output = outContent.toString();
    assertTrue(output.contains("\"exportFiles\""), output);
    assertTrue(output.contains("memory://editor/patches"), output);
  }

  @Test
  public void testJsonFlagIsNotInverted() {
    // Regression: `--json` used to emit plain text.
    int exitCode = new CommandLine(new InspectExportsCommand())
        .execute(SCRIPT_PATH.toString(), SIMULATION_NAME, "--json");

    assertEquals(0, exitCode);
    String output = outContent.toString();
    assertTrue(output.contains("\"exportFiles\""), output);
    assertFalse(output.contains("Export Files:"), output);
  }

  @Test
  public void testNoJsonSelectsPlainText() {
    int exitCode = new CommandLine(new InspectExportsCommand())
        .execute(SCRIPT_PATH.toString(), SIMULATION_NAME, "--no-json");

    assertEquals(0, exitCode);
    String output = outContent.toString();
    assertTrue(output.contains("Export Files:"), output);
    assertFalse(output.contains("\"exportFiles\""), output);
  }

  @Test
  public void testConflictingFormatFlagsRejected() {
    int exitCode = new CommandLine(new InspectExportsCommand())
        .execute(SCRIPT_PATH.toString(), SIMULATION_NAME, "--json", "--no-json");

    assertEquals(6, exitCode);
    assertTrue(errContent.toString().contains("mutually exclusive"), errContent.toString());
  }

  @Test
  public void testMissingSimulationStillReturnsItsOwnCode() {
    // The usage code must not collide with the pre-existing "simulation not found" code.
    int exitCode = new CommandLine(new InspectExportsCommand())
        .execute(SCRIPT_PATH.toString(), "NoSuchSimulation");

    assertEquals(4, exitCode);
    assertTrue(errContent.toString().contains("Could not find simulation"), errContent.toString());
  }
}
