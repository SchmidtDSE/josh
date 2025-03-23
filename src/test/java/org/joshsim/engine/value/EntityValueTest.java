package org.joshsim.engine.value;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import org.joshsim.engine.entity.Entity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
class EntityValueTest {

  @Mock private EngineValueCaster mockCaster;
  @Mock private Entity mockEntity;

  @BeforeEach
  void setUp() {
    when(mockEntity.getName()).thenReturn("TestEntity").thenReturn("TestEntity");
  }

  @Test
  void testConstruction() {
    EntityValue value = new EntityValue(mockCaster, mockEntity);

    assertEquals("TestEntity", value.getUnits().toString());
    assertEquals("TestEntity", value.getLanguageType().toString());
    assertEquals(mockEntity, value.getInnerValue());
  }

  @Test
  void testConversionOperations() {
    EntityValue value = new EntityValue(mockCaster, mockEntity);

    assertThrows(UnsupportedOperationException.class, () -> value.getAsScalar());
    assertThrows(UnsupportedOperationException.class, () -> value.getAsDecimal());
    assertThrows(UnsupportedOperationException.class, () -> value.getAsBoolean());
    assertThrows(UnsupportedOperationException.class, () -> value.getAsString());
    assertThrows(UnsupportedOperationException.class, () -> value.getAsInt());
    assertThrows(UnsupportedOperationException.class, () -> value.getAsDistribution());
  }

  @Test
  void testArithmeticOperations() {
    EntityValue value = new EntityValue(mockCaster, mockEntity);
    EntityValue other = new EntityValue(mockCaster, mockEntity);

    assertThrows(UnsupportedOperationException.class, () -> value.unsafeAdd(other));
    assertThrows(UnsupportedOperationException.class, () -> value.unsafeSubtract(other));
    assertThrows(UnsupportedOperationException.class, () -> value.unsafeMultiply(other));
    assertThrows(UnsupportedOperationException.class, () -> value.unsafeDivide(other));
    assertThrows(UnsupportedOperationException.class, () -> value.unsafeRaiseToPower(other));
    assertThrows(UnsupportedOperationException.class, () -> value.unsafeSubtractFrom(other));
    assertThrows(UnsupportedOperationException.class, () -> value.unsafeDivideFrom(other));
    assertThrows(UnsupportedOperationException.class, () -> value.unsafeRaiseAllToPower(other));
  }

  @Test
  void testCanBePower() {
    EntityValue value = new EntityValue(mockCaster, mockEntity);

    assertFalse(value.canBePower());
  }
}
