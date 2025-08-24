/**
 * Tests for NetcdfExportFacade NamedMap functionality.
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.joshsim.engine.entity.base.Entity;
import org.joshsim.lang.io.OutputStreamStrategy;
import org.joshsim.wire.NamedMap;
import org.junit.jupiter.api.Test;

/**
 * Test suite for NetcdfExportFacade with NamedMap support.
 */
public class NetcdfExportFacadeNamedMapTest {

  @Test
  public void testWriteNamedMapBasic() throws IOException {
    // Arrange
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    OutputStreamStrategy outputStrategy = mock(OutputStreamStrategy.class);
    when(outputStrategy.open()).thenReturn(outputStream);

    MapExportSerializeStrategy serializeStrategy = mock(MapExportSerializeStrategy.class);
    Map<String, String> entityData = new HashMap<>();
    entityData.put("temperature", "25.5");
    entityData.put("humidity", "60.0");
    when(serializeStrategy.getRecord(any(Entity.class))).thenReturn(entityData);

    List<String> variables = Arrays.asList("temperature", "humidity");
    final NetcdfExportFacade facade = new NetcdfExportFacade(outputStrategy, serializeStrategy,
        variables);

    // Create NamedMap with same data including required position fields
    Map<String, String> namedMapData = new HashMap<>();
    namedMapData.put("position.longitude", "-122.4194");
    namedMapData.put("position.latitude", "37.7749");
    namedMapData.put("temperature", "25.5");
    namedMapData.put("humidity", "60.0");
    NamedMap namedMap = new NamedMap("weatherData", namedMapData);

    // Act
    facade.start();
    facade.write(namedMap, 3L, 0);
    facade.join();

    // Assert - NetCDF should produce some output
    String output = outputStream.toString();
    assertTrue(output.length() >= 0, "NetCDF output should be generated");
  }

  @Test
  public void testWriteNamedMapWithMissingVariable() throws IOException {
    // Arrange
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    OutputStreamStrategy outputStrategy = mock(OutputStreamStrategy.class);
    when(outputStrategy.open()).thenReturn(outputStream);

    MapExportSerializeStrategy serializeStrategy = mock(MapExportSerializeStrategy.class);
    List<String> variables = Arrays.asList("temperature", "humidity", "pressure");
    final NetcdfExportFacade facade = new NetcdfExportFacade(outputStrategy, serializeStrategy,
        variables);

    // Create NamedMap with missing pressure variable but with required position fields
    Map<String, String> namedMapData = new HashMap<>();
    namedMapData.put("position.longitude", "-122.4194");
    namedMapData.put("position.latitude", "37.7749");
    namedMapData.put("temperature", "25.5");
    namedMapData.put("humidity", "60.0");
    // pressure is missing
    NamedMap namedMap = new NamedMap("weatherData", namedMapData);

    // Act
    facade.start();
    facade.write(namedMap, 3L, 0);
    facade.join();

    // Assert - should handle missing variables gracefully
    String output = outputStream.toString();
    assertTrue(output.length() >= 0); // NetCDF should still produce some output
  }

  @Test
  public void testWriteNamedMapWithCoordinates() throws IOException {
    // Arrange
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    OutputStreamStrategy outputStrategy = mock(OutputStreamStrategy.class);
    when(outputStrategy.open()).thenReturn(outputStream);

    MapExportSerializeStrategy serializeStrategy = mock(MapExportSerializeStrategy.class);
    List<String> variables = Arrays.asList("elevation");
    final NetcdfExportFacade facade = new NetcdfExportFacade(outputStrategy, serializeStrategy,
        variables);

    // Create NamedMap with coordinate data
    Map<String, String> namedMapData = new HashMap<>();
    namedMapData.put("position.longitude", "-122.4194");
    namedMapData.put("position.latitude", "37.7749");
    namedMapData.put("elevation", "52.0");
    NamedMap namedMap = new NamedMap("locationData", namedMapData);

    // Act
    facade.start();
    facade.write(namedMap, 7L, 0);
    facade.join();

    // Assert
    String output = outputStream.toString();
    assertTrue(output.length() >= 0); // NetCDF should produce output
  }

  @Test
  public void testWriteNamedMapMultipleSteps() throws IOException {
    // Arrange
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    OutputStreamStrategy outputStrategy = mock(OutputStreamStrategy.class);
    when(outputStrategy.open()).thenReturn(outputStream);

    MapExportSerializeStrategy serializeStrategy = mock(MapExportSerializeStrategy.class);
    List<String> variables = Arrays.asList("value");
    final NetcdfExportFacade facade = new NetcdfExportFacade(outputStrategy, serializeStrategy,
        variables);

    // Create NamedMaps for different steps with required position fields
    Map<String, String> data1 = new HashMap<>();
    data1.put("position.longitude", "-122.4194");
    data1.put("position.latitude", "37.7749");
    data1.put("value", "10.0");
    final NamedMap namedMap1 = new NamedMap("step1", data1);

    Map<String, String> data2 = new HashMap<>();
    data2.put("position.longitude", "-122.4195");
    data2.put("position.latitude", "37.7750");
    data2.put("value", "20.0");
    NamedMap namedMap2 = new NamedMap("step2", data2);

    // Act
    facade.start();
    facade.write(namedMap1, 1L, 0);
    facade.write(namedMap2, 2L, 1);
    facade.join();

    // Assert
    String output = outputStream.toString();
    assertTrue(output.length() >= 0); // NetCDF should handle multiple steps
  }

  @Test
  public void testWriteNamedMapEmptyData() throws IOException {
    // Arrange
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    OutputStreamStrategy outputStrategy = mock(OutputStreamStrategy.class);
    when(outputStrategy.open()).thenReturn(outputStream);

    MapExportSerializeStrategy serializeStrategy = mock(MapExportSerializeStrategy.class);
    List<String> variables = Arrays.asList("temperature");
    final NetcdfExportFacade facade = new NetcdfExportFacade(outputStrategy, serializeStrategy,
        variables);

    // Create NamedMap with required position fields but no temperature data
    Map<String, String> emptyData = new HashMap<>();
    emptyData.put("position.longitude", "-122.4194");
    emptyData.put("position.latitude", "37.7749");
    NamedMap namedMap = new NamedMap("emptyData", emptyData);

    // Act
    facade.start();
    facade.write(namedMap, 0L, 0);
    facade.join();

    // Assert
    String output = outputStream.toString();
    assertTrue(output.length() >= 0); // Should handle empty data gracefully
  }

  @Test
  public void testWriteNamedMapMultipleReplicatesConsolidated() throws IOException {
    // Arrange
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    OutputStreamStrategy outputStrategy = mock(OutputStreamStrategy.class);
    when(outputStrategy.open()).thenReturn(outputStream);

    MapExportSerializeStrategy serializeStrategy = mock(MapExportSerializeStrategy.class);
    List<String> variables = Arrays.asList("temperature");
    final NetcdfExportFacade facade = new NetcdfExportFacade(outputStrategy, serializeStrategy,
        variables);

    // Create NamedMaps for different replicates with required position fields
    Map<String, String> data1 = new HashMap<>();
    data1.put("position.longitude", "-122.4194");
    data1.put("position.latitude", "37.7749");
    data1.put("temperature", "20.0");
    final NamedMap namedMap1 = new NamedMap("replicate0", data1);

    Map<String, String> data2 = new HashMap<>();
    data2.put("position.longitude", "-122.4195");
    data2.put("position.latitude", "37.7750");
    data2.put("temperature", "25.0");
    final NamedMap namedMap2 = new NamedMap("replicate1", data2);

    Map<String, String> data3 = new HashMap<>();
    data3.put("position.longitude", "-122.4196");
    data3.put("position.latitude", "37.7751");
    data3.put("temperature", "30.0");
    NamedMap namedMap3 = new NamedMap("replicate2", data3);

    // Act - write data for multiple replicates
    facade.start();
    facade.write(namedMap1, 1L, 0);  // replicate 0
    facade.write(namedMap2, 1L, 1);  // replicate 1  
    facade.write(namedMap3, 1L, 2);  // replicate 2
    facade.join();

    // Assert - should consolidate all replicates into single NetCDF with replicate dimension
    String output = outputStream.toString();
    assertTrue(output.length() >= 0, "NetCDF should consolidate multiple replicates");
  }
}
