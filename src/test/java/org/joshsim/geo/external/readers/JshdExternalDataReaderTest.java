package org.joshsim.geo.external.readers;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.joshsim.engine.geometry.PatchBuilderExtents;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.engine.value.type.EngineValue;
import org.joshsim.geo.external.ExternalSpatialDimensions;
import org.joshsim.precompute.DoublePrecomputedGrid;
import org.joshsim.precompute.JshdUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for the {@link JshdExternalDataReader} class.
 * This class verifies the functionality of reading and processing JSHD files.
 */
@ExtendWith(MockitoExtension.class)
public class JshdExternalDataReaderTest {

  private JshdExternalDataReader reader;
  private EngineValueFactory valueFactory;
  private Path testJshdFile;

  @TempDir
  Path tempDir;

  /**
   * Sets up the test environment before each test.
   * Creates a test JSHD file with known data for testing.
   */
  @BeforeEach
  public void setUp() throws Exception {
    valueFactory = new EngineValueFactory();
    reader = new JshdExternalDataReader(valueFactory);

    // Create a test JSHD file
    testJshdFile = createTestJshdFile();
  }

  /**
   * Cleans up resources after each test by closing the reader.
   */
  @AfterEach
  public void tearDown() throws Exception {
    if (reader != null) {
      reader.close();
    }
  }

  /**
   * Creates a test JSHD file with known data for testing purposes.
   */
  private Path createTestJshdFile() throws IOException {
    // Create mock extents
    PatchBuilderExtents extents = mock(PatchBuilderExtents.class);
    when(extents.getTopLeftX()).thenReturn(BigDecimal.valueOf(0));
    when(extents.getTopLeftY()).thenReturn(BigDecimal.valueOf(0));
    when(extents.getBottomRightX()).thenReturn(BigDecimal.valueOf(2));
    when(extents.getBottomRightY()).thenReturn(BigDecimal.valueOf(2));

    // Create test data
    double[][][] innerValues = new double[2][3][3]; // 2 timesteps, 3x3 grid
    innerValues[0][0][0] = 1.0;
    innerValues[0][1][1] = 2.0;
    innerValues[0][2][2] = 3.0;
    innerValues[1][0][0] = 4.0;
    innerValues[1][1][1] = 5.0;
    innerValues[1][2][2] = 6.0;

    // Create DoublePrecomputedGrid
    DoublePrecomputedGrid grid = new DoublePrecomputedGrid(
        valueFactory,
        extents,
        0, // minTimestep
        1, // maxTimestep
        Units.of("meters"),
        innerValues
    );

    // Serialize to bytes
    byte[] jshdData = JshdUtil.serializeToBytes(grid);

    // Write to temp file
    Path jshdFile = tempDir.resolve("test.jshd");
    Files.write(jshdFile, jshdData);

    return jshdFile;
  }

  @Test
  public void testCanHandle() {
    assertTrue(reader.canHandle("test.jshd"));
    assertTrue(reader.canHandle("TEST.JSHD"));
    assertTrue(reader.canHandle("/path/to/file.jshd"));

    assertFalse(reader.canHandle("test.nc"));
    assertFalse(reader.canHandle("test.tif"));
    assertFalse(reader.canHandle("test.csv"));
    assertFalse(reader.canHandle(null));
    assertFalse(reader.canHandle(""));
    assertFalse(reader.canHandle("test"));
  }

  @Test
  public void testOpenAndClose() throws Exception {
    assertDoesNotThrow(() -> reader.open(testJshdFile.toString()));
    assertNotNull(reader.getGrid());

    reader.close();
    // After close, grid should be null but we can't test this since getGrid is public
  }

  @Test
  public void testOpenNonExistentFile() {
    Path nonExistentFile = tempDir.resolve("nonexistent.jshd");

    IOException exception = assertThrows(IOException.class, () ->
        reader.open(nonExistentFile.toString())
    );
    assertTrue(exception.getMessage().contains("Failed to open JSHD file"));
  }

  @Test
  public void testGetVariableNames() throws Exception {
    reader.open(testJshdFile.toString());

    List<String> variableNames = reader.getVariableNames();
    assertNotNull(variableNames);
    assertEquals(1, variableNames.size());
    assertEquals("data", variableNames.get(0));
  }

  @Test
  public void testGetVariableNamesWithoutOpening() {
    IOException exception = assertThrows(IOException.class, () ->
        reader.getVariableNames()
    );
    assertTrue(exception.getMessage().contains("JSHD file not opened"));
  }

  @Test
  public void testGetCrsCode() throws Exception {
    String defaultCrs = reader.getCrsCode();
    assertEquals("EPSG:4326", defaultCrs);

    reader.setCrsCode("EPSG:3857");
    assertEquals("EPSG:3857", reader.getCrsCode());
  }

  @Test
  public void testGetTimeDimensionSize() throws Exception {
    reader.open(testJshdFile.toString());

    Optional<Integer> timeSize = reader.getTimeDimensionSize();
    assertTrue(timeSize.isPresent());
    assertEquals(2, timeSize.get()); // 2 timesteps (0 to 1)
  }

  @Test
  public void testGetSpatialDimensions() throws Exception {
    reader.open(testJshdFile.toString());

    ExternalSpatialDimensions dimensions = reader.getSpatialDimensions();
    assertNotNull(dimensions);
    assertEquals("x", dimensions.getDimensionNameX());
    assertEquals("y", dimensions.getDimensionNameY());
    assertEquals("time", dimensions.getDimensionNameTime());
    assertEquals("EPSG:4326", dimensions.getCrs());

    List<BigDecimal> coordsX = dimensions.getCoordinatesX();
    List<BigDecimal> coordsY = dimensions.getCoordinatesY();

    assertEquals(3, coordsX.size()); // X coordinates: 0, 1, 2
    assertEquals(3, coordsY.size()); // Y coordinates: 0, 1, 2

    assertEquals(BigDecimal.valueOf(0).setScale(6), coordsX.get(0));
    assertEquals(BigDecimal.valueOf(1).setScale(6), coordsX.get(1));
    assertEquals(BigDecimal.valueOf(2).setScale(6), coordsX.get(2));
  }

  @Test
  public void testReadValueAt() throws Exception {
    reader.open(testJshdFile.toString());

    // Read known values
    Optional<EngineValue> value1 = reader.readValueAt("data", BigDecimal.valueOf(0),
        BigDecimal.valueOf(0), 0);
    assertTrue(value1.isPresent());
    assertEquals(1.0, value1.get().getAsDecimal().doubleValue(), 0.001);

    Optional<EngineValue> value2 = reader.readValueAt("data", BigDecimal.valueOf(1),
        BigDecimal.valueOf(1), 0);
    assertTrue(value2.isPresent());
    assertEquals(2.0, value2.get().getAsDecimal().doubleValue(), 0.001);

    Optional<EngineValue> value3 = reader.readValueAt("data", BigDecimal.valueOf(2),
        BigDecimal.valueOf(2), 1);
    assertTrue(value3.isPresent());
    assertEquals(6.0, value3.get().getAsDecimal().doubleValue(), 0.001);

    // Read value at location with no data (should return 0.0 as default)
    Optional<EngineValue> value4 = reader.readValueAt("data", BigDecimal.valueOf(0),
        BigDecimal.valueOf(1), 0);
    assertTrue(value4.isPresent());
    assertEquals(0.0, value4.get().getAsDecimal().doubleValue(), 0.001);
  }

  @Test
  public void testReadValueAtOutOfBounds() throws Exception {
    reader.open(testJshdFile.toString());

    // Read value outside grid bounds
    Optional<EngineValue> value1 = reader.readValueAt("data", BigDecimal.valueOf(-1),
        BigDecimal.valueOf(0), 0);
    assertFalse(value1.isPresent());

    Optional<EngineValue> value2 = reader.readValueAt("data", BigDecimal.valueOf(3),
        BigDecimal.valueOf(0), 0);
    assertFalse(value2.isPresent());

    Optional<EngineValue> value3 = reader.readValueAt("data", BigDecimal.valueOf(0),
        BigDecimal.valueOf(3), 0);
    assertFalse(value3.isPresent());

    // Read value at invalid timestep
    Optional<EngineValue> value4 = reader.readValueAt("data", BigDecimal.valueOf(0),
        BigDecimal.valueOf(0), 2);
    assertFalse(value4.isPresent());

    Optional<EngineValue> value5 = reader.readValueAt("data", BigDecimal.valueOf(0),
        BigDecimal.valueOf(0), -1);
    assertFalse(value5.isPresent());
  }

  @Test
  public void testSetDimensions() throws Exception {
    reader.setDimensions("longitude", "latitude", Optional.of("calendar_year"));
    reader.open(testJshdFile.toString());

    ExternalSpatialDimensions dimensions = reader.getSpatialDimensions();
    assertEquals("longitude", dimensions.getDimensionNameX());
    assertEquals("latitude", dimensions.getDimensionNameY());
    assertEquals("calendar_year", dimensions.getDimensionNameTime());
  }

  @Test
  public void testSetDimensionsWithoutTime() throws Exception {
    reader.setDimensions("x", "y", Optional.empty());
    reader.open(testJshdFile.toString());

    ExternalSpatialDimensions dimensions = reader.getSpatialDimensions();
    assertEquals("x", dimensions.getDimensionNameX());
    assertEquals("y", dimensions.getDimensionNameY());
    assertEquals("time", dimensions.getDimensionNameTime()); // Default value
  }

  @Test
  public void testGetGridBounds() throws Exception {
    reader.open(testJshdFile.toString());

    assertEquals(BigDecimal.valueOf(0), reader.getMinX());
    assertEquals(BigDecimal.valueOf(2), reader.getMaxX());
    assertEquals(BigDecimal.valueOf(0), reader.getMinY());
    assertEquals(BigDecimal.valueOf(2), reader.getMaxY());
  }

  @Test
  public void testGetGridBoundsWithoutOpening() {
    // Should return null when file is not opened
    assertEquals(null, reader.getMinX());
    assertEquals(null, reader.getMaxX());
    assertEquals(null, reader.getMinY());
    assertEquals(null, reader.getMaxY());
  }

  @Test
  public void testReadValueAtWithoutOpening() {
    IOException exception = assertThrows(IOException.class, () ->
        reader.readValueAt("data", BigDecimal.valueOf(0), BigDecimal.valueOf(0), 0)
    );
    assertTrue(exception.getMessage().contains("JSHD file not opened"));
  }

  @Test
  public void testGetSpatialDimensionsWithoutOpening() {
    IOException exception = assertThrows(IOException.class, () ->
        reader.getSpatialDimensions()
    );
    assertTrue(exception.getMessage().contains("JSHD file not opened"));
  }

  @Test
  public void testGetTimeDimensionSizeWithoutOpening() {
    IOException exception = assertThrows(IOException.class, () ->
        reader.getTimeDimensionSize()
    );
    assertTrue(exception.getMessage().contains("JSHD file not opened"));
  }
}
