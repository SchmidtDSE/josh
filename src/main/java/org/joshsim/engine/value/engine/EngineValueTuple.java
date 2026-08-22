/**
 * Structures describing pairs of interacting engine values.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.value.engine;

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
  private static final ThreadLocal<PairTable<LanguageType, LanguageType, TypesTuple>>
      TYPES_TUPLE_CACHE = ThreadLocal.withInitial(PairTable::new);
  private static final ThreadLocal<PairTable<Units, Units, UnitsTuple>> UNITS_TUPLE_CACHE =
      ThreadLocal.withInitial(PairTable::new);

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
   * Private constructor for creating EngineValueTuple with pre-computed tuples.
   *
   * <p>Used internally by reverse() to avoid redundant cache lookups when
   * the reversed TypesTuple and UnitsTuple are already available.</p>
   *
   * @param first the first engine value
   * @param second the second engine value
   * @param types pre-computed TypesTuple
   * @param units pre-computed UnitsTuple
   */
  private EngineValueTuple(
      EngineValue first,
      EngineValue second,
      TypesTuple types,
      UnitsTuple units) {
    this.first = first;
    this.second = second;
    this.types = types;
    this.units = units;
  }

  /**
   * Factory method to get or create an EngineValueTuple with caching of nested tuples.
   *
   * <p>This method caches TypesTuple and UnitsTuple instances using long-based composite keys
   * computed from the dense identities of LanguageType and Units objects. Since both
   * LanguageType.of() and Units.of() return cached singleton instances, identities are
   * stable and suitable for cache keys.</p>
   *
   * <p>Note: The EngineValueTuple itself is NOT cached as it must hold references to the
   * specific EngineValue instances passed in. Only the nested TypesTuple and UnitsTuple
   * objects are cached to reduce allocations.</p>
   *
   * <p>Tuple caches are thread-local: each thread builds its own tuple instances without
   * synchronization or boxing on the lookup path. Tuples compare by content so duplicated
   * instances across threads are equivalent.</p>
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
  private static TypesTuple getOrCreateTypesTuple(
      LanguageType firstType, LanguageType secondType) {
    long key = computeTypesCacheKey(firstType, secondType);
    TypesTuple tuple = getOrPutTypesTuple(key, firstType, secondType);

    // Establish bidirectional linking for reverse() optimization
    // Check if reversed tuple needs to be created and linked
    if (tuple.getReversed() == null) {
      long reversedKey = computeTypesCacheKey(secondType, firstType);
      TypesTuple reversedTuple = getOrPutTypesTuple(reversedKey, secondType, firstType);

      // Link bidirectionally (benign race: both threads compute same result)
      tuple.setReversed(reversedTuple);
      reversedTuple.setReversed(tuple);
    }

    return tuple;
  }

  /**
   * Look up a cached TypesTuple, inserting a freshly built one only on a miss.
   *
   * <p>Uses a thread-local open-addressed table so the cache-hit path allocates nothing and
   * boxes nothing; the TypesTuple is constructed only when actually absent.</p>
   *
   * @param key composite identity key for the type pair
   * @param first LanguageType of first operand
   * @param second LanguageType of second operand
   * @return the cached (or newly cached) TypesTuple for this pair
   */
  private static TypesTuple getOrPutTypesTuple(long key, LanguageType first, LanguageType second) {
    return TYPES_TUPLE_CACHE.get().getOrPut(key, first, second, TypesTuple::new);
  }

  /**
   * Get or create a cached UnitsTuple for the given Units pair.
   *
   * @param firstUnits Units of first operand
   * @param secondUnits Units of second operand
   * @return cached or newly created UnitsTuple
   */
  private static UnitsTuple getOrCreateUnitsTuple(
      Units firstUnits, Units secondUnits) {
    long key = computeUnitsCacheKey(firstUnits, secondUnits);
    UnitsTuple tuple = getOrPutUnitsTuple(key, firstUnits, secondUnits);

    // Establish bidirectional linking for reverse() optimization
    // Check if reversed tuple needs to be created and linked
    if (tuple.getReversed() == null) {
      long reversedKey = computeUnitsCacheKey(secondUnits, firstUnits);
      UnitsTuple reversedTuple = getOrPutUnitsTuple(reversedKey, secondUnits, firstUnits);

      // Link bidirectionally (benign race: both threads compute same result)
      tuple.setReversed(reversedTuple);
      reversedTuple.setReversed(tuple);
    }

    return tuple;
  }

  /**
   * Look up a cached UnitsTuple, inserting a freshly built one only on a miss.
   *
   * <p>Uses a thread-local open-addressed table so the cache-hit path allocates nothing and
   * boxes nothing; the UnitsTuple is constructed only when actually absent.</p>
   *
   * @param key composite identity key for the units pair
   * @param first Units of first operand
   * @param second Units of second operand
   * @return the cached (or newly cached) UnitsTuple for this pair
   */
  private static UnitsTuple getOrPutUnitsTuple(long key, Units first, Units second) {
    return UNITS_TUPLE_CACHE.get().getOrPut(key, first, second, UnitsTuple::new);
  }

  /**
   * Compute a long-based composite key from type identities.
   *
   * <p>Packs 2 dense identities into a 64-bit long key. Each component uses 32 bits.</p>
   *
   * @param firstType LanguageType of first operand
   * @param secondType LanguageType of second operand
   * @return 64-bit composite key
   */
  private static long computeTypesCacheKey(LanguageType firstType, LanguageType secondType) {
    return ((long) firstType.getId() << 32) | (secondType.getId() & 0xFFFFFFFFL);
  }

  /**
   * Compute a long-based composite key from unit identities.
   *
   * <p>Packs 2 dense identities into a 64-bit long key. Each component uses 32 bits.</p>
   *
   * @param firstUnits Units of first operand
   * @param secondUnits Units of second operand
   * @return 64-bit composite key
   */
  private static long computeUnitsCacheKey(Units firstUnits, Units secondUnits) {
    return ((long) firstUnits.getId() << 32) | (secondUnits.getId() & 0xFFFFFFFFL);
  }

  /**
   * Reverse the order of the values within the tuple.
   *
   * @returns copy of this tuple with order of operands reversed.
   */
  public EngineValueTuple reverse() {
    // Use pre-linked reversed tuples to avoid cache lookups
    // The reversed TypesTuple and UnitsTuple are already cached and linked bidirectionally
    return new EngineValueTuple(getSecond(), getFirst(), types.getReversed(), units.getReversed());
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
    private final boolean areCompatible;
    private final String rootString;
    private final int cachedHashCode;
    // Not final for performance - allows bidirectional linking without allocation overhead
    private TypesTuple reversed;

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
      this.areCompatible = first.getRootType().equals(second.getRootType());
      this.rootString = first.getRootType() + "," + second.getRootType();
      this.cachedHashCode = rootString.hashCode();
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
      return areCompatible;
    }

    /**
     * Get the reversed version of this types tuple.
     *
     * <p>Returns the cached reversed tuple where first and second are swapped.
     * This is pre-computed during cache insertion to avoid repeated cache lookups.</p>
     *
     * @return the reversed types tuple with swapped first and second types
     */
    public TypesTuple getReversed() {
      return reversed;
    }

    /**
     * Set the reversed version of this types tuple.
     *
     * <p>Used internally for bidirectional linking during cache initialization.</p>
     *
     * @param reversed the reversed types tuple with swapped first and second types
     */
    void setReversed(TypesTuple reversed) {
      this.reversed = reversed;
    }

    /**
     * Convert to a string representation using the roots of both types.
     *
     * @returns string representation using root types.
     */
    public String toRootString() {
      return rootString;
    }

    /**
     * Determine equality by if two language types tuples are compatible.
     *
     * @param other operand
     * @return true if compatible (equivalent for purposes of operations) and false otherwise.
     */
    public boolean equals(TypesTuple other) {
      return rootString.equals(other.rootString);
    }

    @Override
    public boolean equals(Object other) {
      return equals((TypesTuple) other);
    }

    @Override
    public int hashCode() {
      return cachedHashCode;
    }

  }

  /**
   * Typle describing two units that are in this engine value tuple such as meters and centimeters.
   */
  public static class UnitsTuple {

    private final Units first;
    private final Units second;
    private final boolean areCompatible;
    private final String cachedString;
    private final int cachedHashCode;
    // Not final for performance - allows bidirectional linking without allocation overhead
    private UnitsTuple reversed;

    /**
     * Create a new tuple to represent a pair of identifying names.
     *
     * @param first the first value, for example from the left-side operand.
     * @param second the second value, for example from the right-side operand.
     */
    public UnitsTuple(Units first, Units second) {
      this.first = first;
      this.second = second;
      if (first.equals(second)) {
        this.areCompatible = true;
      } else if (first.toString().isBlank()) {
        this.areCompatible = true;
      } else if (second.toString().isBlank()) {
        this.areCompatible = true;
      } else {
        this.areCompatible = false;
      }
      this.cachedString = "units: " + first + ", " + second;
      this.cachedHashCode = cachedString.hashCode();
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
      return areCompatible;
    }

    /**
     * Get the reversed version of this units tuple.
     *
     * <p>Returns the cached reversed tuple where first and second are swapped.
     * This is pre-computed during cache insertion to avoid repeated cache lookups.</p>
     *
     * @return the reversed units tuple with swapped first and second units
     */
    public UnitsTuple getReversed() {
      return reversed;
    }

    /**
     * Set the reversed version of this units tuple.
     *
     * <p>Used internally for bidirectional linking during cache initialization.</p>
     *
     * @param reversed the reversed units tuple with swapped first and second units
     */
    void setReversed(UnitsTuple reversed) {
      this.reversed = reversed;
    }

    @Override
    public boolean equals(Object other) {
      return cachedString.equals(other.toString());
    }

    @Override
    public String toString() {
      return cachedString;
    }

    @Override
    public int hashCode() {
      return cachedHashCode;
    }

  }

  /**
   * Creator of a tuple value for a cache miss.
   *
   * <p>Implemented with constructor references (eg TypesTuple::new) so that no capturing
   * lambda is allocated on cache hits or misses.</p>
   *
   * @param <A> type of the first identifying value of the pair.
   * @param <B> type of the second identifying value of the pair.
   * @param <V> type of tuple created on a cache miss.
   */
  @FunctionalInterface
  public interface PairCreator<A, B, V> {

    /**
     * Create the tuple to cache for this pair.
     *
     * @param first the first identifying value of the pair.
     * @param second the second identifying value of the pair.
     * @return newly created tuple which has not yet been cached.
     */
    V create(A first, B second);

  }

  /**
   * Single-threaded open-addressed map from long keys to values.
   *
   * <p>Avoids the boxing of Long keys required by HashMap or ConcurrentHashMap. Tables are kept
   * per thread so lookups need no synchronization and inserts need no compare-and-swap. Key 0 is
   * reserved as the empty marker; callers must use non-zero keys. Grows by rehashing when more
   * than half full, which keeps probing short.</p>
   *
   * @param <A> type of the first identifying value of the pair.
   * @param <B> type of the second identifying value of the pair.
   * @param <V> type of value cached for each key.
   */
  public static final class PairTable<A, B, V> {

    private static final int INITIAL_CAPACITY = 64;

    private long[] keys;
    private Object[] values;
    private int used;
    private int mask;

    /**
     * Create a new, empty pair table.
     */
    public PairTable() {
      keys = new long[INITIAL_CAPACITY];
      values = new Object[INITIAL_CAPACITY];
      mask = INITIAL_CAPACITY - 1;
      used = 0;
    }

    /**
     * Get the value for a key, creating and caching it on a miss.
     *
     * @param key non-zero composite key for the pair.
     * @param first the first identifying value of the pair, used on a miss.
     * @param second the second identifying value of the pair, used on a miss.
     * @param creator constructor reference used to build a value on a miss. Must not return null,
     *     which this table cannot distinguish from an absent key.
     * @return the cached or newly created value for this key.
     */
    public V getOrPut(long key, A first, B second, PairCreator<A, B, V> creator) {
      V cached = getIfPresent(key);
      if (cached != null) {
        return cached;
      }

      // The creator runs before a slot is claimed, and the slot is located again afterwards.
      // Claiming it first would break if a creator ever inserted into this same table, since
      // that insert can grow it and leave the pending index pointing into the replaced array.
      V created = creator.create(first, second);
      return putIfAbsent(key, created);
    }

    /**
     * Find the value already cached for a key.
     *
     * @param key non-zero composite key for the pair.
     * @return the cached value, or null if this key has no value cached.
     */
    @SuppressWarnings("unchecked")
    private V getIfPresent(long key) {
      int index = hash(key) & mask;
      while (true) {
        long existing = keys[index];
        if (existing == key) {
          return (V) values[index];
        }
        if (existing == 0L) {
          return null;
        }
        index = (index + 1) & mask;
      }
    }

    /**
     * Cache a value for a key unless one arrived for it while it was being created.
     *
     * @param key non-zero composite key for the pair.
     * @param value the value to cache for this key.
     * @return the newly cached value, or the value already cached for this key.
     */
    @SuppressWarnings("unchecked")
    private V putIfAbsent(long key, V value) {
      if ((used + 1) * 2 > keys.length) {
        grow();
      }

      int index = hash(key) & mask;
      while (true) {
        long existing = keys[index];
        if (existing == key) {
          return (V) values[index];
        }
        if (existing == 0L) {
          keys[index] = key;
          values[index] = value;
          used += 1;
          return value;
        }
        index = (index + 1) & mask;
      }
    }

    /**
     * Double the table capacity and reinsert all prior entries.
     */
    private void grow() {
      final long[] oldKeys = keys;
      final Object[] oldValues = values;

      int newCapacity = oldKeys.length * 2;
      keys = new long[newCapacity];
      values = new Object[newCapacity];
      mask = newCapacity - 1;
      used = 0;

      for (int i = 0; i < oldKeys.length; i++) {
        long key = oldKeys[i];
        if (key != 0L) {
          insertDuringGrow(key, oldValues[i]);
        }
      }
    }

    /**
     * Insert an entry which is known not to be present, used while rehashing.
     *
     * @param key non-zero composite key for the pair.
     * @param value value previously cached for this key.
     */
    private void insertDuringGrow(long key, Object value) {
      int index = hash(key) & mask;
      while (keys[index] != 0L) {
        index = (index + 1) & mask;
      }
      keys[index] = key;
      values[index] = value;
      used += 1;
    }

    /**
     * Spread the bits of a composite key so consecutive ids do not cluster.
     *
     * @param key non-zero composite key for the pair.
     * @return mixed hash value which may be used with the table mask.
     */
    private static int hash(long key) {
      long mixed = key * 0x9E3779B97F4A7C15L;
      return (int) (mixed ^ (mixed >>> 32));
    }

  }

}
