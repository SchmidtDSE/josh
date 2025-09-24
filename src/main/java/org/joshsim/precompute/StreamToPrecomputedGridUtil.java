package org.joshsim.precompute;

import java.util.Optional;
import java.util.stream.Stream;
import org.joshsim.engine.geometry.PatchBuilderExtents;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueFactory;


/**
 * Utility which converts from streams to a precomputed grid.
 *
 * <p>Utility which converts from a stream of geo keys and corresponding engine values to a
 * dense precomputed grid once per timestep.</p>
 */
public class StreamToPrecomputedGridUtil {

  /**
   * Convert a set of streams of geo keys and values to a precomputed grid.
   *
   * @param engineValueFactory The factory which should be used in creating values returned.
   * @param streamGetter The stream getter which will provide the streams for each timestep.
   * @param extents The extents of the grid to be created.
   * @param minTimestep The start of the timestep series that should be supported in this grid.
   * @param maxTimestep The end of the timestep series that should be supported in this grid.
   * @param units The units that returned EngineValues should be created with.
   * @return The precomputed grid created from the streams.
   */
  public static DataGridLayer streamToGrid(EngineValueFactory engineValueFactory,
        StreamGetter streamGetter, PatchBuilderExtents extents, long minTimestep,
        long maxTimestep, Units units) {
    return streamToGrid(engineValueFactory, streamGetter, extents, minTimestep, maxTimestep,
        units, Optional.empty());
  }

  /**
   * Convert a set of streams of geo keys and values to a precomputed grid with default value.
   *
   * @param engineValueFactory The factory which should be used in creating values returned.
   * @param streamGetter The stream getter which will provide the streams for each timestep.
   * @param extents The extents of the grid to be created.
   * @param minTimestep The start of the timestep series that should be supported in this grid.
   * @param maxTimestep The end of the timestep series that should be supported in this grid.
   * @param units The units that returned EngineValues should be created with.
   * @param defaultValue Optional default value to fill grid spaces before copying data.
   * @return The precomputed grid created from the streams.
   */
  public static DataGridLayer streamToGrid(EngineValueFactory engineValueFactory,
        StreamGetter streamGetter, PatchBuilderExtents extents, long minTimestep,
        long maxTimestep, Units units, Optional<Double> defaultValue) {

    DoublePrecomputedGrid grid = new DoublePrecomputedGrid(
        engineValueFactory,
        extents,
        minTimestep,
        maxTimestep,
        units
    );

    // Fill with default value if provided
    if (defaultValue.isPresent()) {
      grid.fill(defaultValue.get());
    }

    for (long timestep = minTimestep; timestep <= maxTimestep; timestep++) {
      Stream<PatchKeyConverter.ProjectedValue> values = streamGetter.getForTimestep(timestep);
      final long timestepRealized = timestep;
      values.forEach(entry -> {
        double value = entry.getValue().doubleValue();

        // Skip values that match the default value (within tolerance)
        if (defaultValue.isPresent()) {
          double defaultVal = defaultValue.get();
          double tolerance = 0.000001;
          if (Math.abs(value - defaultVal) <= tolerance) {
            return; // Skip this value
          }
        }

        grid.setAt(
            entry.getX().longValue(),
            entry.getY().longValue(),
            timestepRealized,
            value
        );
      });
    }

    return grid;
  }

  /**
   * Strategy for getting a stream of projected values for a given timestep.
   */
  public interface StreamGetter {

    /**
     * Get a stream of projected values for a given timestep.
     *
     * @param timestep The timestep for which to get the stream.
     * @return The stream of projected values for the requested timestep.
     */
    Stream<PatchKeyConverter.ProjectedValue> getForTimestep(long timestep);

  }
}
