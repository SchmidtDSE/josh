/**
 * Tests for DecimalScalar.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.value;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueCaster;
import org.joshsim.engine.value.engine.EngineValueWideningCaster;
import org.joshsim.engine.value.type.BooleanScalar;
import org.joshsim.engine.value.type.DecimalScalar;
import org.joshsim.engine.value.type.LanguageType;
import org.junit.jupiter.api.Test;


/**
 * Tests for the DecimalScalar class which represents decimal values in the engine.
 */
class DecimalScalarTest {

  @Test
  void testConstructorAndGetters() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    DecimalScalar scalar = new DecimalScalar(caster, new BigDecimal("42.5"), new Units("m"));

    assertEquals(new BigDecimal("42.5"), scalar.getAsDecimal());
    assertEquals(new LanguageType("decimal"), scalar.getLanguageType());
    assertEquals("42.5", scalar.getAsString());
    assertEquals(new BigDecimal("42.5"), scalar.getInnerValue());
    assertEquals(new Units("m"), scalar.getUnits());
  }

  @Test
  void testGetAsBoolean() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    DecimalScalar nonZeroScalar = new DecimalScalar(caster, new BigDecimal("42.5"), new Units(""));
    DecimalScalar zeroScalar = new DecimalScalar(caster, BigDecimal.ZERO, new Units(""));

    assertThrows(UnsupportedOperationException.class, () -> nonZeroScalar.getAsBoolean());
    assertThrows(UnsupportedOperationException.class, () -> zeroScalar.getAsBoolean());
  }

  @Test
  void testGetAsInt() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    DecimalScalar scalar = new DecimalScalar(caster, new BigDecimal("42.5"), new Units(""));

    assertEquals(42L, scalar.getAsInt());
  }

  @Test
  void testAdd() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    DecimalScalar scalar1 = new DecimalScalar(caster, new BigDecimal("10.5"), new Units("m"));
    DecimalScalar scalar2 = new DecimalScalar(caster, new BigDecimal("5.3"), new Units("m"));

    DecimalScalar result = (DecimalScalar) scalar1.add(scalar2);
    assertEquals(new BigDecimal("15.8"), result.getAsDecimal());
    assertEquals(new Units("m"), result.getUnits());
  }

  @Test
  void testAddWithDifferentUnits() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    DecimalScalar scalar1 = new DecimalScalar(caster, new BigDecimal("10.5"), new Units("m"));
    DecimalScalar scalar2 = new DecimalScalar(caster, new BigDecimal("5.3"), new Units("s"));

    assertThrows(IllegalArgumentException.class, () -> scalar1.add(scalar2));
  }

  @Test
  void testSubtract() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    DecimalScalar scalar1 = new DecimalScalar(caster, new BigDecimal("10.5"), new Units("m"));
    DecimalScalar scalar2 = new DecimalScalar(caster, new BigDecimal("5.3"), new Units("m"));

    DecimalScalar result = (DecimalScalar) scalar1.subtract(scalar2);
    assertEquals(new BigDecimal("5.2"), result.getAsDecimal());
    assertEquals(new Units("m"), result.getUnits());
  }

  @Test
  void testSubtractWithDifferentUnits() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    DecimalScalar scalar1 = new DecimalScalar(caster, new BigDecimal("10.5"), new Units("m"));
    DecimalScalar scalar2 = new DecimalScalar(caster, new BigDecimal("5.3"), new Units("s"));

    assertThrows(IllegalArgumentException.class, () -> scalar1.subtract(scalar2));
  }

  @Test
  void testMultiply() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    DecimalScalar scalar1 = new DecimalScalar(caster, new BigDecimal("10.5"), new Units("m"));
    DecimalScalar scalar2 = new DecimalScalar(caster, new BigDecimal("2.0"), new Units("s"));

    DecimalScalar result = (DecimalScalar) scalar1.multiply(scalar2);
    assertEquals(new BigDecimal("21.00"), result.getAsDecimal());
    assertEquals(new Units("m * s"), result.getUnits());
  }

  @Test
  void testDivide() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    DecimalScalar scalar1 = new DecimalScalar(caster, new BigDecimal("10.5"), new Units("m"));
    DecimalScalar scalar2 = new DecimalScalar(caster, new BigDecimal("2.0"), new Units("s"));

    DecimalScalar result = (DecimalScalar) scalar1.divide(scalar2);
    assertEquals(new BigDecimal("5.25"), result.getAsDecimal());
    assertEquals(new Units("m / s"), result.getUnits());
  }

  @Test
  void testDivideByZero() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    DecimalScalar scalar1 = new DecimalScalar(caster, new BigDecimal("10.5"), new Units("m"));
    DecimalScalar zeroScalar = new DecimalScalar(caster, BigDecimal.ZERO, new Units("s"));

    assertThrows(ArithmeticException.class, () -> scalar1.divide(zeroScalar));
  }

  @Test
  void testRaiseToPower() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    DecimalScalar scalar1 = new DecimalScalar(caster, new BigDecimal("2.0"), new Units("m"));
    DecimalScalar scalar2 = new DecimalScalar(caster, new BigDecimal("3.0"), new Units(""));

    DecimalScalar result = (DecimalScalar) scalar1.raiseToPower(scalar2);
    assertEquals(new BigDecimal("8.0"), result.getAsDecimal());
    assertEquals(new Units("m * m * m"), result.getUnits());
  }

  @Test
  void testRaiseToPowerWithUnits() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    DecimalScalar scalar1 = new DecimalScalar(caster, new BigDecimal("2.0"), new Units("m"));
    DecimalScalar scalar2 = new DecimalScalar(caster, new BigDecimal("3.0"), new Units("s"));

    assertThrows(IllegalArgumentException.class, () -> scalar1.raiseToPower(scalar2));
  }

  @Test
  void testRaiseToPowerWithUncoercableDecimal() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    DecimalScalar scalar1 = new DecimalScalar(caster, new BigDecimal("2.0"), new Units(""));
    DecimalScalar scalar2 = new DecimalScalar(caster, new BigDecimal("3.5"), new Units(""));

    assertThrows(UnsupportedOperationException.class, () -> scalar1.raiseToPower(scalar2));
  }

  @Test
  void testGetAsEntityThrowsException() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    BooleanScalar scalar = new BooleanScalar(caster, true, new Units(""));

    assertThrows(UnsupportedOperationException.class, scalar::getAsEntity);
  }

  // @Test
  // void testGetAsDistribution() {
  //   EngineValueCaster caster = new EngineValueWideningCaster();
  //   DecimalScalar scalar = new DecimalScalar(caster, new BigDecimal("42.5"), new Units("kg"));

  //   Distribution dist = scalar.getAsDistribution();
  //   assertEquals(1, dist.getValues().size());
  //   assertEquals(new BigDecimal("42.5"), dist.getValues().get(0));
  //   assertEquals("kg", dist.getUnits());
  // }


  @Test
  void testScaleAndPrecision() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    DecimalScalar scalar = new DecimalScalar(caster, new BigDecimal("123.456789"), new Units(""));

    assertEquals(new BigDecimal("123.456789"), scalar.getAsDecimal());
    assertEquals("123.456789", scalar.getAsString());
  }
}
