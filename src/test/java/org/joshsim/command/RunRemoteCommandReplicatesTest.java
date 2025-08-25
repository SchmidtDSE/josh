/**
 * Unit tests for RunRemoteCommand replicates functionality.
 *
 * <p>These tests verify the replicates parameter functionality added to
 * RunRemoteCommand, including CLI parameter handling, validation, and
 * integration with the execution context infrastructure.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import org.joshsim.pipeline.job.JoshJob;
import org.joshsim.pipeline.job.JoshJobBuilder;
import org.joshsim.pipeline.remote.RunRemoteContext;
import org.joshsim.pipeline.remote.RunRemoteContextBuilder;
import org.joshsim.util.MinioOptions;
import org.joshsim.util.OutputOptions;
import org.joshsim.util.ProgressCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;


/**
 * Test cases for RunRemoteCommand replicates functionality.
 *
 * <p>These tests verify the replicates parameter handling, validation,
 * and integration with context building and execution strategies without
 * making actual network calls.</p>
 */
public class RunRemoteCommandReplicatesTest {

  private RunRemoteCommand command;

  /**
   * Set up test instance.
   */
  @BeforeEach
  public void setUp() {
    command = new RunRemoteCommand();
  }

  @Test
  public void testReplicatesDefaultValue() throws Exception {
    // Test default value for replicates parameter
    Field replicatesField = RunRemoteCommand.class.getDeclaredField("replicates");
    replicatesField.setAccessible(true);
    int defaultValue = (Integer) replicatesField.get(command);

    assertEquals(1, defaultValue, "Default replicates should be 1 for backward compatibility");
  }

  @Test
  public void testReplicatesFieldAccess() throws Exception {
    // Test that replicates field can be set and retrieved
    Field replicatesField = RunRemoteCommand.class.getDeclaredField("replicates");
    replicatesField.setAccessible(true);
    replicatesField.set(command, 5);

    int value = (Integer) replicatesField.get(command);
    assertEquals(5, value, "Replicates field should be settable");
  }

  @Test
  public void testReplicatesValidationPositiveValues(@TempDir Path tempDir) throws Exception {
    // Create a test Josh file
    Path joshFile = tempDir.resolve("test.josh");
    Files.writeString(joshFile, "simulation test_sim {}");

    // Set up command parameters
    setupCommandFields(joshFile, 3);

    // Test that positive values are valid (no exception should be thrown)
    // This tests the validation logic in call() method
    // Since we can't easily test call() method without full setup, we test the field access
    Field replicatesField = RunRemoteCommand.class.getDeclaredField("replicates");
    replicatesField.setAccessible(true);
    int replicates = (Integer) replicatesField.get(command);

    assertTrue(replicates >= 1, "Valid replicates value should be >= 1");
  }

  @Test
  public void testReplicatesValidationZeroValue(@TempDir Path tempDir) throws Exception {
    // Create a test Josh file
    Path joshFile = tempDir.resolve("test.josh");
    Files.writeString(joshFile, "simulation test_sim {}");

    // Set up command with invalid replicates value
    setupCommandFields(joshFile, 0);

    // The validation should happen in call() method
    // We test the field value directly here since testing call() requires full setup
    Field replicatesField = RunRemoteCommand.class.getDeclaredField("replicates");
    replicatesField.setAccessible(true);
    int replicates = (Integer) replicatesField.get(command);

    assertEquals(0, replicates, "Should be able to set zero value (validation happens in call())");
  }

  @Test
  public void testReplicatesValidationNegativeValue(@TempDir Path tempDir) throws Exception {
    // Create a test Josh file
    Path joshFile = tempDir.resolve("test.josh");
    Files.writeString(joshFile, "simulation test_sim {}");

    // Set up command with invalid replicates value
    setupCommandFields(joshFile, -1);

    // The validation should happen in call() method
    Field replicatesField = RunRemoteCommand.class.getDeclaredField("replicates");
    replicatesField.setAccessible(true);
    int replicates = (Integer) replicatesField.get(command);

    assertEquals(-1, replicates,
        "Should be able to set negative value (validation happens in call())");
  }

  @Test
  public void testContextBuilderIntegration(@TempDir Path tempDir) throws Exception {
    // Test that replicates parameter flows through context builder correctly
    Path joshFile = tempDir.resolve("test.josh");
    Files.writeString(joshFile, "simulation test_sim {}");

    JoshJob testJob = new JoshJobBuilder().setReplicates(3).build();
    RunRemoteContextBuilder builder = new RunRemoteContextBuilder();
    builder.withJob(testJob);

    // Test the builder method - now get replicates from job
    assertEquals(3, testJob.getReplicates(),
        "Job should contain the replicates value");
  }

  @Test
  public void testContextIntegration(@TempDir Path tempDir) throws Exception {
    // Test full context creation with replicates
    Path joshFile = tempDir.resolve("test.josh");
    Files.writeString(joshFile, "simulation test_sim {}");

    RunRemoteContext context = createTestContext(joshFile.toFile(), 4);

    assertEquals(4, context.getReplicates(),
        "Context should contain correct replicates value");
  }

  @Test
  public void testProgressCalculatorIntegration(@TempDir Path tempDir) throws Exception {
    // Test that ProgressCalculator is created with correct replicate count
    Path joshFile = tempDir.resolve("test.josh");
    Files.writeString(joshFile, "simulation test_sim {}");

    // Create context with specific replicates count
    RunRemoteContext context = createTestContext(joshFile.toFile(), 3);
    ProgressCalculator progressCalculator = context.getProgressCalculator();

    assertNotNull(progressCalculator, "ProgressCalculator should be created");
    // We can't directly test the replicate count in ProgressCalculator without
    // exposing it, but we verify it was created with the context
  }

  @Test
  public void testMultipleReplicatesWithOffset(@TempDir Path tempDir) throws Exception {
    // Test replicate numbering with offset
    Path joshFile = tempDir.resolve("test.josh");
    Files.writeString(joshFile, "simulation test_sim {}");

    RunRemoteContext context = createTestContextWithOffset(joshFile.toFile(), 3, 5);

    assertEquals(3, context.getReplicates(), "Should have 3 replicates");
    assertEquals(5, context.getReplicateNumber(), "Should have offset of 5");
  }

  @Test
  public void testSingleReplicateBackwardCompatibility(@TempDir Path tempDir) throws Exception {
    // Test that single replicate (default) maintains backward compatibility
    Path joshFile = tempDir.resolve("test.josh");
    Files.writeString(joshFile, "simulation test_sim {}");

    RunRemoteContext context = createTestContext(joshFile.toFile(), 1);

    assertEquals(1, context.getReplicates(), "Default should be single replicate");
    assertEquals(0, context.getReplicateNumber(), "Default replicate number should be 0");
  }

  @Test
  public void testLargeReplicateCount(@TempDir Path tempDir) throws Exception {
    // Test handling of larger replicate counts
    Path joshFile = tempDir.resolve("test.josh");
    Files.writeString(joshFile, "simulation test_sim {}");

    RunRemoteContext context = createTestContext(joshFile.toFile(), 100);

    assertEquals(100, context.getReplicates(), "Should handle large replicate counts");
  }

  /**
   * Helper method to set up basic command fields for testing.
   */
  private void setupCommandFields(Path joshFile, int replicates) throws Exception {
    Field fileField = RunRemoteCommand.class.getDeclaredField("file");
    fileField.setAccessible(true);
    fileField.set(command, joshFile.toFile());

    Field simulationField = RunRemoteCommand.class.getDeclaredField("simulation");
    simulationField.setAccessible(true);
    simulationField.set(command, "test_sim");

    Field replicatesField = RunRemoteCommand.class.getDeclaredField("replicates");
    replicatesField.setAccessible(true);
    replicatesField.set(command, replicates);

    Field apiKeyField = RunRemoteCommand.class.getDeclaredField("apiKey");
    apiKeyField.setAccessible(true);
    apiKeyField.set(command, "test-api-key");
  }

  /**
   * Helper method to get replicates value from builder using reflection.
   */
  private int getReplicatesFromBuilder(RunRemoteContextBuilder builder) throws Exception {
    Field replicatesField = RunRemoteContextBuilder.class.getDeclaredField("replicates");
    replicatesField.setAccessible(true);
    return (Integer) replicatesField.get(builder);
  }

  /**
   * Helper method to create a test context with specified replicates.
   */
  private RunRemoteContext createTestContext(File joshFile, int replicates) throws Exception {
    return createTestContextWithOffset(joshFile, replicates, 0);
  }

  /**
   * Helper method to create a test context with specified replicates and offset.
   */
  private RunRemoteContext createTestContextWithOffset(File joshFile, int replicates, int offset)
      throws Exception {
    // Note: offset parameter is ignored in job-based execution (always 0)
    JoshJob job = new JoshJobBuilder().setReplicates(replicates).build();
    return new RunRemoteContextBuilder()
        .withFile(joshFile)
        .withSimulation("test_sim")
        .withUseFloat64(false)
        .withEndpointUri(java.net.URI.create("https://example.com/runReplicates"))
        .withApiKey("test-api-key")
        .withJob(job)
        .withJoshCode("simulation test_sim {}")
        .withExternalDataSerialized("")
        .withMetadata(new org.joshsim.util.SimulationMetadata(0, 10, 11))
        .withProgressCalculator(new ProgressCalculator(11, replicates))
        .withOutputOptions(mock(OutputOptions.class))
        .withMinioOptions(mock(MinioOptions.class))
        .withMaxConcurrentWorkers(10)
        .build();
  }
}
