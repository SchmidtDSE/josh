package org.joshsim.engine.value;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class IntScalarTest {

  @Test
  void testConstructorAndGetters() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    IntScalar scalar = new IntScalar(caster, 10L, "m");

    assertEquals(10L, scalar.getAsInt());
    assertEquals("m", scalar.getUnits());
    assertEquals(new BigDecimal(10), scalar.getAsDecimal());
    assertEquals("10", scalar.getAsString());
    assertEquals(new LanguageType("int"), scalar.getLanguageType());
    assertEquals(10L, scalar.getInnerValue());
  }

  @Test
  void testGetAsBoolean() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    IntScalar nonZeroScalar = new IntScalar(caster, 10L, "");
    IntScalar zeroScalar = new IntScalar(caster, 0L, "");
    IntScalar oneScalar = new IntScalar(caster, 1L, "");

    assertThrows(UnsupportedOperationException.class, () -> nonZeroScalar.getAsBoolean());
    assertFalse(zeroScalar.getAsBoolean());
    assertTrue(oneScalar.getAsBoolean());
  }

  @Test
  void testAdd() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    IntScalar scalar1 = new IntScalar(caster, 10L, "m");
    IntScalar scalar2 = new IntScalar(caster, 5L, "m");

    IntScalar result = (IntScalar)scalar1.add(scalar2);
    assertEquals(15L, result.getAsInt());
    assertEquals("m", result.getUnits());
  }

  @Test
  void testAddWithDifferentUnits() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    IntScalar scalar1 = new IntScalar(caster, 10L, "m");
    IntScalar scalar2 = new IntScalar(caster, 5L, "s");

    assertThrows(IllegalArgumentException.class, () -> scalar1.add(scalar2));
  }

  @Test
  void testSubtract() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    IntScalar scalar1 = new IntScalar(caster, 10L, "m");
    IntScalar scalar2 = new IntScalar(caster, 5L, "m");

    IntScalar result = (IntScalar)scalar1.subtract(scalar2);
    assertEquals(5L, result.getAsInt());
    assertEquals("m", result.getUnits());
  }

  @Test
  void testSubtractWithDifferentUnits() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    IntScalar scalar1 = new IntScalar(caster, 10L, "m");
    IntScalar scalar2 = new IntScalar(caster, 5L, "s");

    assertThrows(IllegalArgumentException.class, () -> scalar1.subtract(scalar2));
  }

  @Test
  void testMultiply() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    IntScalar scalar1 = new IntScalar(caster, 10L, "m");
    IntScalar scalar2 = new IntScalar(caster, 2L, "s");

    IntScalar result = (IntScalar) scalar1.multiply(scalar2);
    assertEquals(20L, result.getAsInt());
    assertEquals("m * s", result.getUnits());
  }

  @Test
  void testDivide() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    IntScalar scalar1 = new IntScalar(caster, 10L, "m");
    IntScalar scalar2 = new IntScalar(caster, 2L, "s");

    IntScalar result = (IntScalar) scalar1.divide(scalar2);
    assertEquals(5L, result.getAsInt());
    assertEquals("m / s", result.getUnits());
  }

  @Test
  void testDivideByZero() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    IntScalar scalar1 = new IntScalar(caster, 10L, "m");
    IntScalar zeroScalar = new IntScalar(caster, 0L, "s");

    assertThrows(ArithmeticException.class, () -> scalar1.divide(zeroScalar));
  }

  @Test
  void testDivisionRounding() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    IntScalar scalar1 = new IntScalar(caster, 7L, "m");
    IntScalar scalar2 = new IntScalar(caster, 2L, "s");

    IntScalar result = (IntScalar) scalar1.divide(scalar2);
    assertEquals(3L, result.getAsInt()); // Integer division should truncate
    assertEquals("m / s", result.getUnits());
  }

  @Test
  void testRaiseToPower() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    IntScalar scalar1 = new IntScalar(caster, 2L, "m");
    IntScalar scalar2 = new IntScalar(caster, 3L, "");

    DecimalScalar result = (DecimalScalar) scalar1.raiseToPower(scalar2);
    assertEquals(new BigDecimal(8), result.getAsDecimal());
    assertEquals("m * m * m", result.getUnits());
  }

  @Test
  void testRaiseToPowerWithUnits() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    IntScalar scalar1 = new IntScalar(caster, 2L, "m");
    IntScalar scalar2 = new IntScalar(caster, 3L, "s");

    assertThrows(IllegalArgumentException.class, () -> scalar1.raiseToPower(scalar2));
  }

  // @Test
  // void testGetAsDistribution() {
  //   EngineValueCaster caster = new EngineValueWideningCaster();
  //   IntScalar scalar = new IntScalar(caster, 10L, "m");

  //   Distribution distribution = scalar.getAsDistribution();
  //   assertEquals(1, distribution.getValues().size());
  //   assertEquals(10L, distribution.getValues().get(0));
  //   assertEquals("m", distribution.getUnits());
  // }

  @Test
  void testNegativeNumbers() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    IntScalar scalar = new IntScalar(caster, -42L, "m");
    
    assertEquals(-42L, scalar.getAsInt());
    assertEquals(new BigDecimal(-42), scalar.getAsDecimal());
    assertEquals("-42", scalar.getAsString());
  }
  
  @Test
  void testMaxIntValue() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    long maxValue = Long.MAX_VALUE;
    IntScalar scalar = new IntScalar(caster, maxValue, "");
    
    assertEquals(maxValue, scalar.getAsInt());
    assertEquals(new BigDecimal(maxValue), scalar.getAsDecimal());
  }
  
  @Test
  void testMinIntValue() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    long minValue = Long.MIN_VALUE;
    IntScalar scalar = new IntScalar(caster, minValue, "");
    
    assertEquals(minValue, scalar.getAsInt());
    assertEquals(new BigDecimal(minValue), scalar.getAsDecimal());
  }
}
