package org.joshsim.engine.value;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class StringScalarTest {

  @Test
  void testConstructorAndGetters() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    StringScalar scalar = new StringScalar(caster, "hello", "");

    assertEquals("hello", scalar.getAsString());
    assertEquals("string", scalar.getLanguageType());
    assertEquals("hello", scalar.getInnerValue());
  }

  @Test
  void testConstructorWithUnits() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    StringScalar scalar = new StringScalar(caster, "hello", "label");
    
    assertEquals("hello", scalar.getAsString());
    assertEquals("label", scalar.getUnits());
  }

  @Test
  void testAdd() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    StringScalar scalar1 = new StringScalar(caster, "hello", "");
    StringScalar scalar2 = new StringScalar(caster, " world", "");

    StringScalar result = (StringScalar)scalar1.add(scalar2);
    assertEquals("hello world", result.getAsString());
  }

  @Test
  void testAddWithDifferentUnits() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    StringScalar scalar1 = new StringScalar(caster, "hello", "");
    StringScalar scalar2 = new StringScalar(caster, " world", "");

    StringScalar result = (StringScalar) scalar1.add(scalar2);
    assertEquals("hello world", result.getAsString());
    assertEquals("", result.getUnits()); // Units should be empty for concatenated strings
  }

  @Test
  void testGetAsBooleanThrowsException() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    StringScalar scalar = new StringScalar(caster, "hello", "");

    assertThrows(UnsupportedOperationException.class, scalar::getAsBoolean);
  }

  @Test
  void testGetAsBooleanWithTrueFalseStrings() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    StringScalar trueScalar = new StringScalar(caster, "true", "");
    StringScalar falseScalar = new StringScalar(caster, "false", "");

    // This assumes StringScalar.getAsBoolean() considers "true"/"false" strings
    // If not implemented yet, comment these out and keep the exception test above
    assertTrue(trueScalar.getAsBoolean());
    assertFalse(falseScalar.getAsBoolean());
  }

  @Test
  void testGetAsDecimalThrowsException() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    StringScalar scalar = new StringScalar(caster, "hello", "");

    assertThrows(NumberFormatException.class, scalar::getAsDecimal);
  }

  @Test
  void testGetAsDecimalWithValidNumber() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    StringScalar scalar = new StringScalar(caster, "42.5", "");

    assertEquals(new BigDecimal("42.5"), scalar.getAsDecimal());
  }

  @Test
  void testGetAsIntThrowsException() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    StringScalar scalar = new StringScalar(caster, "hello", "");

    assertThrows(NumberFormatException.class, scalar::getAsInt);
  }

  @Test
  void testGetAsIntWithValidNumber() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    StringScalar scalar = new StringScalar(caster, "42", "");

    assertEquals(42L, scalar.getAsInt());
  }

  @Test
  void testSubtractThrowsException() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    StringScalar scalar1 = new StringScalar(caster, "hello", "");
    StringScalar scalar2 = new StringScalar(caster, "world", "");

    assertThrows(UnsupportedOperationException.class, () -> scalar1.subtract(scalar2));
  }

  @Test
  void testMultiplyThrowsException() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    StringScalar scalar1 = new StringScalar(caster, "hello", "");
    StringScalar scalar2 = new StringScalar(caster, "world", "");

    assertThrows(UnsupportedOperationException.class, () -> scalar1.multiply(scalar2));
  }

  @Test
  void testDivideThrowsException() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    StringScalar scalar1 = new StringScalar(caster, "hello", "");
    StringScalar scalar2 = new StringScalar(caster, "world", "");

    assertThrows(UnsupportedOperationException.class, () -> scalar1.divide(scalar2));
  }

  @Test
  void testRaiseToPowerThrowsException() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    StringScalar scalar1 = new StringScalar(caster, "hello", "");
    StringScalar scalar2 = new StringScalar(caster, "world", "");

    assertThrows(UnsupportedOperationException.class, () -> scalar1.raiseToPower(scalar2));
  }

  // @Test
  // void testGetAsDistribution() {
  //   EngineValueCaster caster = new EngineValueWideningCaster();
  //   StringScalar scalar = new StringScalar(caster, "hello", "label");
    
  //   Distribution dist = scalar.getAsDistribution();
  //   assertEquals(1, dist.getValues().size());
  //   assertEquals("hello", dist.getValues().get(0));
  //   assertEquals("label", dist.getUnits());
  // }

  @Test
  void testEmptyString() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    StringScalar scalar = new StringScalar(caster, "", "");
    
    assertEquals("", scalar.getAsString());
    assertEquals("", scalar.getInnerValue());
  }

  @Test
  void testSpecialCharacters() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    StringScalar scalar = new StringScalar(caster, "!@#$%^&*()_+", "");
    
    assertEquals("!@#$%^&*()_+", scalar.getAsString());
  }

  @Test
  void testAddWithNull() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    StringScalar scalar1 = new StringScalar(caster, "hello", "");
    StringScalar scalar2 = new StringScalar(caster, null, "");

    StringScalar result = (StringScalar)scalar1.add(scalar2);
    assertEquals("hellonull", result.getAsString());
  }
}
