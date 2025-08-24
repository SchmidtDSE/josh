/**
 * Unit tests for RunRemoteContextBuilder class.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.command;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.net.URI;
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

  @BeforeEach
  void setUp() throws Exception {
    builder = new RunRemoteContextBuilder();
    testFile = new File("test.josh");
    testEndpointUri = new URI("https://example.com/runReplicates");
    testMetadata = new SimulationMetadata(0, 10, 11);
    testProgressCalculator = new ProgressCalculator(11, 1);
    testOutputOptions = new OutputOptions();
    testMinioOptions = new MinioOptions();
  }

  @Test
  void testCompleteBuilderChain() throws Exception {
    String[] testDataFiles = {"file1.jshd", "file2.csv"};

    RunRemoteContext context = builder
        .withFile(testFile)
        .withSimulation("TestSim")
        .withReplicateNumber(5)
        .withReplicates(3)
        .withUseFloat64(true)
        .withEndpointUri(testEndpointUri)
        .withApiKey("test-api-key")
        .withDataFiles(testDataFiles)
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
    assertEquals(5, context.getReplicateNumber());
    assertEquals(3, context.getReplicates());
    assertTrue(context.isUseFloat64());
    assertEquals(testEndpointUri, context.getEndpointUri());
    assertEquals("test-api-key", context.getApiKey());
    assertArrayEquals(testDataFiles, context.getDataFiles());
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
    RunRemoteContext context = builder
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

    assertNotNull(context);
    assertEquals(0, context.getReplicateNumber()); // default
    assertEquals(1, context.getReplicates()); // default
    assertEquals(false, context.isUseFloat64()); // default
    assertArrayEquals(new String[0], context.getDataFiles()); // default
    assertEquals(10, context.getMaxConcurrentWorkers()); // default
  }

  @Test
  void testMissingFileThrowsException() {
    Exception exception = assertThrows(IllegalStateException.class, () -> {
      builder
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

    assertEquals("File is required", exception.getMessage());
  }

  @Test
  void testMissingSimulationThrowsException() {
    Exception exception = assertThrows(IllegalStateException.class, () -> {
      builder
          .withFile(testFile)
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

    assertEquals("Simulation name is required", exception.getMessage());
  }

  @Test
  void testMissingEndpointUriThrowsException() {
    Exception exception = assertThrows(IllegalStateException.class, () -> {
      builder
          .withFile(testFile)
          .withSimulation("TestSim")
          .withApiKey("test-api-key")
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
        .withReplicateNumber(1)
        .withReplicates(2)
        .withUseFloat64(true)
        .withEndpointUri(testEndpointUri)
        .withApiKey("test-key")
        .withDataFiles(new String[] {"test.jshd"})
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
  void testWithReplicatesMethod() throws Exception {
    RunRemoteContext context = builder
        .withFile(testFile)
        .withSimulation("TestSim")
        .withReplicateNumber(0)
        .withReplicates(5)
        .withEndpointUri(testEndpointUri)
        .withApiKey("test-api-key")
        .withJoshCode("start simulation TestSim end simulation")
        .withExternalDataSerialized("serialized-data")
        .withMetadata(testMetadata)
        .withProgressCalculator(testProgressCalculator)
        .withOutputOptions(testOutputOptions)
        .withMinioOptions(testMinioOptions)
        .build();

    assertEquals(5, context.getReplicates());
  }

  @Test
  void testWithReplicatesDefaultValue() throws Exception {
    RunRemoteContext context = builder
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

    assertEquals(1, context.getReplicates(), "Default replicates should be 1");
  }
}
