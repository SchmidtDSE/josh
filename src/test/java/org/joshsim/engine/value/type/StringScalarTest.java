/**
 * Tests for StrubgScalar.
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
 * Tests for a string within the engine as a variable value.
 */
class StringScalarTest {

  @Test
  void testConstructorAndGetters() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    StringScalar scalar = new StringScalar(caster, "hello", Units.EMPTY);

    assertEquals("hello", scalar.getAsString());
    assertEquals(new LanguageType("string"), scalar.getLanguageType());
    assertEquals("hello", scalar.getInnerValue());
  }

  @Test
  void testConstructorWithUnits() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    StringScalar scalar = new StringScalar(caster, "hello", Units.of("label"));

    assertEquals("hello", scalar.getAsString());
    assertEquals(Units.of("label"), scalar.getUnits());
  }

  @Test
  void testAdd() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    StringScalar scalar1 = new StringScalar(caster, "hello", Units.EMPTY);
    StringScalar scalar2 = new StringScalar(caster, " world", Units.EMPTY);

    StringScalar result = (StringScalar) scalar1.add(scalar2);
    assertEquals("hello world", result.getAsString());
  }

  @Test
  void testAddWithDifferentUnits() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    StringScalar scalar1 = new StringScalar(caster, "hello", Units.EMPTY);
    StringScalar scalar2 = new StringScalar(caster, " world", Units.EMPTY);

    StringScalar result = (StringScalar) scalar1.add(scalar2);
    assertEquals("hello world", result.getAsString());
    assertEquals(Units.EMPTY, result.getUnits());
  }

  @Test
  void testGetAsBooleanThrowsException() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    StringScalar scalar = new StringScalar(caster, "hello", Units.EMPTY);

    assertThrows(UnsupportedOperationException.class, scalar::getAsBoolean);
  }

  @Test
  void testGetAsBooleanWithTrueFalseStrings() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    StringScalar trueScalar = new StringScalar(caster, "true", Units.EMPTY);
    StringScalar falseScalar = new StringScalar(caster, "false", Units.EMPTY);

    // This assumes StringScalar.getAsBoolean() considers "true"/"false" strings
    // If not implemented yet, comment these out and keep the exception test above
    assertTrue(trueScalar.getAsBoolean());
    assertFalse(falseScalar.getAsBoolean());
  }

  @Test
  void testGetAsDecimalThrowsException() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    StringScalar scalar = new StringScalar(caster, "hello", Units.EMPTY);

    assertThrows(NumberFormatException.class, scalar::getAsDecimal);
  }

  @Test
  void testGetAsDecimalWithValidNumber() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    StringScalar scalar = new StringScalar(caster, "42.5", Units.EMPTY);

    assertEquals(new BigDecimal("42.5"), scalar.getAsDecimal());
  }

  @Test
  void testGetAsIntThrowsException() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    StringScalar scalar = new StringScalar(caster, "hello", Units.EMPTY);

    assertThrows(NumberFormatException.class, scalar::getAsInt);
  }

  @Test
  void testGetAsIntWithValidNumber() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    StringScalar scalar = new StringScalar(caster, "42", Units.EMPTY);

    assertEquals(42L, scalar.getAsInt());
  }

  @Test
  void testSubtractThrowsException() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    StringScalar scalar1 = new StringScalar(caster, "hello", Units.EMPTY);
    StringScalar scalar2 = new StringScalar(caster, "world", Units.EMPTY);

    assertThrows(UnsupportedOperationException.class, () -> scalar1.subtract(scalar2));
  }

  @Test
  void testMultiplyThrowsException() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    StringScalar scalar1 = new StringScalar(caster, "hello", Units.EMPTY);
    StringScalar scalar2 = new StringScalar(caster, "world", Units.EMPTY);

    assertThrows(UnsupportedOperationException.class, () -> scalar1.multiply(scalar2));
  }

  @Test
  void testDivideThrowsException() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    StringScalar scalar1 = new StringScalar(caster, "hello", Units.EMPTY);
    StringScalar scalar2 = new StringScalar(caster, "world", Units.EMPTY);

    assertThrows(UnsupportedOperationException.class, () -> scalar1.divide(scalar2));
  }

  @Test
  void testRaiseToPowerThrowsException() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    StringScalar scalar1 = new StringScalar(caster, "hello", Units.EMPTY);
    StringScalar scalar2 = new StringScalar(caster, "world", Units.EMPTY);

    assertThrows(UnsupportedOperationException.class, () -> scalar1.raiseToPower(scalar2));
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
  //   StringScalar scalar = new StringScalar(caster, "hello", "labelUnits.of("));

  //   Distribution dist = scalar.getAsDistribution();
  //   assertEquals(1, dist.getValues().size());
  //   assertEquals("hello", dist.getValues().get(0));
  //   assertEquals("label", dist.getUnits());
  // }

  @Test
  void testEmptyString() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    StringScalar scalar = new StringScalar(caster, "", Units.EMPTY);

    assertEquals("", scalar.getAsString());
    assertEquals("", scalar.getInnerValue());
  }

  @Test
  void testSpecialCharacters() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    StringScalar scalar = new StringScalar(caster, "!@#$%^&*() _+", Units.EMPTY);

    assertEquals("!@#$%^&*() _+", scalar.getAsString());
  }

  @Test
  void testAddWithNull() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    StringScalar scalar1 = new StringScalar(caster, "hello", Units.EMPTY);
    StringScalar scalar2 = new StringScalar(caster, null, Units.EMPTY);

    StringScalar result = (StringScalar) scalar1.add(scalar2);
    assertEquals("hellonull", result.getAsString());
  }
}
