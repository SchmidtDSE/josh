/**
 * Tests for Units.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.value.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;


/**
 * Tests for structure describing a unit like meter.
 */
class UnitsTest {

  @Test
  void testConstructorWithString() {
    Units units = Units.of("m");

    assertEquals(1, units.getNumeratorUnits().size());
    assertTrue(units.getNumeratorUnits().containsKey("m"));
    assertEquals(1, units.getNumeratorUnits().get("m"));
  }

  @Test
  void testConstructorWithCount() {
    Units units = Units.of("m * count");

    assertEquals(1, units.getNumeratorUnits().size());
    assertTrue(units.getNumeratorUnits().containsKey("m"));
    assertEquals(1, units.getNumeratorUnits().get("m"));
  }

  @Test
  void testConstructorWithStringContainingDenominator() {
    Units units = Units.of("m / s");

    assertEquals(1, units.getNumeratorUnits().size());
    assertTrue(units.getNumeratorUnits().containsKey("m"));
    assertEquals(1, units.getNumeratorUnits().get("m"));

    assertEquals(1, units.getDenominatorUnits().size());
    assertTrue(units.getDenominatorUnits().containsKey("s"));
    assertEquals(1, units.getDenominatorUnits().get("s"));
  }

  @Test
  void testConstructorWithComplexString() {
    Units units = Units.of("m * m / s * s");

    assertEquals(1, units.getNumeratorUnits().size());
    assertTrue(units.getNumeratorUnits().containsKey("m"));
    assertEquals(2, units.getNumeratorUnits().get("m"));

    assertEquals(1, units.getDenominatorUnits().size());
    assertTrue(units.getDenominatorUnits().containsKey("s"));
    assertEquals(2, units.getDenominatorUnits().get("s"));
  }

  @Test
  void testConstructorWithComplexNumeratorString() {
    Units units = Units.of("m * m * kg");

    assertEquals(2, units.getNumeratorUnits().size());
    assertTrue(units.getNumeratorUnits().containsKey("m"));
    assertEquals(2, units.getNumeratorUnits().get("m"));
    assertTrue(units.getNumeratorUnits().containsKey("kg"));
    assertEquals(1, units.getNumeratorUnits().get("kg"));

    assertEquals(0, units.getDenominatorUnits().size());
  }

  @Test
  void testConstructorWithTooManyDenominators() {
    assertThrows(IllegalArgumentException.class, () -> {
      Units.of("m / s / kilogram");
    });
  }

  @Test
  void testConstructorWithMaps() {
    Map<String, Long> numerator = new HashMap<>();
    numerator.put("m", 2L);

    Map<String, Long> denominator = new HashMap<>();
    denominator.put("s", 1L);

    Units units = Units.of(numerator, denominator);

    assertEquals(1, units.getNumeratorUnits().size());
    assertTrue(units.getNumeratorUnits().containsKey("m"));
    assertEquals(2, units.getNumeratorUnits().get("m"));

    assertEquals(1, units.getDenominatorUnits().size());
    assertTrue(units.getDenominatorUnits().containsKey("s"));
    assertEquals(1, units.getDenominatorUnits().get("s"));
  }

  @Test
  void testGetNumeratorUnits() {
    Units units = Units.of("m * m");
    Map<String, Long> numerator = units.getNumeratorUnits();

    assertEquals(1, numerator.size());
    assertTrue(numerator.containsKey("m"));
    assertEquals(2, numerator.get("m"));
  }

  @Test
  void testGetDenominatorUnits() {
    Units units = Units.of("m / s");
    Map<String, Long> denominator = units.getDenominatorUnits();

    assertEquals(1, denominator.size());
    assertTrue(denominator.containsKey("s"));
    assertEquals(1, denominator.get("s"));
  }

  @Test
  void testInvert() {
    Units units = Units.of("m / s");
    Units inverted = units.invert();

    assertEquals(1, inverted.getNumeratorUnits().size());
    assertTrue(inverted.getNumeratorUnits().containsKey("s"));
    assertEquals(1, inverted.getNumeratorUnits().get("s"));

    assertEquals(1, inverted.getDenominatorUnits().size());
    assertTrue(inverted.getDenominatorUnits().containsKey("m"));
    assertEquals(1, inverted.getDenominatorUnits().get("m"));
  }

  @Test
  void testMultiply() {
    Units units1 = Units.of("m");
    Units units2 = Units.of("s");

    Units multiplied = units1.multiply(units2);

    assertEquals(2, multiplied.getNumeratorUnits().size());
    assertTrue(multiplied.getNumeratorUnits().containsKey("m"));
    assertEquals(1, multiplied.getNumeratorUnits().get("m"));
    assertTrue(multiplied.getNumeratorUnits().containsKey("s"));
    assertEquals(1, multiplied.getNumeratorUnits().get("s"));

    assertEquals(0, multiplied.getDenominatorUnits().size());
  }

  @Test
  void testMultiplyWithSameUnits() {
    Units units1 = Units.of("m");
    Units units2 = Units.of("m");

    Units multiplied = units1.multiply(units2);

    assertEquals(1, multiplied.getNumeratorUnits().size());
    assertTrue(multiplied.getNumeratorUnits().containsKey("m"));
    assertEquals(2, multiplied.getNumeratorUnits().get("m"));

    assertEquals(0, multiplied.getDenominatorUnits().size());
  }

  @Test
  void testDivide() {
    Units units1 = Units.of("m");
    Units units2 = Units.of("s");

    Units divided = units1.divide(units2);

    assertEquals(1, divided.getNumeratorUnits().size());
    assertTrue(divided.getNumeratorUnits().containsKey("m"));
    assertEquals(1, divided.getNumeratorUnits().get("m"));

    assertEquals(1, divided.getDenominatorUnits().size());
    assertTrue(divided.getDenominatorUnits().containsKey("s"));
    assertEquals(1, divided.getDenominatorUnits().get("s"));
  }

  @Test
  void testSimplify() {
    Map<String, Long> numerator = new TreeMap<>();
    numerator.put("m", 3L);
    numerator.put("kg", 1L);

    Map<String, Long> denominator = new TreeMap<>();
    denominator.put("m", 1L);
    denominator.put("s", 2L);

    Units units = Units.of(numerator, denominator);
    Units simplified = units.simplify();

    assertEquals(2, simplified.getNumeratorUnits().size());
    assertTrue(simplified.getNumeratorUnits().containsKey("m"));
    assertEquals(2, simplified.getNumeratorUnits().get("m"));
    assertTrue(simplified.getNumeratorUnits().containsKey("kg"));
    assertEquals(1, simplified.getNumeratorUnits().get("kg"));

    assertEquals(1, simplified.getDenominatorUnits().size());
    assertTrue(simplified.getDenominatorUnits().containsKey("s"));
    assertEquals(2, simplified.getDenominatorUnits().get("s"));
  }

  @Test
  void testSimplifyWithFullCancellation() {
    Map<String, Long> numerator = new TreeMap<>();
    numerator.put("m", 2L);

    Map<String, Long> denominator = new TreeMap<>();
    denominator.put("m", 2L);

    Units units = Units.of(numerator, denominator);
    Units simplified = units.simplify();

    assertEquals(0, simplified.getNumeratorUnits().size());
    assertEquals(0, simplified.getDenominatorUnits().size());
  }

  @Test
  void testEquals() {
    Units units1 = Units.of("m / s");
    Units units2 = Units.of("m / s");
    Units units3 = Units.of("kilogram");

    assertTrue(units1.equals(units2));
    assertFalse(units1.equals(units3));
  }

  @Test
  void testToString() {
    Units units = Units.of("m * m / s");
    assertEquals("m * m / s", units.toString());

    // Empty case
    Map<String, Long> emptyMap = new TreeMap<>();
    Units emptyUnits = Units.of(emptyMap, emptyMap);
    assertEquals("", emptyUnits.toString());
  }

  @Test
  void testHashCode() {
    Units units1 = Units.of("m / s");
    Units units2 = Units.of("m / s");

    assertEquals(units1.hashCode(), units2.hashCode());
  }

  @Test
  void testEmptyString() {
    Units units = Units.EMPTY;

    assertEquals(0, units.getNumeratorUnits().size());
    assertEquals(0, units.getDenominatorUnits().size());
  }

  @Test
  void testParseMultiplyString() {
    Units units = Units.of("m * kilogram * m");

    assertEquals(2, units.getNumeratorUnits().size());
    assertTrue(units.getNumeratorUnits().containsKey("m"));
    assertEquals(2, units.getNumeratorUnits().get("m"));
    assertTrue(units.getNumeratorUnits().containsKey("kilogram"));
    assertEquals(1, units.getNumeratorUnits().get("kilogram"));
  }

  @Test
  void testSerializeMultiplyString() {
    Map<String, Long> numerator = new TreeMap<>();
    numerator.put("m", 2L);
    numerator.put("kilogram", 1L);

    Units units = Units.of(numerator, new TreeMap<>());

    // The exact order might depend on TreeMap sorting, but we can check for content
    String result = units.toString();
    assertTrue(result.contains("m"));
    assertTrue(result.contains("kilogram"));
    assertTrue(result.contains("*"));
  }

  @Test
  void testCacheReturnsIdenticalInstance() {
    // Test that repeated calls to Units.of() with same string return same instance
    Units units1 = Units.of("meters");
    Units units2 = Units.of("meters");

    // Verify reference equality (same object instance)
    assertTrue(units1 == units2, "Cache should return same instance for same string");

    // Test with compound units
    Units compound1 = Units.of("meters * meters");
    Units compound2 = Units.of("meters * meters");
    assertTrue(compound1 == compound2, "Cache should return same instance for compound units");

    // Test caching for empty and count strings
    Units empty1 = Units.of("");
    Units empty2 = Units.of("");
    assertTrue(empty1 == empty2, "Cache should return same instance for empty string");

    Units count1 = Units.of("count");
    Units count2 = Units.of("count");
    assertTrue(count1 == count2, "Cache should return same instance for count");

    // COUNT and EMPTY are both semantically empty (count is filtered out), so they're the same
    assertTrue(Units.COUNT == Units.EMPTY, "COUNT should be same instance as EMPTY");
  }

  @Test
  void testCacheWithCanonicalForm() {
    // Test that canonical form is also cached
    // First call with non-canonical form
    Units units1 = Units.of("m * m");

    // Get the canonical string representation
    String canonical = units1.toString();

    // Call with canonical form
    Units units2 = Units.of(canonical);

    // Should return same instance due to double caching strategy
    assertTrue(units1 == units2, "Cache should work with canonical form");
  }

  @Test
  void testMultiplyCacheReturnsIdenticalInstance() {
    // Test that multiply() caches results and returns same instance on repeated calls
    Units meters = Units.of("meters");
    Units seconds = Units.of("seconds");

    Units result1 = meters.multiply(seconds);
    Units result2 = meters.multiply(seconds);

    // Verify reference equality (same object instance)
    assertTrue(result1 == result2, "Multiply cache should return same instance");

    // Test with same units
    Units metersSquared1 = meters.multiply(meters);
    Units metersSquared2 = meters.multiply(meters);
    assertTrue(metersSquared1 == metersSquared2, "Multiply cache should work for same units");
  }

  @Test
  void testDivideCacheReturnsIdenticalInstance() {
    // Test that divide() caches results and returns same instance on repeated calls
    Units kg = Units.of("kg");
    Units hectares = Units.of("hectares");

    Units result1 = kg.divide(hectares);
    Units result2 = kg.divide(hectares);

    // Verify reference equality (same object instance)
    assertTrue(result1 == result2, "Divide cache should return same instance");

    // Test correctness is preserved
    assertEquals(1, result1.getNumeratorUnits().size());
    assertTrue(result1.getNumeratorUnits().containsKey("kg"));
    assertEquals(1, result1.getDenominatorUnits().size());
    assertTrue(result1.getDenominatorUnits().containsKey("hectares"));
  }

  @Test
  void testOfMapsCacheReturnsIdenticalInstance() {
    // Test that Units.of(Map, Map) caches results by canonical form
    Map<String, Long> numerator = new TreeMap<>();
    numerator.put("meters", 2L);
    Map<String, Long> denominator = new TreeMap<>();

    Units result1 = Units.of(numerator, denominator);
    Units result2 = Units.of(numerator, denominator);

    // Should return same instance due to caching by canonical form
    assertTrue(result1 == result2, "of(Map, Map) cache should return same instance");

    // Test with different maps but same content
    Map<String, Long> numerator2 = new TreeMap<>();
    numerator2.put("meters", 2L);
    Units result3 = Units.of(numerator2, new TreeMap<>());

    // Should still return same instance (same canonical form)
    assertTrue(result1 == result3, "of(Map, Map) should cache by canonical form");
  }

  @Test
  void testCacheConsistencyAcrossEntryPoints() {
    // Test that cache is consistent across different entry points (string vs maps)
    // Create via string first
    Units fromString = Units.of("meters * meters");

    // Create via maps with equivalent content
    Map<String, Long> numerator = new TreeMap<>();
    numerator.put("meters", 2L);
    Units fromMaps = Units.of(numerator, new TreeMap<>());

    // Both should have same canonical form
    assertEquals(fromString.toString(), fromMaps.toString());

    // Should return same instance (cached by canonical form)
    assertTrue(fromString == fromMaps, "Cache should be consistent across entry points");
  }

  @Test
  void testMultiplyResultCachedByCanonicalForm() {
    // Test that multiply() results are cached by canonical form
    // and can be retrieved via Units.of(Map, Map)
    Units meters = Units.of("meters");
    Units metersSquared = meters.multiply(meters);

    // Now call of(Map, Map) with equivalent maps
    Map<String, Long> numerator = new TreeMap<>();
    numerator.put("meters", 2L);
    Units fromMaps = Units.of(numerator, new TreeMap<>());

    // Should return same instance (cached by canonical form from multiply)
    assertTrue(metersSquared == fromMaps,
        "Multiply result should be cached by canonical form");
  }

  @Test
  void testEmptyMapsReturnEmptyConstant() {
    // Test that empty maps return cached empty unit
    Units empty = Units.of(new TreeMap<>(), new TreeMap<>());

    // Should have empty canonical form
    assertEquals("", empty.toString());

    // Should be semantically equal to EMPTY constant
    assertTrue(empty.equals(Units.EMPTY));

    // Should return same instance on repeated calls
    Units empty2 = Units.of(new TreeMap<>(), new TreeMap<>());
    assertTrue(empty == empty2, "Empty maps should return same cached instance");
  }
}
