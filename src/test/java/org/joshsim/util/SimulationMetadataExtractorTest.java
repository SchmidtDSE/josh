/**
 * Unit tests for SimulationMetadataExtractor utility.
 *
 * <p>This test class validates the functionality of extracting simulation metadata
 * from Josh script files, including step ranges and total step calculations.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;


/**
 * Tests for SimulationMetadataExtractor utility class.
 *
 * <p>This test suite validates the extraction of simulation metadata from Josh scripts,
 * including proper handling of explicit step definitions, default values, and error cases.</p>
 */
public class SimulationMetadataExtractorTest {

  @TempDir
  File tempDir;

  /**
   * Tests extracting metadata from a Josh script with explicit steps.low and steps.high values.
   */
  @Test
  public void testExtractMetadataWithExplicitSteps() throws IOException {
    String joshCode = "start simulation Test\n"
        + "  steps.low = 5 count\n"
        + "  steps.high = 15 count\n"
        + "end simulation\n"
        + "start patch Default\n"
        + "end patch";
    
    File tempFile = new File(tempDir, "test.josh");
    Files.writeString(tempFile.toPath(), joshCode);
    
    SimulationMetadataExtractor.SimulationMetadata metadata = 
        SimulationMetadataExtractor.extractMetadata(tempFile, "Test");
    
    assertNotNull(metadata);
    assertEquals(5, metadata.getStepsLow());
    assertEquals(15, metadata.getStepsHigh());
    assertEquals(11, metadata.getTotalSteps()); // 15 - 5 + 1
  }

  /**
   * Tests extracting metadata from a Josh script using default step values.
   */
  @Test
  public void testExtractMetadataWithDefaults() throws IOException {
    String joshCode = "start simulation Test\n"
        + "end simulation\n"
        + "start patch Default\n"
        + "end patch";
    
    File tempFile = new File(tempDir, "test.josh");
    Files.writeString(tempFile.toPath(), joshCode);
    
    SimulationMetadataExtractor.SimulationMetadata metadata = 
        SimulationMetadataExtractor.extractMetadata(tempFile, "Test");
    
    assertNotNull(metadata);
    assertEquals(0, metadata.getStepsLow());
    assertEquals(10, metadata.getStepsHigh());
    assertEquals(11, metadata.getTotalSteps()); // 10 - 0 + 1
  }

  /**
   * Tests extracting metadata from a Josh script with custom step range.
   */
  @Test
  public void testExtractMetadataWithCustomRange() throws IOException {
    String joshCode = "start simulation CustomSim\n"
        + "  steps.low = 100 count\n"
        + "  steps.high = 199 count\n"
        + "  grid.size = 1000 m\n"
        + "  grid.low = 33.7 degrees latitude, -115.4 degrees longitude\n"
        + "  grid.high = 34.0 degrees latitude, -116.4 degrees longitude\n"
        + "  grid.patch = \"Default\"\n"
        + "end simulation\n"
        + "start patch Default\n"
        + "end patch";
    
    File tempFile = new File(tempDir, "custom.josh");
    Files.writeString(tempFile.toPath(), joshCode);
    
    SimulationMetadataExtractor.SimulationMetadata metadata = 
        SimulationMetadataExtractor.extractMetadata(tempFile, "CustomSim");
    
    assertNotNull(metadata);
    assertEquals(100, metadata.getStepsLow());
    assertEquals(199, metadata.getStepsHigh());
    assertEquals(100, metadata.getTotalSteps()); // 199 - 100 + 1
  }

  /**
   * Tests extracting metadata from Josh code string directly (without file).
   */
  @Test
  public void testExtractMetadataFromCodeString() {
    String joshCode = "start simulation StringTest\n"
        + "  steps.low = 20 count\n"
        + "  steps.high = 29 count\n"
        + "end simulation\n"
        + "start patch Default\n"
        + "end patch";
    
    SimulationMetadataExtractor.SimulationMetadata metadata = 
        SimulationMetadataExtractor.extractMetadataFromCode(joshCode, "StringTest");
    
    assertNotNull(metadata);
    assertEquals(20, metadata.getStepsLow());
    assertEquals(29, metadata.getStepsHigh());
    assertEquals(10, metadata.getTotalSteps()); // 29 - 20 + 1
  }

  /**
   * Tests that invalid Josh script returns default values instead of throwing exception.
   */
  @Test
  public void testExtractMetadataFromInvalidCode() {
    String invalidJoshCode = "this is not valid josh code at all";
    
    // Should return defaults rather than throwing exception
    SimulationMetadataExtractor.SimulationMetadata metadata = 
        SimulationMetadataExtractor.extractMetadataFromCode(invalidJoshCode, "Invalid");
    
    assertNotNull(metadata);
    assertEquals(0, metadata.getStepsLow());
    assertEquals(10, metadata.getStepsHigh());
    assertEquals(11, metadata.getTotalSteps()); // Default fallback
  }

  /**
   * Tests extracting metadata when file does not exist.
   */
  @Test
  public void testExtractMetadataFromNonexistentFile() {
    File nonexistentFile = new File(tempDir, "nonexistent.josh");
    
    assertThrows(IllegalArgumentException.class, () -> {
      SimulationMetadataExtractor.extractMetadata(nonexistentFile, "Test");
    });
  }

  /**
   * Tests the toString method of SimulationMetadata.
   */
  @Test
  public void testSimulationMetadataToString() {
    SimulationMetadataExtractor.SimulationMetadata metadata = 
        new SimulationMetadataExtractor.SimulationMetadata(5, 15, 11);
    
    String result = metadata.toString();
    assertNotNull(result);
    assertEquals("SimulationMetadata{stepsLow=5, stepsHigh=15, totalSteps=11}", result);
  }

  /**
   * Tests extracting metadata from a complex Josh script with organisms and exports.
   */
  @Test
  public void testExtractMetadataFromComplexScript() throws IOException {
    String complexJoshCode = "start unit year\n"
        + "  alias years\n"
        + "  alias yr\n"
        + "  alias yrs\n"
        + "end unit\n"
        + "\n"
        + "start simulation ComplexSim\n"
        + "  grid.size = 1000 m\n"
        + "  grid.low = 33.7 degrees latitude, -115.4 degrees longitude\n"
        + "  grid.high = 34.0 degrees latitude, -116.4 degrees longitude\n"
        + "  grid.patch = \"Default\"\n"
        + "  \n"
        + "  steps.low = 50 count\n"
        + "  steps.high = 149 count\n"
        + "  \n"
        + "  exportFiles.patch = \"memory://editor/patches\"\n"
        + "end simulation\n"
        + "\n"
        + "start patch Default\n"
        + "  ForeverTree.init = create 10 count of ForeverTree\n"
        + "  \n"
        + "  export.averageAge.step = mean(ForeverTree.age)\n"
        + "  export.averageHeight.step = mean(ForeverTree.height)\n"
        + "end patch\n"
        + "\n"
        + "start organism ForeverTree\n"
        + "  age.init = 0 year\n"
        + "  age.step = prior.age + 1 year\n"
        + "  \n"
        + "  height.init = 0 meters\n"
        + "  height.step = prior.height + sample uniform from 0 meters to 1 meters\n"
        + "end organism";
    
    File tempFile = new File(tempDir, "complex.josh");
    Files.writeString(tempFile.toPath(), complexJoshCode);
    
    SimulationMetadataExtractor.SimulationMetadata metadata = 
        SimulationMetadataExtractor.extractMetadata(tempFile, "ComplexSim");
    
    assertNotNull(metadata);
    assertEquals(50, metadata.getStepsLow());
    assertEquals(149, metadata.getStepsHigh());
    assertEquals(100, metadata.getTotalSteps()); // 149 - 50 + 1
  }

  /**
   * Tests extracting metadata with fractional step values that should be rounded.
   */
  @Test
  public void testExtractMetadataWithFractionalSteps() throws IOException {
    String joshCode = "start simulation FractionalTest\n"
        + "  steps.low = 2.3 count\n"
        + "  steps.high = 7.8 count\n"
        + "end simulation\n"
        + "start patch Default\n"
        + "end patch";
    
    File tempFile = new File(tempDir, "fractional.josh");
    Files.writeString(tempFile.toPath(), joshCode);
    
    SimulationMetadataExtractor.SimulationMetadata metadata = 
        SimulationMetadataExtractor.extractMetadata(tempFile, "FractionalTest");
    
    assertNotNull(metadata);
    assertEquals(2, metadata.getStepsLow()); // 2.3 rounded to 2
    assertEquals(8, metadata.getStepsHigh()); // 7.8 rounded to 8
    assertEquals(7, metadata.getTotalSteps()); // 8 - 2 + 1
  }
}