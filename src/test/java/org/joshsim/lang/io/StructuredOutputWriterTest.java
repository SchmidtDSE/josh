/**
 * Tests for StructuredOutputWriter.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for StructuredOutputWriter.
 *
 * <p>These tests verify that StructuredOutputWriter correctly writes CSV-formatted structured
 * data with proper formatting, column ordering, and step/replicate metadata.</p>
 */
public class StructuredOutputWriterTest {

  private ByteArrayOutputStream outputStream;
  private TestOutputStreamStrategy outputStrategy;

  /**
   * Set up test fixtures before each test.
   */
  @BeforeEach
  public void setUp() {
    outputStream = new ByteArrayOutputStream();
    outputStrategy = new TestOutputStreamStrategy(outputStream);
  }

  /**
   * Clean up test fixtures after each test.
   *
   * @throws IOException if closing output stream fails
   */
  @AfterEach
  public void tearDown() throws IOException {
    outputStream.close();
  }

  @Test
  public void testWriteSingleRow() throws InterruptedException {
    OutputTarget target = new OutputTarget("file", "/tmp/test.csv");
    StructuredOutputWriter writer = new StructuredOutputWriter(target, outputStrategy, 0);

    writer.start();

    Map<String, String> row = new LinkedHashMap<>();
    row.put("age", "5.0");
    row.put("height", "10.2");
    writer.write(row, 100);

    writer.join();

    String output = outputStream.toString();
    String[] lines = output.split("\n");

    // Check header
    assertEquals("age,height,step,replicate", lines[0].trim());

    // Check data row
    assertEquals("5.0,10.2,100,0", lines[1].trim());
  }

  @Test
  public void testWriteMultipleRows() throws InterruptedException {
    OutputTarget target = new OutputTarget("file", "/tmp/test.csv");
    StructuredOutputWriter writer = new StructuredOutputWriter(target, outputStrategy, 0);

    writer.start();

    Map<String, String> row1 = new LinkedHashMap<>();
    row1.put("age", "5.0");
    row1.put("height", "10.2");
    writer.write(row1, 100);

    Map<String, String> row2 = new LinkedHashMap<>();
    row2.put("age", "6.0");
    row2.put("height", "11.5");
    writer.write(row2, 101);

    writer.join();

    String output = outputStream.toString();
    String[] lines = output.split("\n");

    // Check header
    assertEquals("age,height,step,replicate", lines[0].trim());

    // Check data rows
    assertEquals("5.0,10.2,100,0", lines[1].trim());
    assertEquals("6.0,11.5,101,0", lines[2].trim());
  }

  @Test
  public void testReplicateNumber() throws InterruptedException {
    OutputTarget target = new OutputTarget("file", "/tmp/test.csv");
    StructuredOutputWriter writer = new StructuredOutputWriter(target, outputStrategy, 5);

    writer.start();

    Map<String, String> row = new LinkedHashMap<>();
    row.put("biomass", "100.5");
    writer.write(row, 200);

    writer.join();

    String output = outputStream.toString();
    String[] lines = output.split("\n");

    // Check that replicate is 5
    assertEquals("biomass,step,replicate", lines[0].trim());
    assertEquals("100.5,200,5", lines[1].trim());
  }

  @Test
  public void testGetPath() {
    OutputTarget target = new OutputTarget("file", "/tmp/output.csv");
    StructuredOutputWriter writer = new StructuredOutputWriter(target, outputStrategy, 0);

    assertEquals("/tmp/output.csv", writer.getPath());
  }

  @Test
  public void testCsvQuoting() throws InterruptedException {
    OutputTarget target = new OutputTarget("file", "/tmp/test.csv");
    StructuredOutputWriter writer = new StructuredOutputWriter(target, outputStrategy, 0);

    writer.start();

    Map<String, String> row = new LinkedHashMap<>();
    row.put("name", "Tree with, comma");
    row.put("description", "Has \"quotes\" in it");
    writer.write(row, 50);

    writer.join();

    String output = outputStream.toString();
    assertTrue(output.contains("\"Tree with, comma\""));
    assertTrue(output.contains("\"Has \"\"quotes\"\" in it\""));
  }

  @Test
  public void testEmptyValues() throws InterruptedException {
    OutputTarget target = new OutputTarget("file", "/tmp/test.csv");
    StructuredOutputWriter writer = new StructuredOutputWriter(target, outputStrategy, 0);

    writer.start();

    Map<String, String> row = new LinkedHashMap<>();
    row.put("name", "Tree");
    row.put("description", "");
    row.put("notes", "");
    writer.write(row, 25);

    writer.join();

    String output = outputStream.toString();
    String[] lines = output.split("\n");

    // Empty values should still be present
    assertEquals("name,description,notes,step,replicate", lines[0].trim());
    assertEquals("Tree,,,25,0", lines[1].trim());
  }

  @Test
  public void testColumnOrdering() throws InterruptedException {
    OutputTarget target = new OutputTarget("file", "/tmp/test.csv");
    StructuredOutputWriter writer = new StructuredOutputWriter(target, outputStrategy, 0);

    writer.start();

    // Use LinkedHashMap to preserve insertion order
    Map<String, String> row = new LinkedHashMap<>();
    row.put("z_last", "3");
    row.put("a_first", "1");
    row.put("m_middle", "2");
    writer.write(row, 10);

    writer.join();

    String output = outputStream.toString();
    String[] lines = output.split("\n");

    // Columns should be in LinkedHashMap insertion order, followed by step and replicate
    assertEquals("z_last,a_first,m_middle,step,replicate", lines[0].trim());
    assertEquals("3,1,2,10,0", lines[1].trim());
  }

  /**
   * Test OutputStreamStrategy that writes to a ByteArrayOutputStream.
   */
  private static class TestOutputStreamStrategy implements OutputStreamStrategy {
    private final ByteArrayOutputStream output;

    public TestOutputStreamStrategy(ByteArrayOutputStream output) {
      this.output = output;
    }

    @Override
    public OutputStream open() throws IOException {
      return output;
    }
  }
}
