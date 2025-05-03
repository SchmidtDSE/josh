package org.joshsim.geo.external.readers;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.engine.value.type.EngineValue;
import org.joshsim.geo.external.ExternalDataReader;
import org.joshsim.geo.external.ExternalSpatialDimensions;

import thredds.client.catalog.Dataset;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;
import ucar.nc2.dataset.DatasetUrl;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.util.CancelTask;

/**
 * Implementation of ExternalDataReader for NetCDF files using UCAR's netcdf-java library.
 * This reader requires explicit dimension names to be set manually.
 */
public class NetcdfExternalDataReader implements ExternalDataReader {
  private NetcdfFile ncFile;
  private final EngineValueFactory valueFactory;
  private Optional<CancelTask> cancelTask = Optional.empty();

  // Dimension names
  private String dimNameX;
  private String dimNameY;
  private String dimNameTime;

  // Coordinate reference system (if applicable)
  private String crsCode;

  // Bounds
  private BigDecimal minX;
  private BigDecimal maxX;
  private BigDecimal minY;
  private BigDecimal maxY;
  private BigDecimal extendedMinX;
  private BigDecimal extendedMaxX;
  private BigDecimal extendedMinY;
  private BigDecimal extendedMaxY;
  private BigDecimal boundBuffer = new BigDecimal("0.1"); // 10% buffer by default

  /**
   * Constructs a NetcdfExternalDataReader with the specified value factory.
   *
   * @param valueFactory Factory for creating EngineValue objects
   */
  public NetcdfExternalDataReader(EngineValueFactory valueFactory) {
    this.valueFactory = valueFactory;
  }

  /**
   * Sets the cancel task for this reader.
   *
   * @param cancelTask The CancelTask to set
   */
  public void setCancelTask(CancelTask cancelTask) {
    this.cancelTask = Optional.of(cancelTask);
  }

  /**
   * Opens a NetCDF file from the specified source path.
   *
   * @param sourcePath The path to the NetCDF file to open.
   * @throws IOException If the file cannot be opened or an error occurs during opening.
   */
  public void open(String sourcePath) throws IOException {
    try {
      // ncFile = NetcdfFiles.open(sourcePath);
      DatasetUrl datasetUrl = DatasetUrl.findDatasetUrl(sourcePath);
      ncFile = NetcdfDatasets.acquireFile(datasetUrl, cancelTask.orElse(null));
    } catch (Exception e) {
      throw new IOException("Failed to open NetCDF file: " + e.getMessage(), e);
    }
  }

  /**
   * Sets the dimension names explicitly. This must be called after opening
   * the file and before performing any operations that require dimension information.
   *
   * @param dimensionX The name of the X dimension
   * @param dimensionY The name of the Y dimension
   * @param timeDimension The name of the time dimension (can be null)
   */
  public void setDimensions(String dimensionX, String dimensionY, Optional<String> timeDimension) {
    try {
      if (ncFile == null) {
        throw new IOException("NetCDF file not opened yet");
      }

      // Verify dimensions exist
      if (ncFile.findVariable(dimensionX) == null) {
        throw new IOException("X dimension variable not found: " + dimensionX);
      }

      if (ncFile.findVariable(dimensionY) == null) {
        throw new IOException("Y dimension variable not found: " + dimensionY);
      }

      if (timeDimension.isPresent() && ncFile.findVariable(timeDimension.get()) == null) {
        throw new IOException("Time dimension variable not found: " + timeDimension.get());
      }

      this.dimNameX = dimensionX;
      this.dimNameY = dimensionY;
      this.dimNameTime = timeDimension.orElse(null);

      // Initialize bounds after setting dimensions
      initializeBounds();
    } catch (IOException e) {
      throw new RuntimeException("Failed to set dimensions: " + e.getMessage(), e);
    }
  }

  /**
   * Sets the CRS explicitly.
   *
   * @param crsCode The coordinate reference system identifier
   */
  public void setCrsCode(String crsCode) {
    this.crsCode = crsCode;

    // If bounds have been initialized, recalculate extended bounds
    // as they may be affected by CRS constraints (e.g., for WGS84)
    if (minX != null && maxX != null && minY != null && maxY != null) {
      calculateExtendedBounds();
    }
  }

  /**
   * Gets the CRS.
   *
   * @return The coordinate reference system identifier
   */
  public String getCrsCode() {
    return crsCode;
  }

  /**
   * Retrieves the names of all numeric data variables in the NetCDF file
   * that are not coordinate variables and have at least two dimensions.
   *
   * @return A list of variable names.
   * @throws IOException If the NetCDF file is not open or an error occurs while reading.
   */
  public List<String> getVariableNames() throws IOException {
    checkFileOpen();
    ensureDimensionsSet();

    // Get coordinate variable names for exclusion
    List<String> coordVars = new ArrayList<>();
    if (dimNameX != null) {
      coordVars.add(dimNameX);
    }
    if (dimNameY != null) {
      coordVars.add(dimNameY);
    }
    if (dimNameTime != null) {
      coordVars.add(dimNameTime);
    }

    List<String> result = new ArrayList<>();

    for (Variable var : ncFile.getVariables()) {
      // Only include numeric data variables that aren't coordinate variables
      if (!coordVars.contains(var.getFullName())
          && var.getDataType().isNumeric()
          && var.getRank() >= 2) { // Must have at least 2 dimensions (X and Y)
        result.add(var.getFullName());
      }
    }

    return result;
  }

  /**
   * Retrieves the size of the time dimension if it is set and exists in the NetCDF file.
   *
   * <p>@return An Optional containing the size of the time dimension, or an empty Optional if
   * the time dimension is not set or does not exist.</p>
   *
   * @throws IOException If the NetCDF is not open or an error occurs while accessing dimension.
   */
  public Optional<Integer> getTimeDimensionSize() throws IOException {
    checkFileOpen();
    ensureDimensionsSet();

    if (dimNameTime == null) {
      return Optional.empty();
    }

    Dimension timeDim = ncFile.findDimension(dimNameTime);
    if (timeDim == null) {
      return Optional.empty();
    }
    return Optional.of(timeDim.getLength());
  }

  /**
   * Retrieves the spatial dimensions (X, Y, and optionally time) from the NetCDF file.
   *
   * @return An ExternalSpatialDimensions object containing the spatial dimensions and their values.
   * @throws IOException If the NetCDF file is not open or an error occurs while reading dimensions.
   */
  public ExternalSpatialDimensions getSpatialDimensions() throws IOException {
    checkFileOpen();
    ensureDimensionsSet();

    try {
      // Get X and Y coordinate variables
      Variable varX = ncFile.findVariable(dimNameX);
      Variable varY = ncFile.findVariable(dimNameY);

      // Read coordinate values
      Array arrayX = varX.read();
      Array arrayY = varY.read();

      // Create lists instead of arrays
      List<BigDecimal> coordsX = new ArrayList<>((int) arrayX.getSize());
      List<BigDecimal> coordsY = new ArrayList<>((int) arrayY.getSize());

      // Convert to BigDecimal lists
      for (int i = 0; i < arrayX.getSize(); i++) {
        coordsX.add(new BigDecimal(arrayX.getDouble(i)).setScale(6, RoundingMode.HALF_UP));
      }

      for (int i = 0; i < arrayY.getSize(); i++) {
        coordsY.add(new BigDecimal(arrayY.getDouble(i)).setScale(6, RoundingMode.HALF_UP));
      }

      // Use explicitly set CRS
      String effectiveCrs = crsCode;

      return new ExternalSpatialDimensions(
          dimNameX,
          dimNameY,
          dimNameTime,
          effectiveCrs,
          coordsX,
          coordsY);

    } catch (Exception e) {
      throw new IOException("Failed to read spatial dimensions: " + e.getMessage(), e);
    }
  }

  /**
   * Reads a value from the specified variable at the given spatial coordinates and time step.
   *
   * @param variableName The name of the variable to read.
   * @param x The X coordinate as a BigDecimal.
   * @param y The Y coordinate as a BigDecimal.
   * @param timeStep The time step index.
   * @return An Optional containing the EngineValue if found, or an empty Optional if not.
   * @throws IOException If an error occurs while reading the value.
   */
  public Optional<EngineValue> readValueAt(
      String variableName, BigDecimal x, BigDecimal y, int timeStep) throws IOException {
    checkFileOpen();
    ensureDimensionsSet();

    try {
      Variable var = ncFile.findVariable(variableName);
      if (var == null) {
        return Optional.empty();
      }

      // Check if coordinates are within extended bounds
      if (extendedMinX != null && extendedMaxX != null
          && extendedMinY != null && extendedMaxY != null) {
        if (x.compareTo(extendedMinX) < 0 || x.compareTo(extendedMaxX) > 0
            || y.compareTo(extendedMinY) < 0 || y.compareTo(extendedMaxY) > 0) {
          return Optional.empty(); // Coordinates outside extended bounds
        }
      }

      // Get dimensions
      ExternalSpatialDimensions dimensions = getSpatialDimensions();

      // Find indices of closest X and Y coordinates
      int indexX = dimensions.findClosestIndexX(x);
      int indexY = dimensions.findClosestIndexY(y);

      if (indexX < 0 || indexY < 0) {
        return Optional.empty();
      }

      // Determine the shape of the variable
      final int[] shape = var.getShape();
      final int rank = var.getRank();

      // Find the index positions of X, Y, and time dimensions
      List<Dimension> varDims = var.getDimensions();
      int dimIdxX = -1;
      int dimIdxY = -1;
      int timeDimIdx = -1;
      for (int i = 0; i < varDims.size(); i++) {
        Dimension dim = varDims.get(i);
        if (dim.getShortName().equals(dimNameX) || dim.getName().equals(dimNameX)) {
          dimIdxX = i;
        } else if (dim.getShortName().equals(dimNameY) || dim.getName().equals(dimNameY)) {
          dimIdxY = i;
        } else if (dimNameTime != null
            && (dim.getShortName().equals(dimNameTime) || dim.getName().equals(dimNameTime))) {
          timeDimIdx = i;
        }
      }

      // Check if we found the dimensions in this variable
      if (dimIdxX < 0 || dimIdxY < 0) {
        return Optional.empty(); // Can't locate dimensions in this variable
      }

      // Check if we don't have the time dimension
      if (dimNameTime != null && timeStep < 0) {
        return Optional.empty();
      }

      // If we have a time dimension, validate the time step explicitly
      if (dimNameTime != null) {
        Optional<Integer> timeSize = getTimeDimensionSize();
        if (timeSize.isPresent() && timeStep >= timeSize.get()) {
          return Optional.empty(); // Time step is out of bounds
        }
      }

      // Create the section specification for reading just the value we want
      int[] origin = new int[rank];
      int[] size = new int[rank];

      for (int i = 0; i < rank; i++) {
        if (i == dimIdxX) {
          origin[i] = indexX;
          size[i] = 1;
        } else if (i == dimIdxY) {
          origin[i] = indexY;
          size[i] = 1;
        } else if (i == timeDimIdx) {
          // Use the provided time step if the variable has a time dimension
          if (timeStep >= 0 && timeStep < shape[i]) {
            origin[i] = timeStep;
          } else {
            return Optional.empty(); // Time step out of bounds
          }
          size[i] = 1;
        } else {
          // For other dimensions, read the first element only
          origin[i] = 0;
          size[i] = 1;
        }
      }

      // Read the value from the file
      Array data;
      try {
        data = var.read(origin, size).reduce();
      } catch (InvalidRangeException e) {
        return Optional.empty(); // Requested coordinates are invalid
      }

      if (data.getSize() != 1) {
        return Optional.empty(); // Expected a single value
      }

      // Get the value and check for missing/fill values
      double value = data.getDouble(0);

      // Check for NaN or missing value
      if (Double.isNaN(value)) {
        return Optional.empty();
      }

      // Check for _FillValue or missing_value attribute
      Attribute fillValueAttr = var.findAttribute("_FillValue");
      if (fillValueAttr != null && value == fillValueAttr.getNumericValue().doubleValue()) {
        return Optional.empty();
      }

      Attribute missingValueAttr = var.findAttribute("missing_value");
      if (missingValueAttr != null && value == missingValueAttr.getNumericValue().doubleValue()) {
        return Optional.empty();
      }

      // Get the unit if available
      String unit = null;
      Attribute unitAttr = var.findAttribute("units");
      if (unitAttr != null) {
        unit = unitAttr.getStringValue();
      }

      // Create units and BigDecimal value
      Units units = new Units(unit);
      BigDecimal bigDecimalValue = new BigDecimal(value).setScale(6, RoundingMode.HALF_UP);

      // Create an EngineValue with the result
      EngineValue result = valueFactory.build(bigDecimalValue, units);
      return Optional.of(result);

    } catch (Exception e) {
      throw new IOException("Failed to read value: " + e.getMessage(), e);
    }
  }

  /**
   * Initializes the coordinate bounds from coordinate arrays.
   * This is called automatically when dimensions are set.
   */
  private void initializeBounds() {
    try {
      // Get X and Y coordinate variables
      Variable varX = ncFile.findVariable(dimNameX);
      Variable varY = ncFile.findVariable(dimNameY);

      boolean boundsFound = false;

      // Try to get bounds from CF convention attributes
      boundsFound = extractBoundsFromAttribute(varX, varY, "valid_range");

      // Alternative: look for actual_range attribute
      if (!boundsFound) {
        boundsFound = extractBoundsFromAttribute(varX, varY, "actual_range");
      }

      // If bounds not found in metadata, calculate from coordinate arrays
      if (!boundsFound) {
        calculateBoundsFromArrays(varX, varY);
      }

      // Calculate extended bounds with buffer
      calculateExtendedBounds();

    } catch (Exception e) {
      // If bounds initialization fails, clear all bounds
      resetAllBounds();
    }
  }

  /**
   * Tries to extract bounds from a specific attribute in the coordinate variables.
   *
   * @param varX The X coordinate variable
   * @param varY The Y coordinate variable
   * @param attributeName The attribute name to check for bounds
   * @return true if bounds were successfully extracted, false otherwise
   */
  private boolean extractBoundsFromAttribute(Variable varX, Variable varY, String attributeName) {
    Attribute boundsX = varX.findAttribute(attributeName);
    Attribute boundsY = varY.findAttribute(attributeName);

    if (boundsX != null && boundsY != null && boundsX.isArray() && boundsY.isArray()) {
      try {
        minX = new BigDecimal(boundsX.getValues().getDouble(0))
            .setScale(6, RoundingMode.HALF_UP);
        maxX = new BigDecimal(boundsX.getValues().getDouble(1))
            .setScale(6, RoundingMode.HALF_UP);
        minY = new BigDecimal(boundsY.getValues().getDouble(0))
            .setScale(6, RoundingMode.HALF_UP);
        maxY = new BigDecimal(boundsY.getValues().getDouble(1))
            .setScale(6, RoundingMode.HALF_UP);
        return true;
      } catch (Exception e) {
        // Couldn't parse bounds from attributes
        return false;
      }
    }
    return false;
  }

  /**
   * Calculates bounds by analyzing the values in coordinate arrays.
   *
   * @param varX The X coordinate variable
   * @param varY The Y coordinate variable
   * @throws IOException If there is an error reading the data
   */
  private void calculateBoundsFromArrays(Variable varX, Variable varY) throws IOException {
    // Read coordinate values
    Array arrayX = varX.read();
    Array arrayY = varY.read();

    // Find min and max values
    double valMinX = Double.MAX_VALUE;
    double valMaxX = -Double.MAX_VALUE;
    double valMinY = Double.MAX_VALUE;
    double valMaxY = -Double.MAX_VALUE;

    for (int i = 0; i < arrayX.getSize(); i++) {
      double val = arrayX.getDouble(i);
      if (val < valMinX) {
        valMinX = val;
      }
      if (val > valMaxX) {
        valMaxX = val;
      }
    }

    for (int i = 0; i < arrayY.getSize(); i++) {
      double val = arrayY.getDouble(i);
      if (val < valMinY) {
        valMinY = val;
      }
      if (val > valMaxY) {
        valMaxY = val;
      }
    }

    minX = new BigDecimal(valMinX).setScale(6, RoundingMode.HALF_UP);
    maxX = new BigDecimal(valMaxX).setScale(6, RoundingMode.HALF_UP);
    minY = new BigDecimal(valMinY).setScale(6, RoundingMode.HALF_UP);
    maxY = new BigDecimal(valMaxY).setScale(6, RoundingMode.HALF_UP);
  }

  /**
   * Calculates extended bounds by applying a buffer percentage to the actual bounds.
   * Extended bounds create a small margin outside the actual data extent for better queries.
   */
  private void calculateExtendedBounds() {
    // Skip if bounds weren't properly initialized
    if (minX == null || maxX == null || minY == null || maxY == null) {
      return;
    }

    // Apply special case for WGS84 coordinates
    if (crsCode != null && crsCode.equals("EPSG:4326")) {
      // For WGS84, enforce global bounds if needed
      if (minX.compareTo(new BigDecimal("-180")) < 0) {
        minX = new BigDecimal("-180");
      }
      if (maxX.compareTo(new BigDecimal("180")) > 0) {
        maxX = new BigDecimal("180");
      }
      if (minY.compareTo(new BigDecimal("-90")) < 0) {
        minY = new BigDecimal("-90");
      }
      if (maxY.compareTo(new BigDecimal("90")) > 0) {
        maxY = new BigDecimal("90");
      }
    }

    // Calculate extended bounds with buffer
    BigDecimal rangeX = maxX.subtract(minX);
    BigDecimal rangeY = maxY.subtract(minY);
    BigDecimal bufferX = rangeX.multiply(boundBuffer);
    BigDecimal bufferY = rangeY.multiply(boundBuffer);

    extendedMinX = minX.subtract(bufferX);
    extendedMaxX = maxX.add(bufferX);
    extendedMinY = minY.subtract(bufferY);
    extendedMaxY = maxY.add(bufferY);
  }

  /**
   * Resets all bounds variables to null.
   */
  private void resetAllBounds() {
    minX = null;
    maxX = null;
    minY = null;
    maxY = null;
    extendedMinX = null;
    extendedMaxX = null;
    extendedMinY = null;
    extendedMaxY = null;
  }

  /**
   * Sets the buffer percentage and recalculates extended bounds if bounds are set.
   *
   * @param bufferPercent Buffer as decimal percentage (0.1 = 10%)
   */
  public void setBoundsBuffer(BigDecimal bufferPercent) {
    this.boundBuffer = bufferPercent;
    calculateExtendedBounds(); // Recalculate the extended bounds with the new buffer
  }

  /**
   * Gets the minimum X coordinate of the dataset.
   *
   * @return Minimum X value or null if bounds not initialized
   */
  public BigDecimal getMinX() {
    return minX;
  }

  /**
   * Gets the maximum X coordinate of the dataset.
   *
   * @return Maximum X value or null if bounds not initialized
   */
  public BigDecimal getMaxX() {
    return maxX;
  }

  /**
   * Gets the minimum Y coordinate of the dataset.
   *
   * @return Minimum Y value or null if bounds not initialized
   */
  public BigDecimal getMinY() {
    return minY;
  }

  /**
   * Gets the maximum Y coordinate of the dataset.
   *
   * @return Maximum Y value or null if bounds not initialized
   */
  public BigDecimal getMaxY() {
    return maxY;
  }

  /**
   * Gets the extended minimum X coordinate of the dataset.
   *
   * @return Extended minimum X value or null if bounds not initialized
   */
  public BigDecimal getExtendedMinX() {
    return extendedMinX;
  }

  /**
   * Gets the extended maximum X coordinate of the dataset.
   *
   * @return Extended maximum X value or null if bounds not initialized
   */
  public BigDecimal getExtendedMaxX() {
    return extendedMaxX;
  }

  /**
   * Gets the extended minimum Y coordinate of the dataset.
   *
   * @return Extended minimum Y value or null if bounds not initialized
   */
  public BigDecimal getExtendedMinY() {
    return extendedMinY;
  }

  /**
   * Gets the extended maximum Y coordinate of the dataset.
   *
   * @return Extended maximum Y value or null if bounds not initialized
   */
  public BigDecimal getExtendedMaxY() {
    return extendedMaxY;
  }

  /**
   * Checks if the NetCDF file has been opened, throwing an exception if not.
   *
   * @throws IOException if the file has not been opened yet
   */
  private void checkFileOpen() throws IOException {
    if (ncFile == null) {
      throw new IOException("NetCDF file not opened. Call open() first.");
    }
  }

  /**
   * Ensures that dimension names have been set, throwing an exception if not.
   *
   * @throws IOException if dimensions have not been set
   */
  private void ensureDimensionsSet() throws IOException {
    if (dimNameX == null || dimNameY == null) {
      throw new IOException(
          "Spatial dimensions not set. Call setDimensions() to set them explicitly.");
    }
  }

  /**
   * Closes the NetCDF file and releases resources.
   *
   * @throws Exception if closing the file fails
   */
  @Override
  public void close() throws Exception {
    if (ncFile != null) {
      try {
        ncFile.close();
      } finally {
        ncFile = null;
        // Reset state variables when closing
        resetAllBounds();
        dimNameX = null;
        dimNameY = null;
        dimNameTime = null;
        crsCode = null;
      }
    }
  }

  /**
   * Determines whether this reader can handle the given file based on its file extension.
   *
   * @param filePath the path to the file
   * @return true if the file extension indicates a NetCDF file, false otherwise
   */
  @Override
  public boolean canHandle(String filePath) {
    if (filePath == null || filePath.isEmpty()) {
      return false;
    }

    String lowerPath = filePath.toLowerCase();
    return lowerPath.endsWith(".nc")
           || lowerPath.endsWith(".ncf")
           || lowerPath.endsWith(".netcdf")
           || lowerPath.endsWith(".nc4");
  }
}
