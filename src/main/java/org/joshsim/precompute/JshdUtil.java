/**
 * Convienence functions which perform binary conversion in jshd format.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.precompute;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;
import org.joshsim.engine.geometry.PatchBuilderExtents;
import org.joshsim.engine.geometry.PatchBuilderExtentsBuilder;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.ValueSupportFactory;


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

  private static final int JSHD_VERSION = 2;
  private static final int LEGACY_JSHD_VERSION = 1;

  /**
   * Load a DoublePrecomputedGrid from the given bytes serialization.
   *
   * @param engineValueFactory The factory which should be used in creating values returned from the
   *     grid.
   * @param bytes The bytes following the jshd format specification from which to parse a
   *     PrecomputedGrid.
   * @return A DoublePrecomputedGrid parsed from the given bytes.
   */
  public static DoublePrecomputedGrid loadFromBytes(ValueSupportFactory engineValueFactory,
        byte[] bytes) {

    DoublePrecomputedGridBuilder gridBuilder = new DoublePrecomputedGridBuilder();
    gridBuilder.setValueSupportFactory(engineValueFactory);

    ByteBuffer buffer = ByteBuffer.wrap(bytes);

    // Read version
    int version = buffer.getInt();
    if (version != LEGACY_JSHD_VERSION && version != JSHD_VERSION) {
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
    if (version == JSHD_VERSION) {
      gridBuilder.setTimeAxis(readTimeAxis(buffer));
    }

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
    byte[] timeAxisBytes = serializeTimeAxis(target.getTimeAxis());
    int headerSize = Integer.BYTES + (6 * Long.BYTES) + Integer.BYTES + unitsBytes.length
        + timeAxisBytes.length;
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
    buffer.put(timeAxisBytes);

    // Write grid data
    long maxTimestep = target.getMaxTimestep();
    long maxY = target.getMaxY();
    long maxX = target.getMaxX();

    for (long timestep = target.getMinTimestep(); timestep <= maxTimestep; timestep++) {
      for (long y = target.getMinY(); y <= maxY; y++) {
        for (long  x = target.getMinX(); x <= maxX; x++) {
          buffer.putDouble(target.getAt(x, y, timestep).getAsDouble());
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
    String unitsStr = new String(unitsBytes, StandardCharsets.UTF_8);
    Units units = Units.of(unitsStr);
    builder.setUnits(units);
  }

  private static byte[] serializeTimeAxis(Optional<TimeAxis> timeAxis) {
    if (timeAxis.isEmpty()) {
      return new byte[] {0};
    }

    TimeAxis axis = timeAxis.get();
    byte[] coordinateName = axis.getCoordinateName().getBytes(StandardCharsets.UTF_8);
    byte[] first = axis.getType() == TimeAxis.Type.COUNT
        ? axis.getCountStart().toPlainString().getBytes(StandardCharsets.UTF_8)
        : axis.getIsoStart().toString().getBytes(StandardCharsets.UTF_8);
    byte[] increment = axis.getType() == TimeAxis.Type.COUNT
        ? axis.getCountIncrement().toPlainString().getBytes(StandardCharsets.UTF_8)
        : axis.getIsoInterval().toString().getBytes(StandardCharsets.UTF_8);
    byte[] unit = axis.getType() == TimeAxis.Type.COUNT
        ? axis.getCountUnit().getBytes(StandardCharsets.UTF_8) : new byte[0];
    int size = 1 + Integer.BYTES * 6 + coordinateName.length + first.length + increment.length
        + unit.length + Long.BYTES;
    ByteBuffer buffer = ByteBuffer.allocate(size);
    buffer.put((byte) 1);
    buffer.putInt(axis.getType().ordinal());
    buffer.putInt(axis.getKind().ordinal());
    putBytes(buffer, coordinateName);
    putBytes(buffer, unit);
    putBytes(buffer, first);
    putBytes(buffer, increment);
    buffer.putLong(axis.getCount());
    return buffer.array();
  }

  private static Optional<TimeAxis> readTimeAxis(ByteBuffer buffer) {
    if (buffer.get() == 0) {
      return Optional.empty();
    }
    TimeAxis.Type type = TimeAxis.Type.values()[buffer.getInt()];
    TimeAxis.Kind kind = TimeAxis.Kind.values()[buffer.getInt()];
    String coordinateName = readBytes(buffer);
    String unit = readBytes(buffer);
    String first = readBytes(buffer);
    String increment = readBytes(buffer);
    long count = buffer.getLong();
    if (type == TimeAxis.Type.COUNT) {
      return Optional.of(kind == TimeAxis.Kind.INSTANT
          ? TimeAxis.countInstant(coordinateName, unit, new BigDecimal(first))
          : TimeAxis.countRange(coordinateName, unit, new BigDecimal(first),
              new BigDecimal(increment), count));
    }
    return Optional.of(kind == TimeAxis.Kind.INSTANT
        ? TimeAxis.isoInstant(coordinateName, LocalDate.parse(first))
        : TimeAxis.isoRange(
            coordinateName, LocalDate.parse(first), Period.parse(increment), count));
  }

  private static void putBytes(ByteBuffer buffer, byte[] bytes) {
    buffer.putInt(bytes.length);
    buffer.put(bytes);
  }

  private static String readBytes(ByteBuffer buffer) {
    int length = buffer.getInt();
    if (length < 0 || length > buffer.remaining()) {
      throw new IllegalArgumentException("Invalid JSHD temporal metadata string length");
    }
    byte[] bytes = new byte[length];
    buffer.get(bytes);
    return new String(bytes, StandardCharsets.UTF_8);
  }
}
