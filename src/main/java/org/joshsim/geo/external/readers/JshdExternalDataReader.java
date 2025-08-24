package org.joshsim.geo.external.readers;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.engine.value.type.EngineValue;
import org.joshsim.geo.external.ExternalDataReader;
import org.joshsim.geo.external.ExternalSpatialDimensions;
import org.joshsim.precompute.DoublePrecomputedGrid;
import org.joshsim.precompute.JshdUtil;

/**
 * Implementation of ExternalDataReader for JSHD files.
 * This reader provides access to precomputed grid data stored in JSHD binary format.
 */
public class JshdExternalDataReader implements ExternalDataReader {
  private DoublePrecomputedGrid grid;
  private final EngineValueFactory valueFactory;
  private String sourcePath;
  
  // Dimension names - JSHD files use grid coordinates
  private String dimNameX = "x";
  private String dimNameY = "y"; 
  private String dimNameTime = "time";
  
  // CRS code - JSHD files typically work in grid space
  private String crsCode = "EPSG:4326"; // Default, can be overridden

  /**
   * Constructs a JshdExternalDataReader with the specified value factory.
   *
   * @param valueFactory Factory for creating EngineValue objects
   */
  public JshdExternalDataReader(EngineValueFactory valueFactory) {
    this.valueFactory = valueFactory;
  }

  /**
   * Opens a JSHD file from the specified source path.
   *
   * @param sourcePath The path to the JSHD file to open
   * @throws IOException If the file cannot be opened or an error occurs during opening
   */
  @Override
  public void open(String sourcePath) throws IOException {
    this.sourcePath = sourcePath;
    
    try {
      Path path = Paths.get(sourcePath);
      byte[] fileBytes = Files.readAllBytes(path);
      this.grid = JshdUtil.loadFromBytes(valueFactory, fileBytes);
    } catch (Exception e) {
      throw new IOException("Failed to open JSHD file: " + sourcePath + " - " + e.getMessage(), e);
    }
  }

  /**
   * Gets the available variable names in the JSHD file.
   * For JSHD files, there is typically one variable representing the grid data.
   *
   * @return List containing a single variable name "data"
   * @throws IOException If there's an error reading the data source
   */
  @Override
  public List<String> getVariableNames() throws IOException {
    checkFileOpen();
    List<String> variables = new ArrayList<>();
    variables.add("data");
    return variables;
  }

  /**
   * Gets the coordinate reference system code of the JSHD file.
   *
   * @return String containing the CRS code
   * @throws IOException If there's an error reading the data source
   */
  @Override
  public String getCrsCode() throws IOException {
    return crsCode;
  }

  /**
   * Gets the time dimension size if available.
   *
   * @return Optional containing the number of time steps
   * @throws IOException If there's an error reading the data source
   */
  @Override
  public Optional<Integer> getTimeDimensionSize() throws IOException {
    checkFileOpen();
    int timeSteps = (int) (grid.getMaxTimestep() - grid.getMinTimestep() + 1);
    return Optional.of(timeSteps);
  }

  /**
   * Gets spatial dimensions of the JSHD data source.
   *
   * @return ExternalSpatialDimensions object containing coordinate information
   * @throws IOException If there's an error reading the data source
   */
  @Override
  public ExternalSpatialDimensions getSpatialDimensions() throws IOException {
    checkFileOpen();

    // Create coordinate lists for X and Y dimensions
    List<BigDecimal> coordsX = new ArrayList<>();
    List<BigDecimal> coordsY = new ArrayList<>();

    // JSHD files store grid coordinates as integer ranges
    // Convert to BigDecimal lists
    for (long x = grid.getMinX(); x <= grid.getMaxX(); x++) {
      coordsX.add(new BigDecimal(x).setScale(6, RoundingMode.HALF_UP));
    }

    for (long y = grid.getMinY(); y <= grid.getMaxY(); y++) {
      coordsY.add(new BigDecimal(y).setScale(6, RoundingMode.HALF_UP));
    }

    return new ExternalSpatialDimensions(
        dimNameX,
        dimNameY,
        dimNameTime,
        crsCode,
        coordsX,
        coordsY);
  }

  /**
   * Reads a value from the JSHD grid at the given spatial coordinates and time step.
   *
   * @param variableName The name of the variable to read (ignored for JSHD, always "data")
   * @param x The X coordinate as a BigDecimal (grid coordinate)
   * @param y The Y coordinate as a BigDecimal (grid coordinate)
   * @param timeStep The time step index
   * @return An Optional containing the EngineValue if found, or an empty Optional if not
   * @throws IOException If an error occurs while reading the value
   */
  @Override
  public Optional<EngineValue> readValueAt(
      String variableName, BigDecimal x, BigDecimal y, int timeStep) throws IOException {
    checkFileOpen();

    try {
      // Convert coordinates to long values for grid access
      long gridX = x.longValue();
      long gridY = y.longValue();
      
      // Check bounds
      if (gridX < grid.getMinX() || gridX > grid.getMaxX() 
          || gridY < grid.getMinY() || gridY > grid.getMaxY()) {
        return Optional.empty();
      }

      // Check time bounds
      long gridTimeStep = timeStep + grid.getMinTimestep();
      if (gridTimeStep < grid.getMinTimestep() || gridTimeStep > grid.getMaxTimestep()) {
        return Optional.empty();
      }

      // Get the value from the grid
      EngineValue value = grid.getAt(gridX, gridY, gridTimeStep);
      return Optional.of(value);

    } catch (Exception e) {
      throw new IOException("Failed to read value: " + e.getMessage(), e);
    }
  }

  /**
   * Checks if this reader can handle the given file format.
   *
   * @param filePath Path to the data file
   * @return true if this reader can handle the file format
   */
  @Override
  public boolean canHandle(String filePath) {
    if (filePath == null || filePath.isEmpty()) {
      return false;
    }
    return filePath.toLowerCase().endsWith(".jshd");
  }

  /**
   * Sets the spatial and optional time dimensions for the data source.
   * For JSHD files, dimensions are fixed but this allows for customization.
   *
   * @param dimensionX Name of the X dimension
   * @param dimensionY Name of the Y dimension
   * @param dimensionTime Optional name of the time dimension
   */
  @Override
  public void setDimensions(String dimensionX, String dimensionY, Optional<String> dimensionTime) {
    this.dimNameX = dimensionX;
    this.dimNameY = dimensionY;
    this.dimNameTime = dimensionTime.orElse("time");
  }

  /**
   * Sets the coordinate reference system code for the data source.
   *
   * @param crsCode String containing the CRS code
   */
  @Override
  public void setCrsCode(String crsCode) {
    this.crsCode = crsCode;
  }

  /**
   * Gets the minimum X coordinate of the grid.
   *
   * @return Minimum X value
   */
  public BigDecimal getMinX() {
    if (grid == null) {
      return null;
    }
    return new BigDecimal(grid.getMinX());
  }

  /**
   * Gets the maximum X coordinate of the grid.
   *
   * @return Maximum X value
   */
  public BigDecimal getMaxX() {
    if (grid == null) {
      return null;
    }
    return new BigDecimal(grid.getMaxX());
  }

  /**
   * Gets the minimum Y coordinate of the grid.
   *
   * @return Minimum Y value
   */
  public BigDecimal getMinY() {
    if (grid == null) {
      return null;
    }
    return new BigDecimal(grid.getMinY());
  }

  /**
   * Gets the maximum Y coordinate of the grid.
   *
   * @return Maximum Y value
   */
  public BigDecimal getMaxY() {
    if (grid == null) {
      return null;
    }
    return new BigDecimal(grid.getMaxY());
  }

  /**
   * Gets the underlying precomputed grid.
   *
   * @return The DoublePrecomputedGrid instance
   */
  public DoublePrecomputedGrid getGrid() {
    return grid;
  }

  /**
   * Checks if the JSHD file has been opened, throwing an exception if not.
   *
   * @throws IOException if the file has not been opened yet
   */
  private void checkFileOpen() throws IOException {
    if (grid == null) {
      throw new IOException("JSHD file not opened. Call open() first.");
    }
  }

  /**
   * Closes the JSHD reader and releases resources.
   *
   * @throws Exception if closing fails
   */
  @Override
  public void close() throws Exception {
    grid = null;
    sourcePath = null;
  }
}