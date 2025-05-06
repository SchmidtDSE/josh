/**
 * Precomputed grid with double (64 bit) precision.
 *
 * @license BSD-3-Clause
 */
package org.joshsim.precompute;

import org.joshsim.engine.geometry.PatchBuilderExtents;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.engine.value.type.EngineValue;

import java.math.BigDecimal;


/**
 * Create a new precomputed grid with double precision.
 */
public class DoublePrecomputedGrid extends UniformPrecomputedGrid<Double> {

  private final EngineValueFactory factory;
  private final Units units;
  private final double[][][] innerValues;

  /**
   * Create a new precomputed grid.
   *
   * @param engineValueFactory Factory through which to build returned values.
   * @param extents The extents of the grid to be created.
   * @param minTimestep The start of the timestep series that should be supported in this grid.
   * @param maxTimestep The end of the timestep series that should be supported in this grid.
   * @param units The units that returned EngineValues should be created with.
   */
  DoublePrecomputedGrid(EngineValueFactory engineValueFactory, PatchBuilderExtents extents, long minTimestep,
        long maxTimestep, Units units) {
    super(extents, minTimestep, maxTimestep);

    this.factory = engineValueFactory;
    this.units = units;

    int width = (int) getWidth();
    int height = (int) getHeight();
    int timestepsCut = (int) (getMaxTimestep() - getMinTimestep());

    innerValues = new double[timestepsCut][height][width];
  }

  /**
   * Create a new precomputed grid.
   *
   * @param engineValueFactory Factory through which to build returned values.
   * @param extents The extents of the grid to be created.
   * @param minTimestep The start of the timestep series that should be supported in this grid.
   * @param maxTimestep The end of the timestep series that should be supported in this grid.
   * @param units The units that returned EngineValues should be created with.
   * @param innerValues The values with which to populate the grid.
   */
  DoublePrecomputedGrid(EngineValueFactory engineValueFactory, PatchBuilderExtents extents, long minTimestep,
        long maxTimestep, Units units, double[][][] innerValues) {
    super(extents, minTimestep, maxTimestep);

    this.factory = engineValueFactory;
    this.units = units;

    int width = (int) getWidth();
    int height = (int) getHeight();
    int timestepsCut = (int) (getMaxTimestep() - getMinTimestep());

    this.innerValues = innerValues;
  }

  @Override
  public EngineValue getAt(long x, long y, long timestep) {
    int xCut = (int) (x - getMinX());
    int yCut = (int) (y - getMinY());
    int timestepCut = (int) (timestep - getMinTimestep());
    double value = innerValues[timestepCut][yCut][xCut];
    return factory.build(BigDecimal.valueOf(value), units);
  }

}
