/**
 * Unit tests for RunRemoteContext class.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.pipeline.remote;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
 * Test cases for RunRemoteContext parameter object.
 *
 * <p>These tests verify that the RunRemoteContext properly encapsulates and provides
 * access to all parameters needed for remote execution strategies.</p>
 */
public class RunRemoteContextTest {

  private File testFile;
  private String simulation;
  private boolean useFloat64;
  private URI endpointUri;
  private String apiKey;
  private JoshJob testJob;
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
    useFloat64 = false;
    endpointUri = new URI("https://example.com/runReplicates");
    apiKey = "test-api-key";

    // Create test job with file mapping and replicates
    testJob = new JoshJobBuilder()
        .setFilePath("config.jshc", "/path/to/config")
        .setReplicates(3)
        .build();

    joshCode = "simulation TestSim {}";
    externalDataSerialized = "config.jshc\t0\ttest config\t";
    metadata = new SimulationMetadata(0, 10, 11);
    progressCalculator = new ProgressCalculator(11, 1);
    outputOptions = new OutputOptions();
    minioOptions = new MinioOptions();
    maxConcurrentWorkers = 5;

    context = new RunRemoteContext(
        testFile, simulation, useFloat64,
        endpointUri, apiKey, testJob,
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
    assertEquals(0, context.getReplicateNumber()); // Always 0 for job-based execution
  }

  @Test
  public void testGetReplicates() {
    assertEquals(3, context.getReplicates()); // From testJob
  }

  @Test
  public void testGetJob() {
    assertEquals(testJob, context.getJob());
  }

  @Test
  public void testGetFilePaths() {
    assertEquals("/path/to/config", context.getJob().getFilePath("config.jshc"));
    assertEquals(1, context.getFilePaths().size());
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
    // Deprecated method should return empty array for job-based execution
    assertArrayEquals(new String[0], context.getDataFiles());
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
        testFile, simulation, true, // useFloat64 = true
        endpointUri, apiKey, testJob,
        joshCode, externalDataSerialized,
        metadata, progressCalculator,
        outputOptions, minioOptions, maxConcurrentWorkers
    );

    assertEquals(true, float64Context.isUseFloat64());
  }

  @Test
  public void testContextWithDifferentConcurrentWorkers() throws Exception {
    RunRemoteContext differentWorkersContext = new RunRemoteContext(
        testFile, simulation, useFloat64,
        endpointUri, apiKey, testJob,
        joshCode, externalDataSerialized,
        metadata, progressCalculator,
        outputOptions, minioOptions, 20 // different worker count
    );

    assertEquals(20, differentWorkersContext.getMaxConcurrentWorkers());
  }

  @Test
  public void testContextWithEmptyJob() throws Exception {
    // Empty job with default replicates
    JoshJob emptyJob = new JoshJobBuilder().setReplicates(1).build();
    RunRemoteContext emptyDataContext = new RunRemoteContext(
        testFile, simulation, useFloat64,
        endpointUri, apiKey, emptyJob,
        joshCode, "",
        metadata, progressCalculator,
        outputOptions, minioOptions, maxConcurrentWorkers
    );

    assertEquals(0, emptyDataContext.getDataFiles().length); // Deprecated method returns empty
    assertEquals("", emptyDataContext.getExternalDataSerialized());
    assertTrue(emptyDataContext.getJob().getFileNames().isEmpty());
    assertEquals(1, emptyDataContext.getReplicates());
  }

  @Test
  public void testContextWithDifferentReplicates() throws Exception {
    JoshJob tenReplicatesJob = new JoshJobBuilder()
        .setFilePath("config.jshc", "/path/to/config")
        .setReplicates(10) // 10 replicates
        .build();

    RunRemoteContext differentReplicatesContext = new RunRemoteContext(
        testFile, simulation, useFloat64,
        endpointUri, apiKey, tenReplicatesJob,
        joshCode, externalDataSerialized,
        metadata, progressCalculator,
        outputOptions, minioOptions, maxConcurrentWorkers
    );

    assertEquals(10, differentReplicatesContext.getReplicates());
    // Always 0 for job-based execution
    assertEquals(0, differentReplicatesContext.getReplicateNumber());
  }

  @Test
  public void testAllObjectsAreSet() {
    // Verify all objects are properly set and accessible
    assertNotNull(context.getFile());
    assertNotNull(context.getSimulation());
    assertNotNull(context.getEndpointUri());
    assertNotNull(context.getApiKey());
    assertNotNull(context.getJob());
    assertNotNull(context.getFilePaths());
    assertNotNull(context.getDataFiles()); // Deprecated but still returns non-null
    assertNotNull(context.getJoshCode());
    assertNotNull(context.getExternalDataSerialized());
    assertNotNull(context.getMetadata());
    assertNotNull(context.getProgressCalculator());
    assertNotNull(context.getOutputOptions());
    assertNotNull(context.getMinioOptions());
  }
}
