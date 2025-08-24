/**
 * Unit tests for RunRemoteContext class.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.command;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.net.URI;
import org.joshsim.util.MinioOptions;
import org.joshsim.util.OutputOptions;
import org.joshsim.util.ProgressCalculator;
import org.joshsim.util.SimulationMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test cases for RunRemoteContext parameter object.
 *
 * <p>These tests verify that the RunRemoteContext properly encapsulates and provides
 * access to all parameters needed for remote execution strategies.</p>
 */
public class RunRemoteContextTest {

  private File testFile;
  private String simulation;
  private int replicateNumber;
  private boolean useFloat64;
  private URI endpointUri;
  private String apiKey;
  private String[] dataFiles;
  private String joshCode;
  private String externalDataSerialized;
  private SimulationMetadata metadata;
  private ProgressCalculator progressCalculator;
  private OutputOptions outputOptions;
  private MinioOptions minioOptions;
  private int maxConcurrentWorkers;
  private RunRemoteContext context;

  /**
   * Set up test data before each test.
   */
  @BeforeEach
  public void setUp() throws Exception {
    testFile = new File("test.josh");
    simulation = "TestSimulation";
    replicateNumber = 1;
    useFloat64 = false;
    endpointUri = new URI("https://example.com/runReplicates");
    apiKey = "test-api-key";
    dataFiles = new String[]{"config.jshc=/path/to/config"};
    joshCode = "simulation TestSim {}";
    externalDataSerialized = "config.jshc\t0\ttest config\t";
    metadata = new SimulationMetadata(0, 10, 11);
    progressCalculator = new ProgressCalculator(11, 1);
    outputOptions = new OutputOptions();
    minioOptions = new MinioOptions();
    maxConcurrentWorkers = 5;

    context = new RunRemoteContext(
        testFile, simulation, replicateNumber, useFloat64,
        endpointUri, apiKey, dataFiles,
        joshCode, externalDataSerialized,
        metadata, progressCalculator,
        outputOptions, minioOptions, maxConcurrentWorkers
    );
  }

  @Test
  public void testGetFile() {
    assertEquals(testFile, context.getFile());
  }

  @Test
  public void testGetSimulation() {
    assertEquals(simulation, context.getSimulation());
  }

  @Test
  public void testGetReplicateNumber() {
    assertEquals(replicateNumber, context.getReplicateNumber());
  }

  @Test
  public void testIsUseFloat64() {
    assertEquals(useFloat64, context.isUseFloat64());
  }

  @Test
  public void testGetEndpointUri() {
    assertEquals(endpointUri, context.getEndpointUri());
  }

  @Test
  public void testGetApiKey() {
    assertEquals(apiKey, context.getApiKey());
  }

  @Test
  public void testGetDataFiles() {
    assertArrayEquals(dataFiles, context.getDataFiles());
  }

  @Test
  public void testGetJoshCode() {
    assertEquals(joshCode, context.getJoshCode());
  }

  @Test
  public void testGetExternalDataSerialized() {
    assertEquals(externalDataSerialized, context.getExternalDataSerialized());
  }

  @Test
  public void testGetMetadata() {
    assertEquals(metadata, context.getMetadata());
  }

  @Test
  public void testGetProgressCalculator() {
    assertEquals(progressCalculator, context.getProgressCalculator());
  }

  @Test
  public void testGetOutputOptions() {
    assertEquals(outputOptions, context.getOutputOptions());
  }

  @Test
  public void testGetMinioOptions() {
    assertEquals(minioOptions, context.getMinioOptions());
  }

  @Test
  public void testGetMaxConcurrentWorkers() {
    assertEquals(maxConcurrentWorkers, context.getMaxConcurrentWorkers());
  }

  @Test
  public void testContextWithFloat64Enabled() throws Exception {
    RunRemoteContext float64Context = new RunRemoteContext(
        testFile, simulation, replicateNumber, true, // useFloat64 = true
        endpointUri, apiKey, dataFiles,
        joshCode, externalDataSerialized,
        metadata, progressCalculator,
        outputOptions, minioOptions, maxConcurrentWorkers
    );

    assertEquals(true, float64Context.isUseFloat64());
  }

  @Test
  public void testContextWithDifferentConcurrentWorkers() throws Exception {
    RunRemoteContext differentWorkersContext = new RunRemoteContext(
        testFile, simulation, replicateNumber, useFloat64,
        endpointUri, apiKey, dataFiles,
        joshCode, externalDataSerialized,
        metadata, progressCalculator,
        outputOptions, minioOptions, 20 // different worker count
    );

    assertEquals(20, differentWorkersContext.getMaxConcurrentWorkers());
  }

  @Test
  public void testContextWithEmptyDataFiles() throws Exception {
    String[] emptyDataFiles = new String[0];
    RunRemoteContext emptyDataContext = new RunRemoteContext(
        testFile, simulation, replicateNumber, useFloat64,
        endpointUri, apiKey, emptyDataFiles,
        joshCode, "",
        metadata, progressCalculator,
        outputOptions, minioOptions, maxConcurrentWorkers
    );

    assertEquals(0, emptyDataContext.getDataFiles().length);
    assertEquals("", emptyDataContext.getExternalDataSerialized());
  }

  @Test
  public void testAllObjectsAreSet() {
    // Verify all objects are properly set and accessible
    assertNotNull(context.getFile());
    assertNotNull(context.getSimulation());
    assertNotNull(context.getEndpointUri());
    assertNotNull(context.getApiKey());
    assertNotNull(context.getDataFiles());
    assertNotNull(context.getJoshCode());
    assertNotNull(context.getExternalDataSerialized());
    assertNotNull(context.getMetadata());
    assertNotNull(context.getProgressCalculator());
    assertNotNull(context.getOutputOptions());
    assertNotNull(context.getMinioOptions());
  }
}
