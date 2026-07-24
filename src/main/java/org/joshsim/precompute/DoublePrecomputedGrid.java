/**
 * Precomputed grid with double (64 bit) precision.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.precompute;

import java.util.Optional;
import org.joshsim.engine.geometry.PatchBuilderExtents;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.ValueSupportFactory;
import org.joshsim.engine.value.type.EngineValue;


/**
 * Create a new precomputed grid with double precision.
 */
public class DoublePrecomputedGrid extends UniformPrecomputedGrid<Double> {

  private final ValueSupportFactory factory;
  private final PatchBuilderExtents extents;
  private final Units units;
  private final double[][][] innerValues;
  private final Optional<TimeAxis> timeAxis;

  /**
   * Create a new precomputed grid.
   *
   * @param engineValueFactory Factory through which to build returned values.
   * @param extents The extents of the grid to be created.
   * @param minTimestep The start of the timestep series that should be supported in this grid.
   * @param maxTimestep The end of the timestep series that should be supported in this grid.
   * @param units The units that returned EngineValues should be created with.
   */
  DoublePrecomputedGrid(ValueSupportFactory engineValueFactory, PatchBuilderExtents extents,
        long minTimestep, long maxTimestep, Units units) {
    this(engineValueFactory, extents, minTimestep, maxTimestep, units, Optional.empty());
  }

  /** Creates an empty grid with optional persisted temporal metadata. */
  DoublePrecomputedGrid(ValueSupportFactory engineValueFactory, PatchBuilderExtents extents,
      long minTimestep, long maxTimestep, Units units, Optional<TimeAxis> timeAxis) {
    super(extents, minTimestep, maxTimestep);

    this.factory = engineValueFactory;
    this.extents = extents;
    this.units = units;
    this.timeAxis = timeAxis;

    int width = (int) getWidth();
    int height = (int) getHeight();
    int timestepsCut = (int) (getMaxTimestep() - getMinTimestep() + 1);

    innerValues = new double[timestepsCut][height][width];
    timeAxis.ifPresent(axis -> {
      if (axis.getCount() != timestepsCut) {
        throw new IllegalArgumentException(
            "Time axis count must equal the number of grid timesteps");
      }
    });
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
  public DoublePrecomputedGrid(ValueSupportFactory engineValueFactory, PatchBuilderExtents extents,
        long minTimestep, long maxTimestep, Units units, double[][][] innerValues) {
    this(engineValueFactory, extents, minTimestep, maxTimestep, units, innerValues,
        Optional.empty());
  }

  /**
   * Creates a new grid with optional persisted temporal metadata.
   *
   * @param engineValueFactory Factory used to create returned values.
   * @param extents Grid extents.
   * @param minTimestep Minimum stored grid timestep.
   * @param maxTimestep Maximum stored grid timestep.
   * @param units Units of grid values.
   * @param innerValues Grid values.
   * @param timeAxis Declared temporal coordinate system, if present.
   */
  public DoublePrecomputedGrid(ValueSupportFactory engineValueFactory, PatchBuilderExtents extents,
      long minTimestep, long maxTimestep, Units units, double[][][] innerValues,
      Optional<TimeAxis> timeAxis) {
    super(extents, minTimestep, maxTimestep);

    this.factory = engineValueFactory;
    this.extents = extents;
    this.units = units;

    this.innerValues = innerValues;
    this.timeAxis = timeAxis;
    timeAxis.ifPresent(axis -> {
      long gridCount = maxTimestep - minTimestep + 1;
      if (axis.getCount() != gridCount) {
        throw new IllegalArgumentException(
            "Time axis count must equal the number of grid timesteps");
      }
    });
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
    if (horizOutBounds) {
      throw new IllegalArgumentException(String.format(
          "Horizontal out of bounds (%d < 0 || %d >= %d)",
          horizCut,
          horizCut,
          getWidth()
      ));
    }

    boolean vertOutBounds = vertCut < 0 || vertCut >= getHeight();
    if (vertOutBounds) {
      throw new IllegalArgumentException(String.format(
          "Vertical out of bounds (%d < 0 || %d >= %d)",
          vertCut,
          vertCut,
          getHeight()
      ));
    }

    boolean timeOutBounds = timestepCut < 0 || timestepCut > (getMaxTimestep() - getMinTimestep());
    if (timeOutBounds) {
      throw new IllegalArgumentException(String.format(
          "Timestep out of bounds (%d < 0 || %d >= %d)",
          timestepCut,
          timestepCut,
          (getMaxTimestep() - getMinTimestep())
      ));
    }

    innerValues[timestepCut][vertCut][horizCut] = value;
  }

  @Override
  public void fill(Double value) {
    for (int t = 0; t < innerValues.length; t++) {
      for (int y = 0; y < innerValues[t].length; y++) {
        for (int x = 0; x < innerValues[t][y].length; x++) {
          innerValues[t][y][x] = value;
        }
      }
    }
  }

  @Override
  public EngineValue getAt(long x, long y, long timestep) {
    int horizCut = (int) (x - getMinX());
    int vertCut = (int) (y - getMinY());
    int timestepCut = (int) (timestep - getMinTimestep());

    if (horizCut < 0 || horizCut >= getWidth()) {
      throw new IllegalArgumentException(String.format(
          "Horizontal out of bounds (%d < 0 || %d >= %d)",
          horizCut,
          horizCut,
          getWidth()
      ));
    }

    if (vertCut < 0 || vertCut >= getHeight()) {
      throw new IllegalArgumentException(String.format(
          "Vertical out of bounds (%d < 0 || %d >= %d)",
          vertCut,
          vertCut,
          getHeight()
      ));
    }

    if (timestepCut < 0 || timestepCut > (getMaxTimestep() - getMinTimestep())) {
      throw new IllegalArgumentException(String.format(
          "Timestep out of bounds (%d < 0 || %d >= %d)",
          timestepCut,
          timestepCut,
          (getMaxTimestep() - getMinTimestep())
      ));
    }

    double value = innerValues[timestepCut][vertCut][horizCut];
    return factory.buildForNumber(value, units);
  }

  @Override
  public Units getUnits() {
    return units;
  }

  /** Returns the declared temporal coordinate system, if this grid has one. */
  public Optional<TimeAxis> getTimeAxis() {
    return timeAxis;
  }

  /**
   * Creates a copy of this grid with the supplied temporal metadata.
   *
   * <p>Preprocessing constructs numeric grid values before it has finalized the declared temporal
   * axis. This method keeps that pipeline immutable while attaching the validated axis just before
   * serialization.</p>
   *
   * @param newTimeAxis Temporal metadata to attach.
   * @return A grid with identical values and the supplied metadata.
   */
  public DoublePrecomputedGrid withTimeAxis(TimeAxis newTimeAxis) {
    return new DoublePrecomputedGrid(
        factory, extents, getMinTimestep(), getMaxTimestep(), units, innerValues,
        Optional.of(newTimeAxis));
  }
}
