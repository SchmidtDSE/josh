/**
 * Tests for CsvExportFacade NamedMap functionality.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io.strategy;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.joshsim.engine.entity.base.Entity;
import org.joshsim.lang.io.NamedMap;
import org.joshsim.lang.io.OutputStreamStrategy;
import org.junit.jupiter.api.Test;

/**
 * Test suite for CsvExportFacade with NamedMap support.
 */
public class CsvExportFacadeNamedMapTest {

  @Test
  public void testWriteNamedMapBasic() throws IOException {
    // Arrange
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    OutputStreamStrategy outputStrategy = mock(OutputStreamStrategy.class);
    when(outputStrategy.open()).thenReturn(outputStream);

    MapExportSerializeStrategy serializeStrategy = mock(MapExportSerializeStrategy.class);
    Map<String, String> entityData = new HashMap<>();
    entityData.put("attribute1", "value1");
    entityData.put("attribute2", "value2");
    when(serializeStrategy.getRecord(any(Entity.class))).thenReturn(entityData);

    CsvExportFacade facade = new CsvExportFacade(outputStrategy, serializeStrategy);

    // Create NamedMap with same data
    Map<String, String> namedMapData = new HashMap<>();
    namedMapData.put("attribute1", "value1");
    namedMapData.put("attribute2", "value2");
    NamedMap namedMap = new NamedMap("testEntity", namedMapData);

    // Act
    facade.start();
    facade.write(namedMap, 5L);
    facade.join();

    // Assert
    String output = outputStream.toString();
    // CSV export should produce some output with our data
    assertTrue(output.length() > 0, "CSV output should not be empty");
    assertTrue(output.contains("value1") || output.contains("5"), "Output should contain our data");
  }

  @Test
  public void testWriteNamedMapWithHeaders() throws IOException {
    // Arrange
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    OutputStreamStrategy outputStrategy = mock(OutputStreamStrategy.class);
    when(outputStrategy.open()).thenReturn(outputStream);

    MapExportSerializeStrategy serializeStrategy = mock(MapExportSerializeStrategy.class);

    String[] headers = {"attribute1", "attribute2", "step"};
    CsvExportFacade facade = new CsvExportFacade(
        outputStrategy, 
        serializeStrategy, 
        java.util.Arrays.asList(headers)
    );

    // Create NamedMap
    Map<String, String> namedMapData = new HashMap<>();
    namedMapData.put("attribute1", "testValue1");
    namedMapData.put("attribute2", "testValue2");
    NamedMap namedMap = new NamedMap("testEntity", namedMapData);

    // Act
    facade.start();
    facade.write(namedMap, 10L);
    facade.join();

    // Assert
    String output = outputStream.toString();
    assertTrue(output.length() > 0, "CSV output should not be empty");
    assertTrue(output.contains("testValue1") || output.contains("10"), "Output should contain our data");
  }

  @Test
  public void testWriteNamedMapEmptyData() throws IOException {
    // Arrange
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    OutputStreamStrategy outputStrategy = mock(OutputStreamStrategy.class);
    when(outputStrategy.open()).thenReturn(outputStream);

    MapExportSerializeStrategy serializeStrategy = mock(MapExportSerializeStrategy.class);
    CsvExportFacade facade = new CsvExportFacade(outputStrategy, serializeStrategy);

    // Create empty NamedMap
    Map<String, String> emptyData = new HashMap<>();
    NamedMap namedMap = new NamedMap("emptyEntity", emptyData);

    // Act
    facade.start();
    facade.write(namedMap, 0L);
    facade.join();

    // Assert
    String output = outputStream.toString();
    assertTrue(output.length() > 0, "CSV output should not be empty");
  }

  @Test
  public void testWriteNamedMapMultipleRecords() throws IOException {
    // Arrange
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    OutputStreamStrategy outputStrategy = mock(OutputStreamStrategy.class);
    when(outputStrategy.open()).thenReturn(outputStream);

    MapExportSerializeStrategy serializeStrategy = mock(MapExportSerializeStrategy.class);
    CsvExportFacade facade = new CsvExportFacade(outputStrategy, serializeStrategy);

    // Create multiple NamedMaps
    Map<String, String> data1 = new HashMap<>();
    data1.put("name", "entity1");
    data1.put("value", "100");
    NamedMap namedMap1 = new NamedMap("entity1", data1);

    Map<String, String> data2 = new HashMap<>();
    data2.put("name", "entity2");
    data2.put("value", "200");
    NamedMap namedMap2 = new NamedMap("entity2", data2);

    // Act
    facade.start();
    facade.write(namedMap1, 1L);
    facade.write(namedMap2, 2L);
    facade.join();

    // Assert
    String output = outputStream.toString();
    assertTrue(output.length() > 0, "CSV output should not be empty");
    assertTrue(output.contains("entity1") || output.contains("100"), "Output should contain first entity data");
    assertTrue(output.contains("entity2") || output.contains("200"), "Output should contain second entity data");
  }
}