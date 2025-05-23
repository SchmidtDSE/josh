/**
 * Structures describing geometric shapes and their properties in grid space.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.geometry.grid;

import java.math.BigDecimal;
import org.joshsim.engine.entity.prototype.EntityPrototype;
import org.joshsim.engine.geometry.EngineGeometry;
import org.joshsim.engine.geometry.EngineGeometryFactory;
import org.joshsim.engine.geometry.PatchBuilder;

/**
 * Factory for creating geometric shapes in grid space.
 *
 * <p>This factory provides methods to create basic geometric shapes like squares, circles and
 * points in a grid space coordinates.</p>
 */
public class GridGeometryFactory implements EngineGeometryFactory {

  @Override
  public boolean supportsEarthSpace() {
    return false;
  }

  @Override
  public boolean supportsGridSpace() {
    return true;
  }

  @Override
  public EngineGeometry createSquare(BigDecimal centerX, BigDecimal centerY, BigDecimal width) {
    return new GridSquare(centerX, centerY, width);
  }

  @Override
  public EngineGeometry createSquare(BigDecimal topLeftX, BigDecimal topLeftY,
        BigDecimal bottomRightX, BigDecimal bottomRightY) {

    BigDecimal width = bottomRightX.subtract(topLeftX);
    return new GridSquare(
        topLeftX.add(width.divide(BigDecimal.TWO)),
        topLeftY.add(width.divide(BigDecimal.TWO)),
        width
    );
  }

  @Override
  public EngineGeometry createCircle(BigDecimal centerX, BigDecimal centerY, BigDecimal radius) {
    return new GridCircle(centerX, centerY, radius);
  }

  @Override
  public EngineGeometry createCircle(BigDecimal centerX, BigDecimal centerY, BigDecimal circumX,
        BigDecimal circumY) {
    // Calculate radius as distance from center to circumference point
    BigDecimal radius = BigDecimal.valueOf(Math.sqrt(
        circumX.subtract(centerX).pow(2).add(circumY.subtract(centerY).pow(2)).doubleValue()
    ));
    return new GridCircle(centerX, centerY, radius);
  }

  @Override
  public EngineGeometry createPoint(BigDecimal x, BigDecimal y) {
    return new GridPoint(x, y);
  }

  /**
   * Creates a patch builder with the specified grid CRS definition and entity prototype.
   *
   * @param gridCrsDefinition The grid CRS definition
   * @param prototype The entity prototype used to create grid cells
   * @return A patch builder
   */
  public PatchBuilder getPatchBuilder(
        GridCrsDefinition gridCrsDefinition,
        EntityPrototype prototype) {
    return new GridPatchBuilder(gridCrsDefinition, prototype);
  }
}
