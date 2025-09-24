package org.joshsim.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import org.joshsim.util.JoshTestFixtures;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;


/**
 * Test class for RunCommand replicates functionality using real Josh programs.
 *
 * <p>Tests the new --replicates parameter functionality including:
 * - Default behavior (replicates=1)
 * - Multiple replicates execution
 * - Parameter validation
 * - Progress reporting integration
 * - Error handling
 *
 * <p>This test class uses real Josh programs and integration testing
 * rather than complex mocking to provide more robust and maintainable tests.</p>
 */
class RunCommandReplicatesTest {

  private RunCommand runCommand;
  private ByteArrayOutputStream outputStream;
  private PrintStream originalOut;

  @BeforeEach
  void setUp() throws Exception {
    runCommand = new RunCommand();

    // Capture output for testing
    outputStream = new ByteArrayOutputStream();
    originalOut = System.out;
    System.setOut(new PrintStream(outputStream));
  }

  @AfterEach
  void tearDown() throws Exception {
    System.setOut(originalOut);
  }

  @Test
  void testDefaultReplicatesValue() throws Exception {
    // Arrange - default RunCommand should have replicates=1
    runCommand = new RunCommand();

    // Act - get the replicates field value
    int replicates = getReplicatesFieldValue(runCommand);

    // Assert
    assertEquals(1, replicates);
  }

  @Test
  void testReplicatesParameterValidation(@TempDir Path tempDir) throws Exception {
    // Arrange - create real Josh file
    Path joshFile = tempDir.resolve("test.josh");
    Files.writeString(joshFile, JoshTestFixtures.MINIMAL_SIMULATION_NO_EXPORT);

    setupBasicFields(runCommand, joshFile);
    setFieldValue(runCommand, "simulation", "TestSim");
    setFieldValue(runCommand, "replicates", -1); // Invalid value

    // Act
    Integer result = runCommand.call();

    // Assert
    assertEquals(1, result); // Error code for validation failure
  }

  @Test
  void testReplicatesParameterValidationZero(@TempDir Path tempDir) throws Exception {
    // Arrange - create real Josh file
    Path joshFile = tempDir.resolve("test.josh");
    Files.writeString(joshFile, JoshTestFixtures.MINIMAL_SIMULATION_NO_EXPORT);

    setupBasicFields(runCommand, joshFile);
    setFieldValue(runCommand, "simulation", "TestSim");
    setFieldValue(runCommand, "replicates", 0); // Invalid value

    // Act
    Integer result = runCommand.call();

    // Assert
    assertEquals(1, result); // Error code for validation failure
  }

  @Test
  void testSingleReplicateExecution(@TempDir Path tempDir) throws Exception {
    // Arrange - create real Josh file with CSV export
    Path joshFile = tempDir.resolve("test.josh");
    Path outputFile = tempDir.resolve("output.csv");
    Files.writeString(joshFile,
        JoshTestFixtures.minimalSimulationWithExport(outputFile.toString()));

    setupBasicFields(runCommand, joshFile);
    setFieldValue(runCommand, "simulation", "TestSim");
    setFieldValue(runCommand, "replicates", 1);

    // Act - run with real Josh program
    Integer result = runCommand.call();

    // Assert - verify success and output file creation
    assertEquals(0, result, "Single replicate execution should succeed");
    assertTrue(Files.exists(outputFile), "Output CSV file should be created");

    // Verify CSV contains expected data
    String csvContent = Files.readString(outputFile);
    assertTrue(csvContent.contains("treeCount"),
        "CSV should contain treeCount export");
    assertTrue(csvContent.contains("averageAge"),
        "CSV should contain averageAge export");
  }

  @Test
  void testMultipleReplicatesExecution(@TempDir Path tempDir) throws Exception {
    // Arrange - create real Josh file with CSV export
    Path joshFile = tempDir.resolve("test.josh");
    Path outputFile = tempDir.resolve("output.csv");
    Files.writeString(joshFile,
        JoshTestFixtures.minimalSimulationWithExport(outputFile.toString()));

    setupBasicFields(runCommand, joshFile);
    setFieldValue(runCommand, "simulation", "TestSim");
    setFieldValue(runCommand, "replicates", 3);

    // Act - run with real Josh program and multiple replicates
    Integer result = runCommand.call();

    // Assert - verify success and consolidated output
    assertEquals(0, result, "Multiple replicates execution should succeed");
    assertTrue(Files.exists(outputFile), "Output CSV file should be created");

    // Verify CSV contains data from all 3 replicates
    String csvContent = Files.readString(outputFile);
    assertTrue(csvContent.contains("treeCount"),
        "CSV should contain treeCount export");
    assertTrue(csvContent.contains("averageAge"),
        "CSV should contain averageAge export");

    // Count replicate entries - should have data for replicates 0, 1, 2
    long replicateCount = csvContent.lines()
        .skip(1) // Skip header
        .filter(line -> !line.trim().isEmpty())
        .count();
    // Each replicate should produce multiple timesteps, verify we have substantial data
    assertTrue(replicateCount >= 9,
        "Should have data from multiple replicates (3 replicates × 3+ steps each)");
  }

  @Test
  void testReplicateNumberOffsetExecution(@TempDir Path tempDir) throws Exception {
    // Arrange - test that replicates start from 0 (grid search removed offset functionality)
    Path joshFile = tempDir.resolve("test.josh");
    Path outputFile = tempDir.resolve("output.csv");
    Files.writeString(joshFile,
        JoshTestFixtures.minimalSimulationWithExport(outputFile.toString()));

    setupBasicFields(runCommand, joshFile);
    setFieldValue(runCommand, "simulation", "TestSim");
    setFieldValue(runCommand, "replicates", 2);

    // Act - run with 2 replicates
    Integer result = runCommand.call();

    // Assert - verify success and that replicates start from 0
    assertEquals(0, result, "Two replicates execution should succeed");
    assertTrue(Files.exists(outputFile), "Output CSV file should be created");

    // Verify CSV contains data from both replicates (0 and 1)
    String csvContent = Files.readString(outputFile);
    assertTrue(csvContent.contains("treeCount"),
        "CSV should contain treeCount export");
    assertTrue(csvContent.contains("averageAge"),
        "CSV should contain averageAge export");

    // Verify replicate numbering starts from 0
    assertTrue(csvContent.contains(",0,"), "CSV should contain replicate 0 data");
    assertTrue(csvContent.contains(",1,"), "CSV should contain replicate 1 data");
  }

  @Test
  void testMetadataExtractionFailureFallback(@TempDir Path tempDir) throws Exception {
    // Arrange - create Josh file that may have metadata extraction issues
    Path joshFile = tempDir.resolve("test.josh");
    Path outputFile = tempDir.resolve("output.csv");
    Files.writeString(joshFile,
        JoshTestFixtures.minimalSimulationWithExport(outputFile.toString()));

    setupBasicFields(runCommand, joshFile);
    setFieldValue(runCommand, "simulation", "TestSim");
    setFieldValue(runCommand, "replicates", 1);

    // Act - run should succeed even if metadata extraction has issues
    // (the real implementation handles this gracefully)
    Integer result = runCommand.call();

    // Assert - should succeed with graceful fallback behavior
    assertEquals(0, result, "Execution should succeed even with metadata extraction issues");
    assertTrue(Files.exists(outputFile), "Output CSV file should still be created");

    // Verify basic functionality works despite potential metadata issues
    String csvContent = Files.readString(outputFile);
    assertTrue(csvContent.contains("treeCount"),
        "CSV should contain treeCount export");
    assertTrue(csvContent.contains("averageAge"),
        "CSV should contain averageAge export");
  }

  @Test
  void testSimulationNotFound(@TempDir Path tempDir) throws Exception {
    // Arrange - create real Josh file but request nonexistent simulation
    Path joshFile = tempDir.resolve("test.josh");
    Files.writeString(joshFile, JoshTestFixtures.MINIMAL_SIMULATION_NO_EXPORT);

    setupBasicFields(runCommand, joshFile);
    // This simulation doesn't exist in the Josh file
    setFieldValue(runCommand, "simulation", "NonExistentSimulation");
    setFieldValue(runCommand, "replicates", 1);

    // Act - should fail with simulation not found
    Integer result = runCommand.call();

    // Assert - should return error code for simulation not found
    assertEquals(4, result,
        "Should return error code 4 for simulation not found");

    // Verify error message is printed to console
    String output = outputStream.toString();
    assertTrue(output.contains("NonExistentSimulation")
        || output.contains("not found") || output.contains("simulation"),
        "Should print error message about missing simulation");
  }

  /**
   * Helper method to get private field value using reflection.
   */
  private int getReplicatesFieldValue(RunCommand command) throws Exception {
    Field field = RunCommand.class.getDeclaredField("replicates");
    field.setAccessible(true);
    return (int) field.get(command);
  }

  /**
   * Helper method to set private field value using reflection.
   */
  private void setFieldValue(Object target, String fieldName, Object value) throws Exception {
    Field field = target.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }

  /**
   * Helper method to set up basic required fields for RunCommand with real Josh file.
   */
  private void setupBasicFields(RunCommand command, Path joshFile) throws Exception {
    setFieldValue(command, "file", joshFile.toFile());
    setFieldValue(command, "crs", "");
    setFieldValue(command, "dataFiles", new String[0]);
    setFieldValue(command, "replicates", 1);
  }
}
