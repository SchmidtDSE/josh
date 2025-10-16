/**
 * Unit tests for ReplicateOutputStreamGenerator.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.joshsim.lang.io.strategy.ParameterizedCsvExportFacade;
import org.joshsim.lang.io.strategy.ParameterizedNetcdfExportFacade;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for ReplicateOutputStreamGenerator functionality and stream generation.
 */
class ReplicateOutputStreamGeneratorTest {

  @TempDir
  Path tempDir;

  /**
   * Creates a minimal mock factory for testing that only handles local file protocol.
   *
   * @return JvmExportFacadeFactory configured for testing
   */
  private JvmExportFacadeFactory createMockFactory() {
    return new JvmExportFacadeFactory(
        1,
        (org.joshsim.pipeline.job.config.TemplateStringRenderer) null,
        (org.joshsim.util.MinioOptions) null
    );
  }

  @Test
  void testConstructorValidation() {
    // Valid template should work
    ExportTarget validTemplate = new ExportTarget("file", "", "/tmp/data_{replicate}.csv");
    ReplicateOutputStreamGenerator generator = new ReplicateOutputStreamGenerator(
        validTemplate,
        createMockFactory()
    );
    assertEquals(validTemplate, generator.getTargetTemplate());

    // Null template should throw exception
    assertThrows(IllegalArgumentException.class, () ->
        new ReplicateOutputStreamGenerator(null, createMockFactory()));

    // Template with null path should throw exception
    assertThrows(IllegalArgumentException.class, () ->
        new ReplicateOutputStreamGenerator(
            new ExportTarget("file", "", null),
            createMockFactory()));

    // Template with empty path should throw exception
    assertThrows(IllegalArgumentException.class, () ->
        new ReplicateOutputStreamGenerator(
            new ExportTarget("file", "", ""),
            createMockFactory()));

    // Template with whitespace-only path should throw exception
    assertThrows(IllegalArgumentException.class, () ->
        new ReplicateOutputStreamGenerator(
            new ExportTarget("file", "", "   "),
            createMockFactory()));
  }

  @Test
  void testCsvStreamGeneration() throws IOException {
    String pathTemplate = tempDir.resolve("test_{replicate}.csv").toString();
    ExportTarget targetTemplate = new ExportTarget("file", "", pathTemplate);
    ReplicateOutputStreamGenerator generator = new ReplicateOutputStreamGenerator(
        targetTemplate,
        createMockFactory()
    );

    // Test CSV stream reference
    ParameterizedCsvExportFacade.StreamReference ref1 =
        new ParameterizedCsvExportFacade.StreamReference(1);
    ParameterizedCsvExportFacade.StreamReference ref2 =
        new ParameterizedCsvExportFacade.StreamReference(2);

    // Generate streams for different replicates
    try (OutputStream stream1 = generator.getStream(ref1);
         OutputStream stream2 = generator.getStream(ref2)) {

      // Write some test data
      stream1.write("test data 1".getBytes());
      stream2.write("test data 2".getBytes());
    }

    // Verify files were created with correct names
    File file1 = new File(tempDir.resolve("test_1.csv").toString());
    File file2 = new File(tempDir.resolve("test_2.csv").toString());

    assertTrue(file1.exists());
    assertTrue(file2.exists());

    // Verify file contents
    assertEquals("test data 1", Files.readString(file1.toPath()));
    assertEquals("test data 2", Files.readString(file2.toPath()));
  }

  @Test
  void testNetcdfStreamGeneration() throws IOException {
    String pathTemplate = tempDir.resolve("simulation_{replicate}.nc").toString();
    ExportTarget targetTemplate = new ExportTarget("file", "", pathTemplate);
    ReplicateOutputStreamGenerator generator = new ReplicateOutputStreamGenerator(
        targetTemplate,
        createMockFactory()
    );

    // Test NetCDF stream reference
    ParameterizedNetcdfExportFacade.StreamReference ref3 =
        new ParameterizedNetcdfExportFacade.StreamReference(3);

    // Generate stream
    try (OutputStream stream = generator.getStream(ref3)) {
      stream.write("netcdf test data".getBytes());
    }

    // Verify file was created
    File file = new File(tempDir.resolve("simulation_3.nc").toString());
    assertTrue(file.exists());
    assertEquals("netcdf test data", Files.readString(file.toPath()));
  }

  @Test
  void testHasReplicateTemplate() {
    ReplicateOutputStreamGenerator withReplicate =
        new ReplicateOutputStreamGenerator(
            new ExportTarget("file", "", "/tmp/data_{replicate}.csv"),
            createMockFactory());
    assertTrue(withReplicate.hasReplicateTemplate());

    ReplicateOutputStreamGenerator withoutReplicate =
        new ReplicateOutputStreamGenerator(
            new ExportTarget("file", "", "/tmp/static_file.csv"),
            createMockFactory());
    assertFalse(withoutReplicate.hasReplicateTemplate());

    ReplicateOutputStreamGenerator multipleTemplates =
        new ReplicateOutputStreamGenerator(
            new ExportTarget("file", "", "/tmp/{step}_{replicate}_{variable}.tiff"),
            createMockFactory());
    assertTrue(multipleTemplates.hasReplicateTemplate());
  }

  @Test
  void testGetPathTemplate() {
    ExportTarget template = new ExportTarget("file", "", "/tmp/complex_{replicate}_data.nc");
    ReplicateOutputStreamGenerator generator = new ReplicateOutputStreamGenerator(
        template,
        createMockFactory()
    );
    // Test the new method
    assertEquals(template, generator.getTargetTemplate());

    // Test deprecated method still works
    assertEquals("file:///tmp/complex_{replicate}_data.nc", generator.getPathTemplate());
  }

  @Test
  void testToString() {
    ExportTarget template = new ExportTarget("file", "", "/tmp/test_{replicate}.csv");
    ReplicateOutputStreamGenerator generator = new ReplicateOutputStreamGenerator(
        template,
        createMockFactory()
    );

    String toString = generator.toString();
    assertTrue(toString.contains("ReplicateOutputStreamGenerator"));
    assertTrue(toString.contains("file:///tmp/test_{replicate}.csv"));
  }

  @Test
  void testEquals() {
    ExportTarget target1 = new ExportTarget("file", "", "/tmp/test_{replicate}.csv");
    ExportTarget target2 = new ExportTarget("file", "", "/tmp/test_{replicate}.csv");
    ExportTarget target3 = new ExportTarget("file", "", "/tmp/different_{replicate}.csv");

    ReplicateOutputStreamGenerator gen1 =
        new ReplicateOutputStreamGenerator(target1, createMockFactory());
    ReplicateOutputStreamGenerator gen2 =
        new ReplicateOutputStreamGenerator(target2, createMockFactory());
    ReplicateOutputStreamGenerator gen3 =
        new ReplicateOutputStreamGenerator(target3, createMockFactory());

    // Test equality
    assertEquals(gen1, gen2);
    assertEquals(gen1, gen1); // Self-equality

    // Test inequality
    assertNotEquals(gen1, gen3); // Different template
    assertNotEquals(gen1, null); // Null comparison
    assertNotEquals(gen1, "string"); // Different type
  }

  @Test
  void testHashCode() {
    ExportTarget target1 = new ExportTarget("file", "", "/tmp/test_{replicate}.csv");
    ExportTarget target2 = new ExportTarget("file", "", "/tmp/test_{replicate}.csv");
    ExportTarget target3 = new ExportTarget("file", "", "/tmp/different_{replicate}.csv");

    ReplicateOutputStreamGenerator gen1 =
        new ReplicateOutputStreamGenerator(target1, createMockFactory());
    ReplicateOutputStreamGenerator gen2 =
        new ReplicateOutputStreamGenerator(target2, createMockFactory());
    ReplicateOutputStreamGenerator gen3 =
        new ReplicateOutputStreamGenerator(target3, createMockFactory());

    // Equal objects should have equal hash codes
    assertEquals(gen1.hashCode(), gen2.hashCode());

    // Different objects should typically have different hash codes
    assertNotEquals(gen1.hashCode(), gen3.hashCode());
  }

  @Test
  void testMultipleReplicateSubstitution() throws IOException {
    String pathTemplate = tempDir.resolve("data_{replicate}_{replicate}_file.csv").toString();
    ExportTarget targetTemplate = new ExportTarget("file", "", pathTemplate);
    ReplicateOutputStreamGenerator generator = new ReplicateOutputStreamGenerator(
        targetTemplate,
        createMockFactory()
    );

    ParameterizedCsvExportFacade.StreamReference ref =
        new ParameterizedCsvExportFacade.StreamReference(5);

    try (OutputStream stream = generator.getStream(ref)) {
      stream.write("test".getBytes());
    }

    // Both {replicate} should be replaced with 5
    File file = new File(tempDir.resolve("data_5_5_file.csv").toString());
    assertTrue(file.exists());
  }

  @Test
  void testLargeReplicateNumbers() throws IOException {
    String pathTemplate = tempDir.resolve("replicate_{replicate}.csv").toString();
    ExportTarget targetTemplate = new ExportTarget("file", "", pathTemplate);
    ReplicateOutputStreamGenerator generator = new ReplicateOutputStreamGenerator(
        targetTemplate,
        createMockFactory()
    );

    // Test with large replicate number
    ParameterizedCsvExportFacade.StreamReference ref =
        new ParameterizedCsvExportFacade.StreamReference(99999);

    try (OutputStream stream = generator.getStream(ref)) {
      stream.write("large replicate test".getBytes());
    }

    File file = new File(tempDir.resolve("replicate_99999.csv").toString());
    assertTrue(file.exists());
    assertEquals("large replicate test", Files.readString(file.toPath()));
  }

  @Test
  void testInvalidPath() {
    // Use an invalid path that cannot be created (e.g., with null characters)
    ExportTarget invalidTarget = new ExportTarget(
        "file", "", "/dev/null/invalid\0path_{replicate}.csv");
    ReplicateOutputStreamGenerator generator = new ReplicateOutputStreamGenerator(
        invalidTarget,
        createMockFactory()
    );

    ParameterizedCsvExportFacade.StreamReference ref =
        new ParameterizedCsvExportFacade.StreamReference(1);

    // Should throw RuntimeException when trying to create the file
    assertThrows(RuntimeException.class, () -> generator.getStream(ref));
  }
}
