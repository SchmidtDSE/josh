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

  @Test
  void testConstructorValidation() {
    // Valid template should work
    String validTemplate = "file:///tmp/data_{replicate}.csv";
    ReplicateOutputStreamGenerator generator = new ReplicateOutputStreamGenerator(validTemplate);
    assertEquals(validTemplate, generator.getPathTemplate());

    // Null template should throw exception
    assertThrows(IllegalArgumentException.class, () ->
        new ReplicateOutputStreamGenerator(null));

    // Empty template should throw exception
    assertThrows(IllegalArgumentException.class, () ->
        new ReplicateOutputStreamGenerator(""));

    // Whitespace only template should throw exception
    assertThrows(IllegalArgumentException.class, () ->
        new ReplicateOutputStreamGenerator("   "));
  }

  @Test
  void testCsvStreamGeneration() throws IOException {
    String template = tempDir.resolve("test_{replicate}.csv").toString();
    ReplicateOutputStreamGenerator generator = new ReplicateOutputStreamGenerator(template);

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
    String template = tempDir.resolve("simulation_{replicate}.nc").toString();
    ReplicateOutputStreamGenerator generator = new ReplicateOutputStreamGenerator(template);

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
        new ReplicateOutputStreamGenerator("file:///tmp/data_{replicate}.csv");
    assertTrue(withReplicate.hasReplicateTemplate());

    ReplicateOutputStreamGenerator withoutReplicate = 
        new ReplicateOutputStreamGenerator("file:///tmp/static_file.csv");
    assertFalse(withoutReplicate.hasReplicateTemplate());

    ReplicateOutputStreamGenerator multipleTemplates = 
        new ReplicateOutputStreamGenerator("file:///tmp/{step}_{replicate}_{variable}.tiff");
    assertTrue(multipleTemplates.hasReplicateTemplate());
  }

  @Test
  void testGetPathTemplate() {
    String template = "file:///tmp/complex_{replicate}_data.nc";
    ReplicateOutputStreamGenerator generator = new ReplicateOutputStreamGenerator(template);
    assertEquals(template, generator.getPathTemplate());
  }

  @Test
  void testToString() {
    String template = "file:///tmp/test_{replicate}.csv";
    ReplicateOutputStreamGenerator generator = new ReplicateOutputStreamGenerator(template);
    
    String toString = generator.toString();
    assertTrue(toString.contains("ReplicateOutputStreamGenerator"));
    assertTrue(toString.contains(template));
  }

  @Test
  void testEquals() {
    ReplicateOutputStreamGenerator gen1 = 
        new ReplicateOutputStreamGenerator("file:///tmp/test_{replicate}.csv");
    ReplicateOutputStreamGenerator gen2 = 
        new ReplicateOutputStreamGenerator("file:///tmp/test_{replicate}.csv");
    ReplicateOutputStreamGenerator gen3 = 
        new ReplicateOutputStreamGenerator("file:///tmp/different_{replicate}.csv");

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
    ReplicateOutputStreamGenerator gen1 = 
        new ReplicateOutputStreamGenerator("file:///tmp/test_{replicate}.csv");
    ReplicateOutputStreamGenerator gen2 = 
        new ReplicateOutputStreamGenerator("file:///tmp/test_{replicate}.csv");
    ReplicateOutputStreamGenerator gen3 = 
        new ReplicateOutputStreamGenerator("file:///tmp/different_{replicate}.csv");

    // Equal objects should have equal hash codes
    assertEquals(gen1.hashCode(), gen2.hashCode());

    // Different objects should typically have different hash codes
    assertNotEquals(gen1.hashCode(), gen3.hashCode());
  }

  @Test
  void testMultipleReplicateSubstitution() throws IOException {
    String template = tempDir.resolve("data_{replicate}_{replicate}_file.csv").toString();
    ReplicateOutputStreamGenerator generator = new ReplicateOutputStreamGenerator(template);

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
    String template = tempDir.resolve("replicate_{replicate}.csv").toString();
    ReplicateOutputStreamGenerator generator = new ReplicateOutputStreamGenerator(template);

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
    String invalidTemplate = "/dev/null/invalid\0path_{replicate}.csv";
    ReplicateOutputStreamGenerator generator = new ReplicateOutputStreamGenerator(invalidTemplate);

    ParameterizedCsvExportFacade.StreamReference ref = 
        new ParameterizedCsvExportFacade.StreamReference(1);

    // Should throw RuntimeException when trying to create the file
    assertThrows(RuntimeException.class, () -> generator.getStream(ref));
  }
}