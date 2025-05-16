/**
 * Tests for BooleanScalar.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.value.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.joshsim.compat.CompatibilityLayerKeeper;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueCaster;
import org.joshsim.engine.value.engine.EngineValueWideningCaster;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for the BooleanScalar class which represents boolean values in the engine.
 */
class BooleanScalarTest {

  private EngineValueCaster caster;

  @BeforeEach
  void setUp() {
    caster = new EngineValueWideningCaster(
        CompatibilityLayerKeeper.get().getEngineValueFactory()
    );
  }

  @Test
  void testConstructorAndGetters() {
    BooleanScalar scalar = new BooleanScalar(caster, true, Units.EMPTY);

    assertEquals(true, scalar.getAsBoolean());
    assertEquals(new LanguageType("boolean"), scalar.getLanguageType());
    assertEquals("true", scalar.getAsString());
    assertEquals(Boolean.TRUE, scalar.getInnerValue());
  }

  @Test
  void testGetAsInt() {
    BooleanScalar trueScalar = new BooleanScalar(caster, true, Units.EMPTY);
    BooleanScalar falseScalar = new BooleanScalar(caster, false, Units.EMPTY);

    assertEquals(1L, trueScalar.getAsInt());
    assertEquals(0L, falseScalar.getAsInt());
  }

  @Test
  void testGetAsDecimal() {
    BooleanScalar trueScalar = new BooleanScalar(caster, true, Units.EMPTY);
    BooleanScalar falseScalar = new BooleanScalar(caster, false, Units.EMPTY);

    assertEquals(new BigDecimal(1), trueScalar.getAsDecimal());
    assertEquals(new BigDecimal(0), falseScalar.getAsDecimal());
  }

  @Test
  void testGetAsString() {
    BooleanScalar trueScalar = new BooleanScalar(caster, true, Units.EMPTY);
    BooleanScalar falseScalar = new BooleanScalar(caster, false, Units.EMPTY);

    assertEquals("true", trueScalar.getAsString());
    assertEquals("false", falseScalar.getAsString());
  }

  @Test
  void testAddThrowsException() {
    BooleanScalar scalar1 = new BooleanScalar(caster, true, Units.EMPTY);
    BooleanScalar scalar2 = new BooleanScalar(caster, false, Units.EMPTY);

    assertThrows(UnsupportedOperationException.class, () -> scalar1.add(scalar2));
  }

  @Test
  void testSubtractThrowsException() {
    BooleanScalar scalar1 = new BooleanScalar(caster, true, Units.EMPTY);
    BooleanScalar scalar2 = new BooleanScalar(caster, false, Units.EMPTY);

    assertThrows(UnsupportedOperationException.class, () -> scalar1.subtract(scalar2));
  }

  @Test
  void testMultiplyThrowsException() {
    BooleanScalar scalar1 = new BooleanScalar(caster, true, Units.EMPTY);
    BooleanScalar scalar2 = new BooleanScalar(caster, false, Units.EMPTY);

    assertThrows(UnsupportedOperationException.class, () -> scalar1.multiply(scalar2));
  }

  @Test
  void testDivideThrowsException() {
    BooleanScalar scalar1 = new BooleanScalar(caster, true, Units.EMPTY);
    BooleanScalar scalar2 = new BooleanScalar(caster, false, Units.EMPTY);

    assertThrows(UnsupportedOperationException.class, () -> scalar1.divide(scalar2));
  }

  @Test
  void testRaiseToPowerThrowsException() {
    BooleanScalar scalar1 = new BooleanScalar(caster, true, Units.of("count"));
    BooleanScalar scalar2 = new BooleanScalar(caster, false, Units.of("count"));

    assertThrows(UnsupportedOperationException.class, () -> scalar1.raiseToPower(scalar2));
  }

  @Test
  void testGetAsDistribution() {
    BooleanScalar scalar = new BooleanScalar(caster, true, Units.of("units"));

    Distribution dist = scalar.getAsDistribution();
    assertEquals(1, dist.getValues().size());
    assertEquals(true, dist.getValues().get(0));
     assertEquals("units", dist.getUnits());
  }

  @Test
  void testGetAsEntityThrowsException() {
    BooleanScalar scalar = new BooleanScalar(caster, true, Units.EMPTY);

    assertThrows(UnsupportedOperationException.class, scalar::getAsEntity);
  }

  @Test
  void testWithDifferentUnits() {
    BooleanScalar scalar = new BooleanScalar(caster, true, Units.of("meters"));

    assertEquals(Units.of("meters"), scalar.getUnits());
  }


}
