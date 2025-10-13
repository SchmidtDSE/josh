/**
 * Structures describing pairs of interacting engine values.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.value.engine;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.joshsim.compat.CompatibilityLayerKeeper;
import org.joshsim.compat.CompatibleStringJoiner;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.type.EngineValue;
import org.joshsim.engine.value.type.LanguageType;

/**
 * Pair of engine values interacting in an operation.
 */
public class EngineValueTuple {

  private final EngineValue first;
  private final EngineValue second;
  private final TypesTuple types;
  private final UnitsTuple units;
  // Static cache for nested tuple instances using long-based composite keys
  // ConcurrentHashMap provides thread-safe access for parallel processing
  private static final Map<Long, TypesTuple> TYPES_TUPLE_CACHE = new ConcurrentHashMap<>();
  private static final Map<Long, UnitsTuple> UNITS_TUPLE_CACHE = new ConcurrentHashMap<>();

  /**
   * Create a new tuple of engine values.
   *
   * @param first the first engine value for this tuple, for example the left side operand.
   * @param second the second engine value for this tuple, for example the left side operand.
   */
  public EngineValueTuple(EngineValue first, EngineValue second) {
    this.first = first;
    this.second = second;
    types = getOrCreateTypesTuple(first.getLanguageType(), second.getLanguageType());
    units = getOrCreateUnitsTuple(first.getUnits(), second.getUnits());
  }

  /**
   * Factory method to get or create an EngineValueTuple with caching of nested tuples.
   *
   * <p>This method caches TypesTuple and UnitsTuple instances using long-based composite keys
   * computed from identity hashes of LanguageType and Units objects. Since both
   * LanguageType.of() and Units.of() return cached singleton instances, identity hashes are
   * stable and suitable for cache keys.</p>
   *
   * <p>Note: The EngineValueTuple itself is NOT cached as it must hold references to the
   * specific EngineValue instances passed in. Only the nested TypesTuple and UnitsTuple
   * objects are cached to reduce allocations.</p>
   *
   * <p>Thread-safe for concurrent access including parallel streams. ConcurrentHashMap
   * handles synchronization without blocking reads.</p>
   *
   * @param first the first engine value for this tuple, for example the left side operand.
   * @param second the second engine value for this tuple, for example the right side operand.
   * @return newly created EngineValueTuple instance with cached nested tuples
   */
  public static EngineValueTuple of(EngineValue first, EngineValue second) {
    return new EngineValueTuple(first, second);
  }

  /**
   * Get or create a cached TypesTuple for the given LanguageType pair.
   *
   * @param firstType LanguageType of first operand
   * @param secondType LanguageType of second operand
   * @return cached or newly created TypesTuple
   */
  private static TypesTuple getOrCreateTypesTuple(LanguageType firstType, LanguageType secondType) {
    long key = computeTypesCacheKey(firstType, secondType);
    return TYPES_TUPLE_CACHE.computeIfAbsent(key, k -> new TypesTuple(firstType, secondType));
  }

  /**
   * Get or create a cached UnitsTuple for the given Units pair.
   *
   * @param firstUnits Units of first operand
   * @param secondUnits Units of second operand
   * @return cached or newly created UnitsTuple
   */
  private static UnitsTuple getOrCreateUnitsTuple(Units firstUnits, Units secondUnits) {
    long key = computUnitsCacheKey(firstUnits, secondUnits);
    return UNITS_TUPLE_CACHE.computeIfAbsent(key, k -> new UnitsTuple(firstUnits, secondUnits));
  }

  /**
   * Compute a long-based composite key from type identity hashes.
   *
   * <p>Packs 2 identity hashes into a 64-bit long key. Each component uses 32 bits.</p>
   *
   * @param firstType LanguageType of first operand
   * @param secondType LanguageType of second operand
   * @return 64-bit composite key
   */
  private static long computeTypesCacheKey(LanguageType firstType, LanguageType secondType) {
    int firstTypeHash = System.identityHashCode(firstType);
    int secondTypeHash = System.identityHashCode(secondType);
    return ((long) firstTypeHash << 32) | (secondTypeHash & 0xFFFFFFFFL);
  }

  /**
   * Compute a long-based composite key from unit identity hashes.
   *
   * <p>Packs 2 identity hashes into a 64-bit long key. Each component uses 32 bits.</p>
   *
   * @param firstUnits Units of first operand
   * @param secondUnits Units of second operand
   * @return 64-bit composite key
   */
  private static long computUnitsCacheKey(Units firstUnits, Units secondUnits) {
    int firstUnitsHash = System.identityHashCode(firstUnits);
    int secondUnitsHash = System.identityHashCode(secondUnits);
    return ((long) firstUnitsHash << 32) | (secondUnitsHash & 0xFFFFFFFFL);
  }

  /**
   * Reverse the order of the values within the tuple.
   *
   * @returns copy of this tuple with order of operands reversed.
   */
  public EngineValueTuple reverse() {
    return EngineValueTuple.of(getSecond(), getFirst());
  }

  /**
   * Determine if the two values in this tuple are compatable without further casting.
   */
  public boolean getAreCompatible() {
    return types.getAreCompatible() && units.getAreCompatible();
  }

  /**
   * Get the first engine value for this tuple.
   *
   * @return the first engine value, for example the left operand.
   */
  public EngineValue getFirst() {
    return first;
  }

  /**
   * Get the second engine value for this tuple.
   *
   * @return the second engine value, for example the right operand.
   */
  public EngineValue getSecond() {
    return second;
  }

  /**
   * Get the types tuple for this engine value tuple.
   *
   * @return the types tuple like for int and decimal.
   */
  public TypesTuple getTypes() {
    return types;
  }

  /**
   * Get the units tuple for this engine value tuple.
   *
   * @return the units tuple like for meters and cenitmeters.
   */
  public UnitsTuple getUnits() {
    return units;
  }

  /**
   * Typle describing two types that are in this engine value tuple such as int and decimal.
   */
  public static class TypesTuple {

    private final LanguageType first;
    private final LanguageType second;

    /**
     * Create a new types tuple representing a pair of types.
     *
     * <p>This constructor initializes a new pair of types given for the first and second values.
     * This might include types, for example, like int or decimal.</p>
     *
     * @param first the first type, representing for example the type of the left-side operand.
     * @param second the second type, representing for example the type of the right-side
     *     operand.
     */
    public TypesTuple(LanguageType first, LanguageType second) {
      this.first = first;
      this.second = second;
    }

    /**
     * Get the first identifying value, for example from the left hand operand.
     *
     * @returns the first identifying value.
     */
    public LanguageType getFirst() {
      return first;
    }

    /**
     * Get the second identifying value, for example from the right hand operand.
     *
     * @returns the second identifying value.
     */
    public LanguageType getSecond() {
      return second;
    }

    /**
     * Determine if the two language types in this tuple are compatable for use in operations.
     *
     * @return true if compatiable and false otherwise.
     */
    public boolean getAreCompatible() {
      return first.getRootType().equals(second.getRootType());
    }

    /**
     * Convert to a string representation using the roots of both types.
     *
     * @returns string representation using root types.
     */
    public String toRootString() {
      CompatibleStringJoiner joiner = CompatibilityLayerKeeper.get().createStringJoiner(",");
      joiner.add(first.getRootType());
      joiner.add(second.getRootType());
      return joiner.toString();
    }

    /**
     * Determine equality by if two language types tuples are compatible.
     *
     * @param other operand
     * @return true if compatible (equivalent for purposes of operations) and false otherwise.
     */
    public boolean equals(TypesTuple other) {
      return toRootString().equals(other.toRootString());
    }

    @Override
    public boolean equals(Object other) {
      return equals((TypesTuple) other);
    }

    @Override
    public int hashCode() {
      return toRootString().hashCode();
    }

  }

  /**
   * Typle describing two units that are in this engine value tuple such as meters and centimeters.
   */
  public static class UnitsTuple {

    private final Units first;
    private final Units second;

    /**
     * Create a new tuple to represent a pair of identifying names.
     *
     * @param first the first value, for example from the left-side operand.
     * @param second the second value, for example from the right-side operand.
     */
    public UnitsTuple(Units first, Units second) {
      this.first = first;
      this.second = second;
    }

    /**
     * Get the first identifying value, for example from the left hand operand.
     *
     * @returns the first identifying value.
     */
    public Units getFirst() {
      return first;
    }

    /**
     * Get the second identifying value, for example from the right hand operand.
     *
     * @returns the second identifying value.
     */
    public Units getSecond() {
      return second;
    }

    /**
     * Determine if these two identities are compatible without furter casting.
     */
    public boolean getAreCompatible() {
      if (getFirst().equals(getSecond())) {
        return true;
      } else if (getFirst().toString().isBlank()) {
        return true;
      } else if (getSecond().toString().isBlank()) {
        return true;
      } else {
        return false;
      }
    }

    @Override
    public boolean equals(Object other) {
      return toString().equals(other.toString());
    }

    @Override
    public String toString() {
      return String.format("units: %s, %s", first, second);
    }

    @Override
    public int hashCode() {
      return toString().hashCode();
    }

  }

}
