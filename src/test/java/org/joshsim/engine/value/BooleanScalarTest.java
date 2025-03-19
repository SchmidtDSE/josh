package org.joshsim.engine.value;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class BooleanScalarTest {

  @Test
  void testConstructorAndGetters() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    BooleanScalar scalar = new BooleanScalar(caster, true, "");

    assertEquals(true, scalar.getAsBoolean());
    assertEquals("boolean", scalar.getLanguageType());
    assertEquals("true", scalar.getAsString());
    assertEquals(Boolean.TRUE, scalar.getInnerValue());
  }

  @Test
  void testGetAsInt() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    BooleanScalar trueScalar = new BooleanScalar(caster, true, "");
    BooleanScalar falseScalar = new BooleanScalar(caster, false, "");

    assertEquals(1L, trueScalar.getAsInt());
    assertEquals(0L, falseScalar.getAsInt());
  }

  @Test
  void testGetAsDecimal() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    BooleanScalar trueScalar = new BooleanScalar(caster, true, "");
    BooleanScalar falseScalar = new BooleanScalar(caster, false, "");

    assertEquals(new BigDecimal(1), trueScalar.getAsDecimal());
    assertEquals(new BigDecimal(0), falseScalar.getAsDecimal());
  }

  @Test
  void testGetAsString() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    BooleanScalar trueScalar = new BooleanScalar(caster, true, "");
    BooleanScalar falseScalar = new BooleanScalar(caster, false, "");

    assertEquals("true", trueScalar.getAsString());
    assertEquals("false", falseScalar.getAsString());
  }
  
  @Test
  void testAddThrowsException() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    BooleanScalar scalar1 = new BooleanScalar(caster, true, "");
    BooleanScalar scalar2 = new BooleanScalar(caster, false, "");
    
    assertThrows(UnsupportedOperationException.class, () -> scalar1.add(scalar2));
  }
  
  @Test
  void testSubtractThrowsException() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    BooleanScalar scalar1 = new BooleanScalar(caster, true, "");
    BooleanScalar scalar2 = new BooleanScalar(caster, false, "");
    
    assertThrows(UnsupportedOperationException.class, () -> scalar1.subtract(scalar2));
  }
  
  @Test
  void testMultiply() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    BooleanScalar scalar1 = new BooleanScalar(caster, true, "");
    BooleanScalar scalar2 = new BooleanScalar(caster, false, "");
    BooleanScalar scalar3 = new BooleanScalar(caster, true, "");
    
    BooleanScalar result1 = (BooleanScalar) scalar1.multiply(scalar2);
    assertEquals(false, result1.getAsBoolean());
    
    BooleanScalar result2 = (BooleanScalar) scalar1.multiply(scalar3);
    assertEquals(true, result2.getAsBoolean());
  }
  
  @Test
  void testDivideThrowsException() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    BooleanScalar scalar1 = new BooleanScalar(caster, true, "");
    BooleanScalar scalar2 = new BooleanScalar(caster, false, "");
    
    assertThrows(UnsupportedOperationException.class, () -> scalar1.divide(scalar2));
  }
  
  @Test
  void testRaiseToPowerThrowsException() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    BooleanScalar scalar1 = new BooleanScalar(caster, true, "");
    BooleanScalar scalar2 = new BooleanScalar(caster, false, "");
    
    assertThrows(UnsupportedOperationException.class, () -> scalar1.raiseToPower(scalar2));
  }
  
  // @Test
  // void testGetAsDistribution() {
  //   EngineValueCaster caster = new EngineValueWideningCaster();
  //   BooleanScalar scalar = new BooleanScalar(caster, true, "units");
    
  //   Distribution dist = scalar.getAsDistribution();
  //   assertEquals(1, dist.getValues().size());
  //   assertEquals(true, dist.getValues().get(0));
  //   assertEquals("units", dist.getUnits());
  // }
  
  @Test
  void testWithDifferentUnits() {
    EngineValueCaster caster = new EngineValueWideningCaster();
    BooleanScalar scalar = new BooleanScalar(caster, true, "meters");
    
    assertEquals("meters", scalar.getUnits());
  }
}