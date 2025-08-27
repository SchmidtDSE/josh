/**
 * Unit tests for JoshJobBuilder class.
 *
 * <p>Tests the builder pattern implementation including fluent interface,
 * validation, error handling, and correct job construction.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.pipeline.job;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for JoshJobBuilder fluent interface and validation.
 *
 * <p>Validates builder pattern implementation, method chaining,
 * input validation, and correct job construction.</p>
 */
public class JoshJobBuilderTest {

  private JoshJobBuilder builder;

  /**
   * Set up test instance before each test.
   */
  @BeforeEach
  public void setUp() {
    builder = new JoshJobBuilder();
  }

  @Test
  @SuppressWarnings("deprecation")
  public void testFluentInterface() {
    JoshJobBuilder result = builder
        .setFilePath("example.jshc", "test_data/example_1.jshc")
        .setReplicates(5);

    // Should return the same builder instance for chaining
    assertSame(builder, result);
  }

  @Test
  @SuppressWarnings("deprecation")
  public void testBasicJobConstruction() {
    JoshJob job = builder
        .setFilePath("example.jshc", "test_data/example_1.jshc")
        .setFilePath("other.jshd", "test_data/other_1.jshd")
        .setReplicates(3)
        .build();

    assertNotNull(job);
    assertEquals(3, job.getReplicates());
    assertEquals("test_data/example_1.jshc", job.getFilePath("example.jshc"));
    assertEquals("test_data/other_1.jshd", job.getFilePath("other.jshd"));
    assertEquals(2, job.getFileNames().size());
  }

  @Test
  @SuppressWarnings("deprecation")
  public void testDefaultReplicate() {
    JoshJob job = builder
        .setFilePath("example.jshc", "test_data/example_1.jshc")
        .build();

    assertEquals(1, job.getReplicates()); // Default should be 1
  }

  @Test
  public void testEmptyFilePathsValid() {
    JoshJob job = builder
        .setReplicates(2)
        .build();

    assertNotNull(job);
    assertEquals(2, job.getReplicates());
    assertTrue(job.getFileNames().isEmpty());
  }

  @Test
  @SuppressWarnings("deprecation")
  public void testSetFilePathNullName() {
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> builder.setFilePath(null, "test_data/example.jshc")
    );

    assertTrue(exception.getMessage().contains("Logical file name cannot be null or empty"));
  }

  @Test
  @SuppressWarnings("deprecation")
  public void testSetFilePathEmptyName() {
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> builder.setFilePath("", "test_data/example.jshc")
    );

    assertTrue(exception.getMessage().contains("Logical file name cannot be null or empty"));
  }

  @Test
  @SuppressWarnings("deprecation")
  public void testSetFilePathWhitespaceName() {
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> builder.setFilePath("   ", "test_data/example.jshc")
    );

    assertTrue(exception.getMessage().contains("Logical file name cannot be null or empty"));
  }

  @Test
  @SuppressWarnings("deprecation")
  public void testSetFilePathNullPath() {
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> builder.setFilePath("example.jshc", null)
    );

    assertTrue(exception.getMessage().contains("Path cannot be null or empty"));
  }

  @Test
  @SuppressWarnings("deprecation")
  public void testSetFilePathEmptyPath() {
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> builder.setFilePath("example.jshc", "")
    );

    assertTrue(exception.getMessage().contains("Path cannot be null or empty"));
  }

  @Test
  @SuppressWarnings("deprecation")
  public void testSetFilePathWhitespacePath() {
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> builder.setFilePath("example.jshc", "   ")
    );

    assertTrue(exception.getMessage().contains("Path cannot be null or empty"));
  }

  @Test
  public void testSetReplicatesZero() {
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> builder.setReplicates(0)
    );

    assertTrue(exception.getMessage().contains("Number of replicates must be greater than 0"));
  }

  @Test
  public void testSetReplicatesNegative() {
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> builder.setReplicates(-5)
    );

    assertTrue(exception.getMessage().contains("Number of replicates must be greater than 0"));
  }

  @Test
  @SuppressWarnings("deprecation")
  public void testOverwriteFilePath() {
    JoshJob job = builder
        .setFilePath("example.jshc", "test_data/example_1.jshc")
        .setFilePath("example.jshc", "test_data/example_2.jshc")  // Overwrite
        .build();

    assertEquals("test_data/example_2.jshc", job.getFilePath("example.jshc"));
    assertEquals(1, job.getFileNames().size());
  }

  @Test
  @SuppressWarnings("deprecation")
  public void testTrimmingWhitespace() {
    JoshJob job = builder
        .setFilePath("  example.jshc  ", "  test_data/example_1.jshc  ")
        .build();

    // Trimmed name and path should be used
    assertEquals("test_data/example_1.jshc", job.getFilePath("example.jshc"));
    assertTrue(job.getFileNames().contains("example.jshc"));
  }

  @Test
  @SuppressWarnings("deprecation")
  public void testMultipleBuildsFromSameBuilder() {
    builder.setFilePath("example.jshc", "test_data/example_1.jshc");

    JoshJob job1 = builder.setReplicates(3).build();
    JoshJob job2 = builder.setReplicates(5).build();

    // Each build should create a new job with current state
    assertEquals(3, job1.getReplicates());
    assertEquals(5, job2.getReplicates());

    // Both should have the same file mapping
    assertEquals("test_data/example_1.jshc", job1.getFilePath("example.jshc"));
    assertEquals("test_data/example_1.jshc", job2.getFilePath("example.jshc"));
  }

  @Test
  @SuppressWarnings("deprecation")
  public void testChainedCalls() {
    JoshJob job = new JoshJobBuilder()
        .setFilePath("config1.jshc", "path1.jshc")
        .setFilePath("config2.jshc", "path2.jshc")
        .setFilePath("data1.jshd", "path1.jshd")
        .setReplicates(10)
        .build();

    assertNotNull(job);
    assertEquals(10, job.getReplicates());
    assertEquals(3, job.getFileNames().size());
    assertEquals("path1.jshc", job.getFilePath("config1.jshc"));
    assertEquals("path2.jshc", job.getFilePath("config2.jshc"));
    assertEquals("path1.jshd", job.getFilePath("data1.jshd"));
  }

  @Test
  public void testLargeNumberOfReplicates() {
    JoshJob job = builder
        .setReplicates(Integer.MAX_VALUE)
        .build();

    assertEquals(Integer.MAX_VALUE, job.getReplicates());
  }

  @Test
  public void testSetFileInfoWithObject() {
    JoshJobFileInfo fileInfo = new JoshJobFileInfo("example_1", "test_data/example_1.jshc");

    JoshJobBuilder result = builder.setFileInfo("example.jshc", fileInfo);
    JoshJob job = builder.build();

    // Should return the same builder instance for chaining
    assertSame(builder, result);

    assertEquals("test_data/example_1.jshc", job.getFilePath("example.jshc"));
    assertEquals(fileInfo, job.getFileInfo("example.jshc"));
    assertEquals("example_1", job.getFileInfo("example.jshc").getName());
  }

  @Test
  public void testSetFileInfoWithNameAndPath() {
    JoshJobBuilder result = builder.setFileInfo("example.jshc", "example_1",
        "test_data/example_1.jshc");
    JoshJob job = builder.build();

    // Should return the same builder instance for chaining
    assertSame(builder, result);

    assertEquals("test_data/example_1.jshc", job.getFilePath("example.jshc"));
    JoshJobFileInfo fileInfo = job.getFileInfo("example.jshc");
    assertEquals("example_1", fileInfo.getName());
    assertEquals("test_data/example_1.jshc", fileInfo.getPath());
  }

  @Test
  public void testSetFileInfoNullLogicalName() {
    JoshJobFileInfo fileInfo = new JoshJobFileInfo("example_1", "test_data/example_1.jshc");

    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> builder.setFileInfo(null, fileInfo)
    );

    assertTrue(exception.getMessage().contains("Logical file name cannot be null or empty"));
  }

  @Test
  public void testSetFileInfoEmptyLogicalName() {
    JoshJobFileInfo fileInfo = new JoshJobFileInfo("example_1", "test_data/example_1.jshc");

    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> builder.setFileInfo("", fileInfo)
    );

    assertTrue(exception.getMessage().contains("Logical file name cannot be null or empty"));
  }

  @Test
  public void testSetFileInfoNullFileInfo() {
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> builder.setFileInfo("example.jshc", null)
    );

    assertTrue(exception.getMessage().contains("File info cannot be null"));
  }

  @Test
  @SuppressWarnings("deprecation")
  public void testSetFilePathDeprecatedMethod() {
    // Test that the deprecated method still works and creates JoshJobFileInfo internally
    JoshJobBuilder result = builder.setFilePath("example.jshc", "test_data/example_1.jshc");
    JoshJob job = builder.build();

    // Should return the same builder instance for chaining
    assertSame(builder, result);

    // Should work with both old and new API
    assertEquals("test_data/example_1.jshc", job.getFilePath("example.jshc"));
    JoshJobFileInfo fileInfo = job.getFileInfo("example.jshc");
    assertNotNull(fileInfo);
    assertEquals("example_1", fileInfo.getName()); // Extracted from path
    assertEquals("test_data/example_1.jshc", fileInfo.getPath());
  }

  @Test
  public void testMixedFileInfoAndFilePathMethods() {
    JoshJobFileInfo customFileInfo = new JoshJobFileInfo("custom_name", "test_data/example_1.jshc");

    @SuppressWarnings("deprecation")
    JoshJob job = builder
        .setFileInfo("example.jshc", customFileInfo)
        .setFilePath("other.jshd", "test_data/other_1.jshd")  // Uses deprecated method
        .setFileInfo("third.jshc", "third_name", "test_data/third.jshc")
        .build();

    // Custom file info should preserve custom name
    assertEquals("custom_name", job.getFileInfo("example.jshc").getName());
    assertEquals("test_data/example_1.jshc", job.getFilePath("example.jshc"));

    // Deprecated method should extract name from path
    assertEquals("other_1", job.getFileInfo("other.jshd").getName());
    assertEquals("test_data/other_1.jshd", job.getFilePath("other.jshd"));

    // Direct name specification should use provided name
    assertEquals("third_name", job.getFileInfo("third.jshc").getName());
    assertEquals("test_data/third.jshc", job.getFilePath("third.jshc"));

    assertEquals(3, job.getFileNames().size());
  }

  @Test
  public void testCustomParameters() {
    Map<String, String> customParams = new HashMap<>();
    customParams.put("environment", "test");
    customParams.put("version", "v1.0");

    JoshJob job = builder
        .setCustomParameters(customParams)
        .setReplicates(2)
        .build();

    assertEquals(customParams, job.getCustomParameters());
    assertEquals(2, job.getCustomParameters().size());
    assertEquals("test", job.getCustomParameters().get("environment"));
    assertEquals("v1.0", job.getCustomParameters().get("version"));
  }

  @Test
  public void testCustomParametersDefensiveCopy() {
    Map<String, String> customParams = new HashMap<>();
    customParams.put("test", "value");

    JoshJob job = builder
        .setCustomParameters(customParams)
        .build();

    // Modify original map - should not affect job
    customParams.put("modified", "new_value");

    assertEquals(1, job.getCustomParameters().size());
    assertEquals("value", job.getCustomParameters().get("test"));
  }

  @Test
  public void testSetCustomParameter() {
    JoshJobBuilder result = builder.setCustomParameter("environment", "prod");
    JoshJob job = builder.build();

    // Should return the same builder instance for chaining
    assertSame(builder, result);

    assertEquals(1, job.getCustomParameters().size());
    assertEquals("prod", job.getCustomParameters().get("environment"));
  }

  @Test
  public void testSetCustomParameterChaining() {
    JoshJob job = builder
        .setCustomParameter("env", "dev")
        .setCustomParameter("version", "v2.1")
        .setCustomParameter("region", "us-west")
        .build();

    Map<String, String> customParams = job.getCustomParameters();
    assertEquals(3, customParams.size());
    assertEquals("dev", customParams.get("env"));
    assertEquals("v2.1", customParams.get("version"));
    assertEquals("us-west", customParams.get("region"));
  }

  @Test
  public void testSetCustomParameterNullName() {
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> builder.setCustomParameter(null, "value")
    );

    assertTrue(exception.getMessage().contains("Custom parameter name cannot be null or empty"));
  }

  @Test
  public void testSetCustomParameterEmptyName() {
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> builder.setCustomParameter("", "value")
    );

    assertTrue(exception.getMessage().contains("Custom parameter name cannot be null or empty"));
  }

  @Test
  public void testSetCustomParameterWhitespaceName() {
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> builder.setCustomParameter("   ", "value")
    );

    assertTrue(exception.getMessage().contains("Custom parameter name cannot be null or empty"));
  }

  @Test
  public void testSetCustomParameterNullValue() {
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> builder.setCustomParameter("name", null)
    );

    assertTrue(exception.getMessage().contains("Custom parameter value cannot be null"));
  }

  @Test
  public void testSetCustomParametersNull() {
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> builder.setCustomParameters(null)
    );

    assertTrue(exception.getMessage().contains("Custom parameters map cannot be null"));
  }

  @Test
  public void testCustomParameterNameTrimming() {
    JoshJob job = builder
        .setCustomParameter("  environment  ", "test")
        .build();

    assertEquals(1, job.getCustomParameters().size());
    assertEquals("test", job.getCustomParameters().get("environment"));
  }

  @Test
  public void testOverwriteCustomParameter() {
    JoshJob job = builder
        .setCustomParameter("env", "dev")
        .setCustomParameter("env", "prod")  // Overwrite
        .build();

    assertEquals(1, job.getCustomParameters().size());
    assertEquals("prod", job.getCustomParameters().get("env"));
  }

  @Test
  public void testClearAndSetCustomParameters() {
    // First set individual parameters
    builder.setCustomParameter("initial", "value");

    // Then set new parameters map
    Map<String, String> newParams = new HashMap<>();
    newParams.put("env", "test");
    newParams.put("version", "v1.0");

    JoshJob job = builder
        .setCustomParameters(newParams)
        .build();

    // Should only have the new parameters
    assertEquals(2, job.getCustomParameters().size());
    assertEquals("test", job.getCustomParameters().get("env"));
    assertEquals("v1.0", job.getCustomParameters().get("version"));
  }

  @Test
  public void testEmptyCustomParameters() {
    JoshJob job = builder.build();

    assertNotNull(job.getCustomParameters());
    assertTrue(job.getCustomParameters().isEmpty());
  }
}
