/**
 * Tests that MCP backend operations do not write to stdout.
 *
 * <p>The MCP server uses stdio transport where stdout is exclusively reserved for
 * JSON-RPC messages. Any accidental writes to System.out would corrupt the MCP session.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;


/**
 * Verifies that LocalBackend operations do not write to System.out.
 *
 * <p>This is a critical constraint for MCP servers using stdio transport: stdout must remain
 * clean for JSON-RPC message framing. All diagnostic output must go to stderr.</p>
 */
public class McpStdoutCleanlinessTest {

  private static final String MINIMAL_SCRIPT = """
      start simulation TestSim
        grid.size = 100 m
        grid.low = 0 degrees latitude, 0 degrees longitude
        grid.high = 0.1 degrees latitude, 0.1 degrees longitude
        grid.patch = "Default"
        steps.low = 0 count
        steps.high = 2 count
        exportFiles.patch = "file:///tmp/test_out.csv"
      end simulation

      start patch Default
        export.count.step = 1 count
      end patch
      """;

  @TempDir
  Path tempDir;

  private PrintStream originalStdout;
  private ByteArrayOutputStream capturedStdout;
  private LocalBackend backend;

  /**
   * Sets up stdout capture before each test.
   */
  @BeforeEach
  public void setUp() {
    originalStdout = System.out;
    capturedStdout = new ByteArrayOutputStream();
    System.setOut(new PrintStream(capturedStdout));
    backend = new LocalBackend(new StderrOutputOptions());
  }

  /**
   * Restores stdout after each test.
   */
  @AfterEach
  public void tearDown() {
    System.setOut(originalStdout);
  }

  /**
   * Tests that validate() does not write to stdout.
   */
  @Test
  public void testValidateDoesNotWriteToStdout() throws IOException {
    Path scriptFile = tempDir.resolve("valid.josh");
    Files.writeString(scriptFile, MINIMAL_SCRIPT);

    backend.validate(scriptFile);

    String stdoutContent = capturedStdout.toString();
    assertEquals("", stdoutContent,
        "validate() must not write to stdout, but got: " + stdoutContent);
  }

  /**
   * Tests that discoverConfig() does not write to stdout.
   */
  @Test
  public void testDiscoverConfigDoesNotWriteToStdout() throws IOException {
    Path scriptFile = tempDir.resolve("valid.josh");
    Files.writeString(scriptFile, MINIMAL_SCRIPT);

    backend.discoverConfig(scriptFile);

    String stdoutContent = capturedStdout.toString();
    assertEquals("", stdoutContent,
        "discoverConfig() must not write to stdout, but got: " + stdoutContent);
  }

  /**
   * Tests that validate() on a missing file does not write to stdout.
   */
  @Test
  public void testValidateMissingFileDoesNotWriteToStdout() {
    Path nonExistent = tempDir.resolve("missing.josh");

    backend.validate(nonExistent);

    String stdoutContent = capturedStdout.toString();
    assertEquals("", stdoutContent,
        "validate() on missing file must not write to stdout, but got: " + stdoutContent);
  }

  /**
   * Tests that StderrOutputOptions.printInfo does not write to stdout.
   */
  @Test
  public void testStderrOutputOptionsInfoGoesToStderr() {
    StderrOutputOptions opts = new StderrOutputOptions();
    opts.printInfo("test info message");

    String stdoutContent = capturedStdout.toString();
    assertEquals("", stdoutContent,
        "StderrOutputOptions.printInfo must not write to stdout");
  }

  /**
   * Tests that StderrOutputOptions.printError does not write to stdout.
   */
  @Test
  public void testStderrOutputOptionsErrorGoesToStderr() {
    StderrOutputOptions opts = new StderrOutputOptions();
    opts.printError("test error message");

    String stdoutContent = capturedStdout.toString();
    assertEquals("", stdoutContent,
        "StderrOutputOptions.printError must not write to stdout");
  }
}
