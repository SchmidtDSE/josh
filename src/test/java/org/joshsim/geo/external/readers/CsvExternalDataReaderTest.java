
package org.joshsim.geo.external.readers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.util.Optional;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.engine.value.type.EngineValue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


/**
 * Test reading a CSV file as external data.
 */
public class CsvExternalDataReaderTest {
  private static final String CSV_RESOURCE_PATH = "csv/local.csv";
  private String csvFilePath;
  private CsvExternalDataReader reader;
  private EngineValueFactory valueFactory;

  /**
   * Get the test CSV file.
   */
  @BeforeEach
  public void setUp() throws IOException {
    // Initialize value factory
    valueFactory = new EngineValueFactory();

    // Initialize reader
    reader = new CsvExternalDataReader(valueFactory);

    // Get resource path
    URL resourceUrl = getClass().getClassLoader().getResource(CSV_RESOURCE_PATH);
    if (resourceUrl == null) {
      throw new IOException("Test resource not found: " + CSV_RESOURCE_PATH);
    }
    csvFilePath = new File(resourceUrl.getFile()).getAbsolutePath();

    // Open the file
    reader.open(csvFilePath);
    reader.setCrsCode("EPSG:4326");
  }

  /**
   * Close the test CSV file.
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
    String variableName = "value";

    Optional<EngineValue> value = reader.readValueAt(variableName, lon, lat, 0);

    assertTrue(value.isPresent(), "Value should be present at test coordinates");
    assertEquals(
        10.0,
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
    String variableName = "value";

    Optional<EngineValue> value = reader.readValueAt(variableName, lon, lat, 0);

    assertTrue(value.isPresent(), "Value should be present at test coordinates");
    assertEquals(
        9.0,
        value.get().getAsDecimal().doubleValue(),
        0.0001,
        "Value at test coordinates does not match expected value"
    );
  }

  @Test
  public void testCanHandle() {
    assertTrue(reader.canHandle("test.csv"));
    assertTrue(reader.canHandle("TEST.CSV"));
  }
}
