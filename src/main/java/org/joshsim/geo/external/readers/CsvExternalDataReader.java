/**
 * Implementation of an external data reader which uses CSV files.
 *
 *
 * @license BSD-3-Clause
 */

package org.joshsim.geo.external.readers;

import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.engine.value.type.EngineValue;
import org.joshsim.geo.external.ExternalDataReader;
import org.joshsim.geo.external.ExternalSpatialDimensions;

/**
 * Create a new external data reader which parses CSV files.
 *
 * <p>Implementation of the ExternalDataReader interface for reading grid-based geospatial data from
 * CSV files. This class is specifically designed to handle CSV files containing spatial coordinate
 * data (longitude and latitude) and optional variable-based data columns.</p>
 */
public class CsvExternalDataReader implements ExternalDataReader {
  private final EngineValueFactory valueFactory;
  private String crsCode = "EPSG:4326"; // Default to WGS84
  private final Map<String, List<BigDecimal>> data = new HashMap<>();
  private boolean isOpen = false;

  /**
   * Constructs a new instance of CsvExternalDataReader.
   *
   * @param valueFactory an instance of EngineValueFactory used to create EngineValue objects.
   */
  public CsvExternalDataReader(EngineValueFactory valueFactory) {
    this.valueFactory = valueFactory;
  }

  @Override
  public void open(String sourcePath) throws IOException {
    try (FileReader reader = new FileReader(sourcePath);
         CSVParser parser = CSVFormat.DEFAULT.builder()
             .setHeader()
             .setSkipHeaderRecord(true)
             .build()
             .parse(reader)) {

      // Initialize columns
      for (String header : parser.getHeaderNames()) {
        data.put(header, new ArrayList<>());
      }

      // Validate required columns
      if (!data.containsKey("longitude") || !data.containsKey("latitude")) {
        throw new IOException("CSV must contain 'longitude' and 'latitude' columns");
      }

      // Parse records
      for (CSVRecord record : parser) {
        for (String header : parser.getHeaderNames()) {
          try {
            BigDecimal value = new BigDecimal(record.get(header));
            data.get(header).add(value);
          } catch (NumberFormatException e) {
            throw new RuntimeException(
                String.format("Invalid numeric value in column '%s': %s",
                    header, record.get(header)));
          }
        }
      }
      isOpen = true;
    }
  }

  @Override
  public List<String> getVariableNames() throws IOException {
    checkOpen();
    List<String> variables = new ArrayList<>(data.keySet());
    variables.remove("longitude");
    variables.remove("latitude");
    return variables;
  }

  @Override
  public String getCrsCode() throws IOException {
    return crsCode;
  }

  @Override
  public Optional<Integer> getTimeDimensionSize() throws IOException {
    return Optional.empty(); // CSV implementation doesn't support time dimension
  }

  @Override
  public ExternalSpatialDimensions getSpatialDimensions() throws IOException {
    checkOpen();
    return new ExternalSpatialDimensions(
        "longitude",
        "latitude",
        null,
        crsCode,
        data.get("longitude"),
        data.get("latitude")
    );
  }

  @Override
  public Optional<EngineValue> readValueAt(
      String variableName, BigDecimal x, BigDecimal y, int timeStep) throws IOException {
    checkOpen();
    if (!data.containsKey(variableName)) {
      return Optional.empty();
    }

    // Find closest point
    int closestIndex = -1;
    BigDecimal closestDistance = null;

    List<BigDecimal> lons = data.get("longitude");
    List<BigDecimal> lats = data.get("latitude");

    for (int i = 0; i < lons.size(); i++) {
      BigDecimal lon = lons.get(i);
      BigDecimal lat = lats.get(i);

      // Simple Euclidean distance (could be enhanced for better geographic accuracy)
      BigDecimal dx = x.subtract(lon);
      BigDecimal dy = y.subtract(lat);
      BigDecimal distance = dx.multiply(dx).add(dy.multiply(dy));

      if (closestDistance == null || distance.compareTo(closestDistance) < 0) {
        closestDistance = distance;
        closestIndex = i;
      }
    }

    if (closestIndex == -1) {
      return Optional.empty();
    }

    return Optional.of(valueFactory.build(data.get(variableName).get(closestIndex), Units.EMPTY));
  }

  @Override
  public boolean canHandle(String filePath) {
    return filePath != null && filePath.toLowerCase().endsWith(".csv");
  }

  @Override
  public void setDimensions(String dimensionX, String dimensionY, Optional<String> dimensionTime) {
    // CSV implementation uses fixed dimension names
  }

  @Override
  public void setCrsCode(String crsCode) {
    this.crsCode = crsCode;
  }

  @Override
  public void close() throws Exception {
    data.clear();
    isOpen = false;
  }

  private void checkOpen() throws IOException {
    if (!isOpen) {
      throw new IOException("CSV file not opened. Call open() first.");
    }
  }
}
