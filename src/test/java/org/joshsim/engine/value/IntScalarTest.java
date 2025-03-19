package org.joshsim.engine.value;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
// import org.joshsim.engine.value.EngineValueWideningCaster;

class IntScalarTest {

    @Test
    void testConstructorAndGetters() {
        EngineValueCaster caster = new EngineValueWideningCaster();
        IntScalar scalar = new IntScalar(caster, 10L, "m");

        assertEquals(10L, scalar.getAsInt());
        assertEquals("m", scalar.getUnits());
        assertEquals(new BigDecimal(10), scalar.getAsDecimal());
        assertEquals("10", scalar.getAsString());
        assertEquals("int", scalar.getLanguageType());
    }

    @Test
    void testAdd() {
        EngineValueCaster caster = new EngineValueWideningCaster();
        IntScalar scalar1 = new IntScalar(caster, 10L, "m");
        IntScalar scalar2 = new IntScalar(caster, 5L, "m");

        IntScalar result = (IntScalar)scalar1.add(scalar2);
        assertEquals(15L, result.getAsInt());
        assertEquals("m", result.getUnits());
    }

    @Test
    void testSubtract() {
        EngineValueCaster caster = new EngineValueWideningCaster();
        IntScalar scalar1 = new IntScalar(caster, 10L, "m");
        IntScalar scalar2 = new IntScalar(caster, 5L, "m");

        IntScalar result = (IntScalar)scalar1.subtract(scalar2);
        assertEquals(5L, result.getAsInt());
        assertEquals("m", result.getUnits());
    }

    @Test
    void testMultiply() {
        EngineValueCaster caster = new EngineValueWideningCaster();
        IntScalar scalar1 = new IntScalar(caster, 10L, "m");
        IntScalar scalar2 = new IntScalar(caster, 2L, "s");

        IntScalar result = (IntScalar)scalar1.multiply(scalar2);
        assertEquals(20L, result.getAsInt());
        assertEquals("m*s", result.getUnits());
    }

    @Test
    void testDivide() {
        EngineValueCaster caster = new EngineValueWideningCaster();
        IntScalar scalar1 = new IntScalar(caster, 10L, "m");
        IntScalar scalar2 = new IntScalar(caster, 2L, "s");

        IntScalar result = (IntScalar)scalar1.divide(scalar2);
        assertEquals(5L, result.getAsInt());
        assertEquals("m/s", result.getUnits());
    }

    @Test
    void testRaiseToPower() {
        EngineValueCaster caster = new EngineValueWideningCaster();
        IntScalar scalar1 = new IntScalar(caster, 2L, "m");
        IntScalar scalar2 = new IntScalar(caster, 3L, "");

        DecimalScalar result = (DecimalScalar)scalar1.raiseToPower(scalar2);
        assertEquals(new BigDecimal(8), result.getAsDecimal());
        assertEquals("m", result.getUnits());
    }

    // @Test
    // void testGetAsDistribution() {
    //     EngineValueCaster caster = new EngineValueWideningCaster();
    //     IntScalar scalar = new IntScalar(caster, 10L, "m");

    //     Distribution distribution = scalar.getAsDistribution();
    //     assertEquals(1, distribution.getValues().size());
    //     assertEquals(10L, distribution.getValues().get(0));
    //     assertEquals("m", distribution.getUnits());
    // }

    @Test
    void testGetAsBooleanThrowsException() {
        EngineValueCaster caster = new EngineValueWideningCaster();
        IntScalar scalar = new IntScalar(caster, 10L, "m");

        assertThrows(UnsupportedOperationException.class, scalar::getAsBoolean);
    }
}
