/**
 * Tests for PairTable.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


/**
 * Tests for the caching, growth, and key handling of the pair keyed lookup table.
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
    String first = table.getOrPut(7, "a", "b", this::join);
    String second = table.getOrPut(7, "a", "b", this::join);

    assertSame(first, second);
    assertEquals(1, creatorCalls.get());
  }

  @Test
  void testCreatorReceivesBothIdentities() {
    assertEquals("meters:feet", table.getOrPut(7, "meters", "feet", this::join));
  }

  @Test
  void testZeroKeyIsCachedLikeAnyOther() {
    // An earlier version of this table reserved the key zero to mark a slot empty, which meant a
    // zero key was never recognised as cached and the creator ran on every lookup.
    String first = table.getOrPut(0, "a", "b", this::join);
    String second = table.getOrPut(0, "a", "b", this::join);

    assertSame(first, second);
    assertEquals(1, creatorCalls.get());
  }

  @Test
  void testDistinctKeysStayDistinctAcrossGrowth() {
    // Well past the initial capacity of 64 so that several rehashes happen along the way.
    int keyCount = 500;
    List<String> created = new ArrayList<>();
    for (int key = 0; key < keyCount; key++) {
      created.add(table.getOrPut(key, "first" + key, "second" + key, this::join));
    }

    assertEquals(keyCount, creatorCalls.get());
    for (int key = 0; key < keyCount; key++) {
      assertSame(created.get(key), table.getOrPut(key, "unused", "unused", this::join));
    }
    assertEquals(keyCount, creatorCalls.get());
  }

  @Test
  void testNegativeAndWideKeysAreAddressedDistinctly() {
    long[] keys = {-1, Long.MIN_VALUE, Long.MAX_VALUE, 1L << 32, -(1L << 32)};
    List<String> created = new ArrayList<>();
    for (long key : keys) {
      created.add(table.getOrPut(key, "first" + key, "second" + key, this::join));
    }

    for (int index = 0; index < keys.length; index++) {
      assertSame(created.get(index), table.getOrPut(keys[index], "unused", "unused", this::join));
    }
    assertEquals(keys.length, creatorCalls.get());
  }

  @Test
  void testCreatorMayInsertIntoTheSameTable() {
    // A creator that fills the table forces a rehash partway through the outer insert, so the
    // slot found before the creator ran no longer belongs to the array being written.
    String outer = table.getOrPut(1, "outer", "value", (first, second) -> {
      for (int key = 100; key < 400; key++) {
        table.getOrPut(key, "inner" + key, "value", this::join);
      }
      return join(first, second);
    });

    assertSame(outer, table.getOrPut(1, "unused", "unused", this::join));
    for (int key = 100; key < 400; key++) {
      assertEquals("inner" + key + ":value", table.getOrPut(key, "unused", "unused", this::join));
    }
  }

  @Test
  void testValueInsertedByCreatorForItsOwnKeyIsKept() {
    String inner = table.getOrPut(5, "outer", "value", (first, second) -> {
      table.getOrPut(5, "inner", "value", this::join);
      return join(first, second);
    });

    assertEquals("inner:value", inner);
    assertSame(inner, table.getOrPut(5, "unused", "unused", this::join));
  }

}
