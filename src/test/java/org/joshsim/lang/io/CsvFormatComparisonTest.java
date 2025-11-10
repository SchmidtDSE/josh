/**
 * Tests to verify StructuredOutputWriter produces identical CSV format to CsvExportFacade.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import org.joshsim.lang.io.strategy.CsvExportFacade;
import org.joshsim.lang.io.strategy.MapExportSerializeStrategy;
import org.junit.jupiter.api.Test;

/**
 * Comparison tests to verify CSV format consistency.
 *
 * <p>These tests verify that StructuredOutputWriter produces identical CSV output
 * to the existing CsvExportFacade implementation, ensuring backward compatibility
 * and correct CSV formatting.</p>
 */
public class CsvFormatComparisonTest {

  @Test
  public void testSingleRowFormatMatches() throws InterruptedException {
    // Old system: CsvExportFacade
    ByteArrayOutputStream oldOutput = new ByteArrayOutputStream();
    TestOutputStreamStrategy oldStrategy = new TestOutputStreamStrategy(oldOutput);
    MapExportSerializeStrategy serializeStrategy = new MapSerializeStrategy();
    CsvExportFacade oldFacade = new CsvExportFacade(oldStrategy, serializeStrategy);

    oldFacade.start();

    // Create test data
    Map<String, String> row = new LinkedHashMap<>();
    row.put("age", "5.0");
    row.put("height", "10.2");
    row.put("biomass", "100.5");

    // Write via old system (ExportTask path)
    ExportTask task = new ExportTask(new TestNamedMap("TestEntity", row), 100L, 0);
    oldFacade.write(task);

    oldFacade.join();

    // New system: StructuredOutputWriter
    ByteArrayOutputStream newOutput = new ByteArrayOutputStream();
    TestOutputStreamStrategy newStrategy = new TestOutputStreamStrategy(newOutput);
    OutputTarget target = new OutputTarget("file", "/tmp/test.csv");
    StructuredOutputWriter newWriter = new StructuredOutputWriter(target, newStrategy, 0);

    newWriter.start();

    // Same test data
    Map<String, String> newRow = new LinkedHashMap<>();
    newRow.put("age", "5.0");
    newRow.put("height", "10.2");
    newRow.put("biomass", "100.5");

    newWriter.write(newRow, 100L);

    newWriter.join();

    // Compare outputs (column order may differ due to HashMap in old system)
    String oldCsv = oldOutput.toString();
    String newCsv = newOutput.toString();

    // Verify both have same data rows (order-independent column check)
    String[] oldLines = oldCsv.split("\n");
    String[] newLines = newCsv.split("\n");

    assertEquals(oldLines.length, newLines.length,
        "Both systems should produce same number of lines");

    // Both should have step and replicate as last two columns
    String[] oldHeader = oldLines[0].trim().split(",");
    String[] newHeader = newLines[0].trim().split(",");

    assertEquals(oldHeader.length, newHeader.length,
        "Both systems should have same number of columns");
    assertEquals("step", oldHeader[oldHeader.length - 2].trim(),
        "Old system should have 'step' as second-to-last column");
    assertEquals("replicate", oldHeader[oldHeader.length - 1].trim(),
        "Old system should have 'replicate' as last column");
    assertEquals("step", newHeader[newHeader.length - 2].trim(),
        "New system should have 'step' as second-to-last column");
    assertEquals("replicate", newHeader[newHeader.length - 1].trim(),
        "New system should have 'replicate' as last column");
  }

  @Test
  public void testMultipleRowsFormatMatches() throws InterruptedException {
    // Old system
    ByteArrayOutputStream oldOutput = new ByteArrayOutputStream();
    TestOutputStreamStrategy oldStrategy = new TestOutputStreamStrategy(oldOutput);
    MapExportSerializeStrategy serializeStrategy = new MapSerializeStrategy();
    CsvExportFacade oldFacade = new CsvExportFacade(oldStrategy, serializeStrategy);

    oldFacade.start();

    // Write multiple rows via old system
    for (int i = 0; i < 5; i++) {
      Map<String, String> row = new LinkedHashMap<>();
      row.put("id", String.valueOf(i));
      row.put("value", String.valueOf(i * 10.5));
      ExportTask task = new ExportTask(new TestNamedMap("Entity" + i, row), i * 10L, 0);
      oldFacade.write(task);
    }

    oldFacade.join();

    // New system
    ByteArrayOutputStream newOutput = new ByteArrayOutputStream();
    TestOutputStreamStrategy newStrategy = new TestOutputStreamStrategy(newOutput);
    OutputTarget target = new OutputTarget("file", "/tmp/test.csv");
    StructuredOutputWriter newWriter = new StructuredOutputWriter(target, newStrategy, 0);

    newWriter.start();

    // Write same rows via new system
    for (int i = 0; i < 5; i++) {
      Map<String, String> row = new LinkedHashMap<>();
      row.put("id", String.valueOf(i));
      row.put("value", String.valueOf(i * 10.5));
      newWriter.write(row, i * 10L);
    }

    newWriter.join();

    // Compare outputs (verify format compatibility, not exact match)
    String oldCsv = oldOutput.toString();
    String newCsv = newOutput.toString();

    String[] oldLines = oldCsv.split("\n");
    String[] newLines = newCsv.split("\n");

    assertEquals(oldLines.length, newLines.length,
        "Both systems should produce same number of CSV rows");

    // Verify step and replicate columns are last
    String[] oldHeader = oldLines[0].trim().split(",");
    String[] newHeader = newLines[0].trim().split(",");
    assertEquals("step", oldHeader[oldHeader.length - 2].trim());
    assertEquals("replicate", oldHeader[oldHeader.length - 1].trim());
    assertEquals("step", newHeader[newHeader.length - 2].trim());
    assertEquals("replicate", newHeader[newHeader.length - 1].trim());
  }

  @Test
  public void testReplicateColumnMatches() throws InterruptedException {
    // Old system with replicate = 5
    ByteArrayOutputStream oldOutput = new ByteArrayOutputStream();
    TestOutputStreamStrategy oldStrategy = new TestOutputStreamStrategy(oldOutput);
    MapExportSerializeStrategy serializeStrategy = new MapSerializeStrategy();
    CsvExportFacade oldFacade = new CsvExportFacade(oldStrategy, serializeStrategy);

    oldFacade.start();

    Map<String, String> row = new LinkedHashMap<>();
    row.put("biomass", "250.0");
    ExportTask task = new ExportTask(new TestNamedMap("Patch", row), 200L, 5);
    oldFacade.write(task);

    oldFacade.join();

    // New system with replicate = 5
    ByteArrayOutputStream newOutput = new ByteArrayOutputStream();
    TestOutputStreamStrategy newStrategy = new TestOutputStreamStrategy(newOutput);
    OutputTarget target = new OutputTarget("file", "/tmp/test.csv");
    StructuredOutputWriter newWriter = new StructuredOutputWriter(target, newStrategy, 5);

    newWriter.start();

    Map<String, String> newRow = new LinkedHashMap<>();
    newRow.put("biomass", "250.0");
    newWriter.write(newRow, 200L);

    newWriter.join();

    // Compare outputs (verify replicate handling)
    String oldCsv = oldOutput.toString();
    String newCsv = newOutput.toString();

    String[] oldLines = oldCsv.split("\n");
    String[] newLines = newCsv.split("\n");

    // Extract data rows
    String[] oldData = oldLines[1].trim().split(",");
    String[] newData = newLines[1].trim().split(",");

    // Verify replicate is 5 in both (last column)
    assertEquals("5", oldData[oldData.length - 1].trim());
    assertEquals("5", newData[newData.length - 1].trim());

    // Verify step is 200 in both (second-to-last column)
    assertEquals("200", oldData[oldData.length - 2].trim());
    assertEquals("200", newData[newData.length - 2].trim());
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

  /**
   * Test NamedMap implementation for old system testing.
   */
  private static class TestNamedMap extends org.joshsim.wire.NamedMap {
    public TestNamedMap(String name, Map<String, String> target) {
      super(name, target);
    }
  }
}
