/**
 * Represents a grid cell offset (offsetX, offsetY) for circle rasterization.
 *
 * <p>Stored as primitive ints for memory efficiency. Each instance consumes
 * 8 bytes (2 × 4-byte ints) plus object overhead.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.simulation;

/**
 * Represents a grid cell offset for spatial queries.
 */
class GridOffset {
  private final int offsetX;
  private final int offsetY;

  /**
   * Creates a new grid offset pair.
   *
   * @param offsetX the x-offset in grid cells
   * @param offsetY the y-offset in grid cells
   */
  GridOffset(int offsetX, int offsetY) {
    this.offsetX = offsetX;
    this.offsetY = offsetY;
  }

  /**
   * Gets the x-offset in grid cells.
   *
   * @return the x-offset
   */
  public int getOffsetX() {
    return offsetX;
  }

  /**
   * Gets the y-offset in grid cells.
   *
   * @return the y-offset
   */
  public int getOffsetY() {
    return offsetY;
  }
}
