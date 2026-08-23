/**
 * Table caching values built from pairs of identities.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.util;

import java.util.function.BiFunction;


/**
 * Single-threaded open-addressed map from long keys to values.
 *
 * <p>Avoids the boxing of Long keys required by HashMap or ConcurrentHashMap. Tables are kept
 * per thread so lookups need no synchronization and inserts need no compare-and-swap.</p>
 *
 * <p>Collisions are resolved by walking forward to the next free slot, so a run of occupied slots
 * must always be terminated by an empty one for a lookup to know when to stop. The table
 * therefore grows once it is half full. A slot is empty when it holds a null value, which leaves
 * every key usable but means a cached value may never be null.</p>
 *
 * <p>A third party library was measured in place of this class rather than assumed to be better.
 * Agrona's BiInt2ObjectMap is the same design, is permissively licensed, and would have replaced
 * this file outright, but it ran about 2.2% slower on the serial stress simulation over 24 paired
 * order-rotated runs. The cost is its stronger per-lookup bit mixing, which the dense sequential
 * ids used here do not need; fastutil documents the same tradeoff between a cheap multiplicative
 * mix and a stronger one. That measurement is the reason this table is hand rolled.</p>
 *
 * @param <FirstT> type of the first identifying value of the pair.
 * @param <SecondT> type of the second identifying value of the pair.
 * @param <ValueT> type of value cached for each key.
 */
public final class PairTable<FirstT, SecondT, ValueT> {

  private static final int INITIAL_CAPACITY = 64;

  /**
   * Multiplier used to spread key bits, equal to Odd((phi - 1) * 2 to the 64th).
   *
   * <p>RFC 2040 defines this value as the 64-bit member of a family of constants derived from the
   * golden ratio, alongside the more familiar 32-bit 0x9E3779B9. It reaches hash tables through
   * Knuth's multiplicative hashing; fastutil carries the same constant as HashCommon.LONG_PHI
   * under the Apache License, which is the permissively licensed reference implementation for
   * this use.</p>
   */
  private static final long GOLDEN_RATIO_64 = 0x9E3779B97F4A7C15L;

  // Parallel arrays rather than an entry object per key, so that a lookup touches no
  // per-entry allocation. Slot i of slotValues holds the value for slot i of slotKeys.
  private long[] slotKeys;
  private Object[] slotValues;
  private int occupiedSlots;
  private int indexMask;

  /**
   * Create a new, empty pair table.
   */
  public PairTable() {
    slotKeys = new long[INITIAL_CAPACITY];
    slotValues = new Object[INITIAL_CAPACITY];
    indexMask = INITIAL_CAPACITY - 1;
    occupiedSlots = 0;
  }

  /**
   * Get the value for a key, creating and caching it on a miss.
   *
   * <p>The creator runs before any slot is claimed and the slot is located again afterwards.
   * Claiming the slot first would be shorter but would break if a creator ever inserted into
   * this same table, since that insert can grow it and leave the pending slot index pointing
   * into the replaced array, overwriting an unrelated entry.</p>
   *
   * @param key composite key for the pair.
   * @param first the first identifying value of the pair, used on a miss.
   * @param second the second identifying value of the pair, used on a miss.
   * @param creator constructor reference used to build a value on a miss. Must not return null,
   *     which this table cannot distinguish from an absent key.
   * @return the cached or newly created value for this key.
   */
  public ValueT getOrPut(long key, FirstT first, SecondT second,
      BiFunction<FirstT, SecondT, ValueT> creator) {
    ValueT cached = findValue(key);
    if (cached != null) {
      return cached;
    }

    ValueT created = creator.apply(first, second);
    return cacheIfAbsent(key, created);
  }

  /**
   * Find the value already cached for a key.
   *
   * @param key composite key for the pair.
   * @return the cached value, or null if this key has no value cached.
   */
  @SuppressWarnings("unchecked")
  private ValueT findValue(long key) {
    int slotIndex = spreadKey(key) & indexMask;
    while (true) {
      Object valueAtSlot = slotValues[slotIndex];
      if (valueAtSlot == null) {
        return null;
      }
      if (slotKeys[slotIndex] == key) {
        return (ValueT) valueAtSlot;
      }
      slotIndex = nextSlot(slotIndex);
    }
  }

  /**
   * Cache a value for a key unless one arrived for it while it was being created.
   *
   * @param key composite key for the pair.
   * @param value the value to cache for this key.
   * @return the newly cached value, or the value already cached for this key.
   */
  @SuppressWarnings("unchecked")
  private ValueT cacheIfAbsent(long key, ValueT value) {
    if (needsGrowthForInsert()) {
      growAndRehash();
    }

    int slotIndex = spreadKey(key) & indexMask;
    while (true) {
      Object valueAtSlot = slotValues[slotIndex];
      if (valueAtSlot == null) {
        slotKeys[slotIndex] = key;
        slotValues[slotIndex] = value;
        occupiedSlots += 1;
        return value;
      }
      if (slotKeys[slotIndex] == key) {
        return (ValueT) valueAtSlot;
      }
      slotIndex = nextSlot(slotIndex);
    }
  }

  /**
   * Determine whether one more entry would leave the table more than half full.
   *
   * @return true if the table must grow before another entry is inserted.
   */
  private boolean needsGrowthForInsert() {
    return (occupiedSlots + 1) * 2 > slotKeys.length;
  }

  /**
   * Double the table capacity and reinsert all prior entries.
   */
  private void growAndRehash() {
    final long[] priorKeys = slotKeys;
    final Object[] priorValues = slotValues;

    int newCapacity = priorKeys.length * 2;
    slotKeys = new long[newCapacity];
    slotValues = new Object[newCapacity];
    indexMask = newCapacity - 1;
    occupiedSlots = 0;

    for (int priorSlot = 0; priorSlot < priorValues.length; priorSlot++) {
      Object value = priorValues[priorSlot];
      if (value != null) {
        insertKnownAbsent(priorKeys[priorSlot], value);
      }
    }
  }

  /**
   * Insert an entry which is known not to be present, used while rehashing.
   *
   * @param key composite key for the pair.
   * @param value value previously cached for this key.
   */
  private void insertKnownAbsent(long key, Object value) {
    int slotIndex = spreadKey(key) & indexMask;
    while (slotValues[slotIndex] != null) {
      slotIndex = nextSlot(slotIndex);
    }
    slotKeys[slotIndex] = key;
    slotValues[slotIndex] = value;
    occupiedSlots += 1;
  }

  /**
   * Get the slot to probe after a collision, wrapping at the end of the table.
   *
   * @param slotIndex the slot which was found to be occupied by another key.
   * @return the next slot to probe.
   */
  private int nextSlot(int slotIndex) {
    return (slotIndex + 1) & indexMask;
  }

  /**
   * Spread the bits of a composite key so consecutive ids do not cluster.
   *
   * <p>Ids are handed out in sequence, so the low bits of a composite key advance far more often
   * than the high ones, and the mask applied by callers reads only low bits. Multiplying spreads
   * the sequential low bits across the whole word, and folding the halves together with an xor
   * brings the high half back down into the bits the mask reads. An odd multiplier is required
   * because an even one would discard the top bits of the key.</p>
   *
   * <p>Only one fold is applied here where fastutil's HashCommon.mix follows with a second
   * xorshift to further improve the lowest bits. That step earns its cost for adversarially
   * distributed keys and is not needed for the dense sequential ids this table sees.</p>
   *
   * @param key composite key for the pair.
   * @return mixed hash value which may be used with the table mask.
   */
  private static int spreadKey(long key) {
    long spread = key * GOLDEN_RATIO_64;
    return (int) (spread ^ (spread >>> 32));
  }

}
