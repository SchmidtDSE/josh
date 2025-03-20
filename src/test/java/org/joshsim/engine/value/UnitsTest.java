package org.joshsim.engine.value;

import static org.junit.jupiter.api.Assertions.*;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

class UnitsTest {

  @Test
  void testConstructorWithString() {
    Units units = new Units("m");
    
    assertEquals(1, units.getNumeratorUnits().size());
    assertTrue(units.getNumeratorUnits().containsKey("m"));
    assertEquals(1, units.getNumeratorUnits().get("m"));
  }

  @Test
  void testConstructorWithStringContainingDenominator() {
    Units units = new Units("m / s");
    
    assertEquals(1, units.getNumeratorUnits().size());
    assertTrue(units.getNumeratorUnits().containsKey("m"));
    assertEquals(1, units.getNumeratorUnits().get("m"));
    
    assertEquals(1, units.getDenominatorUnits().size());
    assertTrue(units.getDenominatorUnits().containsKey("s"));
    assertEquals(1, units.getDenominatorUnits().get("s"));
  }

  @Test
  void testConstructorWithComplexString() {
    Units units = new Units("m * m / s * s");
    
    assertEquals(1, units.getNumeratorUnits().size());
    assertTrue(units.getNumeratorUnits().containsKey("m"));
    assertEquals(2, units.getNumeratorUnits().get("m"));
    
    assertEquals(1, units.getDenominatorUnits().size());
    assertTrue(units.getDenominatorUnits().containsKey("s"));
    assertEquals(2, units.getDenominatorUnits().get("s"));
  }

  @Test
  void testConstructorWithComplexNumeratorString() {
    Units units = new Units("m * m * kg");
    
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
      new Units("m / s / kilogram");
    });
  }

  @Test
  void testConstructorWithMaps() {
    Map<String, Integer> numerator = new HashMap<>();
    numerator.put("m", 2);
    
    Map<String, Integer> denominator = new HashMap<>();
    denominator.put("s", 1);
    
    Units units = new Units(numerator, denominator);
    
    assertEquals(1, units.getNumeratorUnits().size());
    assertTrue(units.getNumeratorUnits().containsKey("m"));
    assertEquals(2, units.getNumeratorUnits().get("m"));
    
    assertEquals(1, units.getDenominatorUnits().size());
    assertTrue(units.getDenominatorUnits().containsKey("s"));
    assertEquals(1, units.getDenominatorUnits().get("s"));
  }

  @Test
  void testGetNumeratorUnits() {
    Units units = new Units("m * m");
    Map<String, Integer> numerator = units.getNumeratorUnits();
    
    assertEquals(1, numerator.size());
    assertTrue(numerator.containsKey("m"));
    assertEquals(2, numerator.get("m"));
  }

  @Test
  void testGetDenominatorUnits() {
    Units units = new Units("m / s");
    Map<String, Integer> denominator = units.getDenominatorUnits();
    
    assertEquals(1, denominator.size());
    assertTrue(denominator.containsKey("s"));
    assertEquals(1, denominator.get("s"));
  }

  @Test
  void testInvert() {
    Units units = new Units("m / s");
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
    Units units1 = new Units("m");
    Units units2 = new Units("s");
    
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
    Units units1 = new Units("m");
    Units units2 = new Units("m");
    
    Units multiplied = units1.multiply(units2);
    
    assertEquals(1, multiplied.getNumeratorUnits().size());
    assertTrue(multiplied.getNumeratorUnits().containsKey("m"));
    assertEquals(2, multiplied.getNumeratorUnits().get("m"));
    
    assertEquals(0, multiplied.getDenominatorUnits().size());
  }

  @Test
  void testDivide() {
    Units units1 = new Units("m");
    Units units2 = new Units("s");
    
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
    Map<String, Integer> numerator = new TreeMap<>();
    numerator.put("m", 3);
    numerator.put("kilogram", 1);
    
    Map<String, Integer> denominator = new TreeMap<>();
    denominator.put("m", 1);
    denominator.put("s", 2);
    
    Units units = new Units(numerator, denominator);
    Units simplified = units.simplify();
    
    assertEquals(2, simplified.getNumeratorUnits().size());
    assertTrue(simplified.getNumeratorUnits().containsKey("m"));
    assertEquals(2, simplified.getNumeratorUnits().get("m"));
    assertTrue(simplified.getNumeratorUnits().containsKey("kilogram"));
    assertEquals(1, simplified.getNumeratorUnits().get("kilogram"));
    
    assertEquals(1, simplified.getDenominatorUnits().size());
    assertTrue(simplified.getDenominatorUnits().containsKey("s"));
    assertEquals(2, simplified.getDenominatorUnits().get("s"));
  }

  @Test
  void testSimplifyWithFullCancellation() {
    Map<String, Integer> numerator = new TreeMap<>();
    numerator.put("m", 2);
    
    Map<String, Integer> denominator = new TreeMap<>();
    denominator.put("m", 2);
    
    Units units = new Units(numerator, denominator);
    Units simplified = units.simplify();
    
    assertEquals(0, simplified.getNumeratorUnits().size());
    assertEquals(0, simplified.getDenominatorUnits().size());
  }

  @Test
  void testEquals() {
    Units units1 = new Units("m / s");
    Units units2 = new Units("m / s");
    Units units3 = new Units("kilogram");
    
    assertTrue(units1.equals(units2));
    assertFalse(units1.equals(units3));
  }

  @Test
  void testToString() {
    Units units = new Units("m * m / s");
    assertEquals("m * m/s", units.toString());
    
    // Empty case
    Map<String, Integer> emptyMap = new TreeMap<>();
    Units emptyUnits = new Units(emptyMap, emptyMap);
    assertEquals("", emptyUnits.toString());
  }

  @Test
  void testHashCode() {
    Units units1 = new Units("m / s");
    Units units2 = new Units("m / s");
    
    assertEquals(units1.hashCode(), units2.hashCode());
  }

  @Test
  void testEmptyString() {
    Units units = new Units("");
    
    assertEquals(0, units.getNumeratorUnits().size());
    assertEquals(0, units.getDenominatorUnits().size());
  }

  @Test
  void testParseMultiplyString() {
    Units units = new Units("m * kilogram * m");
    
    assertEquals(2, units.getNumeratorUnits().size());
    assertTrue(units.getNumeratorUnits().containsKey("m"));
    assertEquals(2, units.getNumeratorUnits().get("m"));
    assertTrue(units.getNumeratorUnits().containsKey("kilogram"));
    assertEquals(1, units.getNumeratorUnits().get("kilogram"));
  }

  @Test
  void testSerializeMultiplyString() {
    Map<String, Integer> numerator = new TreeMap<>();
    numerator.put("m", 2);
    numerator.put("kilogram", 1);
    
    Units units = new Units(numerator, new TreeMap<>());
    
    // The exact order might depend on TreeMap sorting, but we can check for content
    String result = units.toString();
    assertTrue(result.contains("m"));
    assertTrue(result.contains("kilogram"));
    assertTrue(result.contains("*"));
  }
}