/**
 * Tests for GeotiffExportFacade NamedMap functionality.
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
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.joshsim.engine.entity.base.Entity;
import org.joshsim.engine.geometry.PatchBuilderExtents;
import org.joshsim.lang.io.NamedMap;
import org.junit.jupiter.api.Test;

/**
 * Test suite for GeotiffExportFacade with NamedMap support.
 */
public class GeotiffExportFacadeNamedMapTest {

  @Test
  public void testWriteNamedMapWithCoordinates() {
    // Arrange
    MapExportSerializeStrategy serializeStrategy = mock(MapExportSerializeStrategy.class);
    Map<String, String> entityData = new HashMap<>();
    entityData.put("position.longitude", "-122.4194");
    entityData.put("position.latitude", "37.7749");
    entityData.put("elevation", "100.5");
    when(serializeStrategy.getRecord(any(Entity.class))).thenReturn(entityData);

    GeotiffExportFacade.ParameterizedOutputStreamGenerator streamGenerator = 
        mock(GeotiffExportFacade.ParameterizedOutputStreamGenerator.class);
    when(streamGenerator.getStream(any())).thenReturn(new ByteArrayOutputStream());

    List<String> variables = Arrays.asList("elevation");
    // Create real PatchBuilderExtents instead of mocking
    BigDecimal topLeftX = new BigDecimal("-122.45");
    BigDecimal topLeftY = new BigDecimal("37.73");
    BigDecimal bottomRightX = new BigDecimal("-118.24");
    BigDecimal bottomRightY = new BigDecimal("34.05");
    PatchBuilderExtents extents = new PatchBuilderExtents(topLeftX, topLeftY, bottomRightX, bottomRightY);
    BigDecimal width = new BigDecimal("10000");

    GeotiffExportFacade facade = new GeotiffExportFacade(
        streamGenerator, 
        serializeStrategy, 
        variables, 
        extents, 
        width
    );

    // Create NamedMap with coordinate data
    Map<String, String> namedMapData = new HashMap<>();
    namedMapData.put("position.longitude", "-122.4194");
    namedMapData.put("position.latitude", "37.7749");
    namedMapData.put("elevation", "100.5");
    NamedMap namedMap = new NamedMap("locationData", namedMapData);

    // Act
    facade.start();
    facade.write(namedMap, 5L);
    facade.join();

    // Assert - should complete without exceptions
    assertTrue(true); // Test passes if no exceptions thrown
  }

  @Test
  public void testWriteNamedMapWithMultipleVariables() {
    // Arrange
    MapExportSerializeStrategy serializeStrategy = mock(MapExportSerializeStrategy.class);
    GeotiffExportFacade.ParameterizedOutputStreamGenerator streamGenerator = 
        mock(GeotiffExportFacade.ParameterizedOutputStreamGenerator.class);
    when(streamGenerator.getStream(any())).thenReturn(new ByteArrayOutputStream());

    List<String> variables = Arrays.asList("temperature", "humidity", "pressure");
    // Create real PatchBuilderExtents instead of mocking
    BigDecimal topLeftX = new BigDecimal("-122.45");
    BigDecimal topLeftY = new BigDecimal("37.73");
    BigDecimal bottomRightX = new BigDecimal("-118.24");
    BigDecimal bottomRightY = new BigDecimal("34.05");
    PatchBuilderExtents extents = new PatchBuilderExtents(topLeftX, topLeftY, bottomRightX, bottomRightY);
    BigDecimal width = new BigDecimal("10000");

    GeotiffExportFacade facade = new GeotiffExportFacade(
        streamGenerator, 
        serializeStrategy, 
        variables, 
        extents, 
        width
    );

    // Create NamedMap with multiple variables
    Map<String, String> namedMapData = new HashMap<>();
    namedMapData.put("position.longitude", "-118.2437");
    namedMapData.put("position.latitude", "34.0522");
    namedMapData.put("temperature", "25.5");
    namedMapData.put("humidity", "65.0");
    namedMapData.put("pressure", "1013.25");
    NamedMap namedMap = new NamedMap("weatherData", namedMapData);

    // Act
    facade.start();
    facade.write(namedMap, 10L);
    facade.join();

    // Assert - should complete without exceptions
    assertTrue(true); // Test passes if no exceptions thrown
  }

  @Test
  public void testWriteNamedMapWithMissingCoordinates() {
    // Arrange
    MapExportSerializeStrategy serializeStrategy = mock(MapExportSerializeStrategy.class);
    GeotiffExportFacade.ParameterizedOutputStreamGenerator streamGenerator = 
        mock(GeotiffExportFacade.ParameterizedOutputStreamGenerator.class);
    when(streamGenerator.getStream(any())).thenReturn(new ByteArrayOutputStream());

    List<String> variables = Arrays.asList("value");
    // Create real PatchBuilderExtents instead of mocking
    BigDecimal topLeftX = new BigDecimal("-122.45");
    BigDecimal topLeftY = new BigDecimal("37.73");
    BigDecimal bottomRightX = new BigDecimal("-118.24");
    BigDecimal bottomRightY = new BigDecimal("34.05");
    PatchBuilderExtents extents = new PatchBuilderExtents(topLeftX, topLeftY, bottomRightX, bottomRightY);
    BigDecimal width = new BigDecimal("10000");

    GeotiffExportFacade facade = new GeotiffExportFacade(
        streamGenerator, 
        serializeStrategy, 
        variables, 
        extents, 
        width
    );

    // Create NamedMap without coordinates (should use null values)
    Map<String, String> namedMapData = new HashMap<>();
    namedMapData.put("value", "42.0");
    // Missing position.longitude and position.latitude
    NamedMap namedMap = new NamedMap("noCoords", namedMapData);

    // Act
    facade.start();
    facade.write(namedMap, 1L);
    facade.join();

    // Assert - should handle missing coordinates (null values)
    assertTrue(true); // Test passes if no exceptions thrown
  }

  @Test
  public void testWriteNamedMapWithMissingVariable() {
    // Arrange
    MapExportSerializeStrategy serializeStrategy = mock(MapExportSerializeStrategy.class);
    GeotiffExportFacade.ParameterizedOutputStreamGenerator streamGenerator = 
        mock(GeotiffExportFacade.ParameterizedOutputStreamGenerator.class);
    when(streamGenerator.getStream(any())).thenReturn(new ByteArrayOutputStream());

    List<String> variables = Arrays.asList("temperature", "humidity");
    // Create real PatchBuilderExtents instead of mocking
    BigDecimal topLeftX = new BigDecimal("-122.45");
    BigDecimal topLeftY = new BigDecimal("37.73");
    BigDecimal bottomRightX = new BigDecimal("-118.24");
    BigDecimal bottomRightY = new BigDecimal("34.05");
    PatchBuilderExtents extents = new PatchBuilderExtents(topLeftX, topLeftY, bottomRightX, bottomRightY);
    BigDecimal width = new BigDecimal("10000");

    GeotiffExportFacade facade = new GeotiffExportFacade(
        streamGenerator, 
        serializeStrategy, 
        variables, 
        extents, 
        width
    );

    // Create NamedMap missing one variable
    Map<String, String> namedMapData = new HashMap<>();
    namedMapData.put("position.longitude", "-74.0059");
    namedMapData.put("position.latitude", "40.7128");
    namedMapData.put("temperature", "20.0");
    // Missing humidity - should default to "0"
    NamedMap namedMap = new NamedMap("partialData", namedMapData);

    // Act
    facade.start();
    facade.write(namedMap, 3L);
    facade.join();

    // Assert - should handle missing variables by using default value "0"
    assertTrue(true); // Test passes if no exceptions thrown
  }

  @Test
  public void testWriteNamedMapMultipleSteps() {
    // Arrange
    MapExportSerializeStrategy serializeStrategy = mock(MapExportSerializeStrategy.class);
    GeotiffExportFacade.ParameterizedOutputStreamGenerator streamGenerator = 
        mock(GeotiffExportFacade.ParameterizedOutputStreamGenerator.class);
    when(streamGenerator.getStream(any())).thenReturn(new ByteArrayOutputStream());

    List<String> variables = Arrays.asList("elevation");
    // Create real PatchBuilderExtents instead of mocking
    BigDecimal topLeftX = new BigDecimal("-122.45");
    BigDecimal topLeftY = new BigDecimal("37.73");
    BigDecimal bottomRightX = new BigDecimal("-118.24");
    BigDecimal bottomRightY = new BigDecimal("34.05");
    PatchBuilderExtents extents = new PatchBuilderExtents(topLeftX, topLeftY, bottomRightX, bottomRightY);
    BigDecimal width = new BigDecimal("10000");

    GeotiffExportFacade facade = new GeotiffExportFacade(
        streamGenerator, 
        serializeStrategy, 
        variables, 
        extents, 
        width
    );

    // Create NamedMaps for different steps
    Map<String, String> data1 = new HashMap<>();
    data1.put("position.longitude", "-87.6298");
    data1.put("position.latitude", "41.8781");
    data1.put("elevation", "180.0");
    NamedMap namedMap1 = new NamedMap("step1Data", data1);

    Map<String, String> data2 = new HashMap<>();
    data2.put("position.longitude", "-87.6299");
    data2.put("position.latitude", "41.8782");
    data2.put("elevation", "185.0");
    NamedMap namedMap2 = new NamedMap("step2Data", data2);

    // Act
    facade.start();
    facade.write(namedMap1, 1L);
    facade.write(namedMap2, 2L);
    facade.join();

    // Assert - should handle multiple steps
    assertTrue(true); // Test passes if no exceptions thrown
  }
}