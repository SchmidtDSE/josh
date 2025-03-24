/**
 * A keyable object which represents a serializable Geomtry.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.geometry;

import java.math.BigDecimal;
import java.util.Optional;


/**
 * Momento which represents a Geometry that can be keyed and converted back to a Geometry.
 *
 * <p>Momento which represents a subset of possible Geometry objects such that the momento can be
 * serialized and compared to other momentos to support caching operations.</p>
 */
public class GeometryMomento {

  private final String shapeName;
  private final BigDecimal centerX;
  private final BigDecimal centerY;
  private final BigDecimal diameter;

  public GeometryMomento(String shapeName, BigDecimal centerX, BigDecimal centerY, 
      BigDecimal diameter) {
    this.shapeName = shapeName;
    this.centerX = centerX;
    this.centerY = centerY;
    this.diameter = diameter;

    if (getBuilder().isEmpty()) {
      throw new IllegalArgumentException("Unsupported momento shape: " + shapeName);
    }
  }
  
  public Geometry build() {
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

  private Optional<MomentoShapeBuilder> getBuilder() {
    switch (shapeName) {
      case "sqaure": return Optional.of(
        () -> GeometryFactory.createSquare(diameter, centerX, centerY)
      );
      case "circle": return Optional.of(
        () -> GeometryFactory.createCircle(diameter, centerX, centerY)
      );
      default: return Optional.empty();
    }
  }

  private interface MomentoShapeBuilder {

    Geometry build();
    
  }

}
