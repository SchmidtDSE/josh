
package org.joshsim.precompute;

import org.joshsim.engine.geometry.PatchBuilderExtents;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueFactory;

/**
 * Builder for DoublePrecomputedGrid.
 * Creates and configures DoublePrecomputedGrid instances.
 */
public class DoublePrecomputedGridBuilder {
  private EngineValueFactory engineValueFactory;
  private PatchBuilderExtents extents;
  private long minTimestep;
  private long maxTimestep;
  private Units units;
  private double[][][] innerValues;

  /**
   * Sets the engine value factory for the grid.
   *
   * @param factory Factory for creating EngineValue objects
   * @return This builder instance
   */
  public DoublePrecomputedGridBuilder setEngineValueFactory(EngineValueFactory factory) {
    this.engineValueFactory = factory;
    return this;
  }

  /**
   * Sets the extents for the grid.
   *
   * @param extents The extents of the grid to be created
   * @return This builder instance
   */
  public DoublePrecomputedGridBuilder setExtents(PatchBuilderExtents extents) {
    this.extents = extents;
    return this;
  }

  /**
   * Sets the timestep range for the grid.
   *
   * @param minTimestep The start of the timestep series
   * @param maxTimestep The end of the timestep series
   * @return This builder instance
   */
  public DoublePrecomputedGridBuilder setTimestepRange(long minTimestep, long maxTimestep) {
    this.minTimestep = minTimestep;
    this.maxTimestep = maxTimestep;
    return this;
  }

  /**
   * Sets the units for the grid.
   *
   * @param units The units that returned EngineValues should be created with
   * @return This builder instance
   */
  public DoublePrecomputedGridBuilder setUnits(Units units) {
    this.units = units;
    return this;
  }

  /**
   * Sets the inner values for the grid.
   *
   * @param innerValues The values with which to populate the grid
   * @return This builder instance
   */
  public DoublePrecomputedGridBuilder setInnerValues(double[][][] innerValues) {
    this.innerValues = innerValues;
    return this;
  }

  /**
   * Builds and returns a configured DoublePrecomputedGrid instance.
   *
   * @return A new DoublePrecomputedGrid instance
   * @throws IllegalArgumentException if required parameters are missing
   */
  public DoublePrecomputedGrid build() {
    if (engineValueFactory == null) {
      throw new IllegalArgumentException("EngineValueFactory must be set");
    }
    if (extents == null) {
      throw new IllegalArgumentException("Extents must be set");
    }
    if (units == null) {
      throw new IllegalArgumentException("Units must be set");
    }

    if (innerValues != null) {
      return new DoublePrecomputedGrid(
          engineValueFactory,
          extents,
          minTimestep,
          maxTimestep,
          units,
          innerValues);
    } else {
      return new DoublePrecomputedGrid(
          engineValueFactory,
          extents,
          minTimestep,
          maxTimestep,
          units);
    }
  }
}
