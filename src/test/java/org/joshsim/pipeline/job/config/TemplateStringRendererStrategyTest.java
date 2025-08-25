/**
 * Unit tests for TemplateStringRenderer strategy detection functionality.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.pipeline.job.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.joshsim.pipeline.job.JoshJob;
import org.joshsim.pipeline.job.JoshJobBuilder;
import org.joshsim.pipeline.job.JoshJobFileInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for TemplateStringRenderer strategy detection and two-phase processing.
 */
class TemplateStringRendererStrategyTest {

  private JoshJob job;
  private TemplateStringRenderer renderer;

  @BeforeEach
  void setUp() {
    JoshJobBuilder builder = new JoshJobBuilder()
        .setReplicates(5)
        .setFileInfo("example.jshc", new JoshJobFileInfo("example_1", "test_data/example_1.jshc"))
        .setFileInfo("other.jshd", new JoshJobFileInfo("other_1", "test_data/other_1.jshd"));
    
    job = builder.build();
    renderer = new TemplateStringRenderer(job, 1);
  }

  @Test
  void testRenderTemplateWithStrategy() {
    String template = "file:///tmp/test_{example}_{other}_{replicate}.csv";
    TemplateResult result = renderer.renderTemplateWithStrategy(template);

    // Check processed template has job-specific templates replaced
    assertEquals("file:///tmp/test_example_1_other_1_{replicate}.csv", 
                 result.getProcessedTemplate());
    
    // Check strategy indicators
    assertTrue(result.hasReplicateTemplate());
    assertFalse(result.hasStepTemplate());
    assertFalse(result.hasVariableTemplate());
    assertTrue(result.requiresParameterizedOutput());
  }

  @Test
  void testStrategyDetectionWithMultipleTemplates() {
    String template = "file:///tmp/data_{example}_{step}_{variable}_{replicate}.nc";
    TemplateResult result = renderer.renderTemplateWithStrategy(template);

    assertEquals("file:///tmp/data_example_1_{step}_{variable}_{replicate}.nc", 
                 result.getProcessedTemplate());
    
    assertTrue(result.hasReplicateTemplate());
    assertTrue(result.hasStepTemplate());
    assertTrue(result.hasVariableTemplate());
    assertTrue(result.requiresParameterizedOutput());
    assertTrue(result.requiresStepParameterization());
    assertTrue(result.requiresVariableParameterization());
  }

  @Test
  void testStrategyDetectionWithoutReplicate() {
    String template = "file:///tmp/data_{example}_{other}.csv";
    TemplateResult result = renderer.renderTemplateWithStrategy(template);

    assertEquals("file:///tmp/data_example_1_other_1.csv", 
                 result.getProcessedTemplate());
    
    assertFalse(result.hasReplicateTemplate());
    assertFalse(result.hasStepTemplate());
    assertFalse(result.hasVariableTemplate());
    assertFalse(result.requiresParameterizedOutput());
  }

  @Test
  void testStrategyDetectionWithStepAndVariable() {
    String template = "file:///tmp/data_{example}_{step}_{variable}.tif";
    TemplateResult result = renderer.renderTemplateWithStrategy(template);

    assertEquals("file:///tmp/data_example_1_{step}_{variable}.tif", 
                 result.getProcessedTemplate());
    
    assertFalse(result.hasReplicateTemplate());
    assertTrue(result.hasStepTemplate());
    assertTrue(result.hasVariableTemplate());
    assertFalse(result.requiresParameterizedOutput());
    assertTrue(result.requiresStepParameterization());
    assertTrue(result.requiresVariableParameterization());
  }

  @Test
  void testEmptyTemplate() {
    TemplateResult result = renderer.renderTemplateWithStrategy("");
    assertEquals("", result.getProcessedTemplate());
    assertFalse(result.hasReplicateTemplate());
    assertFalse(result.hasStepTemplate());
    assertFalse(result.hasVariableTemplate());
  }

  @Test
  void testNullTemplate() {
    TemplateResult result = renderer.renderTemplateWithStrategy(null);
    assertEquals(null, result.getProcessedTemplate());
    assertFalse(result.hasReplicateTemplate());
    assertFalse(result.hasStepTemplate());
    assertFalse(result.hasVariableTemplate());
  }

  @Test
  void testUnknownJobTemplate() {
    String template = "file:///tmp/data_{unknown}.csv";
    
    RuntimeException exception = assertThrows(RuntimeException.class, () ->
        renderer.renderTemplateWithStrategy(template));
    
    assertTrue(exception.getMessage().contains("Unknown template variables: {unknown}"));
    assertTrue(exception.getMessage().contains(
        "Available: {example}, {other}, {replicate}, {step}, {variable}"));
  }

  @Test
  void testBackwardCompatibilityWithRenderTemplate() {
    String template = "file:///tmp/test_{example}_{other}_{replicate}.csv";
    
    // Old method should still work and produce fully processed result
    String oldResult = renderer.renderTemplate(template);
    
    // New method should produce same job-specific processing but keep export templates
    TemplateResult newResult = renderer.renderTemplateWithStrategy(template);
    
    // For CSV, the old method removes {replicate}, new method keeps it in processed template
    assertEquals("file:///tmp/test_example_1_other_1_.csv", oldResult);
    assertEquals("file:///tmp/test_example_1_other_1_{replicate}.csv", 
                 newResult.getProcessedTemplate());
  }

  @Test
  void testTiffTemplatePreservation() {
    String template = "file:///tmp/test_{example}_{other}_{replicate}.tiff";
    
    // For TIFF files, old method substitutes replicate with actual number
    String oldResult = renderer.renderTemplate(template);
    TemplateResult newResult = renderer.renderTemplateWithStrategy(template);
    
    assertEquals("file:///tmp/test_example_1_other_1_1.tiff", oldResult);
    assertEquals("file:///tmp/test_example_1_other_1_{replicate}.tiff", 
                 newResult.getProcessedTemplate());
    assertTrue(newResult.hasReplicateTemplate());
  }

  @Test
  void testComplexJobTemplateSubstitution() {
    // Add a file with dots in the name
    JoshJobBuilder builder = new JoshJobBuilder()
        .setReplicates(3)
        .setFileInfo("config.backup.jshc", new JoshJobFileInfo("config_v2", "data/config_v2.jshc"))
        .setFileInfo("weather.data.jshd", 
            new JoshJobFileInfo("weather_2023", "data/weather_2023.jshd"));
    
    JoshJob complexJob = builder.build();
    TemplateStringRenderer complexRenderer = new TemplateStringRenderer(complexJob, 0);
    
    String template = "file:///tmp/{config.backup}_{weather.data}_{replicate}.nc";
    TemplateResult result = complexRenderer.renderTemplateWithStrategy(template);
    
    assertEquals("file:///tmp/config_v2_weather_2023_{replicate}.nc", 
        result.getProcessedTemplate());
    assertTrue(result.hasReplicateTemplate());
  }

  @Test
  void testReplicateOnlyTemplate() {
    String template = "file:///tmp/data_{replicate}.csv";
    TemplateResult result = renderer.renderTemplateWithStrategy(template);

    assertEquals("file:///tmp/data_{replicate}.csv", result.getProcessedTemplate());
    assertTrue(result.hasReplicateTemplate());
    assertTrue(result.requiresParameterizedOutput());
  }

  @Test
  void testNoTemplatesAtAll() {
    String template = "file:///tmp/static_file.csv";
    TemplateResult result = renderer.renderTemplateWithStrategy(template);

    assertEquals("file:///tmp/static_file.csv", result.getProcessedTemplate());
    assertFalse(result.hasReplicateTemplate());
    assertFalse(result.hasStepTemplate());
    assertFalse(result.hasVariableTemplate());
    assertFalse(result.requiresParameterizedOutput());
  }
}