
/**
 * Tests for MemoryWriteStrategy.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.joshsim.lang.io.strategy.MemoryWriteStrategy;
import org.junit.jupiter.api.Test;


/**
 * Test suite for the MemoryWriteStrategy class.
 */
class MemoryWriteStrategyTest {

  @Test
  void shouldWriteSingleRecordToOutputStream() throws IOException {
    // Given
    MemoryWriteStrategy strategy = new MemoryWriteStrategy("target");
    Map<String, String> record = new HashMap<>();
    record.put("type", "Forever");
    record.put("age", "30");
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    // When
    strategy.write(record, output);
    strategy.flush();

    // Then
    String result = output.toString();
    assertTrue(result.contains("target:"));
    assertTrue(result.contains("type=Forever"));
    assertTrue(result.contains("age=30"));
  }

  @Test
  void shouldHandleEmptyRecord() throws IOException {
    // Given
    MemoryWriteStrategy strategy = new MemoryWriteStrategy("target");
    Map<String, String> record = new HashMap<>();
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    // When
    strategy.write(record, output);
    strategy.flush();

    // Then
    String result = output.toString();
    assertTrue(result.contains("target:"));
  }

  @Test
  void shouldEscapeTabsAndNewlines() throws IOException {
    // Given
    MemoryWriteStrategy strategy = new MemoryWriteStrategy("target");
    Map<String, String> record = new HashMap<>();
    record.put("description", "Line 1\nLine 2\tTabbed");
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    // When
    strategy.write(record, output);
    strategy.flush();

    // Then
    String result = output.toString();
    assertTrue(result.contains("target:"));
    assertTrue(result.contains("description=Line 1    Line 2    Tabbed"));
  }

  @Test
  void shouldHandleMultipleRecords() throws IOException {
    // Given
    MemoryWriteStrategy strategy = new MemoryWriteStrategy("target");
    Map<String, String> record1 = new HashMap<>();
    record1.put("type", "Type1");
    Map<String, String> record2 = new HashMap<>();
    record2.put("type", "Type2");
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    // When
    strategy.write(record1, output);
    strategy.write(record2, output);
    strategy.flush();

    // Then
    String result = output.toString();
    assertTrue(result.contains("target:"));
    assertTrue(result.contains("type=Type1"));
    assertTrue(result.contains("type=Type2"));
  }
}
