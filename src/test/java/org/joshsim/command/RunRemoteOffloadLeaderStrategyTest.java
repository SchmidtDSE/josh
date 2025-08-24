/**
 * Unit tests for RunRemoteOffloadLeaderStrategy class.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import org.joshsim.pipeline.DataFilesStringParser;
import org.joshsim.pipeline.job.JoshJob;
import org.joshsim.pipeline.job.JoshJobBuilder;
import org.joshsim.pipeline.remote.RunRemoteContext;
import org.joshsim.pipeline.remote.RunRemoteOffloadLeaderStrategy;
import org.joshsim.pipeline.remote.RunRemoteStrategy;
import org.joshsim.util.MinioOptions;
import org.joshsim.util.OutputOptions;
import org.joshsim.util.ProgressCalculator;
import org.joshsim.util.SimulationMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test cases for RunRemoteOffloadLeaderStrategy functionality.
 *
 * <p>These tests verify the logic and data processing methods of the offload leader strategy
 * without generating any actual network traffic. Network-related functionality is tested
 * through method validation and parameter verification.</p>
 */
public class RunRemoteOffloadLeaderStrategyTest {

  private RunRemoteOffloadLeaderStrategy strategy;
  private RunRemoteContext testContext;

  /**
   * Set up test instances before each test.
   */
  @BeforeEach
  public void setUp() throws Exception {
    strategy = new RunRemoteOffloadLeaderStrategy();

    // Create test context with valid parameters
    File testFile = new File("test.josh");
    String simulation = "TestSimulation";
    int replicates = 3;
    boolean useFloat64 = false;
    URI endpointUri = new URI("https://example.com/runReplicates");
    String apiKey = "test-api-key";
    String[] dataFiles = new String[]{"config.jshc=/path/to/config"};
    String joshCode = "simulation TestSim {}";
    String externalDataSerialized = "config.jshc\t0\ttest config\t";
    SimulationMetadata metadata = new SimulationMetadata(0, 10, 11);
    ProgressCalculator progressCalculator = new ProgressCalculator(11, replicates);
    OutputOptions outputOptions = new OutputOptions();
    MinioOptions minioOptions = new MinioOptions();
    int maxConcurrentWorkers = 10;

    // Create JoshJob from dataFiles using DataFilesStringParser
    JoshJobBuilder jobBuilder = new JoshJobBuilder().setReplicates(replicates);
    DataFilesStringParser parser = new DataFilesStringParser();
    JoshJob job = parser.parseDataFiles(jobBuilder, dataFiles).build();

    testContext = new RunRemoteContext(
        testFile, simulation, useFloat64,
        endpointUri, apiKey, job,
        joshCode, externalDataSerialized,
        metadata, progressCalculator,
        outputOptions, minioOptions, maxConcurrentWorkers
    );
  }

  @Test
  public void testStrategyImplementsInterface() {
    // Verify that the strategy implements the RunRemoteStrategy interface
    assertNotNull(strategy);
    assertEquals(true, strategy instanceof RunRemoteStrategy);
  }

  @Test
  public void testCreateRemoteLeaderRequestMethod() throws Exception {
    // Test the request creation logic through reflection
    Method method = RunRemoteOffloadLeaderStrategy.class.getDeclaredMethod(
        "createRemoteLeaderRequest", RunRemoteContext.class);
    method.setAccessible(true);

    // Verify method exists and is accessible
    assertNotNull(method);
    assertEquals("createRemoteLeaderRequest", method.getName());
  }

  @Test
  public void testBuildFormDataMethod() throws Exception {
    // Test the form data building logic through reflection
    Method method = RunRemoteOffloadLeaderStrategy.class.getDeclaredMethod(
        "buildFormData", RunRemoteContext.class);
    method.setAccessible(true);

    String result = (String) method.invoke(strategy, testContext);

    // Verify form data contains required fields
    assertNotNull(result);
    assertEquals(true, result.contains("code="));
    assertEquals(true, result.contains("name=TestSimulation"));
    assertEquals(true, result.contains("apiKey=test-api-key"));
    assertEquals(true, result.contains("replicates=3"));
    assertEquals(true, result.contains("favorBigDecimal=true"));
    assertEquals(true, result.contains("externalData="));
  }

  @Test
  public void testBuildFormDataWithFloat64() throws Exception {
    // Create context with float64 enabled using job from main context
    RunRemoteContext float64Context = new RunRemoteContext(
        testContext.getFile(), testContext.getSimulation(), true, // useFloat64 = true
        testContext.getEndpointUri(), testContext.getApiKey(),
        testContext.getJob(),
        testContext.getJoshCode(), testContext.getExternalDataSerialized(),
        testContext.getMetadata(), testContext.getProgressCalculator(),
        testContext.getOutputOptions(), testContext.getMinioOptions(),
        testContext.getMaxConcurrentWorkers()
    );

    Method method = RunRemoteOffloadLeaderStrategy.class.getDeclaredMethod(
        "buildFormData", RunRemoteContext.class);
    method.setAccessible(true);

    String result = (String) method.invoke(strategy, float64Context);

    // Verify favorBigDecimal is false when useFloat64 is true
    assertEquals(true, result.contains("favorBigDecimal=false"));
  }

  @Test
  public void testBuildFormDataUrlEncoding() throws Exception {
    // Create context with special characters that need URL encoding
    RunRemoteContext specialCharContext = new RunRemoteContext(
        testContext.getFile(), "Test Simulation & More", testContext.isUseFloat64(),
        testContext.getEndpointUri(), "api-key/with+special&chars",
        testContext.getJob(),
        "simulation code with spaces", "external data with spaces",
        testContext.getMetadata(), testContext.getProgressCalculator(),
        testContext.getOutputOptions(), testContext.getMinioOptions(),
        testContext.getMaxConcurrentWorkers()
    );

    Method method = RunRemoteOffloadLeaderStrategy.class.getDeclaredMethod(
        "buildFormData", RunRemoteContext.class);
    method.setAccessible(true);

    String result = (String) method.invoke(strategy, specialCharContext);

    // Verify URL encoding occurs (spaces become + in application/x-www-form-urlencoded)
    assertEquals(true, result.contains("simulation+code+with+spaces"));
    assertEquals(true, result.contains("external+data+with+spaces"));
    assertEquals(true, result.contains("Test+Simulation+%26+More"));
    assertEquals(true, result.contains("api-key%2Fwith%2Bspecial%26chars"));
  }

  @Test
  public void testProcessStreamingResponseMethod() throws Exception {
    // Test that the streaming response processing method exists
    Method method = RunRemoteOffloadLeaderStrategy.class.getDeclaredMethod(
        "processStreamingResponse",
        java.util.stream.Stream.class, RunRemoteContext.class);
    method.setAccessible(true);

    // Verify method exists and is accessible
    assertNotNull(method);
    assertEquals("processStreamingResponse", method.getName());
  }

  @Test
  public void testFormDataStructure() throws Exception {
    Method method = RunRemoteOffloadLeaderStrategy.class.getDeclaredMethod(
        "buildFormData", RunRemoteContext.class);
    method.setAccessible(true);

    String result = (String) method.invoke(strategy, testContext);

    // Parse the form data to verify structure
    Map<String, String> formFields = parseFormData(result);

    assertEquals("simulation TestSim {}", formFields.get("code"));
    assertEquals("TestSimulation", formFields.get("name"));
    assertEquals("test-api-key", formFields.get("apiKey"));
    assertEquals("3", formFields.get("replicates"));
    assertEquals("true", formFields.get("favorBigDecimal"));
    assertEquals("config.jshc\t0\ttest config\t", formFields.get("externalData"));
  }

  @Test
  public void testNullContextHandling() {
    // Test that null context is handled appropriately
    assertThrows(Exception.class, () -> {
      strategy.execute(null);
    });
  }

  /**
   * Helper method to parse form data for testing.
   *
   * @param formData The URL-encoded form data string
   * @return Map of field names to values
   */
  private Map<String, String> parseFormData(String formData) {
    Map<String, String> fields = new HashMap<>();
    String[] pairs = formData.split("&");

    for (String pair : pairs) {
      String[] keyValue = pair.split("=", 2);
      if (keyValue.length == 2) {
        try {
          String key = java.net.URLDecoder.decode(keyValue[0], "UTF-8");
          String value = java.net.URLDecoder.decode(keyValue[1], "UTF-8");
          fields.put(key, value);
        } catch (Exception e) {
          // Skip malformed pairs
        }
      }
    }

    return fields;
  }

  @Test
  public void testExecuteRequiresNetworkMocking() {
    // This test documents that execute() method requires network mocking
    // for comprehensive testing. The method structure is validated through
    // other tests that check individual components.

    // Note: Full execute() testing would require:
    // 1. HTTP client mocking
    // 2. Streaming response simulation
    // 3. Export facade mocking
    // This is beyond the scope of unit tests and would be integration tests

    assertNotNull(strategy);
    assertNotNull(testContext);
  }

  @Test
  public void testBuildFormDataWithDifferentReplicates() throws Exception {
    // Create job with different replicate count
    JoshJobBuilder jobBuilder = new JoshJobBuilder().setReplicates(7);
    DataFilesStringParser parser = new DataFilesStringParser();
    String[] dataFiles = new String[]{"config.jshc=/path/to/config"};
    JoshJob jobWith7Replicates = parser.parseDataFiles(jobBuilder, dataFiles).build();
    
    // Create context with different replicate count
    RunRemoteContext differentReplicatesContext = new RunRemoteContext(
        testContext.getFile(), testContext.getSimulation(), testContext.isUseFloat64(),
        testContext.getEndpointUri(), testContext.getApiKey(),
        jobWith7Replicates,
        testContext.getJoshCode(), testContext.getExternalDataSerialized(),
        testContext.getMetadata(), testContext.getProgressCalculator(),
        testContext.getOutputOptions(), testContext.getMinioOptions(),
        testContext.getMaxConcurrentWorkers()
    );

    Method method = RunRemoteOffloadLeaderStrategy.class.getDeclaredMethod(
        "buildFormData", RunRemoteContext.class);
    method.setAccessible(true);

    String result = (String) method.invoke(strategy, differentReplicatesContext);

    // Verify replicates field contains the dynamic value
    assertEquals(true, result.contains("replicates=7"));
  }

  @Test
  public void testBuildFormDataWithSingleReplicate() throws Exception {
    // Create job with single replicate
    JoshJobBuilder jobBuilder = new JoshJobBuilder().setReplicates(1);
    DataFilesStringParser parser = new DataFilesStringParser();
    String[] dataFiles = new String[]{"config.jshc=/path/to/config"};
    JoshJob jobWith1Replicate = parser.parseDataFiles(jobBuilder, dataFiles).build();
    
    // Create context with single replicate (default behavior)
    RunRemoteContext singleReplicateContext = new RunRemoteContext(
        testContext.getFile(), testContext.getSimulation(), testContext.isUseFloat64(),
        testContext.getEndpointUri(), testContext.getApiKey(),
        jobWith1Replicate,
        testContext.getJoshCode(), testContext.getExternalDataSerialized(),
        testContext.getMetadata(), testContext.getProgressCalculator(),
        testContext.getOutputOptions(), testContext.getMinioOptions(),
        testContext.getMaxConcurrentWorkers()
    );

    Method method = RunRemoteOffloadLeaderStrategy.class.getDeclaredMethod(
        "buildFormData", RunRemoteContext.class);
    method.setAccessible(true);

    String result = (String) method.invoke(strategy, singleReplicateContext);

    // Verify replicates field contains the single value
    assertEquals(true, result.contains("replicates=1"));
  }
}
