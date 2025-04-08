/**
 * A keyable object which represents a serializable Geomtry.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.bridge;

import java.math.BigDecimal;
import java.util.Optional;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.joshsim.engine.geometry.EngineGeometry;
import org.joshsim.engine.geometry.GeometryFactory;


/**
 * Momento which represents a EngineGeometry that can be keyed and converted back to a Geometry.
 *
 * <p>Momento which represents a subset of possible EngineGeometry objects such that the
 * momento can be serialized and compared to other momentos to support caching operations.</p>
 */
public class GeometryMomento {

  private final String shapeName;
  private final BigDecimal centerX;
  private final BigDecimal centerY;
  private final BigDecimal diameter;
  private final CoordinateReferenceSystem crs;

  /**
   * Constructs a GeometryMomento with the specified shape parameters.
   *
   * @param shapeName The name of the shape (e.g., "square", "circle").
   * @param centerX The x-coordinate of the shape's center.
   * @param centerY The y-coordinate of the shape's center.
   * @param diameter The diameter or width of the shape.
   * @throws IllegalArgumentException if the shape name is not supported.
   */
  public GeometryMomento(String shapeName, BigDecimal centerX, BigDecimal centerY,
      BigDecimal diameter, CoordinateReferenceSystem crs) {
    this.shapeName = shapeName;
    this.centerX = centerX;
    this.centerY = centerY;
    this.diameter = diameter;
    this.crs = crs;

    if (getBuilder().isEmpty()) {
      throw new IllegalArgumentException("Unsupported momento shape: " + shapeName);
    }
  }

  /**
   * Builds and returns a EngineGeometry object from this momento.
   *
   * @return A new EngineGeometry instance representing the shape described by this momento.
   */
  public EngineGeometry build() {
    return getBuilder().get().build();
  }

  @Override
  public String toString() {
    return String.format(
      "%s momento at (%.6f, %.6f) of diameter %.6f.",
      shapeName,
      centerX.doubleValue(),
      centerY.doubleValue(),
      diameter.doubleValue()
    );
  }

  @Override
  public boolean equals(Object other) {
    return toString().equals(other.toString());
  }

  @Override
  public int hashCode() {
    return toString().hashCode();
  }

  /**
   * Gets the appropriate shape builder for this momento's shape type.
   *
   * @return Optional containing a MomentoShapeBuilder if the shape type is supported, empty
   *     Optional otherwise.
   */
  private Optional<MomentoShapeBuilder> getBuilder() {
    return switch (shapeName) {
      case "square" -> Optional.of(
        () -> GeometryFactory.createSquare(diameter, centerX, centerY, crs)
      );
      case "circle" -> Optional.of(
        () -> GeometryFactory.createCircle(diameter, centerX, centerY, crs)
      );
      default -> Optional.empty();
    };
  }

  /**
   * Interface for building geometries from momentos.
   */
  private interface MomentoShapeBuilder {

    /**
     * Builds a geometry from the momento's parameters.
     *
     * @return A new EngineGeometry instance.
     */
    EngineGeometry build();

  }

}
