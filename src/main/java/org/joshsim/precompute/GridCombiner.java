
/**
 * Utility to combine two DataGridLayers.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.precompute;

import java.math.BigDecimal;
import java.util.Optional;
import org.joshsim.engine.entity.base.GeoKey;
import org.joshsim.engine.geometry.EngineGeometry;
import org.joshsim.engine.geometry.EngineGeometryFactory;
import org.joshsim.engine.geometry.PatchBuilderExtents;
import org.joshsim.engine.geometry.PatchBuilderExtentsBuilder;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.engine.value.type.EngineValue;

/**
 * Utility to combine two DataGridLayers.
 *
 * <p>Utility which combines two DataGridLayers by specifying which one's values should take
 * precedence if there is a collision. Note that this specifically combines into a
 * DoublePrecomputedGrid.</p>
 */
public class GridCombiner {

  private final EngineGeometryFactory geometryFactory;

  /**
   * Create a new grid combiner which uses the given factory to build geometries.
   *
   * @param geometryFactory The factory to use when building geometries within the new grid or when
   *     supporting its construction.
   */
  public GridCombiner(EngineGeometryFactory geometryFactory) {
    this.geometryFactory = geometryFactory;
  }

  /**
   * Combine two DataGridLayers into a single DataGridLayer.
   *
   * <p>Combine two DataGridLayers together into a single DataGridLayer where values from the right
   * grid are used if there is a conflict. Note that this specifically combines into a
   * DoublePrecomputedGrid so values are held in memory.</p>
   *
   * @param left The first DataGridLayer to combine and from which values are overwritten by right
   *     in the case of a conflict.
   * @param right The second DataGridLayer to combine and which takes precedence if there is a
   *     conflict.
   * @return Newly created DataGridLayer which combines the two input layers.
   */
  public DataGridLayer combine(DataGridLayer left, DataGridLayer right) {
    PatchBuilderExtents combinedExtents = getCombinedExtents(left, right);
    long minTimestep = getMinTimestep(left, right);
    long maxTimestep = getMaxTimestep(left, right);
    Units units = getUnits(left, right);

    DoublePrecomputedGrid combinedGrid = new DoublePrecomputedGridBuilder()
        .setEngineValueFactory(EngineValueFactory.getDefault())
        .setExtents(combinedExtents)
        .setTimestepRange(minTimestep, maxTimestep)
        .setUnits(units)
        .build();

    // Add values from left grid first
    addInValues(combinedGrid, left);
    
    // Then overlay values from right grid (taking precedence)
    addInValues(combinedGrid, right);

    return combinedGrid;
  }

  /**
   * Determine the combined superset of extents for the two given grids.
   *
   * <p>Determine the combined extents for the two given grids. Note that, in the case that one
   * grid extents does not fit fully within another, the minimum and maximum values of the returned
   * extents will be expanded to encompass both.</p>
   *
   * @param left The first DataGridLayer from which the fully enclosing extents should be built.
   * @param right The second DataGridLayer from which the fully enclosing extents should be built.
   * @return Extents which fully enclose the extents of both input grids. These extents are to be
   *     be given in grid-space.
   */
  private PatchBuilderExtents getCombinedExtents(DataGridLayer left, DataGridLayer right) {
    PatchBuilderExtentsBuilder builder = new PatchBuilderExtentsBuilder();
    
    // Get min/max coordinates from both grids
    BigDecimal leftMinX = BigDecimal.valueOf(left.getMinX());
    BigDecimal leftMaxX = BigDecimal.valueOf(left.getMaxX());
    BigDecimal leftMinY = BigDecimal.valueOf(left.getMinY());
    BigDecimal leftMaxY = BigDecimal.valueOf(left.getMaxY());
    
    BigDecimal rightMinX = BigDecimal.valueOf(right.getMinX());
    BigDecimal rightMaxX = BigDecimal.valueOf(right.getMaxX());
    BigDecimal rightMinY = BigDecimal.valueOf(right.getMinY());
    BigDecimal rightMaxY = BigDecimal.valueOf(right.getMaxY());

    // Find the most extreme points to encompass both grids
    BigDecimal topLeftX = leftMinX.min(rightMinX);
    BigDecimal topLeftY = leftMaxY.max(rightMaxY);
    BigDecimal bottomRightX = leftMaxX.max(rightMaxX);
    BigDecimal bottomRightY = leftMinY.min(rightMinY);

    return builder
        .setTopLeftX(topLeftX)
        .setTopLeftY(topLeftY)
        .setBottomRightX(bottomRightX)
        .setBottomRightY(bottomRightY)
        .build();
  }

  /**
   * Get the minimum timestep across both input grids.
   *
   * @param left The first grid from which the minimum timestep should be determined.
   * @param right The second grid from which the minimum timestep should be determined.
   * @return The minimum of the minimum timestep across both input grids.
   */
  private long getMinTimestep(DataGridLayer left, DataGridLayer right) {
    return Math.min(left.getMinTimestep(), right.getMinTimestep());
  }

  /**
   * Get the maximum timestep across both input grids.
   *
   * @param left The first grid from which the maximum timestep should be determined.
   * @param right The second grid from which the maximum timestep should be determined.
   * @return The maximum of the maximum timestep across both input grids.
   */
  private long getMaxTimestep(DataGridLayer left, DataGridLayer right) {
    return Math.max(left.getMaxTimestep(), right.getMaxTimestep());
  }

  /**
   * Get the units that should be used for the combined grid.
   *
   * <p>Get the units that should be used for the combined grid which, at this time, requires that
   * the two are precisely equal.</p>
   *
   * @param left The first grid from which the units should be determined.
   * @param right The first grid from which the units should be determined.
   * @return The units from the right grid.
   * @throws IllegalArgumentException If the units of the two input grids are not compatible.
   */
  private Units getUnits(DataGridLayer left, DataGridLayer right) {
    Units leftUnits = left.getUnits();
    Units rightUnits = right.getUnits();
    
    if (!leftUnits.equals(rightUnits)) {
      throw new IllegalArgumentException(String.format(
          "Units must be equal: left=%s, right=%s",
          leftUnits,
          rightUnits
      ));
    }
    
    return rightUnits;
  }

  /**
   * Add in values from a source grid into the combined grid.
   *
   * <p>Add in values from the given source grid into the given combined grid. This assumes that the
   * combinedGrid extents, time range, units, and type are compatible or a superset of the
   * source.</p>
   *
   * @param combinedGrid The grid into which all values from source will be copied.
   * @param source The grid from which values will be copied into combinedGrid.
   */
  private void addInValues(DoublePrecomputedGrid combinedGrid, DataGridLayer source) {
    for (long x = source.getMinX(); x <= source.getMaxX(); x++) {
      for (long y = source.getMinY(); y <= source.getMaxY(); y++) {
        for (long timestep = source.getMinTimestep(); 
             timestep <= source.getMaxTimestep(); 
             timestep++) {
          EngineGeometry geometry = geometryFactory.createPoint(
              BigDecimal.valueOf(x),
              BigDecimal.valueOf(y)
          );
          GeoKey key = new GeoKey(Optional.of(geometry), "");
          EngineValue value = source.getAt(key, timestep);
          combinedGrid.setAt(x, y, timestep, value.getAsDecimal().doubleValue());
        }
      }
    }
  }
}
