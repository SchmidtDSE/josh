
/**
 * Convienence functions which perform binary conversion in jshd format.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.precompute;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility which facilitates conversion between jshd format and PrecomputedGrids.
 *
 * <p>Utility which facilitates conversions involving the jshd binary format. This has the first 64
 * bits for the minimum x coordinate followed by 64 bits for maximum x coordinate, 64 bits for
 * minimum y coordinate, 64 bits for maximum y coordinate, 64 bits for minimum timestep, and 64 bits
 * for maximum timestep to conclude the header section. After the header containing these longs, the
 * doubles of the grid through time are listed one after another in which each row is written in
 * ordered from low to high column and then each set of rows follows from the minimum to maximum
 * timestep.</p>
 */
public class JshdUtil {

  /**
   * Load a DoublePrecomputedGrid from the given bytes serialization.
   *
   * @param bytes The bytes following the jshd format specification from which to parse a
   *     PrecomputedGrid.
   * @return A DoublePrecomputedGrid parsed from the given bytes.
   */
  public static DoublePrecomputedGrid loadFromBytes(byte[] bytes) {
    ByteBuffer buffer = ByteBuffer.wrap(bytes);
    
    // Read header
    long minX = buffer.getLong();
    long maxX = buffer.getLong();
    long minY = buffer.getLong();
    long maxY = buffer.getLong();
    long minTimestep = buffer.getLong();
    long maxTimestep = buffer.getLong();
    
    int width = (int)(maxX - minX + 1);
    int height = (int)(maxY - minY + 1);
    int timesteps = (int)(maxTimestep - minTimestep + 1);
    
    // Read grid data
    List<double[][]> timeframes = new ArrayList<>();
    for (int t = 0; t < timesteps; t++) {
      double[][] grid = new double[height][width];
      for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
          grid[y][x] = buffer.getDouble();
        }
      }
      timeframes.add(grid);
    }
    
    return new DoublePrecomputedGrid(minX, maxX, minY, maxY, minTimestep, timeframes);
  }

  /**
   * Convert DoublePrecomputedGrid from the given bytes serialization.
   *
   * @param bytes The bytes following the jshd format specification from which to parse a
   *     PrecomputedGrid.
   * @return A DoublePrecomputedGrid parsed from the given bytes.
   */
  public static byte[] serializeToBytes(DoublePrecomputedGrid target) {
    int width = (int)(target.getMaxX() - target.getMinX() + 1);
    int height = (int)(target.getMaxY() - target.getMinY() + 1);
    int timesteps = target.getTimeframes().size();
    
    // Calculate buffer size: 6 longs for header + doubles for all grid values
    int bufferSize = (6 * Long.BYTES) + (width * height * timesteps * Double.BYTES);
    ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
    
    // Write header
    buffer.putLong(target.getMinX());
    buffer.putLong(target.getMaxX());
    buffer.putLong(target.getMinY());
    buffer.putLong(target.getMaxY());
    buffer.putLong(target.getMinTimestep());
    buffer.putLong(target.getMinTimestep() + timesteps - 1);
    
    // Write grid data
    for (double[][] timeframe : target.getTimeframes()) {
      for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
          buffer.putDouble(timeframe[y][x]);
        }
      }
    }
    
    return buffer.array();
  }
}
