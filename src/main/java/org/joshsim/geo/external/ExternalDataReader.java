package org.joshsim.geo.external;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.joshsim.engine.value.type.EngineValue;

/**
 * Interface for reading grid-based geospatial data from various external sources.
 */
public interface ExternalDataReader extends AutoCloseable {
  /**
   * Opens a data source from a path.
   *
   * @param sourcePath Path to the data source
   * @throws IOException If there's an error opening the source
   */
  void open(String sourcePath) throws IOException;

  /**
   * Gets the available variable names in the data source.
   *
   * @return List of variable names
   * @throws IOException If there's an error reading the data source
   */
  List<String> getVariableNames() throws IOException;

  /**
   * Gets the coordinate reference system code of the data source.
   *
   * @return String containing the CRS code
   * @throws IOException If there's an error reading the data source
   */
  String getCrsCode() throws IOException;

  /**
   * Gets the time dimension size if available.
   *
   * @return Optional containing the number of time steps, or empty if no time dimension
   * @throws IOException If there's an error reading the data source
   */
  Optional<Integer> getTimeDimensionSize() throws IOException;

  /**
   * Gets spatial dimensions of the data source.
   *
   * @return ExternalSpatialDimensions object containing coordinate information
   * @throws IOException If there's an error reading the data source
   */
  ExternalSpatialDimensions getSpatialDimensions() throws IOException;

  /**
   * Reads a value at a specific location and time step.
   *
   * @param variableName Name of the variable to read
   * @param x X coordinate in the data source's coordinate system
   * @param y Y coordinate in the data source's coordinate system
   * @param timeStep Time step index (ignored if data has no time dimension)
   * @return Optional EngineValue if value exists at coordinates, empty otherwise
   * @throws IOException If there's an error reading the data source
   */
  Optional<EngineValue> readValueAt(
      String variableName,
      BigDecimal x,
      BigDecimal y,
      int timeStep) throws IOException;

  /**
   * Checks if this reader can handle the given file format.
   *
   * @param filePath Path to the data file
   * @return true if this reader can handle the file format
   */
  boolean canHandle(String filePath);

  void setDimensions(String dimensionX, String dimensionY, Optional<String> dimensionTime);

  void setCrsCode(String crsCode);
}
