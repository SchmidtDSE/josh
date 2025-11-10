/**
 * Tests for CombinedTextWriter.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Tests for CombinedTextWriter implementation.
 */
public class CombinedTextWriterTest {

  /**
   * Test that routing works correctly by entity type.
   */
  @Test
  public void testEntityTypeRouting() throws IOException {
    // Create separate output streams for different entity types
    ByteArrayOutputStream patchOutput = new ByteArrayOutputStream();
    ByteArrayOutputStream organismOutput = new ByteArrayOutputStream();

    Map<String, OutputWriter<String>> writers = new HashMap<>();
    writers.put("patch", createTestWriter(patchOutput, "stdout"));
    writers.put("organism", createTestWriter(organismOutput, "stdout"));

    CombinedTextWriter combined = new CombinedTextWriter(writers);
    combined.start();

    // Write to patch
    combined.setCurrentEntityType("patch");
    combined.write("Patch message", 1);

    // Write to organism
    combined.setCurrentEntityType("organism");
    combined.write("Organism message", 2);

    combined.join();

    // Check that messages went to the right places
    String patchStr = patchOutput.toString();
    String organismStr = organismOutput.toString();

    assertTrue(patchStr.contains("Patch message"));
    assertFalse(patchStr.contains("Organism message"));

    assertTrue(organismStr.contains("Organism message"));
    assertFalse(organismStr.contains("Patch message"));
  }

  /**
   * Test that writes are silently ignored for unconfigured entity types.
   */
  @Test
  public void testUnconfiguredEntityType() throws IOException {
    ByteArrayOutputStream patchOutput = new ByteArrayOutputStream();

    Map<String, OutputWriter<String>> writers = new HashMap<>();
    writers.put("patch", createTestWriter(patchOutput, "stdout"));

    CombinedTextWriter combined = new CombinedTextWriter(writers);
    combined.start();

    // Try to write to an unconfigured entity type
    combined.setCurrentEntityType("nonexistent");
    combined.write("This should be ignored", 1);

    combined.join();

    // Should have no output
    String output = patchOutput.toString();
    assertFalse(output.contains("This should be ignored"));
  }

  /**
   * Test isConfigured returns correct value.
   */
  @Test
  public void testIsConfigured() throws IOException {
    Map<String, OutputWriter<String>> writers = new HashMap<>();
    CombinedTextWriter combined = new CombinedTextWriter(writers);
    assertFalse(combined.isConfigured());

    writers.put("patch", createTestWriter(new ByteArrayOutputStream(), "stdout"));
    combined = new CombinedTextWriter(writers);
    assertTrue(combined.isConfigured());
  }

  /**
   * Test getWriterCount returns correct value.
   */
  @Test
  public void testGetWriterCount() throws IOException {
    Map<String, OutputWriter<String>> writers = new HashMap<>();
    CombinedTextWriter combined = new CombinedTextWriter(writers);
    assertEquals(0, combined.getWriterCount());

    writers.put("patch", createTestWriter(new ByteArrayOutputStream(), "stdout"));
    combined = new CombinedTextWriter(writers);
    assertEquals(1, combined.getWriterCount());

    writers.put("organism", createTestWriter(new ByteArrayOutputStream(), "stdout"));
    combined = new CombinedTextWriter(writers);
    assertEquals(2, combined.getWriterCount());
  }

  /**
   * Test clearCurrentEntityType.
   */
  @Test
  public void testClearCurrentEntityType() throws IOException {
    ByteArrayOutputStream patchOutput = new ByteArrayOutputStream();

    Map<String, OutputWriter<String>> writers = new HashMap<>();
    writers.put("patch", createTestWriter(patchOutput, "stdout"));

    CombinedTextWriter combined = new CombinedTextWriter(writers);
    combined.start();

    combined.setCurrentEntityType("patch");
    combined.clearCurrentEntityType();

    // After clearing, this write should be ignored
    combined.write("Should be ignored", 1);

    combined.join();

    String output = patchOutput.toString();
    assertFalse(output.contains("Should be ignored"));
  }

  /**
   * Test getCurrentEntityType.
   */
  @Test
  public void testGetCurrentEntityType() {
    Map<String, OutputWriter<String>> writers = new HashMap<>();
    CombinedTextWriter combined = new CombinedTextWriter(writers);

    assertNull(combined.getCurrentEntityType());

    combined.setCurrentEntityType("patch");
    assertEquals("patch", combined.getCurrentEntityType());

    combined.clearCurrentEntityType();
    assertNull(combined.getCurrentEntityType());
  }

  /**
   * Helper to create a test text writer.
   */
  private TextOutputWriter createTestWriter(ByteArrayOutputStream output, String protocol)
      throws IOException {
    OutputStreamStrategy strategy = new TestOutputStreamStrategy(output);
    OutputTarget target = new OutputTarget(protocol, "", "");
    return new TextOutputWriter(target, strategy);
  }

  /**
   * Test output stream strategy for testing.
   */
  private static class TestOutputStreamStrategy implements OutputStreamStrategy {
    private final OutputStream stream;

    public TestOutputStreamStrategy(OutputStream stream) {
      this.stream = stream;
    }

    @Override
    public OutputStream open() throws IOException {
      return stream;
    }
  }
}
