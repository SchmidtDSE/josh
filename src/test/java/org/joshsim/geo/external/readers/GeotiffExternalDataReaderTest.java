
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
    reader = new GeotiffExternalDataReader(valueFactory, new Units("mm"));

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
    assertEquals(
        39.580078125,
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
}
