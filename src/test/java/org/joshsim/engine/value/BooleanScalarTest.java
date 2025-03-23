
/**
 * Tests for BooleanScalar.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.value;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/**
 * Tests for the BooleanScalar class which represents boolean values in the engine.
 */
class BooleanScalarTest {

  @Test
  void testConstructorAndGetters() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    BooleanScalar scalar = new BooleanScalar(caster, true, new Units(""));

    assertEquals(true, scalar.getAsBoolean());
    assertEquals(new LanguageType("boolean"), scalar.getLanguageType());
    assertEquals("true", scalar.getAsString());
    assertEquals(Boolean.TRUE, scalar.getInnerValue());
  }

  @Test
  void testGetAsInt() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    BooleanScalar trueScalar = new BooleanScalar(caster, true, new Units(""));
    BooleanScalar falseScalar = new BooleanScalar(caster, false, new Units(""));

    assertEquals(1L, trueScalar.getAsInt());
    assertEquals(0L, falseScalar.getAsInt());
  }

  @Test
  void testGetAsDecimal() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    BooleanScalar trueScalar = new BooleanScalar(caster, true, new Units(""));
    BooleanScalar falseScalar = new BooleanScalar(caster, false, new Units(""));

    assertEquals(new BigDecimal(1), trueScalar.getAsDecimal());
    assertEquals(new BigDecimal(0), falseScalar.getAsDecimal());
  }

  @Test
  void testGetAsString() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    BooleanScalar trueScalar = new BooleanScalar(caster, true, new Units(""));
    BooleanScalar falseScalar = new BooleanScalar(caster, false, new Units(""));

    assertEquals("true", trueScalar.getAsString());
    assertEquals("false", falseScalar.getAsString());
  }

  @Test
  void testAddThrowsException() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    BooleanScalar scalar1 = new BooleanScalar(caster, true, new Units(""));
    BooleanScalar scalar2 = new BooleanScalar(caster, false, new Units(""));

    assertThrows(UnsupportedOperationException.class, () -> scalar1.add(scalar2));
  }

  @Test
  void testSubtractThrowsException() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    BooleanScalar scalar1 = new BooleanScalar(caster, true, new Units(""));
    BooleanScalar scalar2 = new BooleanScalar(caster, false, new Units(""));

    assertThrows(UnsupportedOperationException.class, () -> scalar1.subtract(scalar2));
  }

  @Test
  void testMultiplyThrowsException() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    BooleanScalar scalar1 = new BooleanScalar(caster, true, new Units(""));
    BooleanScalar scalar2 = new BooleanScalar(caster, false, new Units(""));

    assertThrows(UnsupportedOperationException.class, () -> scalar1.multiply(scalar2));
  }

  @Test
  void testDivideThrowsException() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    BooleanScalar scalar1 = new BooleanScalar(caster, true, new Units(""));
    BooleanScalar scalar2 = new BooleanScalar(caster, false, new Units(""));

    assertThrows(UnsupportedOperationException.class, () -> scalar1.divide(scalar2));
  }

  @Test
  void testRaiseToPowerThrowsException() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    BooleanScalar scalar1 = new BooleanScalar(caster, true, new Units("count"));
    BooleanScalar scalar2 = new BooleanScalar(caster, false, new Units("count"));

    assertThrows(UnsupportedOperationException.class, () -> scalar1.raiseToPower(scalar2));
  }

  @Test
  void testGetAsEntityThrowsException() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    BooleanScalar scalar = new BooleanScalar(caster, true, new Units(""));

    assertThrows(UnsupportedOperationException.class, scalar::getAsEntity);
  }

  @Test
  void testWithDifferentUnits() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    BooleanScalar scalar = new BooleanScalar(caster, true, new Units("meters"));

    assertEquals(new Units("meters"), scalar.getUnits());
  }
}
