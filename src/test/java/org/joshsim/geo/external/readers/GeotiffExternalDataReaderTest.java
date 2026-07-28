
package org.joshsim.geo.external.readers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.ValueSupportFactory;
import org.joshsim.engine.value.type.EngineValue;
import org.joshsim.geo.external.ExternalSpatialDimensions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


/**
 * Tests which sample an actual geotiff with known values.
 */
public class GeotiffExternalDataReaderTest {

  private static final String GEOTIFF_RESOURCE_PATH = "cog/nclimgrid-prcp-202111.tif";
  private String geotiffFilePath;
  private GeotiffExternalDataReader reader;
  private ValueSupportFactory valueFactory;

  /**
   * Load the sample file.
   */
  @BeforeEach
  public void setUp() throws IOException {
    // Initialize value factory
    valueFactory = new ValueSupportFactory();

    // Initialize reader
    reader = new GeotiffExternalDataReader(valueFactory, Units.of("mm"));

    // Get resource path
    URL resourceUrl = getClass().getClassLoader().getResource(GEOTIFF_RESOURCE_PATH);
    if (resourceUrl == null) {
      throw new IOException("Test resource not found: " + GEOTIFF_RESOURCE_PATH);
    }
    geotiffFilePath = new File(resourceUrl.getFile()).getAbsolutePath();

    // Open the file
    reader.open(geotiffFilePath);
    reader.setCrsCode("EPSG:4326");
  }

  /**
   * Close reader after each test.
   */
  @AfterEach
  public void tearDown() throws Exception {
    if (reader != null) {
      reader.close();
    }
  }

  @Test
  public void testKnownPoint1() throws IOException {
    // Test first specific point. This lands 0.638 of a pixel east of the west edge of column 58,
    // so it belongs to column 58 (39.580078) and not to column 59 (45.040039). Anchoring the
    // coordinate list on pixel edges instead of pixel centers used to hand back the neighbor.
    BigDecimal lat = new BigDecimal("37.871878");
    BigDecimal lon = new BigDecimal("-122.265088");
    String variableName = "0"; // First band

    Optional<EngineValue> value = reader.readValueAt(variableName, lon, lat, 0);

    assertTrue(value.isPresent(), "Value should be present at test coordinates");
    assertEquals(
        39.580078,
        value.get().getAsDecimal().doubleValue(),
        0.0001,
        "Value at test coordinates does not match expected value"
    );
  }

  /**
   * Pin the reported coordinates to pixel centers spaced at the raster's own pixel scale.
   *
   * <p>The sample file declares a 1385x596 grid with a pixel scale of 0.04166667 degrees and its
   * tie point at (-124.70833333, 49.37500127), which GeoTIFF places on the outer corner of the
   * top-left pixel. Reporting edges rather than centers biases every nearest-neighbor lookup half
   * a pixel toward the origin, and spacing the coordinates over n - 1 intervals rather than n
   * stretches the axis enough to drift a full pixel by the far side of a large raster.</p>
   */
  @Test
  public void testCoordinatesAreCellCenters() throws IOException {
    final double pixelScale = 0.04166667;
    final double tolerance = 1e-9;

    ExternalSpatialDimensions dimensions = reader.getSpatialDimensions();
    List<BigDecimal> coordsX = dimensions.getCoordinatesX();
    List<BigDecimal> coordsY = dimensions.getCoordinatesY();

    assertEquals(1385, coordsX.size(), "One X coordinate per raster column");
    assertEquals(596, coordsY.size(), "One Y coordinate per raster row");

    assertEquals(
        pixelScale,
        coordsX.get(1).subtract(coordsX.get(0)).doubleValue(),
        tolerance,
        "X spacing should equal the raster pixel scale"
    );
    assertEquals(
        pixelScale,
        coordsY.get(0).subtract(coordsY.get(1)).doubleValue(),
        tolerance,
        "Y spacing should equal the raster pixel scale"
    );

    assertEquals(
        -124.70833333 + pixelScale / 2,
        coordsX.get(0).doubleValue(),
        tolerance,
        "First X coordinate should be the center of the westmost column"
    );
    assertEquals(
        49.37500127 - pixelScale / 2,
        coordsY.get(0).doubleValue(),
        tolerance,
        "First Y coordinate should be the center of the northmost row"
    );
    assertEquals(
        -124.70833333 + pixelScale * (1385 - 0.5),
        coordsX.get(coordsX.size() - 1).doubleValue(),
        tolerance,
        "Last X coordinate should be the center of the eastmost column"
    );
    assertEquals(
        49.37500127 - pixelScale * (596 - 0.5),
        coordsY.get(coordsY.size() - 1).doubleValue(),
        tolerance,
        "Last Y coordinate should be the center of the southmost row"
    );
  }

  @Test
  public void testKnownPoint2() throws IOException {
    // Test second specific point
    BigDecimal lat = new BigDecimal("37.767801");
    BigDecimal lon = new BigDecimal("-122.29092");
    String variableName = "0"; // First band

    Optional<EngineValue> value = reader.readValueAt(variableName, lon, lat, 0);

    assertTrue(value.isPresent(), "Value should be present at test coordinates");
    assertEquals(
        29.33984375,
        value.get().getAsDecimal().doubleValue(),
        0.0001,
        "Value at test coordinates does not match expected value"
    );
  }

  @Test
  public void testCanHandle() {
    assertTrue(reader.canHandle("test.tif"));
    assertTrue(reader.canHandle("test.tiff"));
    assertTrue(reader.canHandle("TEST.TIF"));
  }

  /**
   * Test reading CHC-CMIP6 precipitation data that should return non-zero values.
   * This test verifies that the coordinate calculation correctly maps world coordinates
   * to pixel indices for GeoTIFF files.
   */
  @Test
  public void testChcCmip6PrecipitationNonZeroValues() throws IOException {
    // Use CHC-CMIP6 data file
    URL resourceUrl = getClass().getClassLoader()
        .getResource("cog/CHC-CMIP6_SSP245_CHIRPS_2008_annual.tif");
    if (resourceUrl == null) {
      throw new IOException("CHC-CMIP6 test resource not found");
    }
    String chcFilePath = new File(resourceUrl.getFile()).getAbsolutePath();

    // Create new reader for CHC data
    GeotiffExternalDataReader chcReader = new GeotiffExternalDataReader(
        valueFactory, Units.of("mm"));

    try {
      chcReader.open(chcFilePath);
      chcReader.setCrsCode("EPSG:4326");

      // Test coordinates from the grass_shrub_fire simulation area
      // These are the same coordinates that are failing in preprocessing
      BigDecimal lat = new BigDecimal("35.4955033919704");
      BigDecimal lon = new BigDecimal("-119.99447700450675");
      String variableName = "0"; // First band

      Optional<EngineValue> value = chcReader.readValueAt(variableName, lon, lat, 0);

      // Verify that coordinate calculation works correctly
      assertTrue(value.isPresent(),
          "CHC-CMIP6 value should be present at coordinates (" + lon + ", " + lat + ")");

      // The value should be non-zero for precipitation data
      double precipValue = value.get().getAsDecimal().doubleValue();
      assertTrue(precipValue > 0.0,
          "CHC-CMIP6 precipitation value should be > 0, got: " + precipValue);

    } finally {
      try {
        chcReader.close();
      } catch (Exception e) {
        // Ignore close exceptions in test
      }
    }
  }
}
