/**
 * Utility class for packing pairs of identities into single long keys.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.util;


/**
 * Utility class for building composite cache keys from pairs of identities.
 *
 * <p>Several caches are keyed on an ordered pair of instances which each carry a dense integer
 * identity, such as a pair of Units or a pair of LanguageTypes. Packing both identities into one
 * long lets those caches use a primitive keyed table, avoiding the boxing that a map of object
 * keys would require on every lookup.</p>
 */
public final class CompositeKeyUtil {

  private CompositeKeyUtil() {}

  /**
   * Pack two identities into a single long key, first in the high bits and second in the low.
   *
   * <p>The pairing is order sensitive, so the key for (a, b) differs from the key for (b, a).
   * Each identity keeps its full 32 bits, so distinct pairs of distinct identities always produce
   * distinct keys.</p>
   *
   * <p>This packing is the same function as Agrona's Hashing.compoundKey, which was consulted
   * when the alternative of taking that library was evaluated and declined; see PairTable for
   * why.</p>
   *
   * <p>The second identity is masked with {@code 0xFFFFFFFFL} because widening an int to a long
   * sign extends it. Without the mask a negative second identity would set every high bit and
   * corrupt the first identity's half of the key. Identities are handed out as positive counters
   * today, so the mask guards against a counter that has wrapped rather than against normal
   * operation.</p>
   *
   * @param firstId identity of the first member of the pair, placed in the high 32 bits.
   * @param secondId identity of the second member of the pair, placed in the low 32 bits.
   * @return composite key for this ordered pair of identities.
   */
  public static long packIds(int firstId, int secondId) {
    return ((long) firstId << 32) | (secondId & 0xFFFFFFFFL);
  }

}
