/**
 * A mutable data grids like those loaded from jshd files with uniform type.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.precompute;

import org.joshsim.engine.entity.base.GeoKey;
import org.joshsim.engine.geometry.PatchBuilderExtents;
import org.joshsim.engine.value.type.EngineValue;


/**
 * Grid which includes precomputed data for Josh simulations using the given uniform type.
 *
 * <p>Grid which uses a uniform comparable type as the precomputed value across all patches that are
 * converted to EngineValues at time of access.</p>
 */
public abstract class UniformPrecomputedGrid<T extends Comparable> implements
    PrecomputedGrid {

  private final long minX;
  private final long minY;
  private final long maxX;
  private final long maxY;
  private final long width;
  private final long height;
  private final long minTimestep;
  private final long maxTimestep;

  /**
   * Create a new precomputed grid.
   *
   * @param extents The extents of the simulation against which the the grid will be matched.
   * @param minTimestep The minimum timestep that needs to be available for this simulation.
   * @param maxTimestep The maximum timestep that needs to be available for this simulation.
   */
  public UniformPrecomputedGrid(PatchBuilderExtents extents, long minTimestep, long maxTimestep) {
    this.minTimestep = Math.min(minTimestep, maxTimestep);
    this.maxTimestep = Math.max(minTimestep, maxTimestep);

    long leftX = extents.getTopLeftX().longValue();
    long rightX = extents.getBottomRightX().longValue();
    long topY = extents.getTopLeftY().longValue();
    long bottomY = extents.getBottomRightY().longValue();
    System.out.println(leftX + ":" + rightX + ":" + topY + ":" + bottomY);

    minX = Math.min(leftX, rightX);
    minY = Math.min(topY, bottomY);
    maxX = Math.max(leftX, rightX);
    maxY = Math.max(topY, bottomY);
    width = maxX - minX + 1;
    height = maxY - minY + 1;
  }

  /**
   * Get the value at the given location and timestep.
   *
   * @param x The horizontal position of the value to retrieve in grid space as number of patches.
   * @param y The vertical position of the value to retrieve in grid space as number of patches.
   * @param timestep The timestep count at which to get the value.
   */
  public abstract EngineValue getAt(long x, long y, long timestep);

  @Override
  public EngineValue getAt(GeoKey location, long timestep) {
    long x = location.getCenterX().longValue();
    long y = location.getCenterY().longValue();
    return getAt(x, y, timestep);
  }

  @Override
  public boolean isCompatible(PatchBuilderExtents other, long timestepA, long timestepB) {
    long leftX = other.getTopLeftX().longValue();
    long rightX = other.getBottomRightX().longValue();
    long topY = other.getTopLeftY().longValue();
    long bottomY = other.getBottomRightY().longValue();

    long otherMinX = Math.min(leftX, rightX);
    long otherMinY = Math.min(topY, bottomY);
    long otherMaxX = Math.max(leftX, rightX);
    long otherMaxY = Math.max(topY, bottomY);
    long otherMinTimestep = Math.min(timestepA, timestepB);
    long otherMaxTimestep = Math.max(timestepA, timestepB);

    boolean minHorizOk = otherMinX >= minX;
    boolean minVertOk = otherMinY >= minY;
    boolean maxHorizOk = otherMaxX <= maxX;
    boolean maxVertOk = otherMaxY <= maxY;
    boolean minTimestepOk = otherMinTimestep >= minTimestep;
    boolean maxTimestepOk = otherMaxTimestep <= maxTimestep;

    return minHorizOk && minVertOk && maxHorizOk && maxVertOk && minTimestepOk && maxTimestepOk;
  }

  /**
   * Get the minimum X coordinate of this grid.
   *
   * @return The minimum X coordinate in grid-space.
   */
  public long getMinX() {
    return minX;
  }

  /**
   * Get the minimum Y coordinate of this grid.
   *
   * @return The minimum Y coordinate in grid-space.
   */
  public long getMinY() {
    return minY;
  }

  /**
   * Get the maximum X coordinate of this grid.
   *
   * @return The maximum X coordinate in grid-space.
   */
  public long getMaxX() {
    return maxX;
  }

  /**
   * Get the maximum Y coordinate of this grid.
   *
   * @return The maximum Y coordinate in grid-space.
   */
  public long getMaxY() {
    return maxY;
  }

  /**
   * Get the width of this grid in number of horizontal patches.
   *
   * @return The x size of this grid in grid-space.
   */
  public long getWidth() {
    return width;
  }

  /**
   * Get the height of this grid in number of horizontal patches.
   *
   * @return The y size of this grid in grid-space.
   */
  public long getHeight() {
    return height;
  }

  /**
   * The earliest timestep available in this grid.
   *
   * @return The minimum available timestep.
   */
  public long getMinTimestep() {
    return minTimestep;
  }

  /**
   * The latest timestep available in this grid.
   *
   * @return The maximum available timestep.
   */
  public long getMaxTimestep() {
    return maxTimestep;
  }

}
