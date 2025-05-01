package org.joshsim.geo.external.readers;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.engine.value.type.EngineValue;
import org.joshsim.geo.external.ExternalSpatialDimensions;
import org.joshsim.geo.geometry.JtsTransformUtility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.FactoryException;

/**
 * Unit tests for the {@link NetcdfExternalDataReader} class.
 * This class verifies the functionality of reading and processing
 * NetCDF files using various test cases.
 */
@ExtendWith(MockitoExtension.class)
public class NetcdfExternalDataReaderTest {

  private static final String RIVERSIDE_RESOURCE_PATH = "netcdf/precip_riverside_annual_agg.nc";
  private String riversideFilePath;

  // Define the explicit dimension names
  private static final String DIM_X = "lon";
  private static final String DIM_Y = "lat";
  private static final String DIM_TIME = "calendar_year";

  private NetcdfExternalDataReader reader;

  private EngineValueFactory valueFactory;

  @Mock
  private EngineValue mockEngineValue;

  // Default patch CRS (WGS84)
  private CoordinateReferenceSystem patchCrs;

  /**
   * Sets up the test environment before each test.
   * Initializes the reader, value factory, and loads the test resource file.
   *
   * @throws IOException if the test resource file cannot be found or accessed
   */
  @BeforeEach
  public void setUp() throws IOException {
    valueFactory = new EngineValueFactory();
    reader = new NetcdfExternalDataReader(valueFactory);

    // Get resource path
    URL resourceUrl = getClass().getClassLoader().getResource(RIVERSIDE_RESOURCE_PATH);
    if (resourceUrl == null) {
      throw new IOException("Test resource not found: " + RIVERSIDE_RESOURCE_PATH);
    }
    riversideFilePath = new File(resourceUrl.getFile()).getAbsolutePath();

    // Try to create WGS84 CRS
    try {
      patchCrs = JtsTransformUtility.getRightHandedCrs("EPSG:4326");
    } catch (FactoryException e) {
      System.err.println("Warning: Could not create WGS84 CRS: " + e.getMessage());
    }
  }

  /**
   * Cleans up resources after each test by closing the reader.
   *
   * @throws Exception if an error occurs during cleanup
   */
  @AfterEach
  public void tearDown() throws Exception {
    reader.close();
  }

  /**
   * Helper method to open the file and set the dimensions explicitly.
   */
  private void openAndSetExplicitDimensions(String filePath) throws IOException {
    reader.open(filePath);
    reader.setDimensions(DIM_X, DIM_Y, Optional.of(DIM_TIME));
    // Also set WGS84 CRS since we're using lon/lat
    reader.setCrsCode("EPSG:4326");
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
    assertDoesNotThrow(() -> reader.open(riversideFilePath));
    assertDoesNotThrow(() -> reader.close());
  }

  @Test
  public void testOpenNonExistentFile() {
    assertThrows(IOException.class, () -> reader.open("nonexistent.nc"));
  }

  @Test
  public void testExplicitDimensions() throws IOException {
    openAndSetExplicitDimensions(riversideFilePath);

    ExternalSpatialDimensions dims = reader.getSpatialDimensions();
    assertNotNull(dims);

    // Verify the dimension names match what we expect
    assertEquals(DIM_X, dims.getDimensionNameX(), "X dimension should be 'lon'");
    assertEquals(DIM_Y, dims.getDimensionNameY(), "Y dimension should be 'lat'");
    assertEquals(DIM_TIME, dims.getDimensionNameTime(), "Time dimension should be 'centered_year'");
  }

  @Test
  public void testGetCrs() throws IOException {
    openAndSetExplicitDimensions(riversideFilePath);

    // Get CRS from file
    String crs = reader.getCrsCode();

    // We explicitly set the CRS to WGS84
    assertNotNull(crs);
    assertEquals("EPSG:4326", crs);
  }

  @Test
  public void testGetVariableNames() throws IOException {
    openAndSetExplicitDimensions(riversideFilePath);

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
    openAndSetExplicitDimensions(riversideFilePath);

    ExternalSpatialDimensions dims = reader.getSpatialDimensions();
    assertNotNull(dims);

    // Check X coordinates
    List<BigDecimal> coordsX = dims.getCoordinatesX();
    assertFalse(coordsX.isEmpty());

    // Check Y coordinates
    List<BigDecimal> coordsY = dims.getCoordinatesY();
    assertFalse(coordsY.isEmpty());

    // Print some coordinate info for debugging
    System.out.println("X range: " + coordsX.get(0) + " to " + coordsX.get(coordsX.size() - 1));
    System.out.println("Y range: " + coordsY.get(0) + " to " + coordsY.get(coordsY.size() - 1));
  }

  @Test
  public void testReadValueAt() throws IOException {
    // Use the real factory, not a spy
    EngineValueFactory realFactory = new EngineValueFactory();
    reader = new NetcdfExternalDataReader(realFactory);

    openAndSetExplicitDimensions(riversideFilePath);

    // Get spatial dimensions
    ExternalSpatialDimensions dims = reader.getSpatialDimensions();

    // Get a valid variable name
    List<String> variables = reader.getVariableNames();
    assertFalse(variables.isEmpty());
    String variableName = variables.get(0);

    // Get some valid coordinates from the file
    BigDecimal x = dims.getCoordinatesX().get(dims.getCoordinatesX().size() / 2);
    BigDecimal y = dims.getCoordinatesY().get(dims.getCoordinatesY().size() / 2);

    // Read value using coordinates - with real factory
    Optional<EngineValue> value = reader.readValueAt(variableName, x, y, 0);

    // The value should be present (if data exists at those coordinates)
    assertTrue(value.isPresent(), "Should return a value from the actual NetCDF file");

    // Additional assertions on the real value
    assertNotNull(value.get().getInnerValue(), "Value from NetCDF should not be null");
  }

  @Test
  public void testCompareRiversideFiles() throws Exception {
    // Test Riverside file
    openAndSetExplicitDimensions(riversideFilePath);
    final List<String> riversideVariables = reader.getVariableNames();
    final ExternalSpatialDimensions riversideDims = reader.getSpatialDimensions();
    final String riversideCrsCode = reader.getCrsCode();

    // Close and open San Bernardino file
    reader.close();

    // Print CRS info
    System.out.println("Riverside CRS: " + riversideCrsCode);

    // Compare variable lists - they may differ between files
    System.out.println("Riverside variables: " + riversideVariables.size());

    // Compare spatial dimensions (may differ for different regions)
    System.out.println("Riverside X size: " + riversideDims.getCoordinatesX().size());
  }

  @Test
  public void testGetTimeDimensionSize() throws IOException {
    openAndSetExplicitDimensions(riversideFilePath);

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
    openAndSetExplicitDimensions(riversideFilePath);

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
    openAndSetExplicitDimensions(riversideFilePath);

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
  public void testExtendedBoundsCalculation() throws IOException {
    reader = new NetcdfExternalDataReader(valueFactory);
    openAndSetExplicitDimensions(riversideFilePath);

    // Get actual bounds
    final BigDecimal minX = reader.getMinX();
    final BigDecimal maxX = reader.getMaxX();
    final BigDecimal minY = reader.getMinY();
    final BigDecimal maxY = reader.getMaxY();

    assertNotNull(minX, "Min X should be set");
    assertNotNull(maxX, "Max X should be set");
    assertNotNull(minY, "Min Y should be set");
    assertNotNull(maxY, "Max Y should be set");

    // Get extended bounds
    final BigDecimal extMinX = reader.getExtendedMinX();
    final BigDecimal extMaxX = reader.getExtendedMaxX();
    final BigDecimal extMinY = reader.getExtendedMinY();
    final BigDecimal extMaxY = reader.getExtendedMaxY();

    assertNotNull(extMinX, "Extended min X should be set");
    assertNotNull(extMaxX, "Extended max X should be set");
    assertNotNull(extMinY, "Extended min Y should be set");
    assertNotNull(extMaxY, "Extended max Y should be set");

    // Verify extended bounds are larger than actual bounds
    assertTrue(extMinX.compareTo(minX) < 0, "Extended min X should be smaller than min X");
    assertTrue(extMaxX.compareTo(maxX) > 0, "Extended max X should be larger than max X");
    assertTrue(extMinY.compareTo(minY) < 0, "Extended min Y should be smaller than min Y");
    assertTrue(extMaxY.compareTo(maxY) > 0, "Extended max Y should be larger than max Y");
  }

  @Test
  public void testCustomBufferSize() throws IOException {
    reader = new NetcdfExternalDataReader(valueFactory);
    openAndSetExplicitDimensions(riversideFilePath);

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
  }

  @Test
  public void testPointsJustOutsideActualBoundsButInsideExtendedBounds() throws IOException {
    reader = new NetcdfExternalDataReader(valueFactory);
    openAndSetExplicitDimensions(riversideFilePath);

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

    // Check if value is present
    assertTrue(value.isPresent());
  }

  @Test
  public void testEnsureDimensionsSetValidation() throws IOException {
    reader = new NetcdfExternalDataReader(valueFactory);
    reader.open(riversideFilePath);
    // Try operations that require dimensions to be set without setting them
    assertThrows(IOException.class, () -> reader.getSpatialDimensions(),
        "Should throw IOException if dimensions not set");
    assertThrows(IOException.class, () -> reader.readValueAt(
          "test", BigDecimal.ONE, BigDecimal.ONE, 0),
        "Should throw IOException if dimensions not set");
  }
}
