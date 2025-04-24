package org.joshsim.geo.geometry;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.apache.sis.referencing.CRS;
import org.joshsim.engine.entity.prototype.EntityPrototype;
import org.joshsim.engine.geometry.EngineGeometry;
import org.joshsim.engine.geometry.EngineGeometryFactory;
import org.joshsim.engine.geometry.PatchBuilder;
import org.joshsim.engine.geometry.grid.GridCrsDefinition;
import org.joshsim.engine.geometry.grid.GridShape;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.util.GeometricShapeFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;

/**
 * Factory methods for creating geometric shapes using JTS geometry.
 */
public class EarthGeometryFactory implements EngineGeometryFactory {

  private static final GeometryFactory JTS_GEOMETRY_FACTORY = new GeometryFactory();
  private static final int DEFAULT_NUM_POINTS = 32; // For circle approximation
  private static final double DEFAULT_SQUARE_TOLERANCE_PCT = 0.01; // Default width for square

  private final CoordinateReferenceSystem earthCrs;
  private CoordinateReferenceSystem gridCrs;

  /**
   * Create a new factory for the given coordinate reference system.
   *
   * @param earthCrs The coordinate reference system to use in constructing these geometries.
   */
  public EarthGeometryFactory(CoordinateReferenceSystem earthCrs) {
    this.earthCrs = earthCrs;
  }

  /**
   * Create a new factory with both Earth CRS and Grid CRS support.
   *
   * @param earthCrs The Earth coordinate reference system
   * @param realizedGridCrs The realized grid CRS for transformations
   */
  public EarthGeometryFactory(CoordinateReferenceSystem earthCrs, RealizedGridCrs realizedGridCrs) {
    this.earthCrs = earthCrs;
    this.gridCrs = realizedGridCrs.getGridCrs();
  }

  /**
   * Sets the grid CRS to use for transformations, assuming a CRS is already instantiated.
   *
   * @param crs The grid CRS to set
   */

  public void setGridCrs(CoordinateReferenceSystem crs) {
    this.gridCrs = crs;
  }

  /**
   * Sets the realized grid CRS to use for transformations.
   *
   * @param realizedGridCrs The realized grid CRS
   */
  public void setRealizedGridCrs(RealizedGridCrs realizedGridCrs) {
    this.gridCrs = realizedGridCrs.getGridCrs();
  }

  /**
   * Sets the realized grid CRS from the provided grid CRS definition.
   *
   * @param gridCrsDefinition The grid CRS definition to use for setting the realized grid CRS.
   * @throws IOException If an error occurs while realizing the grid CRS.
   * @throws TransformException if an error occurs during transformation
   */
  public void setRealizedGridCrsFromDefition(
         GridCrsDefinition gridCrsDefinition
  ) throws IOException, TransformException {
    try {
      RealizedGridCrs realizedGridCrs = new RealizedGridCrs(gridCrsDefinition);
      this.gridCrs = realizedGridCrs.getGridCrs();
    } catch (FactoryException e) {
      throw new RuntimeException("Failed to realize and set grid CRS: " + e.getMessage(), e);
    }
  }

  /**
   * Gets the realized grid CRS.
   *
   * @return The realized grid CRS
   */
  public CoordinateReferenceSystem getGridCrs() {
    return gridCrs;
  }

  /**
   * Gets the Earth coordinate reference system.
   *
   * @return The Earth coordinate reference system
   */
  public CoordinateReferenceSystem getEarthCrs() {
    return earthCrs;
  }

  @Override
  public EngineGeometry createSquare(BigDecimal centerX, BigDecimal centerY, BigDecimal width) {
    GeometricShapeFactory shapeFactory = new GeometricShapeFactory(JTS_GEOMETRY_FACTORY);

    // Set the center of the shape
    shapeFactory.setCentre(new Coordinate(centerX.doubleValue(), centerY.doubleValue()));

    // Set equal width and height to make a square
    shapeFactory.setWidth(width.doubleValue());
    shapeFactory.setHeight(width.doubleValue());

    // Create the square
    Geometry square = shapeFactory.createRectangle();
    return new EarthGeometry(square, earthCrs);
  }

  @Override
  public EngineGeometry createSquare(
      BigDecimal topLeftX, BigDecimal topLeftY, BigDecimal bottomRightX, BigDecimal bottomRightY) {
    double minX = topLeftX.doubleValue();
    double maxY = topLeftY.doubleValue();
    double maxX = bottomRightX.doubleValue();
    double minY = bottomRightY.doubleValue();

    // Validate that this is approximately square
    double width = maxX - minX;
    double height = maxY - minY;
    double tolerance = Math.min(width, height) * DEFAULT_SQUARE_TOLERANCE_PCT;
    if (Math.abs(width - height) > tolerance) {
      throw new IllegalArgumentException(
          "Shape defined by coordinates is not square. Width: " + width + ", Height: " + height);
    }

    GeometricShapeFactory shapeFactory = new GeometricShapeFactory(JTS_GEOMETRY_FACTORY);

    // Set the base (lower left) of the rectangle
    shapeFactory.setBase(new Coordinate(minX, minY));

    // Set width and height
    shapeFactory.setWidth(width);
    shapeFactory.setHeight(height);

    // Create the rectangle
    Geometry rectangle = shapeFactory.createRectangle();
    return new EarthGeometry(rectangle, earthCrs);
  }

  @Override
  public EngineGeometry createCircle(
      BigDecimal point1X, BigDecimal point1Y, BigDecimal point2X, BigDecimal point2Y) {
    double x1 = point1X.doubleValue();
    double y1 = point1Y.doubleValue();
    double x2 = point2X.doubleValue();
    double y2 = point2Y.doubleValue();

    // Calculate center and diameter
    double centerX = (x1 + x2) / 2.0;
    double centerY = (y1 + y2) / 2.0;
    double diameter = Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));

    GeometricShapeFactory shapeFactory = new GeometricShapeFactory(JTS_GEOMETRY_FACTORY);

    // Set center, radius (via width) and number of points
    shapeFactory.setCentre(new Coordinate(centerX, centerY));
    shapeFactory.setWidth(diameter);
    shapeFactory.setHeight(diameter);
    shapeFactory.setNumPoints(DEFAULT_NUM_POINTS);

    // Create the circle
    Geometry circle = shapeFactory.createCircle();
    return new EarthGeometry(circle, earthCrs);
  }

  @Override
  public EngineGeometry createCircle(BigDecimal centerX, BigDecimal centerY, BigDecimal radius) {
    GeometricShapeFactory shapeFactory = new GeometricShapeFactory(JTS_GEOMETRY_FACTORY);

    // Set center coordinates
    shapeFactory.setCentre(new Coordinate(centerX.doubleValue(), centerY.doubleValue()));

    // Set diameter (2 × radius)
    double diameter = radius.multiply(new BigDecimal(2)).doubleValue();
    shapeFactory.setWidth(diameter);
    shapeFactory.setHeight(diameter);

    // Set number of points for a smooth circle
    shapeFactory.setNumPoints(DEFAULT_NUM_POINTS);

    // Create the circle
    Geometry circle = shapeFactory.createCircle();
    return new EarthGeometry(circle, earthCrs);
  }

  @Override
  public EngineGeometry createPoint(BigDecimal x, BigDecimal y) {
    Point point = JTS_GEOMETRY_FACTORY.createPoint(
        new Coordinate(x.doubleValue(), y.doubleValue())
    );
    return new EarthGeometry(point, earthCrs);
  }

  /**
   * Creates geometry from a grid point, converting to target CRS coordinates using RealizedGridCrs.
   *
   * @param gridShape The grid shape to convert
   * @return A point geometry in the target CRS
   */
  public EngineGeometry createPointFromGrid(GridShape gridShape) {
    checkGridCrs();

    try {
      // Create a point geometry in grid space
      Point gridPoint = JTS_GEOMETRY_FACTORY.createPoint(
          new Coordinate(
              gridShape.getCenterX().doubleValue(),
              gridShape.getCenterY().doubleValue()
          ));

      // Transform from grid to Earth CRS
      MathTransform transform = CRS.findOperation(gridCrs, earthCrs, null).getMathTransform();
      Geometry transformedPoint = JtsTransformUtility.transform(gridPoint, transform);

      return new EarthGeometry(transformedPoint, earthCrs);
    } catch (FactoryException | TransformException e) {
      throw new RuntimeException("Failed to transform grid point: " + e.getMessage(), e);
    }
  }

  /**
   * Creates a circle geometry approximation from a grid shape using RealizedGridCrs.
   *
   * @param gridShape The grid shape to convert
   * @return A polygon approximating a circle in the target CRS
   */
  public EngineGeometry createCircleFromGrid(GridShape gridShape) {
    checkGridCrs();

    try {
      // Create circle in grid space
      double centerX = gridShape.getCenterX().doubleValue();
      double centerY = gridShape.getCenterY().doubleValue();
      double radius = gridShape.getWidth().divide(
          new BigDecimal(2),
          RoundingMode.HALF_UP
      ).doubleValue();

      GeometricShapeFactory shapeFactory = new GeometricShapeFactory(JTS_GEOMETRY_FACTORY);
      shapeFactory.setCentre(new Coordinate(centerX, centerY));
      shapeFactory.setWidth(radius * 2);
      shapeFactory.setHeight(radius * 2);
      shapeFactory.setNumPoints(DEFAULT_NUM_POINTS);
      Geometry gridCircle = shapeFactory.createCircle();

      // Transform from grid to Earth CRS
      MathTransform transform = CRS.findOperation(gridCrs, earthCrs, null).getMathTransform();
      Geometry transformedCircle = JtsTransformUtility.transform(gridCircle, transform);

      return new EarthGeometry(transformedCircle, earthCrs);
    } catch (FactoryException | TransformException e) {
      throw new RuntimeException("Failed to transform grid circle: " + e.getMessage(), e);
    }
  }

  /**
   * Creates a rectangle geometry from a grid shape using RealizedGridCrs.
   *
   * @param gridShape The grid shape to convert
   * @return A polygon rectangle in the target CRS
   */
  public EngineGeometry createRectangleFromGrid(GridShape gridShape) {
    checkGridCrs();

    try {
      // Create rectangle in grid space
      double centerX = gridShape.getCenterX().doubleValue();
      double centerY = gridShape.getCenterY().doubleValue();
      double width = gridShape.getWidth().doubleValue();
      double height = gridShape.getHeight().doubleValue();

      GeometricShapeFactory shapeFactory = new GeometricShapeFactory(JTS_GEOMETRY_FACTORY);
      shapeFactory.setCentre(new Coordinate(centerX, centerY));
      shapeFactory.setWidth(width);
      shapeFactory.setHeight(height);
      Geometry gridRectangle = shapeFactory.createRectangle();

      // Transform from grid to Earth CRS
      MathTransform transform = CRS.findOperation(gridCrs, earthCrs, null).getMathTransform();
      Geometry transformedRectangle = JtsTransformUtility.transform(gridRectangle, transform);

      return new EarthGeometry(transformedRectangle, earthCrs);
    } catch (FactoryException | TransformException e) {
      throw new RuntimeException("Failed to transform grid rectangle: " + e.getMessage(), e);
    }
  }

  /**
   * Creates geometry from a grid shape, determining the appropriate shape type automatically.
   *
   * @param gridShape The grid shape to convert
   * @return A geometry in the target CRS
   */
  public EngineGeometry createFromGrid(GridShape gridShape) {
    switch (gridShape.getGridShapeType()) {
      case POINT:
        return createPointFromGrid(gridShape);
      case CIRCLE:
        return createCircleFromGrid(gridShape);
      case SQUARE:
        return createRectangleFromGrid(gridShape);
      default:
        throw new IllegalArgumentException(
          "Unsupported grid shape type: " + gridShape.getGridShapeType());
    }
  }

  /**
   * Ensures gridCrs is available before attempting transformations.
   */
  private void checkGridCrs() {
    if (gridCrs == null) {
      throw new IllegalStateException("Grid CRS not set. Call setRealizedGridCrs first.");
    }
  }

  @Override
  public String toString() {
    return String.format("EarthGeometryFactory with crs of %s", earthCrs);
  }

  /**
   * Creates a patch builder for the given grid CRS definition and entity prototype.
   *
   * @param gridCrsDefinition The grid CRS definition
   * @param prototype The entity prototype
   * @return A patch builder for the specified grid CRS
   */
  @Override
  public PatchBuilder getPatchBuilder(
      GridCrsDefinition gridCrsDefinition,
      EntityPrototype prototype
  ) {
    throw new UnsupportedOperationException(
      "getPatchBuilder is not supported in EarthGeometryFactory - use GridGeometryFactory instead");
  }
}
