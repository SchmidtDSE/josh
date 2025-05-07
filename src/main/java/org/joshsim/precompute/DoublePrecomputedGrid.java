/**
 * Precomputed grid with double (64 bit) precision.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.precompute;

import java.math.BigDecimal;
import org.joshsim.engine.geometry.PatchBuilderExtents;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.engine.value.type.EngineValue;


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
  DoublePrecomputedGrid(EngineValueFactory engineValueFactory, PatchBuilderExtents extents,
        long minTimestep, long maxTimestep, Units units) {
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
  DoublePrecomputedGrid(EngineValueFactory engineValueFactory, PatchBuilderExtents extents,
        long minTimestep, long maxTimestep, Units units, double[][][] innerValues) {
    super(extents, minTimestep, maxTimestep);

    this.factory = engineValueFactory;
    this.units = units;

    this.innerValues = innerValues;
  }

  /**
   * Set a value in the precomputed grid at the given coordinates and timestep.
   *
   * @param x The x-coordinate within the grid where the value will be set.
   * @param y The y-coordinate within the grid where the value will be set.
   * @param timestep The timestep at which the value will be set.
   * @param value The double value to be set at the specified location and timestep.
   */
  public void setAt(long x, long y, long timestep, double value) {
    int horizCut = (int) (x - getMinX());
    int vertCut = (int) (y - getMinY());
    int timestepCut = (int) (timestep - getMinTimestep());

    boolean horizOutBounds = horizCut < 0 || horizCut >= getWidth();
    boolean vertOutBounds = vertCut < 0 || vertCut >= getHeight();
    boolean timeOutBounds = timestepCut < 0 || timestepCut >= getMaxTimestep() - getMinTimestep();

    if (horizOutBounds || vertOutBounds || timeOutBounds) {
      throw new IllegalArgumentException("Coordinates or timestep out of bounds");
    }

    innerValues[timestepCut][vertCut][horizCut] = value;
  }

  @Override
  public EngineValue getAt(long x, long y, long timestep) {
    int horizCut = (int) (x - getMinX());
    int vertCut = (int) (y - getMinY());
    int timestepCut = (int) (timestep - getMinTimestep());
    double value = innerValues[timestepCut][vertCut][horizCut];
    return factory.build(BigDecimal.valueOf(value), units);
  }

}
