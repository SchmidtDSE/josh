/**
 * Unit tests for RunRemoteLocalLeaderStrategy class.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.List;
import org.joshsim.cloud.pipeline.ParallelWorkerHandler;
import org.joshsim.cloud.pipeline.WireResponseHandler;
import org.joshsim.cloud.pipeline.WorkerTask;
import org.joshsim.util.MinioOptions;
import org.joshsim.util.OutputOptions;
import org.joshsim.util.ProgressCalculator;
import org.joshsim.util.SimulationMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test cases for RunRemoteLocalLeaderStrategy functionality.
 *
 * <p>These tests verify the local leadership coordination logic, worker task creation,
 * and URI conversion functionality without generating any actual network traffic.</p>
 */
public class RunRemoteLocalLeaderStrategyTest {

  private RunRemoteLocalLeaderStrategy strategy;
  private RunRemoteContext testContext;

  /**
   * Set up test instances before each test.
   */
  @BeforeEach
  public void setUp() throws Exception {
    strategy = new RunRemoteLocalLeaderStrategy();
    
    // Create test context with valid parameters
    File testFile = new File("test.josh");
    String simulation = "TestSimulation";
    int replicateNumber = 0;
    boolean useFloat64 = false;
    URI endpointUri = new URI("https://example.com/runReplicates");
    String apiKey = "test-api-key";
    String[] dataFiles = new String[]{"config.jshc=/path/to/config"};
    String joshCode = "simulation TestSim {}";
    String externalDataSerialized = "config.jshc\t0\ttest config\t";
    SimulationMetadata metadata = new SimulationMetadata(0, 10, 11);
    ProgressCalculator progressCalculator = new ProgressCalculator(11, 1);
    OutputOptions outputOptions = new OutputOptions();
    MinioOptions minioOptions = new MinioOptions();
    int maxConcurrentWorkers = 5;

    testContext = new RunRemoteContext(
        testFile, simulation, replicateNumber, useFloat64,
        endpointUri, apiKey, dataFiles,
        joshCode, externalDataSerialized,
        metadata, progressCalculator,
        outputOptions, minioOptions, maxConcurrentWorkers
    );
  }

  @Test
  public void testStrategyImplementsInterface() {
    // Verify that the strategy implements the RunRemoteStrategy interface
    assertNotNull(strategy);
    assertTrue(strategy instanceof RunRemoteStrategy);
  }

  @Test
  public void testConvertToWorkerEndpoint() throws Exception {
    // Test URI conversion from leader to worker endpoint
    Method method = RunRemoteLocalLeaderStrategy.class.getDeclaredMethod(
        "convertToWorkerEndpoint", URI.class);
    method.setAccessible(true);

    URI leaderUri = new URI("https://example.com/runReplicates");
    URI workerUri = (URI) method.invoke(strategy, leaderUri);

    assertEquals("https://example.com/runReplicate", workerUri.toString());
  }

  @Test
  public void testConvertToWorkerEndpointWithPath() throws Exception {
    // Test URI conversion with additional path components
    Method method = RunRemoteLocalLeaderStrategy.class.getDeclaredMethod(
        "convertToWorkerEndpoint", URI.class);
    method.setAccessible(true);

    URI leaderUri = new URI("https://example.com/api/v1/runReplicates");
    URI workerUri = (URI) method.invoke(strategy, leaderUri);

    assertEquals("https://example.com/api/v1/runReplicate", workerUri.toString());
  }

  @Test
  public void testConvertToWorkerEndpointWithQueryAndFragment() throws Exception {
    // Test URI conversion preserves query and fragment
    Method method = RunRemoteLocalLeaderStrategy.class.getDeclaredMethod(
        "convertToWorkerEndpoint", URI.class);
    method.setAccessible(true);

    URI leaderUri = new URI("https://example.com/runReplicates?param=value#section");
    URI workerUri = (URI) method.invoke(strategy, leaderUri);

    assertEquals("https://example.com/runReplicate?param=value#section", 
        workerUri.toString());
  }

  @Test
  public void testCreateWorkerTasks() throws Exception {
    // Test worker task creation
    Method method = RunRemoteLocalLeaderStrategy.class.getDeclaredMethod(
        "createWorkerTasks", RunRemoteContext.class);
    method.setAccessible(true);

    @SuppressWarnings("unchecked")
    List<WorkerTask> tasks = 
        (List<WorkerTask>) method.invoke(strategy, testContext);

    // Verify task creation
    assertNotNull(tasks);
    assertEquals(1, tasks.size()); // Currently single replicate support

    WorkerTask task = tasks.get(0);
    assertEquals("simulation TestSim {}", task.getCode());
    assertEquals("TestSimulation", task.getSimulationName());
    assertEquals("test-api-key", task.getApiKey());
    assertEquals("config.jshc\t0\ttest config\t", task.getExternalData());
    assertEquals(true, task.isFavorBigDecimal()); // !useFloat64
    assertEquals(0, task.getReplicateNumber());
  }

  @Test
  public void testCreateWorkerTasksWithFloat64() throws Exception {
    // Create context with float64 enabled
    RunRemoteContext float64Context = new RunRemoteContext(
        testContext.getFile(), testContext.getSimulation(), 
        testContext.getReplicateNumber(), true, // useFloat64 = true
        testContext.getEndpointUri(), testContext.getApiKey(), 
        testContext.getDataFiles(),
        testContext.getJoshCode(), testContext.getExternalDataSerialized(),
        testContext.getMetadata(), testContext.getProgressCalculator(),
        testContext.getOutputOptions(), testContext.getMinioOptions(), 
        testContext.getMaxConcurrentWorkers()
    );

    Method method = RunRemoteLocalLeaderStrategy.class.getDeclaredMethod(
        "createWorkerTasks", RunRemoteContext.class);
    method.setAccessible(true);

    @SuppressWarnings("unchecked")
    List<WorkerTask> tasks = 
        (List<WorkerTask>) method.invoke(strategy, float64Context);

    WorkerTask task = tasks.get(0);
    assertEquals(false, task.isFavorBigDecimal()); // !useFloat64 when useFloat64=true
  }

  @Test
  public void testCreateWorkerTasksWithDifferentReplicate() throws Exception {
    // Create context with different replicate number
    RunRemoteContext differentReplicateContext = new RunRemoteContext(
        testContext.getFile(), testContext.getSimulation(), 
        3, // different replicate number
        testContext.isUseFloat64(),
        testContext.getEndpointUri(), testContext.getApiKey(), 
        testContext.getDataFiles(),
        testContext.getJoshCode(), testContext.getExternalDataSerialized(),
        testContext.getMetadata(), testContext.getProgressCalculator(),
        testContext.getOutputOptions(), testContext.getMinioOptions(), 
        testContext.getMaxConcurrentWorkers()
    );

    Method method = RunRemoteLocalLeaderStrategy.class.getDeclaredMethod(
        "createWorkerTasks", RunRemoteContext.class);
    method.setAccessible(true);

    @SuppressWarnings("unchecked")
    List<WorkerTask> tasks = 
        (List<WorkerTask>) method.invoke(strategy, differentReplicateContext);

    WorkerTask task = tasks.get(0);
    assertEquals(3, task.getReplicateNumber());
  }

  @Test
  public void testLocalLeaderWireResponseHandlerExists() throws Exception {
    // Verify the inner LocalLeaderWireResponseHandler class exists and is accessible
    Class<?>[] innerClasses = RunRemoteLocalLeaderStrategy.class.getDeclaredClasses();
    
    boolean foundWireResponseHandler = false;
    for (Class<?> innerClass : innerClasses) {
      if (innerClass.getSimpleName().equals("LocalLeaderWireResponseHandler")) {
        foundWireResponseHandler = true;
        
        // Verify it implements the correct interface
        assertTrue(WireResponseHandler.class
            .isAssignableFrom(innerClass));
        break;
      }
    }
    
    assertTrue(foundWireResponseHandler, "LocalLeaderWireResponseHandler inner class should exist");
  }

  @Test 
  public void testNullContextHandling() {
    // Test that null context is handled appropriately
    assertThrows(Exception.class, () -> {
      strategy.execute(null);
    });
  }

  @Test
  public void testConvertToWorkerEndpointWithBasicPath() throws Exception {
    // Test URI conversion with a basic path that doesn't contain "runReplicates"
    Method method = RunRemoteLocalLeaderStrategy.class.getDeclaredMethod(
        "convertToWorkerEndpoint", URI.class);
    method.setAccessible(true);

    URI basicUri = new URI("http://example.com/test");
    URI result = (URI) method.invoke(strategy, basicUri);
    
    // The method only replaces "runReplicates" with "runReplicate", so paths without
    // "runReplicates" remain unchanged
    assertEquals("http://example.com/test", result.toString());
  }

  @Test
  public void testWorkerTaskParameterConsistency() throws Exception {
    // Verify that all context parameters are consistently passed to worker tasks
    Method method = RunRemoteLocalLeaderStrategy.class.getDeclaredMethod(
        "createWorkerTasks", RunRemoteContext.class);
    method.setAccessible(true);

    @SuppressWarnings("unchecked")
    List<WorkerTask> tasks = 
        (List<WorkerTask>) method.invoke(strategy, testContext);

    WorkerTask task = tasks.get(0);
    
    // Verify all parameters match the context
    assertEquals(testContext.getJoshCode(), task.getCode());
    assertEquals(testContext.getSimulation(), task.getSimulationName());
    assertEquals(testContext.getApiKey(), task.getApiKey());
    assertEquals(testContext.getExternalDataSerialized(), task.getExternalData());
    assertEquals(!testContext.isUseFloat64(), task.isFavorBigDecimal());
    assertEquals(testContext.getReplicateNumber(), task.getReplicateNumber());
  }

  @Test
  public void testExecuteRequiresComplexMocking() {
    // This test documents that execute() method requires complex mocking
    // for comprehensive testing including ParallelWorkerHandler coordination.
    
    // Note: Full execute() testing would require:
    // 1. ParallelWorkerHandler mocking
    // 2. Export facade factory mocking
    // 3. Worker response simulation
    // 4. Progress calculator interaction testing
    // This is beyond the scope of unit tests and would be integration tests
    
    assertNotNull(strategy);
    assertNotNull(testContext);
    assertEquals(5, testContext.getMaxConcurrentWorkers());
  }
}