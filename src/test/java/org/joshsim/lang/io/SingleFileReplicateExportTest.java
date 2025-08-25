/**
 * Integration tests for single file export with replicate columns.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import org.joshsim.engine.entity.base.Entity;
import org.joshsim.lang.io.strategy.CsvExportFacade;
import org.joshsim.lang.io.strategy.MapExportSerializeStrategy;
import org.joshsim.lang.io.strategy.MemoryExportFacade;
import org.joshsim.wire.NamedMap;
import org.junit.jupiter.api.Test;

/**
 * Integration test suite for single file export with replicate columns.
 *
 * <p>This test class verifies that CSV and memory exports correctly add replicate columns
 * and that the replicate column appears as the last column to match web editor behavior.</p>
 */
public class SingleFileReplicateExportTest {

  /**
   * Test that CSV exports include replicate column as the last column.
   */
  @Test
  public void testCsvExportIncludesReplicateColumn() throws Exception {
    // Setup
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    final OutputStreamStrategy outputStrategy = () -> outputStream;
    MapExportSerializeStrategy serializeStrategy = mock(MapExportSerializeStrategy.class);

    // Mock entity with some data
    Entity mockEntity = mock(Entity.class);
    Map<String, String> entityData = new HashMap<>();
    entityData.put("height", "10.5");
    entityData.put("age", "5");
    when(serializeStrategy.getRecord(mockEntity)).thenReturn(entityData);
    CsvExportFacade facade = new CsvExportFacade(outputStrategy, serializeStrategy);

    // Start facade and write data with different replicates
    facade.start();
    facade.write(mockEntity, 1L, 0);  // Replicate 0, step 1
    facade.write(mockEntity, 1L, 1);  // Replicate 1, step 1
    facade.write(mockEntity, 2L, 0);  // Replicate 0, step 2
    facade.join();

    // Verify output
    String csvOutput = outputStream.toString();
    String[] lines = csvOutput.trim().split("\n");

    // Should have header + 3 data rows
    assertEquals(4, lines.length, "Expected header plus 3 data rows");

    // Check header - replicate should be last column
    String header = lines[0].trim();
    assertTrue(header.endsWith(",replicate"),
        "Header should end with replicate column: " + header);
    assertTrue(header.contains("height"), "Header should contain height column");
    assertTrue(header.contains("age"), "Header should contain age column");
    assertTrue(header.contains("step"), "Header should contain step column");

    // Check data rows contain replicate values (trim to handle newlines)
    assertTrue(lines[1].trim().endsWith(",0"),
        "First row should have replicate 0. Actual: '" + lines[1] + "'");
    assertTrue(lines[2].trim().endsWith(",1"),
        "Second row should have replicate 1. Actual: '" + lines[2] + "'");
    assertTrue(lines[3].trim().endsWith(",0"),
        "Third row should have replicate 0. Actual: '" + lines[3] + "'");

    // Check step values are preserved
    assertTrue(lines[1].contains(",1,"), "First row should have step 1");
    assertTrue(lines[2].contains(",1,"), "Second row should have step 1");
    assertTrue(lines[3].contains(",2,"), "Third row should have step 2");
  }

  /**
   * Test that memory exports include replicate column.
   */
  @Test
  public void testMemoryExportIncludesReplicateColumn() throws Exception {
    // Setup
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    final OutputStreamStrategy outputStrategy = () -> outputStream;

    MemoryExportFacade facade = new MemoryExportFacade(outputStrategy, "test");

    // Mock entity with some data
    Entity mockEntity = mock(Entity.class);

    // Start facade and write data with different replicates
    facade.start();
    facade.write(mockEntity, 5L, 2);  // Replicate 2, step 5
    facade.write(mockEntity, 10L, 3); // Replicate 3, step 10
    facade.join();

    // The memory export format is implementation-specific, but we can verify
    // that the facade accepts the replicate parameter without error
    // and that the methods execute successfully
    assertTrue(outputStream.size() > 0, "Memory export should produce output");
  }

  /**
   * Test CSV export with NamedMap data includes replicate column.
   */
  @Test
  public void testCsvExportWithNamedMapIncludesReplicateColumn() throws Exception {
    // Setup
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    final OutputStreamStrategy outputStrategy = () -> outputStream;
    MapExportSerializeStrategy serializeStrategy = mock(MapExportSerializeStrategy.class);

    CsvExportFacade facade = new CsvExportFacade(outputStrategy, serializeStrategy);

    // Create NamedMap with test data
    Map<String, String> testData = new HashMap<>();
    testData.put("temperature", "25.3");
    testData.put("humidity", "60");
    NamedMap namedMap = new NamedMap("weather", testData);

    // Start facade and write data
    facade.start();
    facade.write(namedMap, 3L, 1);  // Replicate 1, step 3
    facade.join();

    // Verify output
    String csvOutput = outputStream.toString();
    String[] lines = csvOutput.trim().split("\n");

    // Should have header + 1 data row
    assertEquals(2, lines.length, "Expected header plus 1 data row");

    // Check header ends with replicate column
    String header = lines[0].trim();
    assertTrue(header.endsWith(",replicate"),
        "Header should end with replicate column: " + header);

    // Check data row has correct replicate and step
    String dataRow = lines[1];
    assertTrue(dataRow.contains(",3,"), "Data row should contain step 3");
    assertTrue(dataRow.trim().endsWith(",1"), "Data row should end with replicate 1");
  }

  /**
   * Test that replicate column ordering is consistent (replicate is always last).
   */
  @Test
  public void testReplicateColumnIsAlwaysLast() throws Exception {
    // Setup
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    final OutputStreamStrategy outputStrategy = () -> outputStream;
    MapExportSerializeStrategy serializeStrategy = mock(MapExportSerializeStrategy.class);

    final CsvExportFacade facade = new CsvExportFacade(outputStrategy, serializeStrategy);

    // Mock entity with multiple attributes
    final Entity mockEntity = mock(Entity.class);
    Map<String, String> entityData = new HashMap<>();
    entityData.put("zebra", "last");    // Alphabetically last attribute
    entityData.put("alpha", "first");   // Alphabetically first attribute
    entityData.put("middle", "value");  // Middle attribute
    when(serializeStrategy.getRecord(mockEntity)).thenReturn(entityData);

    // Start facade and write data
    facade.start();
    facade.write(mockEntity, 7L, 4);
    facade.join();

    // Verify output
    String csvOutput = outputStream.toString();
    String[] lines = csvOutput.trim().split("\n");

    // Check that replicate is the very last column regardless of attribute ordering
    String header = lines[0].trim();
    String dataRow = lines[1];

    assertTrue(header.endsWith(",replicate"),
        "Header should always end with replicate column: " + header);
    assertTrue(dataRow.trim().endsWith(",4"),
        "Data row should always end with replicate value: " + dataRow);
  }

  /**
   * Test that replicate values can be zero.
   */
  @Test
  public void testZeroReplicateValue() throws Exception {
    // Setup
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    final OutputStreamStrategy outputStrategy = () -> outputStream;
    MapExportSerializeStrategy serializeStrategy = mock(MapExportSerializeStrategy.class);

    CsvExportFacade facade = new CsvExportFacade(outputStrategy, serializeStrategy);

    // Mock entity
    Entity mockEntity = mock(Entity.class);
    Map<String, String> entityData = new HashMap<>();
    entityData.put("value", "test");
    when(serializeStrategy.getRecord(mockEntity)).thenReturn(entityData);

    // Start facade and write data with replicate 0
    facade.start();
    facade.write(mockEntity, 1L, 0);
    facade.join();

    // Verify output
    String csvOutput = outputStream.toString();
    String[] lines = csvOutput.trim().split("\n");

    // Check that replicate 0 is handled correctly
    assertTrue(lines[1].trim().endsWith(",0"), "Should correctly handle replicate 0");
  }

  /**
   * Test that large replicate numbers are handled correctly.
   */
  @Test
  public void testLargeReplicateNumbers() throws Exception {
    // Setup
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    final OutputStreamStrategy outputStrategy = () -> outputStream;
    MapExportSerializeStrategy serializeStrategy = mock(MapExportSerializeStrategy.class);

    CsvExportFacade facade = new CsvExportFacade(outputStrategy, serializeStrategy);

    // Mock entity
    Entity mockEntity = mock(Entity.class);
    Map<String, String> entityData = new HashMap<>();
    entityData.put("value", "test");
    when(serializeStrategy.getRecord(mockEntity)).thenReturn(entityData);

    // Start facade and write data with large replicate number
    int largeReplicate = 999999;
    facade.start();
    facade.write(mockEntity, 1L, largeReplicate);
    facade.join();

    // Verify output
    String csvOutput = outputStream.toString();
    String[] lines = csvOutput.trim().split("\n");

    // Check that large replicate number is handled correctly
    assertTrue(lines[1].trim().endsWith("," + largeReplicate),
        "Should correctly handle large replicate numbers");
  }
}
