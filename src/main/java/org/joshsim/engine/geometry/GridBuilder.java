package org.joshsim.engine.geometry;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.geometry.GeneralDirectPosition;
import org.apache.sis.referencing.CRS;
import org.joshsim.engine.entity.Patch;
import org.locationtech.spatial4j.context.SpatialContext;
import org.locationtech.spatial4j.context.SpatialContextFactory;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;

/**
 * This class is responsible for building grid structures.
 * It creates a rectangular grid of patches based on coordinates in any coordinate reference system,
 * converting them to the target CRS if needed.
 */
public class GridBuilder {
  private BigDecimal cellWidth;
  private boolean inputIsGeographic;

  // CRS-related fields
  private CoordinateReferenceSystem inputCoordinateReferenceSystem;
  private CoordinateReferenceSystem targetCoordinateReferenceSystem;
  
  // Transformed coordinates stored directly as DirectPosition2D
  private DirectPosition2D topLeftTransformed;
  private DirectPosition2D bottomRightTransformed;

  /**
   * Creates a new GridBuilder with specified input and target CRS, and corner coordinates.
   * 
   * @param inputCrsCode EPSG code for the input CRS
   * @param targetCrsCode EPSG code for the target CRS
   * @param cornerCoords Map containing corner coordinates with keys like "topLeftX", "topLeftY",
   *                    "bottomRightX", "bottomRightY"
   * @param cellWidth The width of each cell in the grid (in units of the target CRS)
   * @throws FactoryException if any CRS code is invalid
   * @throws TransformException if coordinate transformation fails
   */
  public GridBuilder(String inputCrsCode, String targetCrsCode, 
                    Map<String, BigDecimal> cornerCoords, BigDecimal cellWidth) 
      throws FactoryException, TransformException {
    
    // Validate cell width
    if (cellWidth == null || cellWidth.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Cell width must be positive");
    }
    this.cellWidth = cellWidth;
    
    // Set up CRS
    this.inputCoordinateReferenceSystem = CRS.forCode(inputCrsCode);
    this.targetCoordinateReferenceSystem = CRS.forCode(targetCrsCode);
    this.inputIsGeographic = inputCoordinateReferenceSystem instanceof GeographicCRS;
    
    // Get axis information to determine coordinate order
    Map<String, String> axisInfo = getAxisAbbreviationsFromCRS();
    String inputX = axisInfo.get("inputAxisZero");
    String inputY = axisInfo.get("inputAxisOne");
    
    // Extract coordinates - handling different naming based on input CRS type
    BigDecimal topLeftX, topLeftY, bottomRightX, bottomRightY;
    
    if (inputIsGeographic) {
        // Geographic CRS usually uses lat/lon
        topLeftX = cornerCoords.get("topLeftLon"); 
        topLeftY = cornerCoords.get("topLeftLat"); 
        bottomRightX = cornerCoords.get("bottomRightLon"); 
        bottomRightY = cornerCoords.get("bottomRightLat"); 
        
    } else {
        // Projected CRS usually uses easting/northing
        topLeftX = cornerCoords.get("topLeftE");
        topLeftY = cornerCoords.get("topLeftN");
        bottomRightX = cornerCoords.get("bottomRightE");
        bottomRightY = cornerCoords.get("bottomRightN"); 
    }
    
    // Validate corners
    validateCornerCoordinates(topLeftX, topLeftY, bottomRightX, bottomRightY);
    
    // Transform coordinates immediately (or copy if same CRS)
    transformCornerCoordinates(topLeftX, topLeftY, bottomRightX, bottomRightY);
  }

  /**
   * Gets the names of the axes for both input CRS and target CRS.
   */
  private Map<String, String> getAxisAbbreviationsFromCRS() {
    String inputAxisZero = inputCoordinateReferenceSystem.getCoordinateSystem().getAxis(0).getAbbreviation();
    String inputAxisOne = inputCoordinateReferenceSystem.getCoordinateSystem().getAxis(1).getAbbreviation();
    String targetAxisZero = targetCoordinateReferenceSystem.getCoordinateSystem().getAxis(0).getAbbreviation();
    String targetAxisOne = targetCoordinateReferenceSystem.getCoordinateSystem().getAxis(1).getAbbreviation();

    Map<String, String> axisAbbreviations = new HashMap<>();
    axisAbbreviations.put("inputAxisZero", inputAxisZero);
    axisAbbreviations.put("inputAxisOne", inputAxisOne);
    axisAbbreviations.put("targetAxisZero", targetAxisZero);
    axisAbbreviations.put("targetAxisOne", targetAxisOne);

    return axisAbbreviations;
  }
  
  /**
   * Validates the input map contains required corner coordinates based on CRS axis abbreviations
   * and constructs DirectPosition2D objects for corners.
   * 
   * @param cornerCoordinates Map containing corner coordinates
   * @param crs The Coordinate Reference System
   * @throws IllegalArgumentException if required fields are missing
   */
  public DirectPosition2D[] validateAndCreatePositions(
        Map<String, Double> cornerCoordinates, 
        CoordinateReferenceSystem crs
    ) {
      // Get axis abbreviations from CRS
      Map<String, String> axisAbbreviations = getAxisAbbreviationsFromCRS();
      
      // Extract axis abbreviations
      String xAxisAbbrev = axisAbbreviations.get("InputAxisZero");
      String yAxisAbbrev = axisAbbreviations.get("InputAxisOne");
      
      if (xAxisAbbrev == null || yAxisAbbrev == null) {
          throw new IllegalArgumentException("Could not determine axis abbreviations from CRS");
      }
      
      // Define required fields
      String topLeftXKey = "topLeft" + xAxisAbbrev;
      String topLeftYKey = "topLeft" + yAxisAbbrev;
      String bottomRightXKey = "bottomRight" + xAxisAbbrev;
      String bottomRightYKey = "bottomRight" + yAxisAbbrev;
      
      // Validate all required fields exist
      String[] requiredKeys = {topLeftXKey, topLeftYKey, bottomRightXKey, bottomRightYKey};
      for (String key : requiredKeys) {
          if (!cornerCoordinates.containsKey(key)) {
              throw new IllegalArgumentException("Missing required field: " + key);
          }
      }
      
      // Create the DirectPosition2D objects
      DirectPosition2D topLeft = new DirectPosition2D(
          cornerCoordinates.get(topLeftXKey),
          cornerCoordinates.get(topLeftYKey)
      );
      
      DirectPosition2D bottomRight = new DirectPosition2D(
          cornerCoordinates.get(bottomRightXKey),
          cornerCoordinates.get(bottomRightYKey)
      );
      
      return new DirectPosition2D[] {topLeft, bottomRight};
  }

  /**
   * Validates corner coordinates based on coordinate system type.
   */
  private void validateCornerCoordinates(
      BigDecimal topLeftX, BigDecimal topLeftY, 
      BigDecimal bottomRightX, BigDecimal bottomRightY) {
      
    if (topLeftX == null || topLeftY == null || bottomRightX == null || bottomRightY == null) {
      throw new IllegalArgumentException("Missing corner coordinates");
    }
    
    if (inputIsGeographic) {
      // For geographic coordinates (longitude increases eastward, latitude increases northward)
      if (topLeftY.compareTo(bottomRightY) <= 0) {
        throw new IllegalArgumentException(
            "Top-left latitude must be greater than bottom-right latitude");
      }
      if (topLeftX.compareTo(bottomRightX) >= 0) {
        throw new IllegalArgumentException(
            "Top-left longitude must be less than bottom-right longitude");
      }
    } else {
      // For projected coordinates (Y increases upward, X increases rightward)
      if (topLeftY.compareTo(bottomRightY) <= 0) {
        throw new IllegalArgumentException(
            "Top-left Y-coordinate must be greater than bottom-right Y-coordinate");
      }
      if (topLeftX.compareTo(bottomRightX) >= 0) {
        throw new IllegalArgumentException(
            "Top-left X-coordinate must be less than bottom-right X-coordinate");
      }
    }
  }

  /**
   * Transforms corner coordinates to a target CRS, respecting the axis order of the target CRS.
   * 
   * @param corners Array of DirectPosition2D objects representing corner points
   * @param sourceCrs The source Coordinate Reference System
   * @param targetCrs The target Coordinate Reference System
   * @return Array of transformed DirectPosition2D objects
   * @throws TransformException if transformation fails
   */
  public DirectPosition2D[] transformCornerCoordinates(DirectPosition2D[] corners, 
                                                    CoordinateReferenceSystem sourceCrs, 
                                                    CoordinateReferenceSystem targetCrs) 
                                                    throws TransformException {
    
    // Get the transformation between the two CRS
    MathTransform transform;
    try {
      transform = CRS.findOperation(
          sourceCrs, targetCrs, null
      ).getMathTransform();
    } catch (FactoryException e) {
      throw new TransformException("Failed to find transformation between CRS: " + e.getMessage());
    }

    DirectPosition2D[] transformedCorners = new DirectPosition2D[corners.length];
    
    for (int i = 0; i < corners.length; i++) {
        // Create a general DirectPosition for the transformation
        DirectPosition result = new GeneralDirectPosition(targetCrs.getCoordinateSystem().getDimension());
        
        // Transform the coordinates
        transform.transform(corners[i], result);
        
        // Get the correct order of coordinates based on target CRS
        double ordinate0 = result.getOrdinate(0);  // First coordinate in target CRS order
        double ordinate1 = result.getOrdinate(1);  // Second coordinate in target CRS order
        
        // Create a new DirectPosition2D with the correct axis order for target CRS
        transformedCorners[i] = new DirectPosition2D(ordinate0, ordinate1);
    }
    
    return transformedCorners;
  }

  /**
   * Builds and returns a Grid based on the transformed coordinates.
   *
   * @return a new Grid instance
   */
  public Grid build() {
    try {
      // Create spatial context
      SpatialContext targetContext = createSpatialContext();
      
      // Calculate grid dimensions
      GridDimensions dimensions = calculateGridDimensions();
      
      // Create all patches
      List<Patch> patches = createPatchGrid(dimensions, targetContext);
      
      return new Grid(patches, cellWidth);
    } catch (Exception e) {
      throw new RuntimeException("Failed to build grid: " + e.getMessage(), e);
    }
  }
  
  private SpatialContext createSpatialContext() {
    SpatialContextFactory factory = new SpatialContextFactory();
    factory.geo = targetCoordinateReferenceSystem instanceof GeographicCRS;
    return factory.newSpatialContext();
  }
  
  private static class GridDimensions {
    final int colCells;
    final int rowCells;
    final double cellWidthUnits;

    GridDimensions(int colCells, int rowCells, double cellWidthUnits) {
      this.colCells = colCells;
      this.rowCells = rowCells;
      this.cellWidthUnits = cellWidthUnits;
    }
  }
  
  private GridDimensions calculateGridDimensions() {
    double cellWidthUnits = this.cellWidth.doubleValue();
    
    double rowDiff = topLeftTransformed.y - bottomRightTransformed.y;
    double colDiff = bottomRightTransformed.x - topLeftTransformed.x;
    
    int rowCells = (int) Math.ceil(rowDiff / cellWidthUnits);
    int colCells = (int) Math.ceil(colDiff / cellWidthUnits);
    
    return new GridDimensions(colCells, rowCells, cellWidthUnits);
  }
  
  /**
   * Creates a single patch geometry for a specific cell.
   */
  private Geometry createCellGeometry(
        double topY, double leftX,
        double bottomY, double rightX,
        SpatialContext context
  ) {
    return GeometryFactory.createSquare(
        BigDecimal.valueOf(topY),
        BigDecimal.valueOf(leftX),
        BigDecimal.valueOf(bottomY),
        BigDecimal.valueOf(rightX),
        context
    );
  }

  /**
   * Creates all patches in the grid.
   */
  private List<Patch> createPatchGrid(
        GridDimensions dimensions,
        SpatialContext context
  ) {
    List<Patch> patches = new ArrayList<>();
    for (int rowIdx = 0; rowIdx < dimensions.rowCells; rowIdx++) {
      for (int colIdx = 0; colIdx < dimensions.colCells; colIdx++) {
        double cellTopLeftY = topLeftTransformed.y - (rowIdx * dimensions.cellWidthUnits);
        double cellTopLeftX = topLeftTransformed.x + (colIdx * dimensions.cellWidthUnits);

        double cellBottomRightY = cellTopLeftY - dimensions.cellWidthUnits;
        double cellBottomRightX = cellTopLeftX + dimensions.cellWidthUnits;

        // Ensure we don't exceed grid boundaries
        cellBottomRightY = Math.max(cellBottomRightY, bottomRightTransformed.y);
        cellBottomRightX = Math.min(cellBottomRightX, bottomRightTransformed.x);

        // Create geometry for this cell
        Geometry cellGeometry = createCellGeometry(
            cellTopLeftY, cellTopLeftX,
            cellBottomRightY, cellBottomRightX,
            context
        );

        if (!(cellGeometry == null)) {
          patches.add(new Patch(cellGeometry));
        }
      }
    }

    return patches;
  }

  /**
   * Validates that all required objects are properly initialized.
   * This is different from validateCornerCoordinates which validates the specific
   * coordinate values.
   */
  private void validateParameters() {
    if (topLeftTransformed == null || bottomRightTransformed == null) {
      throw new IllegalStateException("Corner coordinates not transformed");
    }

    if (cellWidth == null) {
      throw new IllegalStateException("Cell width not specified");
    }

    if (inputCoordinateReferenceSystem == null) {
      throw new IllegalStateException("Input CRS not specified");
    }

    if (targetCoordinateReferenceSystem == null) {
      throw new IllegalStateException("Target CRS not specified");
    }

    if (cellWidth.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Cell width must be positive");
    }

    // Validate that after transformation, the coordinates still make sense
    if (topLeftTransformed.y <= bottomRightTransformed.y) {
      throw new IllegalArgumentException(
        "After transformation, top-left Y-coordinate must be greater than bottom-right Y-coordinate");
    }

    if (topLeftTransformed.x >= bottomRightTransformed.x) {
      throw new IllegalArgumentException(
        "After transformation, top-left X-coordinate must be less than bottom-right X-coordinate");
    }
  }
}