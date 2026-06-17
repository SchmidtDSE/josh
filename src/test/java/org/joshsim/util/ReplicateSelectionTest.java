package org.joshsim.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ReplicateSelection}, the shared resolver that turns replicate-selection knobs
 * into an ordered list of absolute replicate indices.
 */
class ReplicateSelectionTest {

  @Test
  void parseNullIsEmpty() {
    assertTrue(ReplicateSelection.parse(null).isEmpty());
  }

  @Test
  void parseBlankIsEmpty() {
    assertTrue(ReplicateSelection.parse("   ").isEmpty());
  }

  @Test
  void parsePreservesOrder() {
    assertEquals(List.of(3, 7, 8), ReplicateSelection.parse("3,7,8").orElseThrow());
  }

  @Test
  void parseTrimsAndIgnoresEmptyEntries() {
    assertEquals(List.of(1, 4, 5), ReplicateSelection.parse(" 1, 4 ,,5 ").orElseThrow());
  }

  @Test
  void parseRejectsNonNumeric() {
    assertThrows(IllegalArgumentException.class, () -> ReplicateSelection.parse("3,x,8"));
  }

  @Test
  void parseRejectsNegative() {
    assertThrows(IllegalArgumentException.class, () -> ReplicateSelection.parse("3,-1,8"));
  }

  @Test
  void parseRejectsDuplicates() {
    assertThrows(IllegalArgumentException.class, () -> ReplicateSelection.parse("3,7,3"));
  }

  @Test
  void resolveUsesExplicitIndicesWhenPresent() {
    List<Integer> resolved = ReplicateSelection.resolve(Optional.of(List.of(3, 7, 8)), 0, 99);
    assertEquals(List.of(3, 7, 8), resolved);
  }

  @Test
  void resolveExpandsRangeWhenNoIndices() {
    List<Integer> resolved = ReplicateSelection.resolve(Optional.empty(), 5, 3);
    assertEquals(List.of(5, 6, 7), resolved);
  }

  @Test
  void resolveDefaultRangeStartsAtZero() {
    assertEquals(List.of(0, 1, 2), ReplicateSelection.resolve(Optional.empty(), 0, 3));
  }

  @Test
  void resolveZeroCountIsEmpty() {
    assertTrue(ReplicateSelection.resolve(Optional.empty(), 0, 0).isEmpty());
  }

  @Test
  void toCsvRoundTrips() {
    String csv = ReplicateSelection.toCsv(List.of(3, 7, 8));
    assertEquals("3,7,8", csv);
    assertEquals(List.of(3, 7, 8), ReplicateSelection.parse(csv).orElseThrow());
  }

  @Test
  void validateRejectsDuplicate() {
    assertThrows(IllegalArgumentException.class,
        () -> ReplicateSelection.validate(List.of(1, 1)));
  }

  @Test
  void validateAcceptsNonContiguous() {
    ReplicateSelection.validate(List.of(8, 3, 7));
    assertFalse(List.of(8, 3, 7).isEmpty());
  }
}
