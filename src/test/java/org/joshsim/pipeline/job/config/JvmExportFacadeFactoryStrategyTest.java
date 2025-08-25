/**
 * Unit tests for JvmExportFacadeFactory strategy selection functionality.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.pipeline.job.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.joshsim.engine.geometry.PatchBuilderExtents;
import org.joshsim.lang.io.ExportFacade;
import org.joshsim.lang.io.ExportTarget;
import org.joshsim.lang.io.JvmExportFacadeFactory;
import org.joshsim.lang.io.strategy.CsvExportFacade;
import org.joshsim.lang.io.strategy.NetcdfExportFacade;
import org.joshsim.lang.io.strategy.ParameterizedCsvExportFacade;
import org.joshsim.lang.io.strategy.ParameterizedNetcdfExportFacade;
import org.joshsim.pipeline.job.JoshJob;
import org.joshsim.pipeline.job.JoshJobBuilder;
import org.joshsim.pipeline.job.JoshJobFileInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for JvmExportFacadeFactory strategy selection based on template analysis.
 */
class JvmExportFacadeFactoryStrategyTest {

  @TempDir
  Path tempDir;

  private JoshJob job;
  private TemplateStringRenderer renderer;
  private PatchBuilderExtents extents;
  private BigDecimal width;
  private List<String> variables;

  @BeforeEach
  void setUp() {
    // Create a test job with file mappings
    JoshJobBuilder builder = new JoshJobBuilder()
        .setReplicates(5)
        .setFileInfo("example.jshc", new JoshJobFileInfo("example_1", "test_data/example_1.jshc"))
        .setFileInfo("other.jshd", new JoshJobFileInfo("other_1", "test_data/other_1.jshd"));
    
    job = builder.build();
    renderer = new TemplateStringRenderer(job, 1);
    
    // Set up geo coordinates for NetCDF/GeoTIFF tests
    extents = new PatchBuilderExtents(BigDecimal.valueOf(-180.0), BigDecimal.valueOf(-90.0), 
                                      BigDecimal.valueOf(180.0), BigDecimal.valueOf(90.0));
    width = BigDecimal.valueOf(1000.0);
    variables = Arrays.asList("temperature", "humidity", "pressure");
  }

  @Test
  void testCsvConsolidatedStrategy() {
    // Template without {replicate} should use consolidated CSV
    JvmExportFacadeFactory factory = new JvmExportFacadeFactory(1, renderer);
    String testPath = tempDir.resolve("test_example_1_other_1.csv").toString();
    ExportTarget target = new ExportTarget("", testPath);
    
    // getPath is called internally during build process
    String processedPath = factory.getPath(testPath);
    assertEquals(testPath, processedPath); // Static path, no templates to process
    
    ExportFacade facade = factory.build(target, variables);
    assertInstanceOf(CsvExportFacade.class, facade);
  }

  @Test
  void testCsvParameterizedStrategy() {
    // Test template processing that triggers parameterized strategy
    JvmExportFacadeFactory factory = new JvmExportFacadeFactory(1, renderer);
    String templatePath = tempDir.resolve("test_{example}_{other}_{replicate}.csv").toString();
    
    // getPath processes templates and determines strategy
    String processedPath = factory.getPath(templatePath);
    assertTrue(processedPath.contains("example_1_other_1"));
    assertTrue(processedPath.contains("{replicate}")); // Should preserve for parameterized strategy
    
    // Use a basic template path for the target that can actually be processed
    String outputPath = tempDir.resolve("output_{replicate}.csv").toString();
    ExportTarget target = new ExportTarget("", outputPath);
    ExportFacade facade = factory.build(target, variables);
    assertInstanceOf(ParameterizedCsvExportFacade.class, facade);
  }

  @Test
  void testTemplateProcessingOnly() {
    // Test template processing without actually building facades that need file creation
    JvmExportFacadeFactory factory = new JvmExportFacadeFactory(1, renderer);
    
    // Test template with {replicate} - should preserve it in processed template
    String replicateTemplate = "test_{example}_{other}_{replicate}.csv";
    String processedReplicate = factory.getPath(replicateTemplate);
    assertTrue(processedReplicate.contains("example_1_other_1"));
    assertTrue(processedReplicate.contains("{replicate}"));
    
    // Test template without {replicate} - should process job templates only
    String noReplicateTemplate = "test_{example}_{other}.csv";
    String processedNoReplicate = factory.getPath(noReplicateTemplate);
    assertTrue(processedNoReplicate.contains("example_1_other_1"));
    assertTrue(!processedNoReplicate.contains("{replicate}"));
  }

  @Test
  void testLegacyFallbackBehavior() {
    // Factory without TemplateStringRenderer should use legacy behavior
    JvmExportFacadeFactory factory = new JvmExportFacadeFactory(1, (TemplateStringRenderer) null);
    
    // Test template processing with legacy path
    String replicateTemplate = "test_{replicate}.csv";
    String processedReplicate = factory.getPath(replicateTemplate);
    assertTrue(processedReplicate.contains("test_"));
    
    String noReplicateTemplate = "test.csv";  
    String processedNoReplicate = factory.getPath(noReplicateTemplate);
    assertEquals("test.csv", processedNoReplicate);
  }

  @Test  
  void testComplexTemplateProcessing() {
    JvmExportFacadeFactory factory = new JvmExportFacadeFactory(1, renderer);
    
    String template = "/{example}_{other}_{step}_{variable}_{replicate}.csv";
    String processedPath = factory.getPath(template);
    
    // Job templates should be processed, export templates should be preserved
    assertTrue(processedPath.contains("example_1"));
    assertTrue(processedPath.contains("other_1"));
    assertTrue(processedPath.contains("{step}"));
    assertTrue(processedPath.contains("{variable}"));
    assertTrue(processedPath.contains("{replicate}"));
  }

  @Test
  void testStaticTemplateNoSubstitution() {
    JvmExportFacadeFactory factory = new JvmExportFacadeFactory(1, renderer);
    
    String template = "static_file.csv";
    String processedPath = factory.getPath(template);
    assertEquals("static_file.csv", processedPath);
  }
}