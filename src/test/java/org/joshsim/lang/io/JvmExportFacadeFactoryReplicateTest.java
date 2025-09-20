/**
 * Unit tests for JvmExportFacadeFactory replicate template handling.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.joshsim.pipeline.job.JoshJob;
import org.joshsim.pipeline.job.JoshJobBuilder;
import org.joshsim.pipeline.job.JoshJobFileInfo;
import org.joshsim.pipeline.job.config.TemplateStringRenderer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test suite for JvmExportFacadeFactory replicate template handling.
 *
 * <p>This test verifies that the factory correctly handles replicate templates
 * using unified template-driven behavior for all file formats.</p>
 */
public class JvmExportFacadeFactoryReplicateTest {

  private JoshJob testJob;

  @BeforeEach
  void setUp() {
    // Create a minimal test job for template processing
    testJob = new JoshJobBuilder()
        .setFileInfo("example.jshc", new JoshJobFileInfo("example_1", "test_data/example_1.jshc"))
        .setReplicates(1)
        .build();
  }

  /**
   * Test that CSV file paths now replace replicate template with actual number.
   */
  @Test
  public void testCsvPathReplacesReplicateTemplate() {
    TemplateStringRenderer renderer = new TemplateStringRenderer(testJob, 5);
    JvmExportFacadeFactory factory = new JvmExportFacadeFactory(5, renderer);

    String template = "file:///tmp/test_{replicate}.csv";
    String result = factory.getPath(template);

    // Should replace {replicate} with actual number (template-driven behavior)
    assertFalse(result.contains("{replicate}"),
        "CSV path should not contain replicate template: " + result);
    assertTrue(result.contains("5"),
        "CSV path should contain replicate number");
    assertEquals("file:///tmp/test_5.csv", result,
        "CSV path should replace replicate template with actual number");
  }

  /**
   * Test that NetCDF file paths now replace replicate template with actual number.
   */
  @Test
  public void testNetcdfPathReplacesReplicateTemplate() {
    TemplateStringRenderer renderer = new TemplateStringRenderer(testJob, 3);
    JvmExportFacadeFactory factory = new JvmExportFacadeFactory(3, renderer);

    String template = "file:///tmp/test_{replicate}.nc";
    String result = factory.getPath(template);

    // Should replace {replicate} with actual number (template-driven behavior)
    assertFalse(result.contains("{replicate}"),
        "NetCDF path should not contain replicate template placeholder");
    assertTrue(result.contains("3"),
        "NetCDF path should contain replicate number");
    assertEquals("file:///tmp/test_3.nc", result,
        "NetCDF path should replace replicate template with actual number");
  }

  /**
   * Test that GeoTIFF file paths preserve replicate template with substitution.
   */
  @Test
  public void testGeotiffPathPreservesReplicateTemplate() {
    TemplateStringRenderer renderer = new TemplateStringRenderer(testJob, 7);
    JvmExportFacadeFactory factory = new JvmExportFacadeFactory(7, renderer);

    String template = "file:///tmp/test_{replicate}.tiff";
    String result = factory.getPath(template);

    // Should substitute {replicate} with actual replicate number
    assertFalse(result.contains("{replicate}"),
        "GeoTIFF path should not contain replicate template placeholder");
    assertTrue(result.contains("7"),
        "GeoTIFF path should contain replicate number");
    assertEquals("file:///tmp/test_7.tiff", result,
        "GeoTIFF path should substitute replicate template");
  }

  /**
   * Test that .tif extension also works for GeoTIFF.
   */
  @Test
  public void testTifExtensionPreservesReplicateTemplate() {
    TemplateStringRenderer renderer = new TemplateStringRenderer(testJob, 2);
    JvmExportFacadeFactory factory = new JvmExportFacadeFactory(2, renderer);

    String template = "file:///tmp/test_{replicate}.tif";
    String result = factory.getPath(template);

    // Should substitute {replicate} with actual replicate number
    assertTrue(result.contains("2"),
        "TIF path should contain replicate number");
    assertEquals("file:///tmp/test_2.tif", result,
        "TIF path should substitute replicate template");
  }

  /**
   * Test paths without replicate template are handled correctly.
   */
  @Test
  public void testPathWithoutReplicateTemplate() {
    TemplateStringRenderer renderer = new TemplateStringRenderer(testJob, 5);
    JvmExportFacadeFactory factory = new JvmExportFacadeFactory(5, renderer);

    // CSV without replicate template
    String csvTemplate = "file:///tmp/simple.csv";
    String csvResult = factory.getPath(csvTemplate);
    assertEquals("file:///tmp/simple.csv", csvResult,
        "CSV path without template should remain unchanged");

    // NetCDF without replicate template
    String ncTemplate = "file:///tmp/simple.nc";
    String ncResult = factory.getPath(ncTemplate);
    assertEquals("file:///tmp/simple.nc", ncResult,
        "NetCDF path without template should remain unchanged");
  }

  /**
   * Test that step and variable templates are still processed correctly.
   */
  @Test
  public void testStepAndVariableTemplatesStillWork() {
    TemplateStringRenderer renderer = new TemplateStringRenderer(testJob, 1);
    JvmExportFacadeFactory factory = new JvmExportFacadeFactory(1, renderer);

    // CSV with step and variable templates (but no replicate)
    String csvTemplate = "file:///tmp/{variable}_{step}.csv";
    String csvResult = factory.getPath(csvTemplate);
    assertEquals("file:///tmp/__variable_____step__.csv", csvResult,
        "CSV should process step and variable templates");

    // NetCDF with all templates - replicate should now be replaced with number
    String ncTemplate = "file:///tmp/{variable}_{step}_{replicate}.nc";
    String ncResult = factory.getPath(ncTemplate);
    assertEquals("file:///tmp/__variable_____step___1.nc", ncResult,
        "NetCDF should process step/variable templates and replace replicate with number");
  }

  /**
   * Test complex template combinations.
   */
  @Test
  public void testComplexTemplates() {
    TemplateStringRenderer renderer = new TemplateStringRenderer(testJob, 42);
    JvmExportFacadeFactory factory = new JvmExportFacadeFactory(42, renderer);

    // Complex CSV template - replicate should be replaced with number
    String csvTemplate = "file:///tmp/output_{variable}_step{step}_rep{replicate}_final.csv";
    String csvResult = factory.getPath(csvTemplate);
    assertEquals("file:///tmp/output___variable___step__step___rep42_final.csv", csvResult,
        "Complex CSV template should replace replicate with number and keep others");

    // Complex NetCDF template - replicate should be replaced with number, others processed
    String ncTemplate = "file:///tmp/output_{variable}_step{step}_rep{replicate}_final.nc";
    String ncResult = factory.getPath(ncTemplate);
    assertEquals("file:///tmp/output___variable___step__step___rep42_final.nc", ncResult,
        "Complex NetCDF template should replace replicate with number and keep others");
  }

  /**
   * Test replicate number zero is handled correctly (now replaces with actual number).
   */
  @Test
  public void testReplicateZero() {
    TemplateStringRenderer renderer = new TemplateStringRenderer(testJob, 0);
    JvmExportFacadeFactory factory = new JvmExportFacadeFactory(0, renderer);

    String ncTemplate = "file:///tmp/test_{replicate}.nc";
    String ncResult = factory.getPath(ncTemplate);

    assertTrue(ncResult.contains("0"),
        "NetCDF path should replace replicate template with zero");
    assertEquals("file:///tmp/test_0.nc", ncResult,
        "NetCDF path should replace replicate template with actual number");
  }

  /**
   * Test large replicate numbers are handled correctly (now replaces with actual number).
   */
  @Test
  public void testLargeReplicateNumber() {
    TemplateStringRenderer renderer = new TemplateStringRenderer(testJob, 999999);
    JvmExportFacadeFactory factory = new JvmExportFacadeFactory(999999, renderer);

    String ncTemplate = "file:///tmp/test_{replicate}.nc";
    String ncResult = factory.getPath(ncTemplate);

    assertTrue(ncResult.contains("999999"),
        "NetCDF path should replace replicate template with large numbers");
    assertEquals("file:///tmp/test_999999.nc", ncResult,
        "NetCDF path should replace replicate template with actual number");
  }
}
