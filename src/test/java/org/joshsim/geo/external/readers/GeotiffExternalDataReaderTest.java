
package org.joshsim.geo.external.readers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.util.Optional;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.engine.value.type.EngineValue;
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
  private EngineValueFactory valueFactory;

  /**
   * Load the sample file.
   */
  @BeforeEach
  public void setUp() throws IOException {
    // Initialize value factory
    valueFactory = new EngineValueFactory();

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
    // Test first specific point
    BigDecimal lat = new BigDecimal("37.871878");
    BigDecimal lon = new BigDecimal("-122.265088");
    String variableName = "0"; // First band

    Optional<EngineValue> value = reader.readValueAt(variableName, lon, lat, 0);

    assertTrue(value.isPresent(), "Value should be present at test coordinates");
    // Updated expected value based on correct coordinate calculation
    assertEquals(
        45.040039,
        value.get().getAsDecimal().doubleValue(),
        0.0001,
        "Value at test coordinates does not match expected value"
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
   * This test specifically addresses the coordinate calculation bug where all values
   * returned as zero due to out-of-bounds coordinate calculations.
   *
   * <p>This test SHOULD FAIL until the coordinate calculation bug is fixed.</p>
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
      
      // This assertion should FAIL until coordinate calculation is fixed
      assertTrue(value.isPresent(), 
          "CHC-CMIP6 value should be present at coordinates (" + lon + ", " + lat + ") - "
          + "if this fails, coordinate calculation is broken");
      
      // The value should be non-zero for precipitation data
      double precipValue = value.get().getAsDecimal().doubleValue();
      assertTrue(precipValue > 0.0, 
          "CHC-CMIP6 precipitation value should be > 0, got: " + precipValue + " - "
          + "if this fails, we're reading zeros due to coordinate calculation bug");
          
    } finally {
      try {
        chcReader.close();
      } catch (Exception e) {
        // Ignore close exceptions in test
      }
    }
  }
}
