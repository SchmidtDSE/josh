
/**
 * Structure to represent the extents of a grid in terms of its corner coordinates.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.geometry;

import java.math.BigDecimal;


/**
 * Structure to store and validate corner coordinates for grid construction.
 */
public class PatchBuilderExtents {
  private final BigDecimal topLeftX;
  private final BigDecimal topLeftY;
  private final BigDecimal bottomRightX;
  private final BigDecimal bottomRightY;

  /**
   * Create extents over the given coordinates.
   *
   * @param topLeftX The x-coordinate of the top-left corner
   * @param topLeftY The y-coordinate of the top-left corner
   * @param bottomRightX The x-coordinate of the bottom-right corner
   * @param bottomRightY The y-coordinate of the bottom-right corner
   * @throws IllegalArgumentException if coordinates don't form a valid rectangle
   */
  public PatchBuilderExtents(BigDecimal topLeftX, BigDecimal topLeftY, BigDecimal bottomRightX,
                             BigDecimal bottomRightY) {

    assertNotNull(topLeftX, "top left x");
    assertNotNull(topLeftY, "top left y");
    assertNotNull(bottomRightX, "bottom right x");
    assertNotNull(bottomRightY, "bottom right y");

    this.topLeftX = topLeftX;
    this.topLeftY = topLeftY;
    this.bottomRightX = bottomRightX;
    this.bottomRightY = bottomRightY;
  }

  /**
   * Gets the x-coordinate of the top-left corner.
   *
   * @return the x-coordinate as a BigDecimal
   */
  public BigDecimal getTopLeftX() {
    return topLeftX;
  }

  /**
   * Gets the y-coordinate of the top-left corner.
   *
   * @return the y-coordinate as a BigDecimal
   */
  public BigDecimal getTopLeftY() {
    return topLeftY;
  }

  /**
   * Gets the x-coordinate of the bottom-right corner.
   *
   * @return the x-coordinate as a BigDecimal
   */
  public BigDecimal getBottomRightX() {
    return bottomRightX;
  }

  /**
   * Gets the y-coordinate of the bottom-right corner.
   *
   * @return the y-coordinate as a BigDecimal
   */
  public BigDecimal getBottomRightY() {
    return bottomRightY;
  }

  /**
   * Validates that the provided value is not null, throwing an exception if the check fails.
   *
   * @param value The BigDecimal value to validate.
   * @param description A description of the value being validated, used in the exception message.
   * @throws IllegalArgumentException if the provided value is null.
   */
  private void assertNotNull(BigDecimal value, String description) {
    if (value == null) {
      throw new IllegalArgumentException(description + " cannot be null");
    }
  }

}
