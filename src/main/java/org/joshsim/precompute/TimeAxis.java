/**
 * Declared temporal coordinate system for a precomputed external grid.
 *
 * <p>A time axis is authored during preprocessing and persists with the JSHD payload. It is
 * deliberately independent from the simulation clock: a grid can be count-indexed or carry ISO
 * calendar coordinates without causing the engine to infer a forcing policy.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.precompute;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.Objects;


/** Immutable temporal metadata for a {@link DoublePrecomputedGrid}. */
public final class TimeAxis {

  /** Coordinate representation persisted in the grid. */
  public enum Type {
    COUNT,
    ISO
  }

  /** Whether the resource is a sequence or a single dated snapshot. */
  public enum Kind {
    RANGE,
    INSTANT
  }

  private final Type type;
  private final Kind kind;
  private final String coordinateName;
  private final String countUnit;
  private final BigDecimal countStart;
  private final BigDecimal countIncrement;
  private final LocalDate isoStart;
  private final Period isoInterval;
  private final long count;

  private TimeAxis(Type type, Kind kind, String coordinateName, String countUnit,
      BigDecimal countStart, BigDecimal countIncrement, LocalDate isoStart,
      Period isoInterval, long count) {
    this.type = Objects.requireNonNull(type);
    this.kind = Objects.requireNonNull(kind);
    this.coordinateName = requireNonBlank(coordinateName, "coordinateName");
    this.countUnit = countUnit;
    this.countStart = countStart;
    this.countIncrement = countIncrement;
    this.isoStart = isoStart;
    this.isoInterval = isoInterval;
    this.count = count;

    if (count < 1) {
      throw new IllegalArgumentException("Time axis count must be at least one");
    }
    if (kind == Kind.INSTANT && count != 1) {
      throw new IllegalArgumentException("An instant time axis must contain exactly one coordinate");
    }
  }

  /** Creates a regular numeric coordinate axis. */
  public static TimeAxis countRange(String coordinateName, String unit, BigDecimal start,
      BigDecimal increment, long count) {
    return new TimeAxis(Type.COUNT, Kind.RANGE, coordinateName,
        requireNonBlank(unit, "unit"), Objects.requireNonNull(start),
        Objects.requireNonNull(increment), null, null, count);
  }

  /** Creates a single numeric coordinate axis. */
  public static TimeAxis countInstant(String coordinateName, String unit, BigDecimal instant) {
    return new TimeAxis(Type.COUNT, Kind.INSTANT, coordinateName,
        requireNonBlank(unit, "unit"), Objects.requireNonNull(instant), BigDecimal.ZERO,
        null, null, 1);
  }

  /** Creates a regular ISO-8601 date coordinate axis. */
  public static TimeAxis isoRange(String coordinateName, LocalDate start, Period interval,
      long count) {
    if (interval == null || interval.isZero() || interval.isNegative()) {
      throw new IllegalArgumentException("An ISO time axis interval must be positive");
    }
    return new TimeAxis(Type.ISO, Kind.RANGE, coordinateName, null, null, null,
        Objects.requireNonNull(start), interval, count);
  }

  /** Creates a single ISO-8601 date coordinate axis. */
  public static TimeAxis isoInstant(String coordinateName, LocalDate instant) {
    return new TimeAxis(Type.ISO, Kind.INSTANT, coordinateName, null, null, null,
        Objects.requireNonNull(instant), Period.ZERO, 1);
  }

  public Type getType() {
    return type;
  }

  public Kind getKind() {
    return kind;
  }

  public String getCoordinateName() {
    return coordinateName;
  }

  public String getCountUnit() {
    return countUnit;
  }

  public BigDecimal getCountStart() {
    return countStart;
  }

  public BigDecimal getCountIncrement() {
    return countIncrement;
  }

  public LocalDate getIsoStart() {
    return isoStart;
  }

  public Period getIsoInterval() {
    return isoInterval;
  }

  public long getCount() {
    return count;
  }

  /**
   * Combines this axis with its immediately following compatible axis.
   *
   * <p>Composition is intentionally strict: temporal metadata represents exact stored
   * coordinates, not a resampling request. Both operands must therefore have the same coordinate
   * representation and name, be ranges with the same unit or period, and be exactly contiguous.</p>
   *
   * @param following the axis immediately following this one
   * @return one range axis covering both sequences
   */
  public TimeAxis append(TimeAxis following) {
    Objects.requireNonNull(following);
    if (type != following.type || kind != Kind.RANGE || following.kind != Kind.RANGE) {
      throw new IllegalArgumentException("Temporal axes must have the same range type to amend");
    }
    if (!coordinateName.equals(following.coordinateName)) {
      throw new IllegalArgumentException("Temporal axes must use the same coordinate name to amend");
    }
    if (type == Type.COUNT) {
      if (!countUnit.equals(following.countUnit)
          || countIncrement.compareTo(following.countIncrement) != 0) {
        throw new IllegalArgumentException("Count temporal axes must have the same unit and increment");
      }
      BigDecimal expectedStart = countStart.add(countIncrement.multiply(BigDecimal.valueOf(count)));
      if (expectedStart.compareTo(following.countStart) != 0) {
        throw new IllegalArgumentException("Count temporal axes must be exactly contiguous and non-overlapping");
      }
      return countRange(coordinateName, countUnit, countStart, countIncrement,
          Math.addExact(count, following.count));
    }
    if (!isoInterval.equals(following.isoInterval)) {
      throw new IllegalArgumentException("ISO temporal axes must have the same period");
    }
    LocalDate expectedStart = isoStart;
    for (long index = 0; index < count; index++) {
      expectedStart = expectedStart.plus(isoInterval);
    }
    if (!expectedStart.equals(following.isoStart)) {
      throw new IllegalArgumentException("ISO temporal axes must be exactly contiguous and non-overlapping");
    }
    return isoRange(coordinateName, isoStart, isoInterval, Math.addExact(count, following.count));
  }

  /** Resolves an exact numeric coordinate to its zero-based stored index. */
  public long getCountIndex(BigDecimal coordinate) {
    if (type != Type.COUNT) {
      throw new IllegalStateException("This time axis does not use count coordinates");
    }
    BigDecimal offset = coordinate.subtract(countStart);
    if (countIncrement.compareTo(BigDecimal.ZERO) == 0) {
      if (offset.compareTo(BigDecimal.ZERO) != 0) {
        throw unavailableCoordinate(coordinate.toPlainString());
      }
      return 0;
    }
    BigDecimal[] quotientAndRemainder = offset.divideAndRemainder(countIncrement);
    if (quotientAndRemainder[1].compareTo(BigDecimal.ZERO) != 0) {
      throw unavailableCoordinate(coordinate.toPlainString());
    }
    long index;
    try {
      index = quotientAndRemainder[0].longValueExact();
    } catch (ArithmeticException e) {
      throw unavailableCoordinate(coordinate.toPlainString());
    }
    if (index < 0 || index >= count) {
      throw unavailableCoordinate(coordinate.toPlainString());
    }
    return index;
  }

  /** Resolves an exact ISO date to its zero-based stored index. */
  public long getIsoIndex(LocalDate coordinate) {
    if (type != Type.ISO) {
      throw new IllegalStateException("This time axis does not use ISO coordinates");
    }
    LocalDate cursor = isoStart;
    for (long index = 0; index < count; index++) {
      if (cursor.equals(coordinate)) {
        return index;
      }
      cursor = cursor.plus(isoInterval);
    }
    throw unavailableCoordinate(coordinate.toString());
  }

  private IllegalArgumentException unavailableCoordinate(String coordinate) {
    String first = type == Type.COUNT ? countStart.toPlainString() : isoStart.toString();
    return new IllegalArgumentException(
        "Time coordinate " + coordinate + " is unavailable on " + coordinateName
        + " (available from " + first + ", count " + count + ")");
  }

  private static String requireNonBlank(String value, String name) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value;
  }
}
