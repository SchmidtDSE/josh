/**
 * Unit tests for template validation logic in export facades.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.joshsim.pipeline.job.JoshJob;
import org.joshsim.pipeline.job.JoshJobBuilder;
import org.joshsim.pipeline.job.JoshJobFileInfo;
import org.joshsim.pipeline.job.config.TemplateStringRenderer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test suite for template validation logic in export facades.
 *
 * <p>This test verifies that the factory correctly validates templates
 * for different export types, particularly GeoTIFF files which require
 * step and/or variable templates for proper file separation.</p>
 */
public class TemplateValidationTest {

  private JoshJob testJob;
  private TemplateStringRenderer renderer;
  private JvmExportFacadeFactory factory;

  @BeforeEach
  void setUp() {
    testJob = new JoshJobBuilder()
        .setFileInfo("example.jshc", new JoshJobFileInfo("example_1", "test_data/example_1.jshc"))
        .setReplicates(1)
        .build();
    renderer = new TemplateStringRenderer(testJob, 1);
    factory = new JvmExportFacadeFactory(1, renderer, null);
  }

  @Test
  public void testGeoTiffWithStepAndVariableTemplatesIsValid() {
    String template = "file:///tmp/output_{step}_{variable}_{replicate}.tiff";

    assertDoesNotThrow(() -> {
      factory.getPath(template);
    });
  }

  @Test
  public void testGeoTiffWithOnlyStepTemplateIsValid() {
    String template = "file:///tmp/output_{step}_{replicate}.tiff";

    assertDoesNotThrow(() -> {
      factory.getPath(template);
    });
  }

  @Test
  public void testGeoTiffWithOnlyVariableTemplateIsValid() {
    String template = "file:///tmp/output_{variable}_{replicate}.tiff";

    assertDoesNotThrow(() -> {
      factory.getPath(template);
    });
  }

  @Test
  public void testGeoTiffWithoutStepOrVariableTemplatesThrowsError() {
    String template = "file:///tmp/output_{replicate}.tiff";

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      factory.getPath(template);
    });

    String expectedMessage = "GeoTIFF export requires {step} and/or {variable} templates";
    assertTrue(exception.getMessage().contains(expectedMessage));
    assertTrue(exception.getMessage().contains(template));
  }

  @Test
  public void testGeoTiffWithoutAnyTemplatesThrowsError() {
    String template = "file:///tmp/output.tiff";

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      factory.getPath(template);
    });

    String expectedMessage = "GeoTIFF export requires {step} and/or {variable} templates";
    assertTrue(exception.getMessage().contains(expectedMessage));
  }

  @Test
  public void testCsvTemplatesDoNotRequireValidation() {
    // CSV files should not require step/variable templates
    String template = "file:///tmp/output_{replicate}.csv";

    assertDoesNotThrow(() -> {
      factory.getPath(template);
    });
  }

  @Test
  public void testNetcdfTemplatesDoNotRequireValidation() {
    // NetCDF files should not require step/variable templates
    String template = "file:///tmp/output_{replicate}.nc";

    assertDoesNotThrow(() -> {
      factory.getPath(template);
    });
  }

  @Test
  public void testTifExtensionVariantsAreValidated() {
    // Both .tif and .tiff should be validated
    String tifTemplate = "file:///tmp/output_{replicate}.tif";
    String tiffTemplate = "file:///tmp/output_{replicate}.tiff";

    assertThrows(IllegalArgumentException.class, () -> {
      factory.getPath(tifTemplate);
    });

    assertThrows(IllegalArgumentException.class, () -> {
      factory.getPath(tiffTemplate);
    });
  }

  @Test
  public void testGeoTiffValidationWithComplexPaths() {
    // Test with more complex path structures
    String validTemplate = "/path/to/output_{step}_data_{variable}_rep_{replicate}.tiff";
    String invalidTemplate = "/path/to/output_data_rep_{replicate}.tiff";

    assertDoesNotThrow(() -> {
      factory.getPath(validTemplate);
    });

    assertThrows(IllegalArgumentException.class, () -> {
      factory.getPath(invalidTemplate);
    });
  }

  @Test
  public void testGeoTiffValidationWithStepOnly() {
    // Test that only {step} is sufficient for GeoTIFF
    String template = "file:///tmp/simulation_{step}.tiff";

    assertDoesNotThrow(() -> {
      factory.getPath(template);
    });
  }

  @Test
  public void testGeoTiffValidationWithVariableOnly() {
    // Test that only {variable} is sufficient for GeoTIFF
    String template = "file:///tmp/simulation_{variable}.tiff";

    assertDoesNotThrow(() -> {
      factory.getPath(template);
    });
  }

  @Test
  public void testErrorMessageContainsOriginalTemplate() {
    String template = "file:///tmp/invalid_{replicate}.tiff";

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      factory.getPath(template);
    });

    assertTrue(exception.getMessage().contains(template));
    assertTrue(exception.getMessage().contains("Current template:"));
  }
}
