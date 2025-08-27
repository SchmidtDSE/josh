/**
 * Unit tests for RunRemoteContextBuilder class.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.pipeline.remote;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.net.URI;
import org.joshsim.pipeline.job.JoshJob;
import org.joshsim.pipeline.job.JoshJobBuilder;
import org.joshsim.util.MinioOptions;
import org.joshsim.util.OutputOptions;
import org.joshsim.util.ProgressCalculator;
import org.joshsim.util.SimulationMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test suite for RunRemoteContextBuilder.
 *
 * <p>Tests the builder pattern implementation for constructing RunRemoteContext
 * instances with proper parameter validation and fluent interface behavior.</p>
 */
public class RunRemoteContextBuilderTest {

  private RunRemoteContextBuilder builder;
  private File testFile;
  private URI testEndpointUri;
  private SimulationMetadata testMetadata;
  private ProgressCalculator testProgressCalculator;
  private OutputOptions testOutputOptions;
  private MinioOptions testMinioOptions;
  private JoshJob testJob;

  @BeforeEach
  void setUp() throws Exception {
    builder = new RunRemoteContextBuilder();
    testFile = new File("test.josh");
    testEndpointUri = new URI("https://example.com/runReplicates");
    testMetadata = new SimulationMetadata(0, 10, 11);
    testProgressCalculator = new ProgressCalculator(11, 1);
    testOutputOptions = new OutputOptions();
    testMinioOptions = new MinioOptions();

    // Create a test job with file mappings and replicates
    testJob = new JoshJobBuilder()
        .setFilePath("file1.jshd", "path/to/file1.jshd")
        .setFilePath("file2.csv", "path/to/file2.csv")
        .setReplicates(3)
        .build();
  }

  @Test
  void testCompleteBuilderChain() throws Exception {
    RunRemoteContext context = builder
        .withFile(testFile)
        .withSimulation("TestSim")
        .withUseFloat64(true)
        .withEndpointUri(testEndpointUri)
        .withApiKey("test-api-key")
        .withJob(testJob)
        .withJoshCode("start simulation TestSim end simulation")
        .withExternalDataSerialized("serialized-data")
        .withMetadata(testMetadata)
        .withProgressCalculator(testProgressCalculator)
        .withOutputOptions(testOutputOptions)
        .withMinioOptions(testMinioOptions)
        .withMaxConcurrentWorkers(20)
        .build();

    assertNotNull(context);
    assertEquals(testFile, context.getFile());
    assertEquals("TestSim", context.getSimulation());
    assertEquals(0, context.getReplicateNumber()); // Always 0 for job-based execution
    assertEquals(3, context.getReplicates()); // From testJob
    assertTrue(context.isUseFloat64());
    assertEquals(testEndpointUri, context.getEndpointUri());
    assertEquals("test-api-key", context.getApiKey());
    assertEquals(testJob, context.getJob());
    assertEquals("path/to/file1.jshd", context.getJob().getFilePath("file1.jshd"));
    assertEquals("path/to/file2.csv", context.getJob().getFilePath("file2.csv"));
    assertEquals("start simulation TestSim end simulation", context.getJoshCode());
    assertEquals("serialized-data", context.getExternalDataSerialized());
    assertEquals(testMetadata, context.getMetadata());
    assertEquals(testProgressCalculator, context.getProgressCalculator());
    assertEquals(testOutputOptions, context.getOutputOptions());
    assertEquals(testMinioOptions, context.getMinioOptions());
    assertEquals(20, context.getMaxConcurrentWorkers());
  }

  @Test
  void testBuilderWithDefaults() throws Exception {
    JoshJob defaultJob = new JoshJobBuilder().setReplicates(1).build(); // Minimal job

    RunRemoteContext context = builder
        .withFile(testFile)
        .withSimulation("TestSim")
        .withEndpointUri(testEndpointUri)
        .withApiKey("test-api-key")
        .withJob(defaultJob)
        .withJoshCode("start simulation TestSim end simulation")
        .withExternalDataSerialized("serialized-data")
        .withMetadata(testMetadata)
        .withProgressCalculator(testProgressCalculator)
        .withOutputOptions(testOutputOptions)
        .withMinioOptions(testMinioOptions)
        .build();

    assertNotNull(context);
    assertEquals(0, context.getReplicateNumber()); // Always 0 for job-based execution
    assertEquals(1, context.getReplicates()); // From job
    assertEquals(false, context.isUseFloat64()); // default
    assertArrayEquals(new String[0], context.getDataFiles()); // deprecated, returns empty
    assertEquals(10, context.getMaxConcurrentWorkers()); // default
  }

  @Test
  void testMissingJobThrowsException() {
    Exception exception = assertThrows(IllegalStateException.class, () -> {
      builder
          .withFile(testFile)
          .withSimulation("TestSim")
          .withEndpointUri(testEndpointUri)
          .withApiKey("test-api-key")
          .withJoshCode("start simulation TestSim end simulation")
          .withExternalDataSerialized("serialized-data")
          .withMetadata(testMetadata)
          .withProgressCalculator(testProgressCalculator)
          .withOutputOptions(testOutputOptions)
          .withMinioOptions(testMinioOptions)
          .build();
    });

    assertEquals("Job is required", exception.getMessage());
  }

  @Test
  void testMissingFileThrowsException() {
    Exception exception = assertThrows(IllegalStateException.class, () -> {
      builder
          .withSimulation("TestSim")
          .withEndpointUri(testEndpointUri)
          .withApiKey("test-api-key")
          .withJob(testJob)
          .withJoshCode("start simulation TestSim end simulation")
          .withExternalDataSerialized("serialized-data")
          .withMetadata(testMetadata)
          .withProgressCalculator(testProgressCalculator)
          .withOutputOptions(testOutputOptions)
          .withMinioOptions(testMinioOptions)
          .build();
    });

    assertEquals("File is required", exception.getMessage());
  }

  @Test
  void testMissingSimulationThrowsException() {
    Exception exception = assertThrows(IllegalStateException.class, () -> {
      builder
          .withFile(testFile)
          .withEndpointUri(testEndpointUri)
          .withApiKey("test-api-key")
          .withJob(testJob)
          .withJoshCode("start simulation TestSim end simulation")
          .withExternalDataSerialized("serialized-data")
          .withMetadata(testMetadata)
          .withProgressCalculator(testProgressCalculator)
          .withOutputOptions(testOutputOptions)
          .withMinioOptions(testMinioOptions)
          .build();
    });

    assertEquals("Simulation name is required", exception.getMessage());
  }

  @Test
  void testMissingEndpointUriThrowsException() {
    Exception exception = assertThrows(IllegalStateException.class, () -> {
      builder
          .withFile(testFile)
          .withSimulation("TestSim")
          .withApiKey("test-api-key")
          .withJob(testJob)
          .withJoshCode("start simulation TestSim end simulation")
          .withExternalDataSerialized("serialized-data")
          .withMetadata(testMetadata)
          .withProgressCalculator(testProgressCalculator)
          .withOutputOptions(testOutputOptions)
          .withMinioOptions(testMinioOptions)
          .build();
    });

    assertEquals("Endpoint URI is required", exception.getMessage());
  }

  @Test
  void testMissingApiKeyThrowsException() {
    Exception exception = assertThrows(IllegalStateException.class, () -> {
      builder
          .withFile(testFile)
          .withSimulation("TestSim")
          .withEndpointUri(testEndpointUri)
          .withJob(testJob)
          .withJoshCode("start simulation TestSim end simulation")
          .withExternalDataSerialized("serialized-data")
          .withMetadata(testMetadata)
          .withProgressCalculator(testProgressCalculator)
          .withOutputOptions(testOutputOptions)
          .withMinioOptions(testMinioOptions)
          .build();
    });

    assertEquals("API key is required", exception.getMessage());
  }

  @Test
  void testMissingJoshCodeThrowsException() {
    Exception exception = assertThrows(IllegalStateException.class, () -> {
      builder
          .withFile(testFile)
          .withSimulation("TestSim")
          .withEndpointUri(testEndpointUri)
          .withApiKey("test-api-key")
          .withJob(testJob)
          .withExternalDataSerialized("serialized-data")
          .withMetadata(testMetadata)
          .withProgressCalculator(testProgressCalculator)
          .withOutputOptions(testOutputOptions)
          .withMinioOptions(testMinioOptions)
          .build();
    });

    assertEquals("Josh code is required", exception.getMessage());
  }

  @Test
  void testMissingExternalDataSerializedThrowsException() {
    Exception exception = assertThrows(IllegalStateException.class, () -> {
      builder
          .withFile(testFile)
          .withSimulation("TestSim")
          .withEndpointUri(testEndpointUri)
          .withApiKey("test-api-key")
          .withJob(testJob)
          .withJoshCode("start simulation TestSim end simulation")
          .withMetadata(testMetadata)
          .withProgressCalculator(testProgressCalculator)
          .withOutputOptions(testOutputOptions)
          .withMinioOptions(testMinioOptions)
          .build();
    });

    assertEquals("External data serialized is required", exception.getMessage());
  }

  @Test
  void testMissingMetadataThrowsException() {
    Exception exception = assertThrows(IllegalStateException.class, () -> {
      builder
          .withFile(testFile)
          .withSimulation("TestSim")
          .withEndpointUri(testEndpointUri)
          .withApiKey("test-api-key")
          .withJob(testJob)
          .withJoshCode("start simulation TestSim end simulation")
          .withExternalDataSerialized("serialized-data")
          .withProgressCalculator(testProgressCalculator)
          .withOutputOptions(testOutputOptions)
          .withMinioOptions(testMinioOptions)
          .build();
    });

    assertEquals("Metadata is required", exception.getMessage());
  }

  @Test
  void testMissingProgressCalculatorThrowsException() {
    Exception exception = assertThrows(IllegalStateException.class, () -> {
      builder
          .withFile(testFile)
          .withSimulation("TestSim")
          .withEndpointUri(testEndpointUri)
          .withApiKey("test-api-key")
          .withJob(testJob)
          .withJoshCode("start simulation TestSim end simulation")
          .withExternalDataSerialized("serialized-data")
          .withMetadata(testMetadata)
          .withOutputOptions(testOutputOptions)
          .withMinioOptions(testMinioOptions)
          .build();
    });

    assertEquals("Progress calculator is required", exception.getMessage());
  }

  @Test
  void testMissingOutputOptionsThrowsException() {
    Exception exception = assertThrows(IllegalStateException.class, () -> {
      builder
          .withFile(testFile)
          .withSimulation("TestSim")
          .withEndpointUri(testEndpointUri)
          .withApiKey("test-api-key")
          .withJob(testJob)
          .withJoshCode("start simulation TestSim end simulation")
          .withExternalDataSerialized("serialized-data")
          .withMetadata(testMetadata)
          .withProgressCalculator(testProgressCalculator)
          .withMinioOptions(testMinioOptions)
          .build();
    });

    assertEquals("Output options are required", exception.getMessage());
  }

  @Test
  void testMissingMinioOptionsThrowsException() {
    Exception exception = assertThrows(IllegalStateException.class, () -> {
      builder
          .withFile(testFile)
          .withSimulation("TestSim")
          .withEndpointUri(testEndpointUri)
          .withApiKey("test-api-key")
          .withJob(testJob)
          .withJoshCode("start simulation TestSim end simulation")
          .withExternalDataSerialized("serialized-data")
          .withMetadata(testMetadata)
          .withProgressCalculator(testProgressCalculator)
          .withOutputOptions(testOutputOptions)
          .build();
    });

    assertEquals("Minio options are required", exception.getMessage());
  }

  @Test
  void testBuilderChainingReturnsBuilder() {
    // Test that all methods return the builder instance for chaining
    RunRemoteContextBuilder result = builder
        .withFile(testFile)
        .withSimulation("TestSim")
        .withUseFloat64(true)
        .withEndpointUri(testEndpointUri)
        .withApiKey("test-key")
        .withJob(testJob)
        .withJoshCode("test-code")
        .withExternalDataSerialized("test-data")
        .withMetadata(testMetadata)
        .withProgressCalculator(testProgressCalculator)
        .withOutputOptions(testOutputOptions)
        .withMinioOptions(testMinioOptions)
        .withMaxConcurrentWorkers(15);

    assertEquals(builder, result);
  }

  @Test
  void testWithJobReplicates() throws Exception {
    JoshJob customJob = new JoshJobBuilder().setReplicates(5).build();

    RunRemoteContext context = builder
        .withFile(testFile)
        .withSimulation("TestSim")
        .withEndpointUri(testEndpointUri)
        .withApiKey("test-api-key")
        .withJob(customJob)
        .withJoshCode("start simulation TestSim end simulation")
        .withExternalDataSerialized("serialized-data")
        .withMetadata(testMetadata)
        .withProgressCalculator(testProgressCalculator)
        .withOutputOptions(testOutputOptions)
        .withMinioOptions(testMinioOptions)
        .build();

    assertEquals(5, context.getReplicates());
    assertEquals(customJob, context.getJob());
  }

  @Test
  void testWithJobFileMappings() throws Exception {
    JoshJob customJob = new JoshJobBuilder()
        .setFilePath("config.jshc", "path/to/config.jshc")
        .setFilePath("data.jshd", "path/to/data.jshd")
        .setReplicates(2)
        .build();

    RunRemoteContext context = builder
        .withFile(testFile)
        .withSimulation("TestSim")
        .withEndpointUri(testEndpointUri)
        .withApiKey("test-api-key")
        .withJob(customJob)
        .withJoshCode("start simulation TestSim end simulation")
        .withExternalDataSerialized("serialized-data")
        .withMetadata(testMetadata)
        .withProgressCalculator(testProgressCalculator)
        .withOutputOptions(testOutputOptions)
        .withMinioOptions(testMinioOptions)
        .build();

    assertEquals("path/to/config.jshc", context.getJob().getFilePath("config.jshc"));
    assertEquals("path/to/data.jshd", context.getJob().getFilePath("data.jshd"));
    assertEquals(2, context.getReplicates());
  }
}
