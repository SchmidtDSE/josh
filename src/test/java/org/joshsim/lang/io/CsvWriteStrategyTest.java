/**
 * Tests for CsvWriteStrategy.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.csv.CSVPrinter;
import org.joshsim.lang.io.strategy.CsvWriteStrategy;
import org.junit.jupiter.api.Test;


/**
 * Test logic to write to a CSV file after serialization.
 */
class CsvWriteStrategyTest {

  @Test
  void shouldWriteSingleRecordToOutputStream() throws IOException {
    // Given
    CsvWriteStrategy strategy = new CsvWriteStrategy();
    Map<String, String> record = new HashMap<>();
    record.put("Name", "Nick");
    record.put("Age", "30");
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    // When
    strategy.write(record, output);
    strategy.flush();

    // Then
    String result = output.toString();
    assertTrue(result.contains("Name"));
    assertTrue(result.contains("Age"));
    assertTrue(result.contains("Nick"));
    assertTrue(result.contains("30"));
  }

  @Test
  void shouldWriteMultipleRecordsWithHeaders() throws IOException {
    // Given
    Map<String, String> record1 = new HashMap<>();
    record1.put("Name", "Nick");
    record1.put("Age", "30");

    Map<String, String> record2 = new HashMap<>();
    record2.put("Name", "Lucia");
    record2.put("Age", "25");

    ByteArrayOutputStream output = new ByteArrayOutputStream();
    CsvWriteStrategy strategy = new CsvWriteStrategy();

    // When
    strategy.write(record1, output);
    strategy.write(record2, output);
    strategy.flush();

    // Then
    String result = output.toString();
    assertTrue(result.contains("Name"));
    assertTrue(result.contains("Age"));
    assertTrue(result.contains("Nick"));
    assertTrue(result.contains("30"));
    assertTrue(result.contains("Lucia"));
    assertTrue(result.contains("25"));
  }

  @Test
  void shouldThrowRuntimeExceptionWhenPrinterFailsToFlush() throws IOException {
    // Given
    CsvWriteStrategy strategy = new CsvWriteStrategy();
    CSVPrinter mockPrinter = mock(CSVPrinter.class);
    doThrow(IOException.class).when(mockPrinter).flush();
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    Map<String, String> record = new HashMap<>();
    record.put("Name", "Test");
    strategy.write(record, output);
    strategy.flush();

    try {
      var printerField = CsvWriteStrategy.class.getDeclaredField("printer");
      printerField.setAccessible(true);
      printerField.set(strategy, mockPrinter);
    } catch (Exception e) {
      fail("Failed to inject mock CSVPrinter");
    }

    // When & Then
    assertThrows(RuntimeException.class, strategy::flush);

  }

  @Test
  void shouldWriteBlankValuesForMissingHeaderColumns() throws IOException {
    // Given
    List<String> headers = Arrays.asList("Name", "Age", "City");
    CsvWriteStrategy strategy = new CsvWriteStrategy(headers);
    Map<String, String> record = new HashMap<>();
    record.put("Name", "John");
    record.put("Age", "25");
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    // When
    strategy.write(record, output);
    strategy.flush();

    // Then
    String result = output.toString();
    assertTrue(result.contains("Name,Age,City"));
    assertTrue(result.contains("John,25,"));
  }

}
