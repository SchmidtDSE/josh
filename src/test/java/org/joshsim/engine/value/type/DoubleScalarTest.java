
/**
 * Tests for DoubleScalar.
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
 * Tests for the DoubleScalar class which represents double values in the engine.
 */
class DoubleScalarTest {

  @Test
  void testConstructorAndGetters() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    DoubleScalar scalar = new DoubleScalar(caster, 42.5, Units.of("m"));

    assertEquals(new BigDecimal("42.5"), scalar.getAsDecimal());
    assertEquals(new LanguageType("decimal"), scalar.getLanguageType());
    assertEquals("42.5", scalar.getAsString());
    assertEquals(42.5, scalar.getInnerValue());
    assertEquals(Units.of("m"), scalar.getUnits());
  }

  @Test
  void testGetAsBoolean() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    DoubleScalar nonZeroScalar = new DoubleScalar(caster, 42.5, Units.EMPTY);
    DoubleScalar zeroScalar = new DoubleScalar(caster, 0.0, Units.EMPTY);

    assertThrows(UnsupportedOperationException.class, () -> nonZeroScalar.getAsBoolean());
    assertThrows(UnsupportedOperationException.class, () -> zeroScalar.getAsBoolean());
  }

  @Test
  void testGetAsInt() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    DoubleScalar scalar = new DoubleScalar(caster, 42.5, Units.EMPTY);

    assertEquals(42L, scalar.getAsInt());
  }

  @Test
  void testAdd() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    DoubleScalar scalar1 = new DoubleScalar(caster, 10.5, Units.of("m"));
    DoubleScalar scalar2 = new DoubleScalar(caster, 5.3, Units.of("m"));

    DoubleScalar result = (DoubleScalar) scalar1.add(scalar2);
    assertEquals(15.8, result.getAsDouble(), 0.0001);
    assertEquals(Units.of("m"), result.getUnits());
  }

  @Test
  void testAddWithDifferentUnits() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    DoubleScalar scalar1 = new DoubleScalar(caster, 10.5, Units.of("m"));
    DoubleScalar scalar2 = new DoubleScalar(caster, 5.3, Units.of("s"));

    assertThrows(IllegalArgumentException.class, () -> scalar1.add(scalar2));
  }

  @Test
  void testSubtract() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    DoubleScalar scalar1 = new DoubleScalar(caster, 10.5, Units.of("m"));
    DoubleScalar scalar2 = new DoubleScalar(caster, 5.3, Units.of("m"));

    DoubleScalar result = (DoubleScalar) scalar1.subtract(scalar2);
    assertEquals(5.2, result.getAsDouble(), 0.0001);
    assertEquals(Units.of("m"), result.getUnits());
  }

  @Test
  void testSubtractWithDifferentUnits() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    DoubleScalar scalar1 = new DoubleScalar(caster, 10.5, Units.of("m"));
    DoubleScalar scalar2 = new DoubleScalar(caster, 5.3, Units.of("s"));

    assertThrows(IllegalArgumentException.class, () -> scalar1.subtract(scalar2));
  }

  @Test
  void testMultiply() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    DoubleScalar scalar1 = new DoubleScalar(caster, 10.5, Units.of("m"));
    DoubleScalar scalar2 = new DoubleScalar(caster, 2.0, Units.of("s"));

    DoubleScalar result = (DoubleScalar) scalar1.multiply(scalar2);
    assertEquals(21.0, result.getAsDouble(), 0.0001);
    assertEquals(Units.of("m * s"), result.getUnits());
  }

  @Test
  void testDivide() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    DoubleScalar scalar1 = new DoubleScalar(caster, 10.5, Units.of("m"));
    DoubleScalar scalar2 = new DoubleScalar(caster, 2.0, Units.of("s"));

    DoubleScalar result = (DoubleScalar) scalar1.divide(scalar2);
    assertEquals(5.25, result.getAsDouble(), 0.0001);
    assertEquals(Units.of("m / s"), result.getUnits());
  }

  @Test
  void testRaiseToPower() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    DoubleScalar scalar1 = new DoubleScalar(caster, 2.0, Units.of("m"));
    DoubleScalar scalar2 = new DoubleScalar(caster, 3.0, Units.EMPTY);

    DoubleScalar result = (DoubleScalar) scalar1.raiseToPower(scalar2);
    assertEquals(8.0, result.getAsDouble(), 0.0001);
    assertEquals(Units.of("m * m * m"), result.getUnits());
  }

  @Test
  void testRaiseToPowerWithUnits() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    DoubleScalar scalar1 = new DoubleScalar(caster, 2.0, Units.of("m"));
    DoubleScalar scalar2 = new DoubleScalar(caster, 3.0, Units.of("s"));

    assertThrows(IllegalArgumentException.class, () -> scalar1.raiseToPower(scalar2));
  }

  @Test
  void testCompareToSameUnitsDifferentValues() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    Scalar scalar1 = new DoubleScalar(caster, 10.0, Units.of("m"));
    Scalar scalar2 = new DoubleScalar(caster, 20.0, Units.of("m"));

    assertTrue(scalar1.compareTo(scalar2) < 0);
    assertTrue(scalar2.compareTo(scalar1) > 0);
  }

  @Test
  void testCompareToIncompatibleUnits() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    Scalar scalar1 = new DoubleScalar(caster, 10.0, Units.of("m"));
    Scalar scalar2 = new DoubleScalar(caster, 10.0, Units.of("s"));

    assertThrows(IllegalArgumentException.class, () -> scalar1.compareTo(scalar2));
  }

  @Test
  void testGetAsEntityThrowsException() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    DoubleScalar scalar = new DoubleScalar(caster, 42.5, Units.EMPTY);

    assertThrows(UnsupportedOperationException.class, scalar::getAsEntity);
  }

  @Test
  void testScaleAndPrecision() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    DoubleScalar scalar = new DoubleScalar(caster, 123.456789, Units.EMPTY);

    assertEquals(123.456789, scalar.getAsDouble(), 0.000001);
    assertEquals("123.456789", scalar.getAsString());
  }
}
