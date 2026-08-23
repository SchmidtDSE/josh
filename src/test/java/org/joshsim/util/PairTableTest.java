/**
 * Tests for PairTable.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


/**
 * Tests for the caching, growth, and identity pairing of the pair keyed lookup table.
 */
class PairTableTest {

  private PairTable<String, String, String> table;
  private AtomicInteger creatorCalls;

  @BeforeEach
  void setUp() {
    table = new PairTable<>();
    creatorCalls = new AtomicInteger();
  }

  private String join(String first, String second) {
    creatorCalls.incrementAndGet();
    return first + ":" + second;
  }

  @Test
  void testRepeatedLookupReturnsTheCachedInstance() {
    String first = table.getOrPut(3, 4, "a", "b", this::join);
    String second = table.getOrPut(3, 4, "a", "b", this::join);

    assertSame(first, second);
    assertEquals(1, creatorCalls.get());
  }

  @Test
  void testCreatorReceivesBothIdentifiedValues() {
    assertEquals("meters:feet", table.getOrPut(3, 4, "meters", "feet", this::join));
  }

  @Test
  void testPairOrderSelectsDistinctEntries() {
    String forward = table.getOrPut(1, 2, "meters", "feet", this::join);
    String reverse = table.getOrPut(2, 1, "feet", "meters", this::join);

    assertEquals("meters:feet", forward);
    assertEquals("feet:meters", reverse);
    assertNotSame(forward, reverse);
  }

  @Test
  void testZeroIdentitiesAreCachedLikeAnyOther() {
    // An earlier version of this table reserved the key zero to mark a slot empty, which meant the
    // pair of zero identities was never recognised as cached and the creator ran on every lookup.
    String first = table.getOrPut(0, 0, "a", "b", this::join);
    String second = table.getOrPut(0, 0, "a", "b", this::join);

    assertSame(first, second);
    assertEquals(1, creatorCalls.get());
  }

  @Test
  void testNegativeIdentitiesDoNotCollide() {
    // Widening an int to a long sign extends it, so without masking the low identity the pairs
    // (0, -1) and (-1, -1) both pack to a key of every bit set and would share one entry.
    String zeroThenNegative = table.getOrPut(0, -1, "first", "second", this::join);
    String bothNegative = table.getOrPut(-1, -1, "third", "fourth", this::join);
    String negativeThenZero = table.getOrPut(-1, 0, "fifth", "sixth", this::join);

    assertEquals("first:second", zeroThenNegative);
    assertEquals("third:fourth", bothNegative);
    assertEquals("fifth:sixth", negativeThenZero);
    assertEquals(3, creatorCalls.get());
  }

  @Test
  void testExtremeIdentitiesAreAddressedDistinctly() {
    int[][] pairs = {
        {Integer.MIN_VALUE, Integer.MIN_VALUE},
        {Integer.MIN_VALUE, Integer.MAX_VALUE},
        {Integer.MAX_VALUE, Integer.MIN_VALUE},
        {Integer.MAX_VALUE, Integer.MAX_VALUE},
    };
    List<String> created = new ArrayList<>();
    for (int[] pair : pairs) {
      created.add(
          table.getOrPut(pair[0], pair[1], "first" + pair[0], "second" + pair[1], this::join)
      );
    }

    for (int index = 0; index < pairs.length; index++) {
      assertSame(
          created.get(index),
          table.getOrPut(pairs[index][0], pairs[index][1], "unused", "unused", this::join)
      );
    }
    assertEquals(pairs.length, creatorCalls.get());
  }

  @Test
  void testDistinctIdentitiesStayDistinctAcrossGrowth() {
    // Well past the initial capacity of 64 so that several rehashes happen along the way.
    int pairCount = 500;
    List<String> created = new ArrayList<>();
    for (int id = 0; id < pairCount; id++) {
      created.add(table.getOrPut(id, id + 1, "first" + id, "second" + id, this::join));
    }

    assertEquals(pairCount, creatorCalls.get());
    for (int id = 0; id < pairCount; id++) {
      assertSame(created.get(id), table.getOrPut(id, id + 1, "unused", "unused", this::join));
    }
    assertEquals(pairCount, creatorCalls.get());
  }

  @Test
  void testCreatorMayInsertIntoTheSameTable() {
    // A creator that fills the table forces a rehash partway through the outer insert, so the
    // slot found before the creator ran no longer belongs to the array being written.
    String outer = table.getOrPut(1, 1, "outer", "value", (first, second) -> {
      for (int id = 100; id < 400; id++) {
        table.getOrPut(id, id, "inner" + id, "value", this::join);
      }
      return join(first, second);
    });

    assertSame(outer, table.getOrPut(1, 1, "unused", "unused", this::join));
    for (int id = 100; id < 400; id++) {
      assertEquals("inner" + id + ":value", table.getOrPut(id, id, "unused", "unused", this::join));
    }
  }

  @Test
  void testValueInsertedByCreatorForItsOwnPairIsKept() {
    String inner = table.getOrPut(5, 6, "outer", "value", (first, second) -> {
      table.getOrPut(5, 6, "inner", "value", this::join);
      return join(first, second);
    });

    assertEquals("inner:value", inner);
    assertSame(inner, table.getOrPut(5, 6, "unused", "unused", this::join));
  }

}
