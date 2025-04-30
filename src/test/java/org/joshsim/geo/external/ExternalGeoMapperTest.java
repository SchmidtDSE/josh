package org.joshsim.geo.external;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.joshsim.engine.entity.base.GeoKey;
import org.joshsim.engine.entity.base.MutableEntity;
import org.joshsim.engine.geometry.PatchSet;
import org.joshsim.engine.geometry.grid.GridCrsDefinition;
import org.joshsim.engine.value.type.EngineValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for the ExternalGeoMapper class.
 */
@ExtendWith(MockitoExtension.class)
public class ExternalGeoMapperTest {

  @Mock private ExternalCoordinateTransformer mockTransformer;
  @Mock private GeoInterpolationStrategy mockStrategy;
  @Mock private ExternalDataReader mockReader;
  @Mock private ExternalDataReaderFactory mockReaderFactory;
  @Mock private PatchSet mockPatchSet;
  @Mock private MutableEntity mockPatch1;
  @Mock private MutableEntity mockPatch2;
  @Mock private GridCrsDefinition mockGridCrsDefinition;
  @Mock private ExternalSpatialDimensions mockDimensions;
  @Mock private EngineValue mockValue;
  @Mock private GeoKey mockGeoKey1;
  @Mock private GeoKey mockGeoKey2;

  private ExternalGeoMapper mapper;
  private List<String> variableNames;
  private List<MutableEntity> patches;
  private String RIVERSIDE_RESOURCE_PATH;
  private String riversideFilePath;
  private static final String DIM_X = "lon";
  private static final String DIM_Y = "lat";
  private static final String DIM_TIME = "calendar_year";

  @BeforeEach
  public void setUp() throws Exception {
    // Create the mapper with mocked dependencies using the builder
    mapper = new ExternalGeoMapperBuilder()
        .addCoordinateTransformer(mockTransformer)
        .addInterpolationStrategy(mockStrategy)
        .addDimensions(DIM_X, DIM_Y, DIM_TIME) // Adding dummy dimension names for testing
        .build();
        
    variableNames = Arrays.asList("Precipitation_(total)");
    RIVERSIDE_RESOURCE_PATH = "netcdf/precip_riverside_annual_agg.nc";

    // Get resource path
    URL resourceUrl = getClass().getClassLoader().getResource(RIVERSIDE_RESOURCE_PATH);
    if (resourceUrl == null) {
      throw new IOException("Cannot find test resource: " + RIVERSIDE_RESOURCE_PATH);
    }
    riversideFilePath = new File(resourceUrl.getFile()).getAbsolutePath();
    

    patches = Arrays.asList(mockPatch1, mockPatch2);
    
    // Set up common mock behaviors
    doNothing().when(mockReader).open(anyString());
    when(mockPatchSet.getPatches()).thenReturn(patches);
    when(mockPatchSet.getGridCrsDefinition()).thenReturn(mockGridCrsDefinition);
    
    // Set up the reader factory mock to return our mock reader
    mockStaticExternalDataReaderFactory();
  }

  /**
   * Mocks the static ExternalDataReaderFactory to return our mock reader.
   * This is a helper method to handle static method mocking.
   */
  private void mockStaticExternalDataReaderFactory() throws Exception {
    // Since we can't directly mock static methods with standard Mockito,
    // we're setting up our test with the assumption that the actual factory
    // will be used but we'll intercept before the reader is used
    when(mockReader.getVariableNames()).thenReturn(variableNames);
    when(mockReader.getSpatialDimensions()).thenReturn(mockDimensions);
    when(mockReader.getTimeDimensionSize()).thenReturn(Optional.of(3));
    
    // Mock ExternalDataReaderFactory.createReader to return our mock reader
    ExternalDataReaderFactory.setInstance(reader -> mockReader);
  }

  @Test
  public void testMapDataToPatchValues_SingleVariableNetcdf_WithSpecifiedVariables() throws Exception {
    // Set up mock interpolation results
    when(mockStrategy.interpolateValue(
            eq(mockPatch1), anyString(), anyInt(), any(), any(), any(), any()))
        .thenReturn(Optional.of(mockValue));
    when(mockStrategy.interpolateValue(
            eq(mockPatch2), anyString(), anyInt(), any(), any(), any(), any()))
        .thenReturn(Optional.of(mockValue));
    when(mockStrategy.getGeoKey(mockPatch1)).thenReturn(mockGeoKey1);
    when(mockStrategy.getGeoKey(mockPatch2)).thenReturn(mockGeoKey2);

    // Execute the method being tested
    Map<String, Map<Integer, Map<GeoKey, EngineValue>>> result =
        mapper.mapDataToPatchValues(riversideFilePath, variableNames, mockPatchSet, 2);

    // Verify results
    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals(2, result.get("Precipitation_(total)").size());  // 2 time steps
    
    // Verify the reader was used correctly
    verify(mockReader).open(riversideFilePath);
    verify(mockReader).getSpatialDimensions();
    verify(mockReader).getTimeDimensionSize();
    verify(mockReader).close();
    
    // Verify interpolation was called the expected number of times
    // 1 variable × 2 time steps × 2 patches = 4 interpolations
    verify(mockStrategy, times(4)).interpolateValue(
        any(), anyString(), anyInt(), any(), any(), any(), any());
    
    // Verify GeoKey retrieval was called the expected number of times
    verify(mockStrategy, times(4)).getGeoKey(any());
  }

  @Test
  public void testMapDataToPatchValues_SingleVariableNetcdf_WithEmptyVariableList() throws Exception {
    // Set up to return empty list for interpolation (no data found)
    when(mockStrategy.interpolateValue(any(), anyString(), anyInt(), any(), any(), any(), any()))
        .thenReturn(Optional.empty());
    
    // Execute with empty variable list - should get all variables from reader
    Map<String, Map<Integer, Map<GeoKey, EngineValue>>> result =
        mapper.mapDataToPatchValues(riversideFilePath, List.of(), mockPatchSet, 2);

    // Verify results
    assertNotNull(result);
    assertEquals(1, result.size());  // Should have map for one variable
    
    // Verify getVariableNames was called since we provided empty list
    verify(mockReader).getVariableNames();
  }

  @Test
  public void testMapDataToPatchValues_SingleVariableNetcdf_WithNegativeTimeSteps() throws Exception {
    // Set up to return Optional.empty for interpolation
    when(mockStrategy.interpolateValue(any(), anyString(), anyInt(), any(), any(), any(), any()))
        .thenReturn(Optional.empty());
    
    // Execute with -1 for timeSteps (all available)
    Map<String, Map<Integer, Map<GeoKey, EngineValue>>> result =
        mapper.mapDataToPatchValues(riversideFilePath, variableNames, mockPatchSet, -1);

    // Verify results - should process all 3 time steps we mocked
    assertNotNull(result);
    assertEquals(1, result.size());  // Should have map for one variable
    assertEquals(3, result.get("Precipitation_(total)").size());  // 3 time steps
  }

  @Test
  public void testMapDataToPatchValues_SingleVariableNetcdf_HandlesReaderException() throws Exception {
    // Make the reader throw an exception
    doThrow(new IOException("Failed to open file")).when(mockReader).open(anyString());
    
    // Verify the exception is wrapped and thrown
    assertThrows(IOException.class, () -> 
        mapper.mapDataToPatchValues(riversideFilePath, variableNames, mockPatchSet, 2));
  }

  @Test
  public void testMapDataToPatchValues_SingleVariableNetcdf_HandlesInterpolationException() throws Exception {
    // Make the interpolation throw an exception
    when(mockStrategy.interpolateValue(
            eq(mockPatch1), anyString(), anyInt(), any(), any(), any(), any()))
        .thenThrow(new RuntimeException("Interpolation failed"));
    
    // Verify the exception is wrapped
    assertThrows(IOException.class, () -> 
        mapper.mapDataToPatchValues(riversideFilePath, variableNames, mockPatchSet, 1));
  }

  /**
   * Helper class to temporarily override the ExternalDataReaderFactory instance.
   */
  private static class ExternalDataReaderFactory {
    private static java.util.function.Function<String, ExternalDataReader> instance;
    
    static void setInstance(java.util.function.Function<String, ExternalDataReader> factory) {
      instance = factory;
    }
    
    static ExternalDataReader createReader(String path) {
      return instance.apply(path);
    }
  }
}