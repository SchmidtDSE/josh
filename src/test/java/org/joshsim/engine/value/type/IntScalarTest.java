/**
 * Tests for IntScalar.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.value.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueCaster;
import org.joshsim.engine.value.engine.EngineValueWideningCaster;
import org.junit.jupiter.api.Test;


/**
 * Tests for an integer within the engine as a variable value.
 */
class IntScalarTest {

  @Test
  void testConstructorAndGetters() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    IntScalar scalar = new IntScalar(caster, 10L, new Units("m"));

    assertEquals(10L, scalar.getAsInt());
    assertEquals(new Units("m"), scalar.getUnits());
    assertEquals(new BigDecimal(10), scalar.getAsDecimal());
    assertEquals("10", scalar.getAsString());
    assertEquals(new LanguageType("int"), scalar.getLanguageType());
    assertEquals(10L, scalar.getInnerValue());
  }

  @Test
  void testGetAsBoolean() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    IntScalar nonZeroScalar = new IntScalar(caster, 10L, Units.EMPTY);
    IntScalar zeroScalar = new IntScalar(caster, 0L, Units.EMPTY);
    IntScalar oneScalar = new IntScalar(caster, 1L, Units.EMPTY);

    assertThrows(UnsupportedOperationException.class, () -> nonZeroScalar.getAsBoolean());
    assertFalse(zeroScalar.getAsBoolean());
    assertTrue(oneScalar.getAsBoolean());
  }

  @Test
  void testAdd() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    IntScalar scalar1 = new IntScalar(caster, 10L, new Units("m"));
    IntScalar scalar2 = new IntScalar(caster, 5L, new Units("m"));

    IntScalar result = (IntScalar) scalar1.add(scalar2);
    assertEquals(15L, result.getAsInt());
    assertEquals(new Units("m"), result.getUnits());
  }

  @Test
  void testAddWithDifferentUnits() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    IntScalar scalar1 = new IntScalar(caster, 10L, new Units("m"));
    IntScalar scalar2 = new IntScalar(caster, 5L, new Units("s"));

    assertThrows(IllegalArgumentException.class, () -> scalar1.add(scalar2));
  }

  @Test
  void testSubtract() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    IntScalar scalar1 = new IntScalar(caster, 10L, new Units("m"));
    IntScalar scalar2 = new IntScalar(caster, 5L, new Units("m"));

    IntScalar result = (IntScalar) scalar1.subtract(scalar2);
    assertEquals(5L, result.getAsInt());
    assertEquals(new Units("m"), result.getUnits());
  }

  @Test
  void testSubtractWithDifferentUnits() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    IntScalar scalar1 = new IntScalar(caster, 10L, new Units("m"));
    IntScalar scalar2 = new IntScalar(caster, 5L, new Units("s"));

    assertThrows(IllegalArgumentException.class, () -> scalar1.subtract(scalar2));
  }

  @Test
  void testMultiply() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    IntScalar scalar1 = new IntScalar(caster, 10L, new Units("m"));
    IntScalar scalar2 = new IntScalar(caster, 2L, new Units("s"));

    IntScalar result = (IntScalar) scalar1.multiply(scalar2);
    assertEquals(20L, result.getAsInt());
    assertEquals(new Units("m * s"), result.getUnits());
  }

  @Test
  void testDivide() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    IntScalar scalar1 = new IntScalar(caster, 10L, new Units("m"));
    IntScalar scalar2 = new IntScalar(caster, 2L, new Units("s"));

    IntScalar result = (IntScalar) scalar1.divide(scalar2);
    assertEquals(5L, result.getAsInt());
    assertEquals(new Units("m / s"), result.getUnits());
  }

  @Test
  void testDivideByZero() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    IntScalar scalar1 = new IntScalar(caster, 10L, new Units("m"));
    IntScalar zeroScalar = new IntScalar(caster, 0L, new Units("s"));

    assertThrows(ArithmeticException.class, () -> scalar1.divide(zeroScalar));
  }

  @Test
  void testDivisionRounding() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    IntScalar scalar1 = new IntScalar(caster, 7L, new Units("m"));
    IntScalar scalar2 = new IntScalar(caster, 2L, new Units("s"));

    IntScalar result = (IntScalar) scalar1.divide(scalar2);
    assertEquals(3L, result.getAsInt()); // Integer division should truncate
    assertEquals(new Units("m / s"), result.getUnits());
  }

  @Test
  void testRaiseToPower() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    IntScalar scalar1 = new IntScalar(caster, 2L, new Units("m"));
    IntScalar scalar2 = new IntScalar(caster, 3L, Units.EMPTY);

    DecimalScalar result = (DecimalScalar) scalar1.raiseToPower(scalar2);
    assertEquals(new BigDecimal(8), result.getAsDecimal());
    assertEquals(new Units("m * m * m"), result.getUnits());
  }

  @Test
  void testRaiseToPowerWithUnits() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    IntScalar scalar1 = new IntScalar(caster, 2L, new Units("m"));
    IntScalar scalar2 = new IntScalar(caster, 3L, new Units("s"));

    assertThrows(IllegalArgumentException.class, () -> scalar1.raiseToPower(scalar2));
  }

  @Test
  void testGetAsEntityThrowsException() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    BooleanScalar scalar = new BooleanScalar(caster, true, Units.EMPTY);

    assertThrows(UnsupportedOperationException.class, scalar::getAsEntity);
  }

  // @Test
  // void testGetAsDistribution() {
  //   EngineValueCaster caster = new EngineValueWideningCaster();
  //   IntScalar scalar = new IntScalar(caster, 10L, new Units("m"));

  //   Distribution distribution = scalar.getAsDistribution();
  //   assertEquals(1, distribution.getValues().size());
  //   assertEquals(10L, distribution.getValues().get(0));
  //   assertEquals(new Units("m"), distribution.getUnits());
  // }

  @Test
  void testNegativeNumbers() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    IntScalar scalar = new IntScalar(caster, -42L, new Units("m"));

    assertEquals(-42L, scalar.getAsInt());
    assertEquals(new BigDecimal(-42), scalar.getAsDecimal());
    assertEquals("-42", scalar.getAsString());
  }

  @Test
  void testMaxIntValue() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    long maxValue = Long.MAX_VALUE;
    IntScalar scalar = new IntScalar(caster, maxValue, Units.EMPTY);

    assertEquals(maxValue, scalar.getAsInt());
    assertEquals(new BigDecimal(maxValue), scalar.getAsDecimal());
  }

  @Test
  void testMinIntValue() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    long minValue = Long.MIN_VALUE;
    IntScalar scalar = new IntScalar(caster, minValue, Units.EMPTY);

    assertEquals(minValue, scalar.getAsInt());
    assertEquals(new BigDecimal(minValue), scalar.getAsDecimal());
  }

  @Test
  void testCompareToSameUnitsDifferentValues() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    IntScalar scalar1 = new IntScalar(caster, 10L, new Units("m"));
    IntScalar scalar2 = new IntScalar(caster, 20L, new Units("m"));

    assertTrue(scalar1.compareTo(scalar2) < 0);
    assertTrue(scalar2.compareTo(scalar1) > 0);
  }

  @Test
  void testCompareToIncompatibleUnits() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    IntScalar scalar1 = new IntScalar(caster, 10L, new Units("m"));
    IntScalar scalar2 = new IntScalar(caster, 10L, new Units("s"));

    assertThrows(IllegalArgumentException.class, () -> scalar1.compareTo(scalar2));
  }

  @Test
  void testCompareToEqualValues() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    IntScalar scalar1 = new IntScalar(caster, 10L, new Units("m"));
    IntScalar scalar2 = new IntScalar(caster, 10L, new Units("m"));

    assertEquals(0, scalar1.compareTo(scalar2));
  }

  @Test
  void testCompareToDifferentValuesSameOrder() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    IntScalar scalar1 = new IntScalar(caster, 15L, new Units("s"));
    IntScalar scalar2 = new IntScalar(caster, 5L, new Units("s"));

    assertTrue(scalar1.compareTo(scalar2) > 0);
    assertTrue(scalar2.compareTo(scalar1) < 0);
  }
}
