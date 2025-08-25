/**
 * Unit tests for TemplateStringRenderer.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.pipeline.job.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.joshsim.pipeline.job.JoshJob;
import org.joshsim.pipeline.job.JoshJobBuilder;
import org.joshsim.pipeline.job.JoshJobFileInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for TemplateStringRenderer class.
 */
public class TemplateStringRendererTest {

  private JoshJob job;
  private TemplateStringRenderer renderer;

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
    renderer = new TemplateStringRenderer(job, 0);
  }

  @Test
  public void testConstructorNullJob() {
    assertThrows(IllegalArgumentException.class, () -> {
      new TemplateStringRenderer(null, 0);
    });
  }

  @Test
  public void testRenderTemplateNullInput() {
    assertEquals(null, renderer.renderTemplate(null));
    assertEquals("", renderer.renderTemplate(""));
  }

  @Test
  public void testJobSpecificTemplateReplacement() {
    String template = "file:///tmp/josh_{example}_{other}.csv";
    String expected = "file:///tmp/josh_example_1_other_1.csv";
    assertEquals(expected, renderer.renderTemplate(template));
  }

  @Test
  public void testSingleJobSpecificTemplate() {
    String template = "/data/output_{example}.nc";
    String expected = "/data/output_example_1.nc";
    assertEquals(expected, renderer.renderTemplate(template));
  }

  @Test
  public void testJobSpecificTemplateMultipleOccurrences() {
    String template = "{example}_{example}_{other}.txt";
    String expected = "example_1_example_1_other_1.txt";
    assertEquals(expected, renderer.renderTemplate(template));
  }

  @Test
  public void testUnknownJobTemplate() {
    String template = "output_{missing}_{example}.csv";
    RuntimeException exception = assertThrows(RuntimeException.class, () -> {
      renderer.renderTemplate(template);
    });

    String message = exception.getMessage();
    assertTrue(message.contains("Unknown template variables: {missing}"));
    assertTrue(message.contains("Available: {example}, {other}"));
  }

  @Test
  public void testMultipleUnknownTemplates() {
    String template = "output_{missing1}_{missing2}.csv";
    RuntimeException exception = assertThrows(RuntimeException.class, () -> {
      renderer.renderTemplate(template);
    });

    String message = exception.getMessage();
    assertTrue(message.contains("Unknown template variables"));
    assertTrue(message.contains("{missing1}"));
    assertTrue(message.contains("{missing2}"));
  }

  @Test
  public void testExportSpecificTemplatesGeotiff() {
    String template = "/tmp/{example}_output_{replicate}_{step}_{variable}.tif";
    // For GeoTIFF, replicate should be substituted with replicate number (0)
    String expected = "/tmp/example_1_output_0___step_____variable__.tif";
    assertEquals(expected, renderer.renderTemplate(template));
  }

  @Test
  public void testExportSpecificTemplatesCsv() {
    String template = "/tmp/{example}_output_{replicate}_{step}_{variable}.csv";
    // For CSV, replicate should be removed (consolidated files)
    String expected = "/tmp/example_1_output____step_____variable__.csv";
    assertEquals(expected, renderer.renderTemplate(template));
  }

  @Test
  public void testExportSpecificTemplatesNetcdf() {
    String template = "/tmp/{example}_output_{replicate}_{step}_{variable}.nc";
    // For NetCDF, replicate should be removed (consolidated files)
    String expected = "/tmp/example_1_output____step_____variable__.nc";
    assertEquals(expected, renderer.renderTemplate(template));
  }

  @Test
  public void testMixedTemplatesGeotiff() {
    String template = "file:///data/{example}_{other}_{replicate}_{variable}.tiff";
    String expected = "file:///data/example_1_other_1_0___variable__.tiff";
    assertEquals(expected, renderer.renderTemplate(template));
  }

  @Test
  public void testMixedTemplatesCsv() {
    String template = "file:///data/{example}_{other}_{replicate}_{step}.csv";
    String expected = "file:///data/example_1_other_1____step__.csv";
    assertEquals(expected, renderer.renderTemplate(template));
  }

  @Test
  public void testOnlyExportSpecificTemplatesGeotiff() {
    String template = "/output/{replicate}_{step}_{variable}.tif";
    String expected = "/output/0___step_____variable__.tif";
    assertEquals(expected, renderer.renderTemplate(template));
  }

  @Test
  public void testOnlyExportSpecificTemplatesCsv() {
    String template = "/output/{replicate}_{step}_{variable}.csv";
    String expected = "/output/___step_____variable__.csv";
    assertEquals(expected, renderer.renderTemplate(template));
  }

  @Test
  public void testNoTemplates() {
    String template = "/simple/path/to/output.csv";
    assertEquals(template, renderer.renderTemplate(template));
  }

  @Test
  public void testReplicateNumber() {
    // Test with different replicate numbers
    TemplateStringRenderer renderer1 = new TemplateStringRenderer(job, 1);
    TemplateStringRenderer renderer5 = new TemplateStringRenderer(job, 5);

    String template = "/tmp/output_{replicate}.tif";

    assertEquals("/tmp/output_1.tif", renderer1.renderTemplate(template));
    assertEquals("/tmp/output_5.tif", renderer5.renderTemplate(template));
  }

  @Test
  public void testComplexJobTemplate() {
    // Test with files that have complex naming patterns
    JoshJob complexJob = new JoshJobBuilder()
        .setFileInfo("config.backup.jshc",
            new JoshJobFileInfo("config.backup.v1", "data/config.backup.v1.jshc"))
        .setFileInfo("weather_data.jshd",
            new JoshJobFileInfo("weather_2023", "external/weather_2023.jshd"))
        .setReplicates(1)
        .build();
    TemplateStringRenderer complexRenderer = new TemplateStringRenderer(complexJob, 2);

    String template = "output_{config.backup}_{weather_data}_{replicate}.csv";
    // The base names extracted from logical file names are "config.backup" and "weather_data"
    String expected = "output_config.backup.v1_weather_2023_.csv";
    assertEquals(expected, complexRenderer.renderTemplate(template));
  }

  @Test
  public void testTemplateWithSpecialCharacters() {
    String template = "/path/with-dashes/{example}_file.with.dots_{other}.csv";
    String expected = "/path/with-dashes/example_1_file.with.dots_other_1.csv";
    assertEquals(expected, renderer.renderTemplate(template));
  }

  @Test
  public void testEmptyJobNoTemplates() {
    JoshJob emptyJob = new JoshJobBuilder()
        .setReplicates(1)
        .build();
    TemplateStringRenderer emptyRenderer = new TemplateStringRenderer(emptyJob, 0);

    String template = "/simple/path.csv";
    assertEquals(template, emptyRenderer.renderTemplate(template));
  }

  @Test
  public void testEmptyJobWithJobTemplate() {
    JoshJob emptyJob = new JoshJobBuilder()
        .setReplicates(1)
        .build();
    TemplateStringRenderer emptyRenderer = new TemplateStringRenderer(emptyJob, 0);

    String template = "/path/{missing}.csv";
    RuntimeException exception = assertThrows(RuntimeException.class, () -> {
      emptyRenderer.renderTemplate(template);
    });

    String message = exception.getMessage();
    assertTrue(message.contains("Unknown template variables: {missing}"));
    assertTrue(message.contains("Available: {replicate}, {step}, {variable}"));
  }

  @Test
  public void testCaseInsensitiveFileExtensions() {
    // Test various case combinations for file extensions
    final String templateTif = "/tmp/{example}.TIF";
    final String templateTiff = "/tmp/{example}.Tiff";
    final String templateCsv = "/tmp/{example}.CSV";
    final String templateNc = "/tmp/{example}.NC";

    // All should treat as their respective formats
    assertEquals("/tmp/example_1.TIF", renderer.renderTemplate(templateTif));
    assertEquals("/tmp/example_1.Tiff", renderer.renderTemplate(templateTiff));
    assertEquals("/tmp/example_1.CSV", renderer.renderTemplate(templateCsv));
    assertEquals("/tmp/example_1.NC", renderer.renderTemplate(templateNc));
  }

  @Test
  public void testReplicateTemplateInFilenames() {
    // Test that replicate templates are properly handled in different contexts
    TemplateStringRenderer replicateRenderer = new TemplateStringRenderer(job, 3);

    String geotiffTemplate = "/data/{example}_{replicate}.tif";
    String csvTemplate = "/data/{example}_{replicate}.csv";

    assertEquals("/data/example_1_3.tif", replicateRenderer.renderTemplate(geotiffTemplate));
    assertEquals("/data/example_1_.csv", replicateRenderer.renderTemplate(csvTemplate));
  }
}
