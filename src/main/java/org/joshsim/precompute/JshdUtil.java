/**
 * Convienence functions which perform binary conversion in jshd format.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.precompute;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import org.joshsim.engine.geometry.PatchBuilderExtents;
import org.joshsim.engine.geometry.PatchBuilderExtentsBuilder;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueFactory;


/**
 * Utility which facilitates conversion between jshd format and PrecomputedGrids.
 *
 * <p>Utility which facilitates conversions involving the jshd binary format. This has the first 32
 * bits are for the version number (currently always 1), 64 bits for the minimum x coordinate
 * followed by 64 bits for maximum x coordinate, 64 bits for minimum y coordinate, 64 bits for
 * maximum y coordinate, 64 bits for minimum timestep, 64 bits for maximum timestep, and units
 * string to conclude the header section (units limited to 200 characters). After the header
 * containing these longs, the doubles of the grid through time are listed one after another in
 * which each row is written in ordered from low to high column and then each set of rows follows
 * from the minimum to maximum timestep.</p>
 */
public class JshdUtil {

  private static final int JSHD_VERSION = 1;

  /**
   * Load a DoublePrecomputedGrid from the given bytes serialization.
   *
   * @param engineValueFactory The factory which should be used in creating values returned from the
   *     grid.
   * @param bytes The bytes following the jshd format specification from which to parse a
   *     PrecomputedGrid.
   * @return A DoublePrecomputedGrid parsed from the given bytes.
   */
  public static DoublePrecomputedGrid loadFromBytes(EngineValueFactory engineValueFactory,
        byte[] bytes) {

    DoublePrecomputedGridBuilder gridBuilder = new DoublePrecomputedGridBuilder();
    gridBuilder.setEngineValueFactory(engineValueFactory);

    ByteBuffer buffer = ByteBuffer.wrap(bytes);

    // Read version
    int version = buffer.getInt();
    if (version != JSHD_VERSION) {
      throw new IllegalArgumentException("Unsupported JSHD version: " + version);
    }

    // Read header
    long minX = buffer.getLong();
    long maxX = buffer.getLong();
    long minY = buffer.getLong();
    long maxY = buffer.getLong();
    long minTimestep = buffer.getLong();
    long maxTimestep = buffer.getLong();
    gridBuilder.setTimestepRange(minTimestep, maxTimestep);

    int width = (int) (maxX - minX + 1);
    int height = (int) (maxY - minY + 1);
    int timesteps = (int) (maxTimestep - minTimestep + 1);

    readUnitsStr(buffer, gridBuilder);

    // Read grid data
    double[][][] output = new double[timesteps][height][width];
    for (int timestep = 0; timestep < timesteps; timestep++) {
      for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
          double newValue = buffer.getDouble();
          output[timestep][y][x] = newValue;
        }
      }
    }

    gridBuilder.setInnerValues(output);

    // Create extents
    PatchBuilderExtentsBuilder extentsBuilder = new PatchBuilderExtentsBuilder();
    extentsBuilder.setTopLeftX(BigDecimal.valueOf(minX));
    extentsBuilder.setTopLeftY(BigDecimal.valueOf(minY));
    extentsBuilder.setBottomRightX(BigDecimal.valueOf(maxX));
    extentsBuilder.setBottomRightY(BigDecimal.valueOf(maxY));

    PatchBuilderExtents extents = extentsBuilder.build();
    gridBuilder.setExtents(extents);

    // Build
    return gridBuilder.build();
  }

  /**
   * Convert DoublePrecomputedGrid from the given bytes serialization.
   *
   * @param target The bytes following the jshd format specification from which to parse a
   *     PrecomputedGrid.
   * @return A DoublePrecomputedGrid parsed from the given bytes.
   */
  public static byte[] serializeToBytes(DoublePrecomputedGrid target) {
    int width = (int) (target.getMaxX() - target.getMinX() + 1);
    int height = (int) (target.getMaxY() - target.getMinY() + 1);
    int timesteps = (int) (target.getMaxTimestep() - target.getMinTimestep() + 1);

    byte[] unitsBytes = target.getUnits().toString().getBytes();
    if (unitsBytes.length > 200) {
      throw new IllegalArgumentException("Units string exceeds maximum length of 200 characters");
    }

    // Calculate buffer size
    int headerSize = Integer.BYTES + (6 * Long.BYTES) + Integer.BYTES + unitsBytes.length;
    int bodySize = width * height * timesteps * Double.BYTES;
    int bufferSize = headerSize + bodySize;
    ByteBuffer buffer = ByteBuffer.allocate(bufferSize);

    // Write version
    buffer.putInt(JSHD_VERSION);

    // Write header
    buffer.putLong(target.getMinX());
    buffer.putLong(target.getMaxX());
    buffer.putLong(target.getMinY());
    buffer.putLong(target.getMaxY());
    buffer.putLong(target.getMinTimestep());
    buffer.putLong(target.getMaxTimestep());

    // Write units
    buffer.putInt(unitsBytes.length);
    buffer.put(unitsBytes);

    // Write grid data
    long maxTimestep = target.getMaxTimestep();
    long maxY = target.getMaxY();
    long maxX = target.getMaxX();

    for (long timestep = target.getMinTimestep(); timestep <= maxTimestep; timestep++) {
      for (long y = target.getMinY(); y <= maxY; y++) {
        for (long  x = target.getMinX(); x <= maxX; x++) {
          buffer.putDouble(target.getAt(x, y, timestep).getAsDecimal().doubleValue());
        }
      }
    }

    return buffer.array();
  }

  
  /**
   * Read the units string from the given ByteBuffer, setting it in a DoublePrecomputedGridBuilder.
   *
   * @param buffer  The ByteBuffer from which to read the units string.
   * @param builder The DoublePrecomputedGridBuilder instance where the units will be set.
   * @throws IllegalArgumentException If the units string length exceeds 200 characters.
   */
  private static void readUnitsStr(ByteBuffer buffer, DoublePrecomputedGridBuilder builder) {
    int unitsLength = buffer.getInt();
    if (unitsLength > 200) {
      throw new IllegalArgumentException("Units string exceeds maximum length of 200 characters");
    }
    byte[] unitsBytes = new byte[unitsLength];
    buffer.get(unitsBytes);
    String unitsStr = new String(unitsBytes);
    Units units = new Units(unitsStr);
    builder.setUnits(units);
  }
}
