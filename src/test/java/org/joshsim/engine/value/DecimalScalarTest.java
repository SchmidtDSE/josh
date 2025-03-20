package org.joshsim.engine.value;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class DecimalScalarTest {

  @Test
  void testConstructorAndGetters() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    DecimalScalar scalar = new DecimalScalar(caster, new BigDecimal("42.5"), "m");

    assertEquals(new BigDecimal("42.5"), scalar.getAsDecimal());
    assertEquals("decimal", scalar.getLanguageType());
    assertEquals("42.5", scalar.getAsString());
    assertEquals(new BigDecimal("42.5"), scalar.getInnerValue());
    assertEquals("m", scalar.getUnits());
  }

  @Test
  void testGetAsBoolean() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    DecimalScalar nonZeroScalar = new DecimalScalar(caster, new BigDecimal("42.5"), "");
    DecimalScalar zeroScalar = new DecimalScalar(caster, BigDecimal.ZERO, "");

    assertThrows(UnsupportedOperationException.class, () -> nonZeroScalar.getAsBoolean());
    assertThrows(UnsupportedOperationException.class, () -> zeroScalar.getAsBoolean());
  }

  @Test
  void testGetAsInt() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    DecimalScalar scalar = new DecimalScalar(caster, new BigDecimal("42.5"), "");
    
    assertEquals(42L, scalar.getAsInt());
  }

  @Test
  void testAdd() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    DecimalScalar scalar1 = new DecimalScalar(caster, new BigDecimal("10.5"), "m");
    DecimalScalar scalar2 = new DecimalScalar(caster, new BigDecimal("5.3"), "m");

    DecimalScalar result = (DecimalScalar) scalar1.add(scalar2);
    assertEquals(new BigDecimal("15.8"), result.getAsDecimal());
    assertEquals("m", result.getUnits());
  }

  @Test
  void testAddWithDifferentUnits() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    DecimalScalar scalar1 = new DecimalScalar(caster, new BigDecimal("10.5"), "m");
    DecimalScalar scalar2 = new DecimalScalar(caster, new BigDecimal("5.3"), "s");

    assertThrows(IllegalArgumentException.class, () -> scalar1.add(scalar2));
  }

  @Test
  void testSubtract() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    DecimalScalar scalar1 = new DecimalScalar(caster, new BigDecimal("10.5"), "m");
    DecimalScalar scalar2 = new DecimalScalar(caster, new BigDecimal("5.3"), "m");

    DecimalScalar result = (DecimalScalar) scalar1.subtract(scalar2);
    assertEquals(new BigDecimal("5.2"), result.getAsDecimal());
    assertEquals("m", result.getUnits());
  }

  @Test
  void testSubtractWithDifferentUnits() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    DecimalScalar scalar1 = new DecimalScalar(caster, new BigDecimal("10.5"), "m");
    DecimalScalar scalar2 = new DecimalScalar(caster, new BigDecimal("5.3"), "s");

    assertThrows(IllegalArgumentException.class, () -> scalar1.subtract(scalar2));
  }

  @Test
  void testMultiply() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    DecimalScalar scalar1 = new DecimalScalar(caster, new BigDecimal("10.5"), "m");
    DecimalScalar scalar2 = new DecimalScalar(caster, new BigDecimal("2.0"), "s");

    DecimalScalar result = (DecimalScalar)scalar1.multiply(scalar2);
    assertEquals(new BigDecimal("21.00"), result.getAsDecimal());
    assertEquals("m*s", result.getUnits());
  }

  @Test
  void testDivide() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    DecimalScalar scalar1 = new DecimalScalar(caster, new BigDecimal("10.5"), "m");
    DecimalScalar scalar2 = new DecimalScalar(caster, new BigDecimal("2.0"), "s");

    DecimalScalar result = (DecimalScalar)scalar1.divide(scalar2);
    assertEquals(new BigDecimal("5.25"), result.getAsDecimal());
    assertEquals("m/s", result.getUnits());
  }

  @Test
  void testDivideByZero() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    DecimalScalar scalar1 = new DecimalScalar(caster, new BigDecimal("10.5"), "m");
    DecimalScalar zeroScalar = new DecimalScalar(caster, BigDecimal.ZERO, "s");

    assertThrows(ArithmeticException.class, () -> scalar1.divide(zeroScalar));
  }

  @Test
  void testRaiseToPower() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    DecimalScalar scalar1 = new DecimalScalar(caster, new BigDecimal("2.0"), "m");
    DecimalScalar scalar2 = new DecimalScalar(caster, new BigDecimal("3.0"), "");

    DecimalScalar result = (DecimalScalar)scalar1.raiseToPower(scalar2);
    assertEquals(new BigDecimal("8.0"), result.getAsDecimal());
    assertEquals("m^3", result.getUnits());
  }
  
  @Test
  void testRaiseToPowerWithUnits() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    DecimalScalar scalar1 = new DecimalScalar(caster, new BigDecimal("2.0"), "m");
    DecimalScalar scalar2 = new DecimalScalar(caster, new BigDecimal("3.0"), "s");

    assertThrows(IllegalArgumentException.class, () -> scalar1.raiseToPower(scalar2));
  }
  
  // @Test
  // void testGetAsDistribution() {
  //   EngineValueCaster caster = new EngineValueWideningCaster();
  //   DecimalScalar scalar = new DecimalScalar(caster, new BigDecimal("42.5"), "kg");
    
  //   Distribution dist = scalar.getAsDistribution();
  //   assertEquals(1, dist.getValues().size());
  //   assertEquals(new BigDecimal("42.5"), dist.getValues().get(0));
  //   assertEquals("kg", dist.getUnits());
  // }
  
  
  @Test
  void testScaleAndPrecision() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    DecimalScalar scalar = new DecimalScalar(caster, new BigDecimal("123.456789"), "");
    
    assertEquals(new BigDecimal("123.456789"), scalar.getAsDecimal());
    assertEquals("123.456789", scalar.getAsString());
  }
}
