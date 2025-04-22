package org.joshsim.geo.geometry;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.apache.sis.referencing.CRS;
import org.joshsim.engine.entity.prototype.EntityPrototype;
import org.joshsim.engine.geometry.EngineGeometry;
import org.joshsim.engine.geometry.EngineGeometryFactory;
import org.joshsim.engine.geometry.PatchBuilder;
import org.joshsim.engine.geometry.PatchBuilderExtents;
import org.joshsim.engine.geometry.grid.GridPatchBuilder;
import org.joshsim.engine.geometry.grid.GridShape;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.util.GeometricShapeFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;

/**
 * Factory methods for creating geometric shapes using JTS geometry.
 */
public class EarthGeometryFactory implements EngineGeometryFactory {

  private static final GeometryFactory JTS_GEOMETRY_FACTORY = new GeometryFactory();
  private static final int DEFAULT_NUM_POINTS = 32; // For circle approximation
  private static final double DEFAULT_SQUARE_TOLERANCE_PCT = 0.01; // Default width for square

  private final CoordinateReferenceSystem crs;

  /**
   * Create a new factory for the given coordinate reference system.
   *
   * @param crs The coordinate reference system to use in constructing these geometries.
   */
  public EarthGeometryFactory(CoordinateReferenceSystem crs) {
    this.crs = crs;
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
    return new EarthGeometry(square, crs);
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
    return new EarthGeometry(rectangle, crs);
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
    return new EarthGeometry(circle, crs);
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
    return new EarthGeometry(circle, crs);
  }

  @Override
  public EngineGeometry createPoint(BigDecimal x, BigDecimal y) {
    Point point = JTS_GEOMETRY_FACTORY.createPoint(
        new Coordinate(x.doubleValue(), y.doubleValue())
    );
    return new EarthGeometry(point, crs);
  }

  /**
   * Creates geometry from a grid point, converting to target CRS coordinates.
   *
   * @param gridShape The grid shape to convert
   * @param gridOriginX X coordinate in target CRS of grid origin
   * @param gridOriginY Y coordinate in target CRS of grid origin
   * @param cellWidth Width of a grid cell in target CRS units
   * @return A point geometry in the target CRS
   */
  public EngineGeometry createPointFromGrid(
      GridShape gridShape, BigDecimal gridOriginX, BigDecimal gridOriginY, BigDecimal cellWidth) {
    // Transform grid coordinates to target CRS coordinates
    double realX = gridOriginX.doubleValue()
        + gridShape.getCenterX().doubleValue() * cellWidth.doubleValue();
    double realY = gridOriginY.doubleValue()
        - gridShape.getCenterY().doubleValue() * cellWidth.doubleValue();

    Point point = JTS_GEOMETRY_FACTORY.createPoint(new Coordinate(realX, realY));
    return new EarthGeometry(point, crs);
  }

  /**
   * Creates a circle geometry approximation from a grid shape.
   *
   * @param gridShape The grid shape to convert
   * @param gridOriginX X coordinate in target CRS of grid origin
   * @param gridOriginY Y coordinate in target CRS of grid origin
   * @param cellWidth Width of a grid cell in target CRS units
   * @return A polygon approximating a circle in the target CRS
   */
  public EngineGeometry createCircleFromGrid(
      GridShape gridShape, BigDecimal gridOriginX, BigDecimal gridOriginY, BigDecimal cellWidth) {
    // Calculate center point in target CRS coordinates
    double centerX = gridOriginX.doubleValue()
        + gridShape.getCenterX().doubleValue() * cellWidth.doubleValue();
    double centerY = gridOriginY.doubleValue()
        - gridShape.getCenterY().doubleValue() * cellWidth.doubleValue();

    // Calculate radius in target CRS units
    double radius = gridShape.getWidth().multiply(cellWidth)
        .divide(new BigDecimal(2), RoundingMode.HALF_UP).doubleValue();

    // Use GeometricShapeFactory to create the circle
    GeometricShapeFactory shapeFactory = new GeometricShapeFactory(JTS_GEOMETRY_FACTORY);
    shapeFactory.setCentre(new Coordinate(centerX, centerY));
    shapeFactory.setWidth(radius * 2);
    shapeFactory.setHeight(radius * 2);
    shapeFactory.setNumPoints(DEFAULT_NUM_POINTS);

    Geometry circle = shapeFactory.createCircle();
    return new EarthGeometry(circle, crs);
  }

  /**
   * Creates a rectangle geometry from a grid shape.
   *
   * @param gridShape The grid shape to convert
   * @param gridOriginX X coordinate in target CRS of grid origin
   * @param gridOriginY Y coordinate in target CRS of grid origin
   * @param cellWidth Width of a grid cell in target CRS units
   * @return A polygon rectangle in the target CRS
   */
  public EngineGeometry createRectangleFromGrid(
      GridShape gridShape, BigDecimal gridOriginX, BigDecimal gridOriginY, BigDecimal cellWidth) {
    // Get center in grid coordinates
    double gridCenterX = gridShape.getCenterX().doubleValue();
    double gridCenterY = gridShape.getCenterY().doubleValue();

    // Calculate rectangle dimensions in target CRS units
    double width = gridShape.getWidth().multiply(cellWidth).doubleValue();
    double height = gridShape.getHeight().multiply(cellWidth).doubleValue();

    // Calculate center in target CRS coordinates
    double centerX = gridOriginX.doubleValue() + gridCenterX * cellWidth.doubleValue();
    double centerY = gridOriginY.doubleValue() - gridCenterY * cellWidth.doubleValue();

    // Use GeometricShapeFactory to create the rectangle
    GeometricShapeFactory shapeFactory = new GeometricShapeFactory(JTS_GEOMETRY_FACTORY);
    shapeFactory.setCentre(new Coordinate(centerX, centerY));
    shapeFactory.setWidth(width);
    shapeFactory.setHeight(height);

    Geometry rectangle = shapeFactory.createRectangle();
    return new EarthGeometry(rectangle, crs);
  }

  @Override
  public String toString() {
    return String.format("EarthGeometryFactory with crs of %s", crs);
  }

  @Override
  public PatchBuilder getPatchBuilder(
      String inputCrs,
      String targetCrs,
      PatchBuilderExtents extents,
      BigDecimal cellWidth,
      EntityPrototype prototype) {
    try {
      // If CRS strings are empty or null, use default Grid space behavior
      if ((inputCrs == null || inputCrs.isEmpty()) && (targetCrs == null || targetCrs.isEmpty())) {
        return new GridPatchBuilder(extents, cellWidth, prototype);
      }

      // Convert CRS strings to CoordinateReferenceSystem objects
      CoordinateReferenceSystem sourceCrs = CRS.forCode(inputCrs);
      CoordinateReferenceSystem destCrs = CRS.forCode(targetCrs);

      // Create and return the EarthPatchBuilder
      return new EarthPatchBuilder(sourceCrs, destCrs, extents, cellWidth, prototype);
    } catch (FactoryException e) {
      throw new IllegalArgumentException("Invalid CRS: " + e.getMessage(), e);
    } catch (TransformException e) {
      throw new IllegalArgumentException("Error transforming coordinates: " + e.getMessage(), e);
    }
  }
}