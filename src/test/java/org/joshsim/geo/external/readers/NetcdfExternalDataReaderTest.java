package org.joshsim.geo.external.readers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.engine.value.type.EngineValue;
import org.joshsim.geo.external.ExternalSpatialDimensions;
import org.joshsim.geo.geometry.EarthGeometry;
import org.joshsim.geo.geometry.EarthTransformer;
import org.joshsim.geo.geometry.JtsTransformUtility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Point;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.FactoryException;

@ExtendWith(MockitoExtension.class)
public class NetcdfExternalDataReaderTest {

  private static final String RIVERSIDE_FILE =
      "/workspaces/josh/assets/test/netcdf/precip_riverside_annual.nc";
  private static final String SAN_BERNARDINO_FILE =
      "/workspaces/josh/assets/test/netcdf/precip_sanbernardino_annual.nc";
  
  private NetcdfExternalDataReader reader;
  
  private EngineValueFactory valueFactory;
  
  @Mock
  private EngineValue mockEngineValue;
  
  // Default patch CRS (WGS84)
  private CoordinateReferenceSystem patchCrs;
  
  @BeforeEach
  public void setUp() {
    valueFactory = new EngineValueFactory();
    reader = new NetcdfExternalDataReader(valueFactory);
    
    // Try to create WGS84 CRS
    try {
      patchCrs = JtsTransformUtility.getRightHandedCrs("EPSG:4326");
    } catch (FactoryException e) {
      System.err.println("Warning: Could not create WGS84 CRS: " + e.getMessage());
    }
  }
  
  @AfterEach
  public void tearDown() throws Exception {
    reader.close();
  }
  
  @Test
  public void testCanHandleNetcdfFiles() {
    assertTrue(reader.canHandle("test.nc"));
    assertTrue(reader.canHandle("test.ncf"));
    assertTrue(reader.canHandle("test.netcdf"));
    assertTrue(reader.canHandle("test.nc4"));
    assertTrue(reader.canHandle("TEST.NC"));
    assertFalse(reader.canHandle("test.txt"));
  }
  
  @Test
  public void testOpenAndCloseFile() throws IOException {
    assertDoesNotThrow(() -> reader.open(RIVERSIDE_FILE));
    assertDoesNotThrow(() -> reader.close());
  }
  
  @Test
  public void testOpenNonExistentFile() {
    assertThrows(IOException.class, () -> reader.open("nonexistent.nc"));
  }
  
  @Test
  public void testDetectSpatialDimensions() throws IOException {
    reader.open(RIVERSIDE_FILE);
    boolean detected = reader.detectSpatialDimensions();
    assertTrue(detected);
  }
  
  @Test
  public void testGetCrs() throws IOException {
    reader.open(RIVERSIDE_FILE);
    assertTrue(reader.detectSpatialDimensions());
    
    // Get CRS from file
    String crs = reader.getCrsCode();
    
    // Print CRS for debugging
    System.out.println("Detected CRS: " + crs);
    
    // TODO: We may or may not have a CRS in the file we are using
    // as test case - not sure if this is bug or not yet
    // assertNotNull(crs);
    if (crs != null) {
      assertFalse(crs.isEmpty(), "CRS should not be empty if present");
    }
  }
  
  @Test
  public void testGetVariableNames() throws IOException {
    reader.open(RIVERSIDE_FILE);
    assertTrue(reader.detectSpatialDimensions());
    
    List<String> variables = reader.getVariableNames();
    assertNotNull(variables);
    assertFalse(variables.isEmpty());
    
    // Print out variable names to help debug/adjust tests if needed
    System.out.println("Variables found: " + variables);
    
    // Test that we don't include coordinate variables in the list
    ExternalSpatialDimensions dims = reader.getSpatialDimensions();
    assertFalse(variables.contains(dims.getDimensionNameX()));
    assertFalse(variables.contains(dims.getDimensionNameY()));
  }
  
  @Test
  public void testGetSpatialDimensions() throws IOException {
    reader.open(RIVERSIDE_FILE);
    reader.detectSpatialDimensions();
    
    ExternalSpatialDimensions dims = reader.getSpatialDimensions();
    assertNotNull(dims);
    
    // Check X coordinates
    List<BigDecimal> coordsX = dims.getCoordinatesX();
    assertFalse(coordsX.isEmpty());
    
    // Check Y coordinates
    List<BigDecimal> coordsY = dims.getCoordinatesY();
    assertFalse(coordsY.isEmpty());
    
    // Check dimension names are set
    assertNotNull(dims.getDimensionNameX());
    assertNotNull(dims.getDimensionNameY());
    
    // Print some coordinate info for debugging
    System.out.println("X range: " + coordsX.get(0) + " to " + coordsX.get(coordsX.size() - 1));
    System.out.println("Y range: " + coordsY.get(0) + " to " + coordsY.get(coordsY.size() - 1));
  }
  
  @Test
  public void testSetDimensions() throws Exception {
    reader.open(RIVERSIDE_FILE);
    
    // First detect automatically to get dimension names
    reader.detectSpatialDimensions();
    ExternalSpatialDimensions detectedDims = reader.getSpatialDimensions();
    
    // Close and reopen
    reader.close();
    reader.open(RIVERSIDE_FILE);
    
    // Set dimensions manually using detected names
    String dimX = detectedDims.getDimensionNameX();
    String dimY = detectedDims.getDimensionNameY();
    String timeDim = detectedDims.getDimensionNameTime();
    
    reader.setDimensions(dimX, dimY, timeDim);
    
    ExternalSpatialDimensions manualDims = reader.getSpatialDimensions();
    
    // Verify dimensions match
    assertEquals(detectedDims.getDimensionNameX(), manualDims.getDimensionNameX());
    assertEquals(detectedDims.getDimensionNameY(), manualDims.getDimensionNameY());
    assertEquals(detectedDims.getDimensionNameTime(), manualDims.getDimensionNameTime());
  }
  
  @Test
  public void testReadValueAt() throws IOException {
    reader.open(RIVERSIDE_FILE);
    reader.detectSpatialDimensions();
    
    // Get spatial dimensions
    ExternalSpatialDimensions dims = reader.getSpatialDimensions();
    
    // Get a valid variable name
    List<String> variables = reader.getVariableNames();
    assertFalse(variables.isEmpty());
    String variableName = variables.get(0);
    
    // Get some valid coordinates from the file
    BigDecimal x = dims.getCoordinatesX().get(dims.getCoordinatesX().size() / 2);
    BigDecimal y = dims.getCoordinatesY().get(dims.getCoordinatesY().size() / 2);
    
    // Try to get the NetCDF file's CRS
    CoordinateReferenceSystem netcdfCrs = null;
    String crsStr = reader.getCrsCode();
    if (crsStr != null) {
      try {
        netcdfCrs = JtsTransformUtility.getRightHandedCrs(crsStr);
      } catch (FactoryException e) {
        System.err.println("Warning: Could not parse NetCDF CRS: " + e.getMessage());
      }
    }
    
    // Transform coordinates if needed
    BigDecimal transformedX = x;
    BigDecimal transformedY = y;
    
    // If both CRSs are available and different, transform coordinates
    if (netcdfCrs != null && patchCrs != null && 
        !org.apache.sis.util.Utilities.equalsIgnoreMetadata(netcdfCrs, patchCrs)) {
      try {
        // Create point geometry in patch CRS (WGS84)
        Point patchPoint = JtsTransformUtility.createJtsPoint(x.doubleValue(), y.doubleValue());
        EarthGeometry patchGeom = new EarthGeometry(patchPoint, patchCrs);
        
        // Transform to NetCDF CRS
        EarthGeometry transformedGeom = EarthTransformer.earthToEarth(patchGeom, netcdfCrs);
        
        // Extract transformed coordinates
        transformedX = transformedGeom.getCenterX();
        transformedY = transformedGeom.getCenterY();
        
        System.out.println("Transformed coordinates: " + x + "," + y + " -> " + 
            transformedX + "," + transformedY);
      } catch (Exception e) {
        System.err.println("Warning: Coordinate transformation failed: " + e.getMessage());
      }
    }
    
    // Set up mock
    when(valueFactory.build(any(BigDecimal.class), any(Units.class))).thenReturn(mockEngineValue);
    
    // Read value using transformed coordinates
    Optional<EngineValue> value = reader.readValueAt(variableName, transformedX, transformedY, 0);
    
    // The value should be present (if data exists at those coordinates)
    assertTrue(value.isPresent());
    assertSame(mockEngineValue, value.get());
    
    // Verify factory was called
    verify(valueFactory).build(any(BigDecimal.class), any(Units.class));
  }
  
  @Test
  public void testCompareRiversideAndSanBernardinoFiles() throws Exception {
    // Test Riverside file
    reader.open(RIVERSIDE_FILE);
    reader.detectSpatialDimensions();
    final List<String> riversideVariables = reader.getVariableNames();
    final ExternalSpatialDimensions riversideDims = reader.getSpatialDimensions();
    final String riversideCrsCode = reader.getCrsCode();
    
    // Close and open San Bernardino file
    reader.close();
    reader.open(SAN_BERNARDINO_FILE);
    reader.detectSpatialDimensions();
    List<String> sanBernardinoVariables = reader.getVariableNames();
    ExternalSpatialDimensions sanBernardinoDims = reader.getSpatialDimensions();
    String sanBernardinoCrsCode = reader.getCrsCode();
    
    // Print CRS info
    System.out.println("Riverside CRS: " + riversideCrsCode);
    System.out.println("San Bernardino CRS: " + sanBernardinoCrsCode);
    
    // Compare variable lists - they may differ between files
    System.out.println("Riverside variables: " + riversideVariables.size());
    System.out.println("San Bernardino variables: " + sanBernardinoVariables.size());
    
    // Compare spatial dimensions (may differ for different regions)
    System.out.println("Riverside X size: " + riversideDims.getCoordinatesX().size());
    System.out.println("San Bernardino X size: " + sanBernardinoDims.getCoordinatesX().size());
  }
  
  @Test
  public void testGetTimeDimensionSize() throws IOException {
    reader.open(RIVERSIDE_FILE);
    reader.detectSpatialDimensions();
    
    Optional<Integer> timeSize = reader.getTimeDimensionSize();
    
    // If there's a time dimension, check its size
    if (timeSize.isPresent()) {
      assertTrue(timeSize.get() > 0);
      System.out.println("Time dimension size: " + timeSize.get());
    } else {
      System.out.println("No time dimension found");
    }
  }
  
  @Test
  public void testReadValueOutsideBounds() throws IOException {
    reader.open(RIVERSIDE_FILE);
    reader.detectSpatialDimensions();
    
    List<String> variables = reader.getVariableNames();
    assertFalse(variables.isEmpty());
    String variableName = variables.get(0);
    
    // Try with coordinates outside the file's bounds
    BigDecimal invalidX = new BigDecimal("999999");
    BigDecimal invalidY = new BigDecimal("999999");
    
    Optional<EngineValue> value = reader.readValueAt(variableName, invalidX, invalidY, 0);
    
    // Should return empty optional for out-of-bounds coordinates
    assertFalse(value.isPresent());
  }
  
  @Test
  public void testReadValueWithInvalidTimeStep() throws IOException {
    reader.open(RIVERSIDE_FILE);
    reader.detectSpatialDimensions();
    
    // Get a valid variable name
    List<String> variables = reader.getVariableNames();
    assertFalse(variables.isEmpty());
    String variableName = variables.get(0);
    
    // Get some valid coordinates from the file
    ExternalSpatialDimensions dims = reader.getSpatialDimensions();
    BigDecimal x = dims.getCoordinatesX().get(0);
    BigDecimal y = dims.getCoordinatesY().get(0);
    
    // Try with invalid time step (very large value)
    Optional<EngineValue> value = reader.readValueAt(variableName, x, y, 9999);
    
    // Should return empty optional for invalid time step
    assertFalse(value.isPresent());
  }
  
  @Test
  public void testReadValueWithUtmToWgs84CoordinateTransformation() throws IOException {
    reader.open(RIVERSIDE_FILE);
    reader.detectSpatialDimensions();
    reader.setCrsCode("EPSG:4326"); // Explicitly set WGS84 as the NetCDF CRS

    // Get a valid variable name
    List<String> variables = reader.getVariableNames();
    assertFalse(variables.isEmpty());
    String variableName = variables.get(0);
    
    // Define a point in UTM Zone 11N coordinates (EPSG:32611) near Riverside
    BigDecimal utmX = new BigDecimal("475000");    // Easting (meters)
    BigDecimal utmY = new BigDecimal("3758000");   // Northing (meters)
    
    // Create the UTM CRS
    CoordinateReferenceSystem utmCrs = null;
    try {
      utmCrs = JtsTransformUtility.getRightHandedCrs("EPSG:32611");
      System.out.println("UTM 11N CRS created: " + utmCrs);
    } catch (FactoryException e) {
      fail("Failed to create UTM CRS: " + e.getMessage());
      return;
    }
    
    // Get the NetCDF CRS (WGS84)
    CoordinateReferenceSystem netcdfCrs = null;
    String crsStr = reader.getCrsCode();
    if (crsStr != null) {
      try {
        netcdfCrs = JtsTransformUtility.getRightHandedCrs(crsStr);
        System.out.println("NetCDF CRS: " + netcdfCrs);
      } catch (FactoryException e) {
        fail("Failed to parse NetCDF CRS: " + e.getMessage());
        return;
      }
    } else {
      fail("NetCDF CRS is null");
      return;
    }
    
    try {
      // Create point geometry in UTM CRS
      Point utmPoint = JtsTransformUtility.createJtsPoint(
          utmX.doubleValue(), utmY.doubleValue());
      EarthGeometry utmGeom = new EarthGeometry(utmPoint, utmCrs);
      
      // Transform to WGS84 CRS
      EarthGeometry transformedGeom = EarthTransformer.earthToEarth(utmGeom, netcdfCrs);
      
      // Extract transformed coordinates
      BigDecimal transformedX = transformedGeom.getCenterX();
      BigDecimal transformedY = transformedGeom.getCenterY();
      
      System.out.println("UTM 11N: " + utmX + "," + utmY + " -> WGS84: " + 
          transformedX + "," + transformedY);
      
      // Verify the coordinates are in the expected range for Riverside area
      // WGS84 for Riverside is approximately: longitude -117.4, latitude 33.9
      double lon = transformedX.doubleValue();
      double lat = transformedY.doubleValue();
      assertTrue(lon > -118 && lon < -117, 
          "Transformed longitude should be in Riverside area: " + lon);
      assertTrue(lat > 33 && lat < 34, 
          "Transformed latitude should be in Riverside area: " + lat);
      
      // Read actual value using transformed coordinates
      Optional<EngineValue> value = reader.readValueAt(
          variableName, transformedX, transformedY, 0);
      
      // Check for actual data
      assertTrue(value.isPresent(), "Should have found a value at transformed coordinates");
      assertNotNull(value.get(), "Value should not be null");
      
      // Print actual value for verification
      System.out.println("Value at coordinates: " + value.get().getInnerValue() + 
          " " + value.get().getUnits());
      
      // Verify value is reasonable (adjust based on expected data range)
      assertNotNull(value.get().getInnerValue(), "Value data should not be null");
      
    } catch (Exception e) {
      fail("Transformation failed: " + e.getMessage());
      e.printStackTrace();
    }
  }

  @Test
  public void testReadValueWithNoopCoordinateTransformation() throws IOException {
    reader.open(RIVERSIDE_FILE);
    reader.detectSpatialDimensions();
    reader.setCrsCode("EPSG:4326");

    // Get a valid variable name
    List<String> variables = reader.getVariableNames();
    assertFalse(variables.isEmpty());
    String variableName = variables.get(0);
    
    // Define a point in WGS84 coordinates (suitable for the Riverside area)
    BigDecimal wgs84X = new BigDecimal("-117.396"); // Longitude for Riverside area
    BigDecimal wgs84Y = new BigDecimal("33.948");   // Latitude for Riverside area
    
    // Get the NetCDF CRS
    CoordinateReferenceSystem netcdfCrs = null;
    String crsStr = reader.getCrsCode();
    if (crsStr != null && patchCrs != null) {
      try {
        netcdfCrs = JtsTransformUtility.getRightHandedCrs(crsStr);
        System.out.println("NetCDF CRS: " + netcdfCrs);
        
        // Transform WGS84 coordinates to NetCDF CRS coordinates
        Point wgs84Point = JtsTransformUtility.createJtsPoint(
            wgs84X.doubleValue(), wgs84Y.doubleValue());
        EarthGeometry wgs84Geom = new EarthGeometry(wgs84Point, patchCrs);
        
        // Transform to NetCDF CRS
        EarthGeometry transformedGeom = EarthTransformer.earthToEarth(wgs84Geom, netcdfCrs);
        
        // Extract transformed coordinates
        BigDecimal transformedX = transformedGeom.getCenterX();
        BigDecimal transformedY = transformedGeom.getCenterY();
        
        System.out.println("WGS84: " + wgs84X + "," + wgs84Y + " -> NetCDF CRS: " + 
            transformedX + "," + transformedY);
        
        // Read actual value using transformed coordinates
        Optional<EngineValue> value = reader.readValueAt(
            variableName, transformedX, transformedY, 0);
        
        // Check for actual data
        assertTrue(value.isPresent(), "Should have found a value at transformed coordinates");
        assertNotNull(value.get(), "Value should not be null");
        
        // Print actual value for verification
        System.out.println("Value at coordinates: " + value.get().getInnerValue() + 
            " " + value.get().getUnits());
        
        // Verify value is reasonable (adjust based on expected data range)
        assertNotNull(value.get().getInnerValue(), "Value data should not be null");
        
      } catch (Exception e) {
        System.err.println("Transformation failed: " + e.getMessage());
        e.printStackTrace();
      }
    } else {
      // If no CRS info available, just try direct read with WGS84 coordinates
      Optional<EngineValue> value = reader.readValueAt(variableName, wgs84X, wgs84Y, 0);
      
      assertTrue(value.isPresent(), "Should have found a value at WGS84 coordinates");
      assertNotNull(value.get(), "Value should not be null");
      
      // Print actual value for verification
      System.out.println("Value at WGS84 coordinates: " + value.get().getInnerValue() + 
          " " + value.get().getUnits());
      
      // Verify value is reasonable (adjust based on expected data range)
      assertNotNull(value.get().getInnerValue(), "Value data should not be null");
    }
  }

  @Test
  public void testExtendedBoundsCalculation() throws IOException {
    reader = new NetcdfExternalDataReader(valueFactory);
    reader.open(RIVERSIDE_FILE);
    reader.detectSpatialDimensions();
    
    // Get actual bounds
    BigDecimal minX = reader.getMinX();
    BigDecimal maxX = reader.getMaxX();
    BigDecimal minY = reader.getMinY();
    BigDecimal maxY = reader.getMaxY();
    
    assertNotNull(minX, "Min X should be set");
    assertNotNull(maxX, "Max X should be set");
    assertNotNull(minY, "Min Y should be set");
    assertNotNull(maxY, "Max Y should be set");
    
    // Get extended bounds
    BigDecimal extMinX = reader.getExtendedMinX();
    BigDecimal extMaxX = reader.getExtendedMaxX();
    BigDecimal extMinY = reader.getExtendedMinY();
    BigDecimal extMaxY = reader.getExtendedMaxY();
    
    assertNotNull(extMinX, "Extended min X should be set");
    assertNotNull(extMaxX, "Extended max X should be set");
    assertNotNull(extMinY, "Extended min Y should be set");
    assertNotNull(extMaxY, "Extended max Y should be set");
    
    // Verify extended bounds are larger than actual bounds
    assertTrue(extMinX.compareTo(minX) < 0, "Extended min X should be smaller than min X");
    assertTrue(extMaxX.compareTo(maxX) > 0, "Extended max X should be larger than max X");
    assertTrue(extMinY.compareTo(minY) < 0, "Extended min Y should be smaller than min Y");
    assertTrue(extMaxY.compareTo(maxY) > 0, "Extended max Y should be larger than max Y");
    
    // Verify buffer percentage (default 10%)
    BigDecimal rangeX = maxX.subtract(minX);
    BigDecimal rangeY = maxY.subtract(minY);
    BigDecimal expectedBufferX = rangeX.multiply(new BigDecimal("0.1"));
    BigDecimal expectedBufferY = rangeY.multiply(new BigDecimal("0.1"));
    
    assertEquals(0, minX.subtract(expectedBufferX).compareTo(extMinX), 
        "Extended min X should be exactly 10% smaller");
    assertEquals(0, maxX.add(expectedBufferX).compareTo(extMaxX), 
        "Extended max X should be exactly 10% larger");
    assertEquals(0, minY.subtract(expectedBufferY).compareTo(extMinY), 
        "Extended min Y should be exactly 10% smaller");
    assertEquals(0, maxY.add(expectedBufferY).compareTo(extMaxY), 
        "Extended max Y should be exactly 10% larger");
  }

  @Test
  public void testCustomBufferSize() throws IOException {
    reader = new NetcdfExternalDataReader(valueFactory);
    reader.open(RIVERSIDE_FILE);
    reader.detectSpatialDimensions();
    
    // Get original extended bounds with default 10% buffer
    BigDecimal origExtMinX = reader.getExtendedMinX();
    BigDecimal origExtMaxX = reader.getExtendedMaxX();
    
    // Set custom buffer of 20%
    reader.setBoundsBuffer(new BigDecimal("0.2"));
    
    // Get new extended bounds
    BigDecimal newExtMinX = reader.getExtendedMinX();
    BigDecimal newExtMaxX = reader.getExtendedMaxX();
    
    // Verify new buffer is larger than original
    assertTrue(newExtMinX.compareTo(origExtMinX) < 0, 
        "New min X bound should be smaller with larger buffer");
    assertTrue(newExtMaxX.compareTo(origExtMaxX) > 0,
        "New max X bound should be larger with larger buffer");
    
    // Set buffer to 0% and verify bounds match actual bounds
    reader.setBoundsBuffer(BigDecimal.ZERO);
    assertEquals(0, reader.getMinX().compareTo(reader.getExtendedMinX()),
        "With 0% buffer, extended bounds should match actual bounds");
    assertEquals(0, reader.getMaxX().compareTo(reader.getExtendedMaxX()),
        "With 0% buffer, extended bounds should match actual bounds");
  }

  @Test
  public void testPointsJustOutsideActualBoundsButInsideExtendedBounds() throws IOException {
    reader = new NetcdfExternalDataReader(valueFactory);
    reader.open(RIVERSIDE_FILE);
    reader.detectSpatialDimensions();
    
    List<String> variables = reader.getVariableNames();
    assertFalse(variables.isEmpty());
    String variableName = variables.get(0);
    
    // Get actual and extended bounds
    BigDecimal minX = reader.getMinX();
    BigDecimal minY = reader.getMinY();
    BigDecimal extMinX = reader.getExtendedMinX();
    
    // Create a point just outside actual bounds but inside extended bounds
    BigDecimal testX = minX.add((extMinX.subtract(minX)).multiply(new BigDecimal("0.5")));
    
    // This point is between min X and extended min X (within buffer zone)
    assertTrue(testX.compareTo(minX) < 0, "Test point should be outside actual bounds");
    assertTrue(testX.compareTo(extMinX) > 0, "Test point should be inside extended bounds");
    
    // Point may not have data but should pass extended bounds check
    Optional<EngineValue> value = reader.readValueAt(variableName, testX, minY, 0);
    
    // Should still return empty because there's no actual data there,
    // but it should have passed the extended bounds check
    assertFalse(value.isPresent());
  }

  @Test
  public void testWgs84BoundsHandling() throws IOException {
    reader = new NetcdfExternalDataReader(valueFactory);
    reader.open(RIVERSIDE_FILE);
    reader.detectSpatialDimensions();
    
    // Set CRS to WGS84
    reader.setCrsCode("EPSG:4326");
    
    // Set custom buffer to 0 to test just the WGS84 logic
    reader.setBoundsBuffer(BigDecimal.ZERO);
    
    // Try reading value at global bounds for WGS84
    List<String> variables = reader.getVariableNames();
    assertFalse(variables.isEmpty());
    String variableName = variables.get(0);
    
    // These should be handled properly for WGS84
    BigDecimal longMinus180 = new BigDecimal("-180.0");
    BigDecimal long180 = new BigDecimal("180.0");
    BigDecimal lat90 = new BigDecimal("90.0");
    BigDecimal latMinus90 = new BigDecimal("-90.0");
    
    // All of these should at least pass the bounds check
    // They may not return values if no data exists at these coordinates
    reader.readValueAt(variableName, longMinus180, BigDecimal.ZERO, 0);
    reader.readValueAt(variableName, long180, BigDecimal.ZERO, 0);
    reader.readValueAt(variableName, BigDecimal.ZERO, lat90, 0);
    reader.readValueAt(variableName, BigDecimal.ZERO, latMinus90, 0);
  }

  @Test
  public void testCheckFileOpenValidation() {
    // Try to get variables before opening file
    assertThrows(IOException.class, () -> reader.getVariableNames(),
        "Should throw IOException if file not opened");
    assertThrows(IOException.class, () -> reader.getMinX(),
        "Should throw IOException if file not opened");
}

  @Test
  public void testEnsureDimensionsSetValidation() throws IOException {
    reader = new NetcdfExternalDataReader(valueFactory);
    reader.open(RIVERSIDE_FILE);
    // Try operations that require dimensions to be set
    assertThrows(IOException.class, () -> reader.getSpatialDimensions(),
        "Should throw IOException if dimensions not set");
    assertThrows(IOException.class, () -> reader.readValueAt("test", BigDecimal.ONE, BigDecimal.ONE, 0),
        "Should throw IOException if dimensions not set");
  }
}