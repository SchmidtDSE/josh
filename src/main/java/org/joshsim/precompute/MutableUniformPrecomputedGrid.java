/**
 * A mutable data grids like those loaded from jshd files with uniform type.
 *
 * @license BSD-3-Clause
 */

package org.joshsim;

import org.joshsim.engine.entity.base.GeoKey;
import org.joshsim.engine.geometry.PatchBuilderExtents;
import org.joshsim.engine.value.type.EngineValue;


/**
 * Grid which includes precomputed data for Josh simulations using the given uniform type.
 *
 * <p>Grid which uses a uniform comparable type as the precomputed value across all patches that are
 * converted to EngineValues at time of access.</p>
 */
public abstract class MutableUniformPrecomputedGrid<T extends Comparable> implements
      PrecomputedGrid {

  private final long minX;
  private final long minY;
  private final long maxX;
  private final long maxY;
  private final long width;
  private final long height;
  private final long timesteps;

  /**
   * Create a new precomputed grid.
   *
   * @param extents The extents of the simulation against which the the grid will be matched.
   * @param timesteps The timesteps that need to be available for this simulation.
   */
  public MutableUniformPrecomputedGrid(PatchBuilderExtents extents, long timesteps) {
    this.timesteps = timesteps;
    
    long leftX = extents.getTopLeftX().longValue();
    long rightX = extents.getBottomRightX().longValue();
    long topY = extents.getTopLeftY().longValue();
    long bottomY = extents.getBottomRightY().longValue();
    
    minX = leftX < rightX ? leftX : rightX;
    minY = topY < bottomY ? topY : bottomY;
    maxX = leftX > rightX ? leftX : rightX;
    maxY = topY > bottomY ? topY : bottomY;
    width = maxX - minX + 1;
    height = maxY - minY + 1;
  }
  
  @Override
  public EngineValue getAt(GeoKey location, long timestep) {
    long x = location.getCenterX().longValue();
    long y = location.getCenterY().longValue();
    return getAt(x, y, timestep);
  }

  @Override
  public boolean isCompatible(PatchBuilderExtents other, long timesteps) {
    long leftX = extents.getTopLeftX().longValue();
    long rightX = extents.getBottomRightX().longValue();
    long topY = extents.getTopLeftY().longValue();
    long bottomY = extents.getBottomRightY().longValue();

    long otherMinX = leftX < rightX ? leftX : rightX;
    long otherMinY = topY < bottomY ? topY : bottomY;
    long otherMaxX = leftX > rightX ? leftX : rightX;
    long otherMaxY = topY > bottomY ? topY : bottomY;

    boolean minXOk = otherMinX >= minX;
    boolean minYOk = otherMinY >= minY;
    boolean maxXOk = otherMaxX <= maxX;
    boolean maxYOk = otherMaxY <= maxY;

    return minXOk && minYOk && maxXOk && maxYOk;
  }

  /**
   * Get the value at the given location and timestep.
   *
   * @param x The horizontal position of the value to retrieve in grid space as number of patches.
   * @param y The vertical position of the value to retrieve in grid space as number of patches.
   * @param timestep The timestep count at which to get the value.
   */
  protected abstract EngineValue getAt(long x, long y, long timestep);
  
}