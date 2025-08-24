/**
 * Unit tests for JvmExportFacadeFactory replicate template handling.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Test suite for JvmExportFacadeFactory replicate template handling.
 * 
 * <p>This test verifies that the factory correctly handles replicate templates
 * differently for spatial formats (NetCDF, GeoTIFF) versus tabular formats (CSV).</p>
 */
public class JvmExportFacadeFactoryReplicateTest {

  /**
   * Test that CSV file paths have replicate template removed.
   */
  @Test
  public void testCsvPathRemovesReplicateTemplate() {
    JvmExportFacadeFactory factory = new JvmExportFacadeFactory(5);
    
    String template = "file:///tmp/test_{replicate}.csv";
    String result = factory.getPath(template);
    
    // Should remove {replicate} template
    assertFalse(result.contains("{replicate}"), 
        "CSV path should not contain replicate template: " + result);
    assertEquals("file:///tmp/test_.csv", result,
        "CSV path should remove replicate template");
  }

  /**
   * Test that NetCDF file paths now remove replicate template (aligned with CSV behavior).
   */
  @Test
  public void testNetcdfPathRemovesReplicateTemplate() {
    JvmExportFacadeFactory factory = new JvmExportFacadeFactory(3);
    
    String template = "file:///tmp/test_{replicate}.nc";
    String result = factory.getPath(template);
    
    // Should remove {replicate} template like CSV (consolidated files with replicate dimension)
    assertFalse(result.contains("{replicate}"), 
        "NetCDF path should not contain replicate template placeholder");
    assertFalse(result.contains("3"), 
        "NetCDF path should not contain replicate number");
    assertEquals("file:///tmp/test_.nc", result,
        "NetCDF path should remove replicate template");
  }

  /**
   * Test that GeoTIFF file paths preserve replicate template with substitution.
   */
  @Test
  public void testGeotiffPathPreservesReplicateTemplate() {
    JvmExportFacadeFactory factory = new JvmExportFacadeFactory(7);
    
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
    JvmExportFacadeFactory factory = new JvmExportFacadeFactory(2);
    
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
    JvmExportFacadeFactory factory = new JvmExportFacadeFactory(5);
    
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
    JvmExportFacadeFactory factory = new JvmExportFacadeFactory(1);
    
    // CSV with step and variable templates (but no replicate)
    String csvTemplate = "file:///tmp/{variable}_{step}.csv";
    String csvResult = factory.getPath(csvTemplate);
    assertEquals("file:///tmp/__variable_____step__.csv", csvResult,
        "CSV should process step and variable templates");
    
    // NetCDF with all templates - replicate should now be removed
    String ncTemplate = "file:///tmp/{variable}_{step}_{replicate}.nc";
    String ncResult = factory.getPath(ncTemplate);
    assertEquals("file:///tmp/__variable_____step___.nc", ncResult,
        "NetCDF should process step/variable templates but remove replicate");
  }

  /**
   * Test complex template combinations.
   */
  @Test
  public void testComplexTemplates() {
    JvmExportFacadeFactory factory = new JvmExportFacadeFactory(42);
    
    // Complex CSV template - replicate should be removed
    String csvTemplate = "file:///tmp/output_{variable}_step{step}_rep{replicate}_final.csv";
    String csvResult = factory.getPath(csvTemplate);
    assertEquals("file:///tmp/output___variable___step__step___rep_final.csv", csvResult,
        "Complex CSV template should remove replicate but keep others");
    
    // Complex NetCDF template - replicate should be removed, others processed
    String ncTemplate = "file:///tmp/output_{variable}_step{step}_rep{replicate}_final.nc";
    String ncResult = factory.getPath(ncTemplate);
    assertEquals("file:///tmp/output___variable___step__step___rep_final.nc", ncResult,
        "Complex NetCDF template should remove replicate but keep others");
  }

  /**
   * Test replicate number zero is handled correctly (now removes replicate).
   */
  @Test
  public void testReplicateZero() {
    JvmExportFacadeFactory factory = new JvmExportFacadeFactory(0);
    
    String ncTemplate = "file:///tmp/test_{replicate}.nc";
    String ncResult = factory.getPath(ncTemplate);
    
    assertFalse(ncResult.contains("0"), 
        "NetCDF path should remove replicate template, not substitute with zero");
    assertEquals("file:///tmp/test_.nc", ncResult,
        "NetCDF path should remove replicate template");
  }

  /**
   * Test large replicate numbers are handled correctly (now removes replicate).
   */
  @Test
  public void testLargeReplicateNumber() {
    JvmExportFacadeFactory factory = new JvmExportFacadeFactory(999999);
    
    String ncTemplate = "file:///tmp/test_{replicate}.nc";
    String ncResult = factory.getPath(ncTemplate);
    
    assertFalse(ncResult.contains("999999"), 
        "NetCDF path should remove replicate template, not substitute with large numbers");
    assertEquals("file:///tmp/test_.nc", ncResult,
        "NetCDF path should remove replicate template");
  }
}