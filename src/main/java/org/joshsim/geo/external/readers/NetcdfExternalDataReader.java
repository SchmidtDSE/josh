package org.joshsim.geo.external.readers;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.engine.value.type.EngineValue;
import org.joshsim.geo.external.ExternalDataReader;
import org.joshsim.geo.external.ExternalSpatialDimensions;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;

/**
 * Implementation of ExternalDataReader for NetCDF files using UCAR's netcdf-java library.
 * This reader explicitly avoids making assumptions about dimension names or coordinate systems.
 */
public class NetcdfExternalDataReader implements ExternalDataReader {
  private NetcdfFile ncFile;
  private String dimNameX;
  private String dimNameY;
  private String dimNameTime;
  private String crsCode;
  private final EngineValueFactory valueFactory;
  
  /**
   * Constructs a NetcdfExternalDataReader with the specified value factory.
   *
   * @param valueFactory Factory for creating EngineValue objects
   */
  public NetcdfExternalDataReader(EngineValueFactory valueFactory) {
    this.valueFactory = valueFactory;
  }
  
  public void open(String sourcePath) throws IOException {
    try {
      ncFile = NetcdfFiles.open(sourcePath);
    } catch (Exception e) {
      throw new IOException("Failed to open NetCDF file: " + e.getMessage(), e);
    }
  }

  /**
   * Sets the dimension names explicitly.
   *
   * @param dimensionX The name of the X dimension
   * @param dimensionY The name of the Y dimension
   * @param timeDimension The name of the time dimension (can be null)
   * @throws IOException If the specified dimensions don't exist in the file
   */
  public void setDimensions(String dimensionX, String dimensionY, String timeDimension) 
      throws IOException {
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
    
    if (timeDimension != null && ncFile.findVariable(timeDimension) == null) {
      throw new IOException("Time dimension variable not found: " + timeDimension);
    }
    
    this.dimNameX = dimensionX;
    this.dimNameY = dimensionY;
    this.dimNameTime = timeDimension;
  }
  
  /**
   * Sets the CRS explicitly.
   *
   * @param crs The coordinate reference system identifier
   */
  public void setCrsCode(String crsCode) {
    this.crsCode = crsCode;
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
   * Attempts to detect spatial dimensions from file metadata.
   *
   * @return true if dimensions were successfully detected, false otherwise
   */
  public boolean detectSpatialDimensions() {
    boolean success = false;
    
    // Look for variables with standard axis attributes
    for (Variable var : ncFile.getVariables()) {
      Attribute axisAttr = var.findAttribute("axis");
      if (axisAttr != null) {
        String axisVal = axisAttr.getStringValue();
        if ("X".equalsIgnoreCase(axisVal)) {
          dimNameX = var.getFullName();
          success = true;
        } else if ("Y".equalsIgnoreCase(axisVal)) {
          dimNameY = var.getFullName();
          success = true;
        } else if ("T".equalsIgnoreCase(axisVal)) {
          dimNameTime = var.getFullName();
        }
      }
      
      // CF convention: standard_name attribute
      Attribute stdNameAttr = var.findAttribute("standard_name");
      if (stdNameAttr != null) {
        String stdName = stdNameAttr.getStringValue();
        if ("longitude".equals(stdName) && dimNameX == null) {
          dimNameX = var.getFullName();
          success = true;
        } else if ("latitude".equals(stdName) && dimNameY == null) {
          dimNameY = var.getFullName();
          success = true;
        } else if ("time".equals(stdName) && dimNameTime == null) {
          dimNameTime = var.getFullName();
        }
      }
    }
    
    // Try to detect CRS from file metadata (only if unambiguous)
    if (crsCode == null) {
      // Check for explicit CRS variable
      for (Variable var : ncFile.getVariables()) {
        Attribute gridMappingName = var.findAttribute("grid_mapping_name");
        if (gridMappingName != null) {
          // Check if this has an EPSG code
          Attribute epsgAttr = var.findAttribute("epsg_code");
          if (epsgAttr != null) {
            crsCode = "EPSG:" + epsgAttr.getNumericValue();
            break;
          }
        }
      }
    }
    
    return success && dimNameX != null && dimNameY != null;
  }

  public List<String> getVariableNames() throws IOException {
    checkFileOpen();
    
    // Get coordinate variable names for exclusion
    List<String> coordVars = new ArrayList<>();
    if (dimNameX != null) {
      coordVars.add(dimNameX);
    }
    if (dimNameY != null) coordVars.add(dimNameY);
    if (dimNameTime != null) coordVars.add(dimNameTime);
    List<String> result = new ArrayList<>();

    for (Variable var : ncFile.getVariables()) {
      // Only include numeric data variables that aren't coordinate variables
      if (!coordVars.contains(var.getFullName()) && 
          var.getDataType().isNumeric() &&
          var.getRank() >= 2) { // Must have at least 2 dimensions (X and Y)
        result.add(var.getFullName());
      }
    }
    
    return result;
  }

  public Optional<Integer> getTimeDimensionSize() throws IOException {
    checkFileOpen();
    if (dimNameTime == null) {
      return Optional.empty();
    }
    
    Dimension timeDim = ncFile.findDimension(dimNameTime);
    return Optional.of(timeDim.getLength());
  }

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
      
      String effectiveCrs = crsCode; // Use explicitly set CRS
      
      // If no CRS was explicitly set or detected, it will remain null
      // The caller is responsible for handling this case appropriately
      
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

  public Optional<EngineValue> readValueAt(
      String variableName, BigDecimal x, BigDecimal y, int timeStep) throws IOException {
    checkFileOpen();
    ensureDimensionsSet();
    
    try {
      Variable var = ncFile.findVariable(variableName);
      if (var == null) {
        return Optional.empty();
      }
      
      // Get dimensions
      ExternalSpatialDimensions dimensions = getSpatialDimensions();
      
      // Find indices of closest X and Y coordinates
      int indexX = dimensions.findClosestIndexX(x);
      int indexY = dimensions.findClosestIndexY(y);
      
      if (indexX < 0 || indexY < 0) {
        return Optional.empty();
      }
      
      // Find origin for reading data and verify time dimension is valid
      int[] originAndShape = findOriginAndShape(var, indexX, indexY, timeStep);
      if (originAndShape == null) {
        return Optional.empty(); // Time step out of bounds or other issue
      }
      
      int[] origin = Arrays.copyOfRange(originAndShape, 0, var.getRank());
      int[] shape = Arrays.copyOfRange(originAndShape, var.getRank(), originAndShape.length);
      
      // Read the single data point
      Array data = var.read(origin, shape);
      double value = data.getDouble(0);
      
      // Validate the value (check for missing values or NaN)
      if (!isValidValue(var, value)) {
        return Optional.empty();
      }
      
      // Extract units and create EngineValue
      org.joshsim.engine.value.converter.Units units = extractUnits(var);
      return Optional.of(valueFactory.build(new BigDecimal(value), units));
      
    } catch (Exception e) {
      throw new IOException("Failed to read value: " + e.getMessage(), e);
    }
  }
  
  /**
   * Extracts units information from variable metadata.
   *
   * @param var The NetCDF variable
   * @return Units object representing the variable's units
   */
  private Units extractUnits(Variable var) {
    Attribute unitsAttr = var.findAttribute("units");
    if (unitsAttr != null && unitsAttr.isString()) {
      return new org.joshsim.engine.value.converter.Units(unitsAttr.getStringValue());
    }
    
    // Try alternate attribute names
    String[] unitAttributeNames = {"unit", "Unit", "UNITS"};
    for (String attrName : unitAttributeNames) {
      Attribute attr = var.findAttribute(attrName);
      if (attr != null && attr.isString()) {
        return new org.joshsim.engine.value.converter.Units(attr.getStringValue());
      }
    }
    
    return Units.EMPTY;
  }
  
  /**
   * Determines if a value is valid (not NaN or a fill/missing value).
   *
   * @param var The NetCDF variable
   * @param value The value to check
   * @return true if the value is valid, false otherwise
   */
  private boolean isValidValue(Variable var, double value) {
    // Check for NaN
    if (Double.isNaN(value)) {
      return false;
    }
    
    // Check for fill/missing values
    String[] fillValueAttrs = {"_FillValue", "missing_value", "FillValue"};
    for (String attrName : fillValueAttrs) {
      Attribute attr = var.findAttribute(attrName);
      if (attr != null) {
        try {
          double fillValue = attr.getNumericValue().doubleValue();
          if (Math.abs(value - fillValue) < 1e-10) {
            return false;
          }
        } catch (Exception e) {
          // Attribute is not numeric or cannot be converted to double, skip it
        }
      }
    }
    
    return true;
  }
  
  /**
   * Finds the origin and shape arrays for reading data from a variable.
   *
   * @param var The NetCDF variable
   * @param indexX The index for the X dimension
   * @param indexY The index for the Y dimension
   * @param timeStep The time step index
   * @return An array containing origin and shape arrays concatenated, or null if invalid
   */
  private int[] findOriginAndShape(
        Variable var,
        int indexX,
        int indexY,
        int timeStep
  ) {
    List<Dimension> varDims = var.getDimensions();
    int[] origin = new int[var.getRank()];
    int[] shape = new int[var.getRank()];
    Arrays.fill(shape, 1); // We're reading a single point
    
    // Map the variable dimensions to the X, Y, and time dimensions
    for (int d = 0; d < var.getRank(); d++) {
      Dimension dim = varDims.get(d);
      if (dim.getShortName().equals(dimNameX)) {
        origin[d] = indexX;
      } else if (dim.getShortName().equals(dimNameY)) {
        origin[d] = indexY;
      } else if (dimNameTime != null && dim.getShortName().equals(dimNameTime)) {
        // Only set time step if we have a time dimension and it's part of this variable
        if (timeStep >= dim.getLength()) {
          return null; // Time step out of bounds
        }
        origin[d] = timeStep;
      }
    }
    
    // Concatenate origin and shape into a single array for return
    int[] result = new int[origin.length + shape.length];
    System.arraycopy(origin, 0, result, 0, origin.length);
    System.arraycopy(shape, 0, result, origin.length, shape.length);
    
    return result;
  }
  
  public boolean canHandle(String filePath) {
    return filePath.toLowerCase().endsWith(".nc") || 
           filePath.toLowerCase().endsWith(".ncf") || 
           filePath.toLowerCase().endsWith(".netcdf") || 
           filePath.toLowerCase().endsWith(".nc4");
  }

  public void close() throws IOException {
    if (ncFile != null) {
      ncFile.close();
      ncFile = null;
    }
  }
  
  /**
   * Ensures the file is open before operations.
   *
   * @throws IOException If the file is not open
   */
  private void checkFileOpen() throws IOException {
    if (ncFile == null) {
      throw new IOException("NetCDF file not opened");
    }
  }
  
  /**
   * Ensures dimensions are set before operations that require them.
   *
   * @throws IOException If dimensions are not properly set
   */
  private void ensureDimensionsSet() throws IOException {
    if (dimNameX == null || dimNameY == null) {
      throw new IOException(
          "X and Y dimensions not set. Call setDimensions() or detectSpatialDimensions() first.");
    }
  }
}