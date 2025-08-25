/**
 * Unit tests for TemplateResult class.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.pipeline.job.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests for TemplateResult functionality including strategy indicators and utility methods.
 */
class TemplateResultTest {

  @Test
  void testConstructorAndGetters() {
    String processedTemplate = "file:///tmp/test_example_1_other_1.csv";
    TemplateResult result = new TemplateResult(processedTemplate, true, false, true);

    assertEquals(processedTemplate, result.getProcessedTemplate());
    assertTrue(result.hasReplicateTemplate());
    assertFalse(result.hasStepTemplate());
    assertTrue(result.hasVariableTemplate());
  }

  @Test
  void testRequiresParameterizedOutput() {
    // Test with replicate template - should require parameterized output
    TemplateResult withReplicate = new TemplateResult("template", true, false, false);
    assertTrue(withReplicate.requiresParameterizedOutput());

    // Test without replicate template - should not require parameterized output
    TemplateResult withoutReplicate = new TemplateResult("template", false, true, true);
    assertFalse(withoutReplicate.requiresParameterizedOutput());
  }

  @Test
  void testRequiresStepParameterization() {
    // Test with step template
    TemplateResult withStep = new TemplateResult("template", false, true, false);
    assertTrue(withStep.requiresStepParameterization());

    // Test without step template
    TemplateResult withoutStep = new TemplateResult("template", true, false, true);
    assertFalse(withoutStep.requiresStepParameterization());
  }

  @Test
  void testRequiresVariableParameterization() {
    // Test with variable template
    TemplateResult withVariable = new TemplateResult("template", false, false, true);
    assertTrue(withVariable.requiresVariableParameterization());

    // Test without variable template
    TemplateResult withoutVariable = new TemplateResult("template", true, true, false);
    assertFalse(withoutVariable.requiresVariableParameterization());
  }

  @Test
  void testAllTemplateTypes() {
    // Test with all template types present
    TemplateResult allTemplates = new TemplateResult("template", true, true, true);
    assertTrue(allTemplates.requiresParameterizedOutput());
    assertTrue(allTemplates.requiresStepParameterization());
    assertTrue(allTemplates.requiresVariableParameterization());

    // Test with no template types present
    TemplateResult noTemplates = new TemplateResult("template", false, false, false);
    assertFalse(noTemplates.requiresParameterizedOutput());
    assertFalse(noTemplates.requiresStepParameterization());
    assertFalse(noTemplates.requiresVariableParameterization());
  }

  @Test
  void testToString() {
    TemplateResult result = new TemplateResult("test_template", true, false, true);
    String toString = result.toString();

    assertTrue(toString.contains("test_template"));
    assertTrue(toString.contains("hasReplicateTemplate=true"));
    assertTrue(toString.contains("hasStepTemplate=false"));
    assertTrue(toString.contains("hasVariableTemplate=true"));
  }

  @Test
  void testEquals() {
    TemplateResult result1 = new TemplateResult("template", true, false, true);
    TemplateResult result2 = new TemplateResult("template", true, false, true);
    TemplateResult result3 = new TemplateResult("different", true, false, true);

    // Test equality
    assertEquals(result1, result2);
    assertEquals(result1, result1); // Self-equality

    // Test inequality
    assertNotEquals(result1, result3); // Different template
    
    TemplateResult result4 = new TemplateResult("template", false, false, true);
    assertNotEquals(result1, result4); // Different flags
    assertNotEquals(result1, null); // Null comparison
    assertNotEquals(result1, "string"); // Different type
  }

  @Test
  void testHashCode() {
    TemplateResult result1 = new TemplateResult("template", true, false, true);
    TemplateResult result2 = new TemplateResult("template", true, false, true);
    TemplateResult result3 = new TemplateResult("different", true, false, true);

    // Equal objects should have equal hash codes
    assertEquals(result1.hashCode(), result2.hashCode());

    // Different objects should typically have different hash codes
    assertNotEquals(result1.hashCode(), result3.hashCode());
  }

  @Test
  void testEmptyProcessedTemplate() {
    TemplateResult result = new TemplateResult("", false, false, false);
    assertEquals("", result.getProcessedTemplate());
    assertFalse(result.requiresParameterizedOutput());
  }

  @Test
  void testVariousTemplatePatterns() {
    // Test realistic template patterns
    TemplateResult csvWithReplicate = new TemplateResult(
        "file:///tmp/data_example_1_other_1.csv", true, false, false);
    assertTrue(csvWithReplicate.requiresParameterizedOutput());

    TemplateResult netcdfConsolidated = new TemplateResult(
        "file:///tmp/data_example_1_other_1.nc", false, false, false);
    assertFalse(netcdfConsolidated.requiresParameterizedOutput());

    TemplateResult geotiffWithAll = new TemplateResult(
        "file:///tmp/data_example_1_other_1.tif", true, true, true);
    assertTrue(geotiffWithAll.requiresParameterizedOutput());
    assertTrue(geotiffWithAll.requiresStepParameterization());
    assertTrue(geotiffWithAll.requiresVariableParameterization());
  }
}