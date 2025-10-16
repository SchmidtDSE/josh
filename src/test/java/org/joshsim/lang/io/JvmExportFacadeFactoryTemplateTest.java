/**
 * Unit tests for JvmExportFacadeFactory TemplateStringRenderer integration.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.joshsim.pipeline.job.JoshJob;
import org.joshsim.pipeline.job.JoshJobBuilder;
import org.joshsim.pipeline.job.JoshJobFileInfo;
import org.joshsim.pipeline.job.config.TemplateStringRenderer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test suite for JvmExportFacadeFactory integration with TemplateStringRenderer.
 *
 * <p>This test verifies that the factory correctly uses TemplateStringRenderer
 * when provided, and falls back to legacy behavior when not provided.</p>
 */
public class JvmExportFacadeFactoryTemplateTest {

  private JoshJob job;
  private TemplateStringRenderer templateRenderer;

  /**
   * Set up test fixtures.
   */
  @BeforeEach
  public void setUp() {
    job = new JoshJobBuilder()
        .setFileInfo("example.jshc", new JoshJobFileInfo("example_1", "test_data/example_1.jshc"))
        .setFileInfo("other.jshd", new JoshJobFileInfo("other_1", "test_data/other_1.jshd"))
        .setReplicates(1)
        .build();
    templateRenderer = new TemplateStringRenderer(job, 2);
  }

  @Test
  public void testFactoryWithTemplateRenderer() {
    JvmExportFacadeFactory factory = new JvmExportFacadeFactory(2, templateRenderer, null);

    String template = "file:///tmp/josh_{example}_{other}.csv";
    String result = factory.getPath(template);

    // Should use TemplateStringRenderer to process job-specific templates
    assertEquals("file:///tmp/josh_example_1_other_1.csv", result);
  }

  @Test
  public void testFactoryWithTemplateRendererGeotiff() {
    TemplateStringRenderer geotiffRenderer = new TemplateStringRenderer(job, 3);
    JvmExportFacadeFactory factory = new JvmExportFacadeFactory(3, geotiffRenderer, null);

    String template = "file:///tmp/josh_{example}_{other}_{step}_{replicate}.tif";
    String result = factory.getPath(template);

    // Should process job-specific templates and substitute replicate for GeoTIFF
    assertEquals("file:///tmp/josh_example_1_other_1___step___3.tif", result);
  }

  @Test
  public void testFactoryWithTemplateRendererNetcdf() {
    TemplateStringRenderer netcdfRenderer = new TemplateStringRenderer(job, 5);
    JvmExportFacadeFactory factory = new JvmExportFacadeFactory(5, netcdfRenderer, null);

    String template = "file:///tmp/josh_{example}_{other}_{replicate}.nc";
    String result = factory.getPath(template);

    // Should process job-specific templates and replace replicate with number for NetCDF
    assertEquals("file:///tmp/josh_example_1_other_1_5.nc", result);
  }

  @Test
  public void testFactoryWithTemplateRendererComplexTemplate() {
    JvmExportFacadeFactory factory = new JvmExportFacadeFactory(1, templateRenderer, null);

    String template = "/data/{example}_step{step}_var{variable}_{other}.csv";
    String result = factory.getPath(template);

    // Should process both job-specific and export-specific templates
    assertEquals("/data/example_1_step__step___var__variable___other_1.csv", result);
  }

  @Test
  public void testFactoryWithoutTemplateRendererFallsBackToLegacy() {
    JvmExportFacadeFactory factory = new JvmExportFacadeFactory(7);

    String template = "file:///tmp/test_{replicate}_{step}.csv";
    String result = factory.getPath(template);

    // Should use legacy behavior (no job-specific template processing)
    assertEquals("file:///tmp/test____step__.csv", result);
  }

  @Test
  public void testFactoryLegacyConstructorBehavior() {
    JvmExportFacadeFactory factory = new JvmExportFacadeFactory(4);

    String template = "file:///tmp/test_{replicate}.tiff";
    String result = factory.getPath(template);

    // Should use legacy behavior for backward compatibility
    assertEquals("file:///tmp/test_4.tiff", result);
  }

  @Test
  public void testFactoryNullTemplateRenderer() {
    JvmExportFacadeFactory factory = new JvmExportFacadeFactory(
        6,
        (TemplateStringRenderer) null,
        (org.joshsim.util.MinioOptions) null);

    String template = "file:///tmp/test_{replicate}_{step}.nc";
    String result = factory.getPath(template);

    // Should fall back to legacy behavior when templateRenderer is null
    assertEquals("file:///tmp/test____step__.nc", result);
  }

  @Test
  public void testEarthSpaceFactoryWithTemplateRenderer() {
    // Test the basic constructor with template renderer
    TemplateStringRenderer testRenderer = new TemplateStringRenderer(job, 1);
    JvmExportFacadeFactory factory = new JvmExportFacadeFactory(1, testRenderer, null);

    String template = "output_{example}_{other}.csv";
    String result = factory.getPath(template);

    assertEquals("output_example_1_other_1.csv", result);
  }

  @Test
  public void testTemplateRendererWithEmptyJob() {
    // Test with a job that has no file mappings
    JoshJob emptyJob = new JoshJobBuilder()
        .setReplicates(1)
        .build();
    TemplateStringRenderer emptyRenderer = new TemplateStringRenderer(emptyJob, 0);

    JvmExportFacadeFactory factory = new JvmExportFacadeFactory(0, emptyRenderer, null);

    String template = "file:///tmp/simple_{replicate}.csv";
    String result = factory.getPath(template);

    // Should replace replicate with number for CSV even with empty job
    assertEquals("file:///tmp/simple_0.csv", result);
  }

  @Test
  public void testTemplateRendererWithOnlyExportTemplates() {
    TemplateStringRenderer testRenderer = new TemplateStringRenderer(job, 8);
    JvmExportFacadeFactory factory = new JvmExportFacadeFactory(8, testRenderer, null);

    String template = "/tmp/output_{replicate}_{step}_{variable}.tiff";
    String result = factory.getPath(template);

    // Should process only export-specific templates for GeoTIFF
    assertEquals("/tmp/output_8___step_____variable__.tiff", result);
  }

  @Test
  public void testTemplateRendererPreservesNonTemplateContent() {
    JvmExportFacadeFactory factory = new JvmExportFacadeFactory(3, templateRenderer, null);

    String template = "/complex-path/with_underscores/{example}_more-dashes_{other}.file.csv";
    String result = factory.getPath(template);

    // Should preserve all non-template content exactly
    assertEquals("/complex-path/with_underscores/example_1_more-dashes_other_1.file.csv", result);
  }

  @Test
  public void testTemplateRendererZeroReplicate() {
    TemplateStringRenderer zeroRenderer = new TemplateStringRenderer(job, 0);
    JvmExportFacadeFactory factory = new JvmExportFacadeFactory(0, zeroRenderer, null);

    String template = "output_{example}_{variable}_{replicate}.tif";
    String result = factory.getPath(template);

    // Should handle zero replicate correctly
    assertEquals("output_example_1___variable___0.tif", result);
  }
}
