/**
 * Tests for JoshJsSimFacade metadata extraction.
 *
 * <p>This test class validates the functionality of exposing simulation metadata
 * to JavaScript clients, particularly ensuring that stepsLow is properly included
 * for frontend progress bar normalization.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Unit test suite for the JoshJsSimFacade.
 *
 * <p>Tests focus on the getSimulationMetadata method to ensure that stepsLow is properly
 * exposed to JavaScript clients for accurate progress bar calculations.</p>
 */
class JoshJsSimFacadeTest {

  /**
   * Tests that stepsLow is included in metadata with explicit value.
   *
   * <p>When steps.low is explicitly set to 5 and steps.high to 15, the metadata
   * should contain stepsLow="5" and totalSteps="11".</p>
   */
  @Test
  void testGetSimulationMetadataIncludesStepsLow() {
    String joshCode = "start simulation Test\n"
        + "  steps.low = 5 count\n"
        + "  steps.high = 15 count\n"
        + "end simulation\n"
        + "start patch Default\n"
        + "end patch";

    String metadata = JoshJsSimFacade.getSimulationMetadata(joshCode, "Test");

    assertNotNull(metadata);
    assertTrue(metadata.contains("stepsLow"), "Metadata should contain stepsLow field");
    assertTrue(metadata.contains("5"), "Metadata should contain stepsLow value of 5");
    assertTrue(metadata.contains("totalSteps"), "Metadata should contain totalSteps field");
    assertTrue(metadata.contains("11"), "Metadata should contain totalSteps value of 11");
  }

  /**
   * Tests that stepsLow defaults to 0 when not specified.
   *
   * <p>When steps.low is not defined, it should default to 0, and totalSteps
   * should be calculated as stepsHigh - stepsLow + 1 = 10 - 0 + 1 = 11.</p>
   */
  @Test
  void testGetSimulationMetadataWithDefaultStepsLow() {
    String joshCode = "start simulation Test\n"
        + "end simulation\n"
        + "start patch Default\n"
        + "end patch";

    String metadata = JoshJsSimFacade.getSimulationMetadata(joshCode, "Test");

    assertNotNull(metadata);
    assertTrue(metadata.contains("stepsLow"), "Metadata should contain stepsLow field");
    assertTrue(metadata.contains("0"), "Metadata should contain default stepsLow value of 0");
    assertTrue(metadata.contains("totalSteps"), "Metadata should contain totalSteps field");
    assertTrue(metadata.contains("11"), "Metadata should contain totalSteps value of 11");
  }

  /**
   * Tests with steps from 2025-2050 (the original bug scenario).
   *
   * <p>This test case reproduces the exact scenario described in the bug report
   * where progress bars incorrectly showed nearly complete from the start. The
   * metadata should now include stepsLow="2025" so the frontend can normalize
   * the absolute timesteps correctly.</p>
   */
  @Test
  void testGetSimulationMetadataWithCustomStepRange() {
    String joshCode = "start simulation YearSimulation\n"
        + "  steps.low = 2025 count\n"
        + "  steps.high = 2050 count\n"
        + "  grid.size = 1000 m\n"
        + "  grid.low = 33.7 degrees latitude, -115.4 degrees longitude\n"
        + "  grid.high = 34.0 degrees latitude, -116.4 degrees longitude\n"
        + "  grid.patch = \"Default\"\n"
        + "end simulation\n"
        + "start patch Default\n"
        + "end patch";

    String metadata = JoshJsSimFacade.getSimulationMetadata(joshCode, "YearSimulation");

    assertNotNull(metadata);
    assertTrue(metadata.contains("stepsLow"), "Metadata should contain stepsLow field");
    assertTrue(metadata.contains("2025"), "Metadata should contain stepsLow value of 2025");
    assertTrue(metadata.contains("totalSteps"), "Metadata should contain totalSteps field");
    assertTrue(metadata.contains("26"), "Metadata should contain totalSteps value of 26");
  }

  /**
   * Tests backward compatibility by verifying all existing fields are present.
   *
   * <p>Ensures that adding stepsLow does not break existing functionality by
   * checking that all previously existing metadata fields are still present.</p>
   */
  @Test
  void testGetSimulationMetadataBackwardCompatibility() {
    String joshCode = "start simulation CompleteTest\n"
        + "  grid.size = 1000 m\n"
        + "  grid.low = 33.7 degrees latitude, -115.4 degrees longitude\n"
        + "  grid.high = 34.0 degrees latitude, -116.4 degrees longitude\n"
        + "  grid.patch = \"TestPatch\"\n"
        + "  steps.low = 10 count\n"
        + "  steps.high = 20 count\n"
        + "end simulation\n"
        + "start patch TestPatch\n"
        + "end patch";

    String metadata = JoshJsSimFacade.getSimulationMetadata(joshCode, "CompleteTest");

    assertNotNull(metadata);

    // Check all existing fields are still present
    assertTrue(metadata.contains("name"), "Metadata should contain name field");
    assertTrue(metadata.contains("CompleteTest"), "Metadata should contain simulation name");
    assertTrue(metadata.contains("patchName"), "Metadata should contain patchName field");
    assertTrue(metadata.contains("TestPatch"), "Metadata should contain patch name");
    assertTrue(metadata.contains("sizeStr"), "Metadata should contain sizeStr field");
    assertTrue(metadata.contains("totalSteps"), "Metadata should contain totalSteps field");

    // Check new field is present
    assertTrue(metadata.contains("stepsLow"), "Metadata should contain new stepsLow field");
    assertTrue(metadata.contains("10"), "Metadata should contain stepsLow value of 10");
  }

  /**
   * Tests that fractional stepsLow values are properly rounded.
   *
   * <p>When steps.low is 2.7, it should be rounded to 3. This follows the same
   * rounding behavior used in getTotalSteps() calculation.</p>
   */
  @Test
  void testGetSimulationMetadataWithFractionalStepsLow() {
    String joshCode = "start simulation FractionalTest\n"
        + "  steps.low = 2.7 count\n"
        + "  steps.high = 10.3 count\n"
        + "end simulation\n"
        + "start patch Default\n"
        + "end patch";

    String metadata = JoshJsSimFacade.getSimulationMetadata(joshCode, "FractionalTest");

    assertNotNull(metadata);
    assertTrue(metadata.contains("stepsLow"), "Metadata should contain stepsLow field");
    assertTrue(metadata.contains("3"), "Metadata should contain rounded stepsLow value of 3");

    // Verify totalSteps calculation: 10 - 3 + 1 = 8
    assertTrue(metadata.contains("totalSteps"), "Metadata should contain totalSteps field");
    assertTrue(metadata.contains("8"), "Metadata should contain totalSteps value of 8");
  }

  /**
   * Tests metadata extraction with a large step range.
   *
   * <p>Ensures the implementation works correctly with large step values.</p>
   */
  @Test
  void testGetSimulationMetadataWithLargeStepRange() {
    String joshCode = "start simulation LargeRangeTest\n"
        + "  steps.low = 1990 count\n"
        + "  steps.high = 2020 count\n"
        + "end simulation\n"
        + "start patch Default\n"
        + "end patch";

    String metadata = JoshJsSimFacade.getSimulationMetadata(joshCode, "LargeRangeTest");

    assertNotNull(metadata);
    assertTrue(metadata.contains("stepsLow"), "Metadata should contain stepsLow field");
    assertTrue(metadata.contains("1990"), "Metadata should contain stepsLow value of 1990");
    assertTrue(metadata.contains("totalSteps"), "Metadata should contain totalSteps field");
    assertTrue(metadata.contains("31"), "Metadata should contain totalSteps value of 31");
  }
}
