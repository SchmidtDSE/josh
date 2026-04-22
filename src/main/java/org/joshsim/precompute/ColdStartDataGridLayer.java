/**
 * Decorator that adds cold-start spin-up timestep remapping to a DataGridLayer.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.precompute;

import java.util.Random;
import org.joshsim.engine.entity.base.GeoKey;
import org.joshsim.engine.geometry.PatchBuilderExtents;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.type.EngineValue;


/**
 * DataGridLayer decorator that remaps timesteps for cold-start spin-up.
 *
 * <p>During the cold-start phase (timesteps 0 through coldStartDuration - 1), each timestep is
 * mapped to a randomly sampled timestep from the inner grid's available range. For a given
 * cold-start timestep, the same source timestep is used across all spatial locations, preserving
 * spatial coherence. After the cold-start phase, timesteps are offset to align with the inner
 * grid's real time series.</p>
 *
 * <p>The random mapping is pre-generated at construction time from a provided Random instance,
 * ensuring reproducibility when seeded.</p>
 */
public class ColdStartDataGridLayer implements DataGridLayer {

  private final DataGridLayer inner;
  private final long coldStartDuration;
  private final long[] coldStartMapping;
  private final long innerTimestepCount;

  /**
   * Create a new cold-start decorator around an existing DataGridLayer.
   *
   * @param inner The underlying data grid to wrap.
   * @param coldStartDuration The number of cold-start timesteps to prepend. Must be non-negative.
   * @param rng Random instance used to generate the cold-start timestep mapping.
   * @throws IllegalArgumentException if coldStartDuration is negative or inner has no timesteps.
   */
  public ColdStartDataGridLayer(DataGridLayer inner, long coldStartDuration, Random rng) {
    if (coldStartDuration < 0) {
      throw new IllegalArgumentException("Cold-start duration must be non-negative.");
    }

    this.inner = inner;
    this.coldStartDuration = coldStartDuration;
    this.innerTimestepCount = inner.getMaxTimestep() - inner.getMinTimestep() + 1;

    if (innerTimestepCount <= 0) {
      throw new IllegalArgumentException("Inner grid must have at least one timestep.");
    }

    this.coldStartMapping = new long[(int) coldStartDuration];
    for (int i = 0; i < coldStartDuration; i++) {
      coldStartMapping[i] = inner.getMinTimestep() + rng.nextInt((int) innerTimestepCount);
    }
  }

  @Override
  public EngineValue getAt(GeoKey location, long timestep) {
    long remappedTimestep = remapTimestep(timestep);
    return inner.getAt(location, remappedTimestep);
  }

  @Override
  public boolean isCompatible(PatchBuilderExtents extents, long minTimestep, long maxTimestep) {
    long adjustedMin = remapTimestep(Math.min(minTimestep, maxTimestep));
    long adjustedMax = remapTimestep(Math.max(minTimestep, maxTimestep));
    return inner.isCompatible(extents, adjustedMin, adjustedMax);
  }

  @Override
  public Units getUnits() {
    return inner.getUnits();
  }

  @Override
  public long getMinX() {
    return inner.getMinX();
  }

  @Override
  public long getMinY() {
    return inner.getMinY();
  }

  @Override
  public long getMaxX() {
    return inner.getMaxX();
  }

  @Override
  public long getMaxY() {
    return inner.getMaxY();
  }

  @Override
  public long getWidth() {
    return inner.getWidth();
  }

  @Override
  public long getHeight() {
    return inner.getHeight();
  }

  @Override
  public long getMinTimestep() {
    return 0;
  }

  @Override
  public long getMaxTimestep() {
    return coldStartDuration + innerTimestepCount - 1;
  }

  /**
   * Remap a timestep according to cold-start rules.
   *
   * <p>Timesteps in [0, coldStartDuration) are mapped to pre-generated random inner timesteps.
   * Timesteps in [coldStartDuration, coldStartDuration + innerTimestepCount) are offset to align
   * with the inner grid. Timesteps outside these ranges are passed through unchanged for
   * compatibility with literal timestep references.</p>
   *
   * @param timestep The timestep to remap.
   * @return The remapped timestep suitable for the inner grid.
   */
  private long remapTimestep(long timestep) {
    if (timestep >= 0 && timestep < coldStartDuration) {
      return coldStartMapping[(int) timestep];
    } else if (timestep >= coldStartDuration
        && timestep < coldStartDuration + innerTimestepCount) {
      return timestep - coldStartDuration + inner.getMinTimestep();
    } else {
      return timestep;
    }
  }

}
