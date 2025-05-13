/**
 * Tests for DecimalScalar.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.value.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueCaster;
import org.joshsim.engine.value.engine.EngineValueWideningCaster;
import org.junit.jupiter.api.Test;


/**
 * Tests for the DecimalScalar class which represents decimal values in the engine.
 */
class DecimalScalarTest {

  @Test
  void testConstructorAndGetters() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    DecimalScalar scalar = new DecimalScalar(caster, new BigDecimal("42.5"), Units.of("m"));

    assertEquals(new BigDecimal("42.5"), scalar.getAsDecimal());
    assertEquals(new LanguageType("decimal"), scalar.getLanguageType());
    assertEquals("42.5", scalar.getAsString());
    assertEquals(new BigDecimal("42.5"), scalar.getInnerValue());
    assertEquals(Units.of("m"), scalar.getUnits());
  }

  @Test
  void testGetAsBoolean() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    DecimalScalar nonZeroScalar = new DecimalScalar(caster, new BigDecimal("42.5"), Units.EMPTY);
    DecimalScalar zeroScalar = new DecimalScalar(caster, BigDecimal.ZERO, Units.EMPTY);

    assertThrows(UnsupportedOperationException.class, () -> nonZeroScalar.getAsBoolean());
    assertThrows(UnsupportedOperationException.class, () -> zeroScalar.getAsBoolean());
  }

  @Test
  void testGetAsInt() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    DecimalScalar scalar = new DecimalScalar(caster, new BigDecimal("42.5"), Units.EMPTY);

    assertEquals(42L, scalar.getAsInt());
  }

  @Test
  void testAdd() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    DecimalScalar scalar1 = new DecimalScalar(caster, new BigDecimal("10.5"), Units.of("m"));
    DecimalScalar scalar2 = new DecimalScalar(caster, new BigDecimal("5.3"), Units.of("m"));

    DecimalScalar result = (DecimalScalar) scalar1.add(scalar2);
    assertEquals(new BigDecimal("15.8"), result.getAsDecimal());
    assertEquals(Units.of("m"), result.getUnits());
  }

  @Test
  void testAddWithDifferentUnits() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    DecimalScalar scalar1 = new DecimalScalar(caster, new BigDecimal("10.5"), Units.of("m"));
    DecimalScalar scalar2 = new DecimalScalar(caster, new BigDecimal("5.3"), Units.of("s"));

    assertThrows(IllegalArgumentException.class, () -> scalar1.add(scalar2));
  }

  @Test
  void testSubtract() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    DecimalScalar scalar1 = new DecimalScalar(caster, new BigDecimal("10.5"), Units.of("m"));
    DecimalScalar scalar2 = new DecimalScalar(caster, new BigDecimal("5.3"), Units.of("m"));

    DecimalScalar result = (DecimalScalar) scalar1.subtract(scalar2);
    assertEquals(new BigDecimal("5.2"), result.getAsDecimal());
    assertEquals(Units.of("m"), result.getUnits());
  }

  @Test
  void testSubtractWithDifferentUnits() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    DecimalScalar scalar1 = new DecimalScalar(caster, new BigDecimal("10.5"), Units.of("m"));
    DecimalScalar scalar2 = new DecimalScalar(caster, new BigDecimal("5.3"), Units.of("s"));

    assertThrows(IllegalArgumentException.class, () -> scalar1.subtract(scalar2));
  }

  @Test
  void testMultiply() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    DecimalScalar scalar1 = new DecimalScalar(caster, new BigDecimal("10.5"), Units.of("m"));
    DecimalScalar scalar2 = new DecimalScalar(caster, new BigDecimal("2.0"), Units.of("s"));

    DecimalScalar result = (DecimalScalar) scalar1.multiply(scalar2);
    assertEquals(new BigDecimal("21.00"), result.getAsDecimal());
    assertEquals(Units.of("m * s"), result.getUnits());
  }

  @Test
  void testDivide() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    DecimalScalar scalar1 = new DecimalScalar(caster, new BigDecimal("10.5"), Units.of("m"));
    DecimalScalar scalar2 = new DecimalScalar(caster, new BigDecimal("2.0"), Units.of("s"));

    DecimalScalar result = (DecimalScalar) scalar1.divide(scalar2);
    assertEquals(new BigDecimal("5.25"), result.getAsDecimal());
    assertEquals(Units.of("m / s"), result.getUnits());
  }

  @Test
  void testDivideByZero() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    DecimalScalar scalar1 = new DecimalScalar(caster, new BigDecimal("10.5"), Units.of("m"));
    DecimalScalar zeroScalar = new DecimalScalar(caster, BigDecimal.ZERO, Units.of("s"));

    assertThrows(ArithmeticException.class, () -> scalar1.divide(zeroScalar));
  }

  @Test
  void testRaiseToPower() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    DecimalScalar scalar1 = new DecimalScalar(caster, new BigDecimal("2.0"), Units.of("m"));
    DecimalScalar scalar2 = new DecimalScalar(caster, new BigDecimal("3.0"), Units.EMPTY);

    DecimalScalar result = (DecimalScalar) scalar1.raiseToPower(scalar2);
    assertEquals(new BigDecimal("8.0"), result.getAsDecimal());
    assertEquals(Units.of("m * m * m"), result.getUnits());
  }

  @Test
  void testRaiseToPowerWithUnits() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    DecimalScalar scalar1 = new DecimalScalar(caster, new BigDecimal("2.0"), Units.of("m"));
    DecimalScalar scalar2 = new DecimalScalar(caster, new BigDecimal("3.0"), Units.of("s"));

    assertThrows(IllegalArgumentException.class, () -> scalar1.raiseToPower(scalar2));
  }

  @Test
  void testRaiseToPowerWithUncoercableDecimalUnits() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    DecimalScalar scalar1 = new DecimalScalar(caster, new BigDecimal("2.0"), Units.of("m"));
    DecimalScalar scalar2 = new DecimalScalar(caster, new BigDecimal("3.5"), Units.EMPTY);

    assertThrows(UnsupportedOperationException.class, () -> scalar1.raiseToPower(scalar2));
  }

  @Test
  void testRaiseToPowerWithUncoercableDecimalNoUnits() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    DecimalScalar scalar1 = new DecimalScalar(caster, new BigDecimal("2.0"), Units.EMPTY);
    DecimalScalar scalar2 = new DecimalScalar(caster, new BigDecimal("3.5"), Units.EMPTY);

    double result = scalar1.raiseToPower(scalar2).getAsDecimal().doubleValue();
    double expected = Math.pow(2.0, 3.5);
    assertTrue(Math.abs(result - expected) < 0.000001);
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
  //   DecimalScalar scalar = new DecimalScalar(caster, new BigDecimal("42.5"), Units.of("kg"));

  //   Distribution dist = scalar.getAsDistribution();
  //   assertEquals(1, dist.getValues().size());
  //   assertEquals(new BigDecimal("42.5"), dist.getValues().get(0));
  //   assertEquals("kg", dist.getUnits());
  // }


  @Test
  void testScaleAndPrecision() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    DecimalScalar scalar = new DecimalScalar(caster, new BigDecimal("123.456789"), Units.EMPTY);

    assertEquals(new BigDecimal("123.456789"), scalar.getAsDecimal());
    assertEquals("123.456789", scalar.getAsString());
  }


  @Test
  void testCompareToSameUnitsDifferentValues() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    Scalar scalar1 = new DecimalScalar(caster, new BigDecimal(10), Units.of("m"));
    Scalar scalar2 = new DecimalScalar(caster, new BigDecimal(20), Units.of("m"));

    assertTrue(scalar1.compareTo(scalar2) < 0);
    assertTrue(scalar2.compareTo(scalar1) > 0);
  }

  @Test
  void testCompareToIncompatibleUnits() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    Scalar scalar1 = new DecimalScalar(caster, new BigDecimal(10), Units.of("m"));
    Scalar scalar2 = new DecimalScalar(caster, new BigDecimal(10), Units.of("s"));

    assertThrows(IllegalArgumentException.class, () -> scalar1.compareTo(scalar2));
  }

  @Test
  void testCompareToEqualValues() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    Scalar scalar1 = new DecimalScalar(caster, new BigDecimal(10), Units.of("m"));
    Scalar scalar2 = new DecimalScalar(caster, new BigDecimal(10), Units.of("m"));

    assertEquals(0, scalar1.compareTo(scalar2));
  }

  @Test
  void testCompareToDifferentValuesSameOrder() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    Scalar scalar1 = new DecimalScalar(caster, new BigDecimal(15), Units.of("s"));
    Scalar scalar2 = new DecimalScalar(caster, new BigDecimal(5), Units.of("s"));

    assertTrue(scalar1.compareTo(scalar2) > 0);
    assertTrue(scalar2.compareTo(scalar1) < 0);
  }

}
