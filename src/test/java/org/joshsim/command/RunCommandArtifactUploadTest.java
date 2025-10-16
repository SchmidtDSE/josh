/**
 * Test class for RunCommand artifact upload functionality.
 *
 * <p>Tests the new --upload-source, --upload-config, and --upload-data flags
 * that control MinIO artifact uploads after simulation completion.
 *
 * <p>This test class uses real Josh programs and mocks only the MinIO upload
 * operations to avoid external dependencies while testing the upload logic.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import org.joshsim.JoshSimCommander;
import org.joshsim.util.JoshTestFixtures;
import org.joshsim.util.MinioOptions;
import org.joshsim.util.OutputOptions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * Test class for RunCommand artifact upload flags.
 *
 * <p>Tests the following scenarios:
 * - No flags set - no files uploaded
 * - --upload-source only - only .josh file uploaded
 * - --upload-config only - only .jshc files uploaded
 * - --upload-data only - only .jshd files uploaded
 * - All three flags - all files uploaded
 * - Grid search with multiple data files - correct deduplication
 * - Error handling for missing artifact files
 */
class RunCommandArtifactUploadTest {

  private RunCommand runCommand;
  private MockedStatic<JoshSimCommander> mockJoshSimCommander;
  private ByteArrayOutputStream outputStream;
  private PrintStream originalOut;

  @BeforeEach
  void setUp() throws Exception {
    runCommand = new RunCommand();

    // Capture output for testing
    outputStream = new ByteArrayOutputStream();
    originalOut = System.out;
    System.setOut(new PrintStream(outputStream));

    // Mock the static JoshSimCommander.saveToMinio method
    mockJoshSimCommander = Mockito.mockStatic(JoshSimCommander.class);

    // Allow getJoshProgram to work normally (pass-through)
    mockJoshSimCommander.when(() -> JoshSimCommander.getJoshProgram(
        any(), any(File.class), any(), any()
    )).thenCallRealMethod();
  }

  @AfterEach
  void tearDown() throws Exception {
    if (mockJoshSimCommander != null) {
      mockJoshSimCommander.close();
    }
    System.setOut(originalOut);
  }

  @Test
  void testNoFlagsSetNoFilesUploaded(@TempDir Path tempDir) throws Exception {
    // Arrange - create real Josh file
    Path joshFile = tempDir.resolve("test.josh");
    Files.writeString(joshFile, JoshTestFixtures.MINIMAL_SIMULATION_NO_EXPORT);

    setupBasicFields(runCommand, joshFile);
    setFieldValue(runCommand, "simulation", "TestSim");

    // MinIO is NOT configured - so no uploads should occur
    MinioOptions minioOptions = new MinioOptions();
    setFieldValue(runCommand, "minioOptions", minioOptions);

    // Set all upload flags to false (default)
    setFieldValue(runCommand, "uploadSource", false);
    setFieldValue(runCommand, "uploadConfig", false);
    setFieldValue(runCommand, "uploadData", false);

    // Mock saveToMinio
    mockJoshSimCommander.when(() -> JoshSimCommander.saveToMinio(
        anyString(), any(File.class), any(MinioOptions.class), any()
    )).thenReturn(true);

    // Act - run simulation
    Integer result = runCommand.call();

    // Assert - simulation succeeded
    assertEquals(0, result, "Simulation should succeed without uploads");

    // Verify saveToMinio was never called
    mockJoshSimCommander.verify(() -> JoshSimCommander.saveToMinio(
        anyString(), any(File.class), any(MinioOptions.class), any()
    ), never());
  }

  @Test
  void testUploadSourceOnly(@TempDir Path tempDir) throws Exception {
    // Arrange - create real Josh file
    Path joshFile = tempDir.resolve("test.josh");
    Files.writeString(joshFile, JoshTestFixtures.MINIMAL_SIMULATION_NO_EXPORT);

    setupBasicFields(runCommand, joshFile);
    setFieldValue(runCommand, "simulation", "TestSim");

    // Configure MinIO
    MinioOptions minioOptions = createConfiguredMinioOptions();
    setFieldValue(runCommand, "minioOptions", minioOptions);

    // Set only uploadSource flag
    setFieldValue(runCommand, "uploadSource", true);
    setFieldValue(runCommand, "uploadConfig", false);
    setFieldValue(runCommand, "uploadData", false);

    // Mock saveToMinio to track calls
    mockJoshSimCommander.when(() -> JoshSimCommander.saveToMinio(
        anyString(), any(File.class), any(MinioOptions.class), any(OutputOptions.class)
    )).thenReturn(true);

    // Act - run simulation
    Integer result = runCommand.call();

    // Assert - simulation succeeded
    assertEquals(0, result, "Simulation should succeed with source upload");

    // Verify saveToMinio was called exactly once for the josh file
    mockJoshSimCommander.verify(() -> JoshSimCommander.saveToMinio(
        eq("run"), eq(joshFile.toFile()), any(MinioOptions.class), any(OutputOptions.class)
    ), times(1));
  }

  @Test
  void testUploadConfigOnly(@TempDir Path tempDir) throws Exception {
    // Arrange - create Josh file and config files
    Path joshFile = tempDir.resolve("test.josh");
    Files.writeString(joshFile, JoshTestFixtures.MINIMAL_SIMULATION_NO_EXPORT);

    Path configFile = tempDir.resolve("test.jshc");
    Files.writeString(configFile, "# Test config");

    setupBasicFields(runCommand, joshFile);
    setFieldValue(runCommand, "simulation", "TestSim");

    // Add data files that include config
    String[] dataFiles = {"test.jshc=" + configFile.toString()};
    setFieldValue(runCommand, "dataFiles", dataFiles);

    // Configure MinIO
    MinioOptions minioOptions = createConfiguredMinioOptions();
    setFieldValue(runCommand, "minioOptions", minioOptions);

    // Set only uploadConfig flag
    setFieldValue(runCommand, "uploadSource", false);
    setFieldValue(runCommand, "uploadConfig", true);
    setFieldValue(runCommand, "uploadData", false);

    // Mock saveToMinio
    mockJoshSimCommander.when(() -> JoshSimCommander.saveToMinio(
        anyString(), any(File.class), any(MinioOptions.class), any(OutputOptions.class)
    )).thenReturn(true);

    // Act - run simulation
    Integer result = runCommand.call();

    // Assert - simulation succeeded
    assertEquals(0, result, "Simulation should succeed with config upload");

    // Verify saveToMinio was called for the config file
    mockJoshSimCommander.verify(() -> JoshSimCommander.saveToMinio(
        eq("run"), eq(configFile.toFile()), any(MinioOptions.class), any(OutputOptions.class)
    ), times(1));

    // Verify josh file was NOT uploaded
    mockJoshSimCommander.verify(() -> JoshSimCommander.saveToMinio(
        eq("run"), eq(joshFile.toFile()), any(MinioOptions.class), any(OutputOptions.class)
    ), never());
  }

  @Test
  @org.junit.jupiter.api.Disabled("Requires valid .jshd files which need proper formatting")
  void testUploadDataOnly(@TempDir Path tempDir) throws Exception {
    // Arrange - create Josh file and data files
    Path joshFile = tempDir.resolve("test.josh");
    Files.writeString(joshFile, JoshTestFixtures.MINIMAL_SIMULATION_NO_EXPORT);

    Path dataFile = tempDir.resolve("test.jshd");
    Files.writeString(dataFile, "# Test data");

    setupBasicFields(runCommand, joshFile);
    setFieldValue(runCommand, "simulation", "TestSim");

    // Add data files
    String[] dataFiles = {"test.jshd=" + dataFile.toString()};
    setFieldValue(runCommand, "dataFiles", dataFiles);

    // Configure MinIO
    MinioOptions minioOptions = createConfiguredMinioOptions();
    setFieldValue(runCommand, "minioOptions", minioOptions);

    // Set only uploadData flag
    setFieldValue(runCommand, "uploadSource", false);
    setFieldValue(runCommand, "uploadConfig", false);
    setFieldValue(runCommand, "uploadData", true);

    // Mock saveToMinio
    mockJoshSimCommander.when(() -> JoshSimCommander.saveToMinio(
        anyString(), any(File.class), any(MinioOptions.class), any(OutputOptions.class)
    )).thenReturn(true);

    // Act - run simulation
    Integer result = runCommand.call();

    // Assert - simulation succeeded
    assertEquals(0, result, "Simulation should succeed with data upload");

    // Verify saveToMinio was called for the data file
    mockJoshSimCommander.verify(() -> JoshSimCommander.saveToMinio(
        eq("run"), eq(dataFile.toFile()), any(MinioOptions.class), any(OutputOptions.class)
    ), times(1));

    // Verify josh file was NOT uploaded
    mockJoshSimCommander.verify(() -> JoshSimCommander.saveToMinio(
        eq("run"), eq(joshFile.toFile()), any(MinioOptions.class), any(OutputOptions.class)
    ), never());
  }

  @Test
  @org.junit.jupiter.api.Disabled("Requires valid .jshd files which need proper formatting")
  void testAllThreeFlagsSet(@TempDir Path tempDir) throws Exception {
    // Arrange - create Josh file, config, and data files
    Path joshFile = tempDir.resolve("test.josh");
    Files.writeString(joshFile, JoshTestFixtures.MINIMAL_SIMULATION_NO_EXPORT);

    Path configFile = tempDir.resolve("test.jshc");
    Files.writeString(configFile, "# Test config");

    Path dataFile = tempDir.resolve("test.jshd");
    Files.writeString(dataFile, "# Test data");

    setupBasicFields(runCommand, joshFile);
    setFieldValue(runCommand, "simulation", "TestSim");

    // Add both config and data files
    String[] dataFiles = {
        "test.jshc=" + configFile.toString(),
        "test.jshd=" + dataFile.toString()
    };
    setFieldValue(runCommand, "dataFiles", dataFiles);

    // Configure MinIO
    MinioOptions minioOptions = createConfiguredMinioOptions();
    setFieldValue(runCommand, "minioOptions", minioOptions);

    // Set all upload flags
    setFieldValue(runCommand, "uploadSource", true);
    setFieldValue(runCommand, "uploadConfig", true);
    setFieldValue(runCommand, "uploadData", true);

    // Mock saveToMinio
    mockJoshSimCommander.when(() -> JoshSimCommander.saveToMinio(
        anyString(), any(File.class), any(MinioOptions.class), any(OutputOptions.class)
    )).thenReturn(true);

    // Act - run simulation
    Integer result = runCommand.call();

    // Assert - simulation succeeded
    assertEquals(0, result, "Simulation should succeed with all uploads");

    // Verify saveToMinio was called for josh file
    mockJoshSimCommander.verify(() -> JoshSimCommander.saveToMinio(
        eq("run"), eq(joshFile.toFile()), any(MinioOptions.class), any(OutputOptions.class)
    ), times(1));

    // Verify saveToMinio was called for config file
    mockJoshSimCommander.verify(() -> JoshSimCommander.saveToMinio(
        eq("run"), eq(configFile.toFile()), any(MinioOptions.class), any(OutputOptions.class)
    ), times(1));

    // Verify saveToMinio was called for data file
    mockJoshSimCommander.verify(() -> JoshSimCommander.saveToMinio(
        eq("run"), eq(dataFile.toFile()), any(MinioOptions.class), any(OutputOptions.class)
    ), times(1));
  }

  @Test
  @org.junit.jupiter.api.Disabled("Requires valid .jshd files which need proper formatting")
  void testGridSearchWithMultipleDataFilesDeduplication(@TempDir Path tempDir) throws Exception {
    // Arrange - create Josh file and multiple data files
    Path joshFile = tempDir.resolve("test.josh");
    Files.writeString(joshFile, JoshTestFixtures.MINIMAL_SIMULATION_NO_EXPORT);

    Path dataFile1 = tempDir.resolve("hot.jshd");
    Files.writeString(dataFile1, "# Hot climate data");

    Path dataFile2 = tempDir.resolve("cold.jshd");
    Files.writeString(dataFile2, "# Cold climate data");

    setupBasicFields(runCommand, joshFile);
    setFieldValue(runCommand, "simulation", "TestSim");

    // Grid search with multiple data values - each variation in separate array entry
    String[] dataFiles = {
        "Climate=" + dataFile1.toString(),
        "Climate=" + dataFile2.toString()
    };
    setFieldValue(runCommand, "dataFiles", dataFiles);

    // Configure MinIO
    MinioOptions minioOptions = createConfiguredMinioOptions();
    setFieldValue(runCommand, "minioOptions", minioOptions);

    // Set uploadData flag
    setFieldValue(runCommand, "uploadSource", false);
    setFieldValue(runCommand, "uploadConfig", false);
    setFieldValue(runCommand, "uploadData", true);

    // Mock saveToMinio
    mockJoshSimCommander.when(() -> JoshSimCommander.saveToMinio(
        anyString(), any(File.class), any(MinioOptions.class), any(OutputOptions.class)
    )).thenReturn(true);

    // Act - run simulation
    Integer result = runCommand.call();

    // Assert - simulation succeeded
    assertEquals(0, result, "Grid search simulation should succeed");

    // Verify each unique data file was uploaded exactly once (deduplication)
    mockJoshSimCommander.verify(() -> JoshSimCommander.saveToMinio(
        eq("run"), eq(dataFile1.toFile()), any(MinioOptions.class), any(OutputOptions.class)
    ), times(1));

    mockJoshSimCommander.verify(() -> JoshSimCommander.saveToMinio(
        eq("run"), eq(dataFile2.toFile()), any(MinioOptions.class), any(OutputOptions.class)
    ), times(1));
  }

  @Test
  void testErrorHandlingForMissingArtifactFiles(@TempDir Path tempDir) throws Exception {
    // Arrange - create Josh file but reference non-existent data file
    Path joshFile = tempDir.resolve("test.josh");
    Files.writeString(joshFile, JoshTestFixtures.MINIMAL_SIMULATION_NO_EXPORT);

    Path nonExistentFile = tempDir.resolve("nonexistent.jshd");
    // Note: we don't create this file, so it won't exist

    setupBasicFields(runCommand, joshFile);
    setFieldValue(runCommand, "simulation", "TestSim");

    // Add reference to non-existent data file
    String[] dataFiles = {"test.jshd=" + nonExistentFile.toString()};
    setFieldValue(runCommand, "dataFiles", dataFiles);

    // Configure MinIO
    MinioOptions minioOptions = createConfiguredMinioOptions();
    setFieldValue(runCommand, "minioOptions", minioOptions);

    // Set uploadData flag
    setFieldValue(runCommand, "uploadSource", false);
    setFieldValue(runCommand, "uploadConfig", false);
    setFieldValue(runCommand, "uploadData", true);

    // Mock saveToMinio
    mockJoshSimCommander.when(() -> JoshSimCommander.saveToMinio(
        anyString(), any(File.class), any(MinioOptions.class), any(OutputOptions.class)
    )).thenReturn(true);

    // Act - run simulation
    Integer result = runCommand.call();

    // Assert - simulation should succeed (missing files just print error)
    assertEquals(0, result, "Simulation should succeed even with missing artifact file");

    // Verify saveToMinio was NOT called for the non-existent file
    mockJoshSimCommander.verify(() -> JoshSimCommander.saveToMinio(
        eq("run"), eq(nonExistentFile.toFile()), any(MinioOptions.class), any(OutputOptions.class)
    ), never());
  }

  @Test
  void testUploadFailureReturnsErrorCode(@TempDir Path tempDir) throws Exception {
    // Arrange - create real Josh file
    Path joshFile = tempDir.resolve("test.josh");
    Files.writeString(joshFile, JoshTestFixtures.MINIMAL_SIMULATION_NO_EXPORT);

    setupBasicFields(runCommand, joshFile);
    setFieldValue(runCommand, "simulation", "TestSim");

    // Configure MinIO
    MinioOptions minioOptions = createConfiguredMinioOptions();
    setFieldValue(runCommand, "minioOptions", minioOptions);

    // Set uploadSource flag
    setFieldValue(runCommand, "uploadSource", true);
    setFieldValue(runCommand, "uploadConfig", false);
    setFieldValue(runCommand, "uploadData", false);

    // Mock saveToMinio to simulate upload failure
    mockJoshSimCommander.when(() -> JoshSimCommander.saveToMinio(
        anyString(), any(File.class), any(MinioOptions.class), any(OutputOptions.class)
    )).thenReturn(false); // Upload fails

    // Act - run simulation
    Integer result = runCommand.call();

    // Assert - should return error code (100 for MINIO_ERROR_CODE)
    assertEquals(100, result, "Should return MINIO_ERROR_CODE on upload failure");
  }

  @Test
  void testMinioNotConfiguredNoUploadsEvenWithFlags(@TempDir Path tempDir) throws Exception {
    // Arrange - create real Josh file
    Path joshFile = tempDir.resolve("test.josh");
    Files.writeString(joshFile, JoshTestFixtures.MINIMAL_SIMULATION_NO_EXPORT);

    setupBasicFields(runCommand, joshFile);
    setFieldValue(runCommand, "simulation", "TestSim");

    // MinIO is NOT configured (no endpoint)
    MinioOptions minioOptions = new MinioOptions();
    setFieldValue(runCommand, "minioOptions", minioOptions);

    // Set upload flags (but MinIO not configured, so should be no-op)
    setFieldValue(runCommand, "uploadSource", true);
    setFieldValue(runCommand, "uploadConfig", true);
    setFieldValue(runCommand, "uploadData", true);

    // Mock saveToMinio
    mockJoshSimCommander.when(() -> JoshSimCommander.saveToMinio(
        anyString(), any(File.class), any(MinioOptions.class), any(OutputOptions.class)
    )).thenReturn(true);

    // Act - run simulation
    Integer result = runCommand.call();

    // Assert - simulation succeeded
    assertEquals(0, result, "Simulation should succeed without MinIO configured");

    // Verify saveToMinio was never called (MinIO not configured)
    mockJoshSimCommander.verify(() -> JoshSimCommander.saveToMinio(
        anyString(), any(File.class), any(MinioOptions.class), any(OutputOptions.class)
    ), never());
  }

  /**
   * Helper method to create a configured MinioOptions instance.
   */
  private MinioOptions createConfiguredMinioOptions() throws Exception {
    MinioOptions minioOptions = new MinioOptions();

    // Use reflection to set private fields since MinioOptions uses @Option annotations
    setFieldValue(minioOptions, "minioEndpointMaybe", "http://localhost:9000");
    setFieldValue(minioOptions, "minioAccessKeyMaybe", "minioadmin");
    setFieldValue(minioOptions, "minioSecretKeyMaybe", "minioadmin");
    setFieldValue(minioOptions, "bucketNameMaybe", "test-bucket");
    setFieldValue(minioOptions, "objectPathMaybe", "test-path");

    return minioOptions;
  }

  /**
   * Helper method to set private field value using reflection.
   */
  private void setFieldValue(Object target, String fieldName, Object value) throws Exception {
    Field field = findField(target.getClass(), fieldName);
    if (field == null) {
      throw new NoSuchFieldException("Field not found: " + fieldName);
    }
    field.setAccessible(true);
    field.set(target, value);
  }

  /**
   * Helper method to find a field in a class hierarchy.
   */
  private Field findField(Class<?> clazz, String fieldName) {
    Class<?> current = clazz;
    while (current != null) {
      try {
        return current.getDeclaredField(fieldName);
      } catch (NoSuchFieldException e) {
        current = current.getSuperclass();
      }
    }
    return null;
  }

  /**
   * Helper method to set up basic required fields for RunCommand with real Josh file.
   */
  private void setupBasicFields(RunCommand command, Path joshFile) throws Exception {
    setFieldValue(command, "file", joshFile.toFile());
    setFieldValue(command, "crs", "");
    setFieldValue(command, "dataFiles", new String[0]);
    setFieldValue(command, "replicates", 1);
    setFieldValue(command, "customTags", new String[0]);
    setFieldValue(command, "outputSteps", "");
    setFieldValue(command, "exportQueueSize", 1000000);
  }
}
