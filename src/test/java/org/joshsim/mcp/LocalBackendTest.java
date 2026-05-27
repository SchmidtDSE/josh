/**
 * Tests for the LocalBackend MCP backend implementation.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.mcp;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;


/**
 * Unit tests for LocalBackend.
 *
 * <p>Tests the validate, discoverConfig methods with real (in-memory) Josh scripts.
 * Avoids running full simulations or preprocessing to keep tests fast and self-contained.</p>
 */
public class LocalBackendTest {

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

  private static final String INVALID_SCRIPT = """
      start simulation BrokenSim
        grid.size = INVALID_SYNTAX_HERE ???
      end simulation
      """;

  private static final String SCRIPT_WITH_CONFIG = """
      start simulation ConfigSim
        grid.size = config params.gridSize
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

  private LocalBackend backend;

  /**
   * Sets up the test backend before each test.
   */
  @BeforeEach
  public void setUp() {
    backend = new LocalBackend(new StderrOutputOptions());
  }

  /**
   * Tests that a valid script passes validation.
   */
  @Test
  public void testValidateValidScript() throws IOException {
    Path scriptFile = tempDir.resolve("valid.josh");
    Files.writeString(scriptFile, MINIMAL_SCRIPT);

    Backend.ValidateResult result = backend.validate(scriptFile);

    assertNotNull(result);
    assertTrue(result.isSuccess(), "Valid script should pass validation: " + result.getMessage());
  }

  /**
   * Tests that a missing script file returns a failure.
   */
  @Test
  public void testValidateMissingFile() {
    Path nonExistent = tempDir.resolve("does_not_exist.josh");

    Backend.ValidateResult result = backend.validate(nonExistent);

    assertNotNull(result);
    assertFalse(result.isSuccess(), "Missing file should fail validation");
  }

  /**
   * Tests that discoverConfig returns success and a result for a valid script.
   */
  @Test
  public void testDiscoverConfigValidScript() throws IOException {
    Path scriptFile = tempDir.resolve("noconfig.josh");
    Files.writeString(scriptFile, MINIMAL_SCRIPT);

    Backend.DiscoverConfigResult result = backend.discoverConfig(scriptFile);

    assertNotNull(result);
    assertTrue(result.isSuccess(),
        "discoverConfig should succeed for valid script: " + result.getOutput());
  }

  /**
   * Tests that discoverConfig returns "[No variables found]" when there are no config references.
   */
  @Test
  public void testDiscoverConfigNoConfigVariables() throws IOException {
    Path scriptFile = tempDir.resolve("noconfig.josh");
    Files.writeString(scriptFile, MINIMAL_SCRIPT);

    Backend.DiscoverConfigResult result = backend.discoverConfig(scriptFile);

    assertNotNull(result);
    assertTrue(result.isSuccess(), "Should succeed: " + result.getOutput());
    assertTrue(result.getOutput().contains("[No variables found]"),
        "Should report no config variables, got: " + result.getOutput());
  }

  /**
   * Tests that discoverConfig finds config variables when they are present.
   */
  @Test
  public void testDiscoverConfigFindsVariables() throws IOException {
    Path scriptFile = tempDir.resolve("config.josh");
    Files.writeString(scriptFile, SCRIPT_WITH_CONFIG);

    Backend.DiscoverConfigResult result = backend.discoverConfig(scriptFile);

    assertNotNull(result);
    assertTrue(result.isSuccess(), "Should succeed: " + result.getOutput());
    assertFalse(result.getOutput().contains("[No variables found]"),
        "Should find config variables, got: " + result.getOutput());
    assertTrue(result.getOutput().contains("params.gridSize"),
        "Should find params.gridSize config variable, got: " + result.getOutput());
  }

  /**
   * Tests that discoverConfig returns failure for a missing file.
   */
  @Test
  public void testDiscoverConfigMissingFile() {
    Path nonExistent = tempDir.resolve("missing.josh");

    Backend.DiscoverConfigResult result = backend.discoverConfig(nonExistent);

    assertNotNull(result);
    assertFalse(result.isSuccess(), "Should fail for missing file");
  }

  /**
   * Tests that Backend.ValidateResult stores isSuccess and message correctly.
   */
  @Test
  public void testValidateResultFields() {
    Backend.ValidateResult ok = new Backend.ValidateResult(true, "All good");
    assertTrue(ok.isSuccess());
    assertTrue(ok.getMessage().contains("All good"));

    Backend.ValidateResult fail = new Backend.ValidateResult(false, "Bad syntax");
    assertFalse(fail.isSuccess());
    assertTrue(fail.getMessage().contains("Bad syntax"));
  }

  /**
   * Tests that Backend.DiscoverConfigResult stores fields correctly.
   */
  @Test
  public void testDiscoverConfigResultFields() {
    Backend.DiscoverConfigResult ok = new Backend.DiscoverConfigResult(true, "result text");
    assertTrue(ok.isSuccess());
    assertTrue(ok.getOutput().contains("result text"));

    Backend.DiscoverConfigResult fail = new Backend.DiscoverConfigResult(false, "error text");
    assertFalse(fail.isSuccess());
    assertTrue(fail.getOutput().contains("error text"));
  }

  /**
   * Tests that Backend.RunSimulationResult stores fields correctly.
   */
  @Test
  public void testRunSimulationResultFields() {
    Backend.RunSimulationResult ok = new Backend.RunSimulationResult(true, "done", 42L);
    assertTrue(ok.isSuccess());
    assertTrue(ok.getMessage().contains("done"));
    assertTrue(ok.getStepsCompleted() == 42L);
  }

  /**
   * Tests that Backend.PreprocessResult stores fields correctly.
   */
  @Test
  public void testPreprocessResultFields() {
    Backend.PreprocessResult ok = new Backend.PreprocessResult(true, "success");
    assertTrue(ok.isSuccess());
    assertTrue(ok.getMessage().contains("success"));

    Backend.PreprocessResult fail = new Backend.PreprocessResult(false, "failed");
    assertFalse(fail.isSuccess());
    assertTrue(fail.getMessage().contains("failed"));
  }

  /**
   * Tests that a data mapping whose name contains a mini-language separator is rejected with a
   * clear error before any simulation work begins.
   */
  @Test
  public void testRunSimulationRejectsSeparatorInDataName() {
    Path script = tempDir.resolve("any.josh");
    java.util.Map<String, Path> data =
        java.util.Map.of("bad;name.jshd", tempDir.resolve("x.jshd"));

    Backend.RunSimulationResult result =
        backend.runSimulation(script, "TestSim", 1, false, Optional.empty(), data);

    assertFalse(result.isSuccess(), "Separator in data name should fail fast");
    assertTrue(result.getMessage().contains("bad;name.jshd"),
        "Error should name the offending key, got: " + result.getMessage());
  }

  /**
   * Tests that StderrOutputOptions does not throw when methods are called.
   */
  @Test
  public void testStderrOutputOptionsDoesNotThrow() {
    StderrOutputOptions opts = new StderrOutputOptions();
    // These should print to stderr without throwing
    opts.printInfo("info message");
    opts.printError("error message");
  }
}
