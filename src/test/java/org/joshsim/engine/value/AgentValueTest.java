
package org.joshsim.engine.value;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.joshsim.engine.entity.Agent;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AgentValueTest {

  @Test
  void testConstruction() {
    Agent mockAgent = Mockito.mock(Agent.class);
    Mockito.when(mockAgent.getClass().getSimpleName()).thenReturn("TestAgent");
    
    EngineValueCaster mockCaster = Mockito.mock(EngineValueCaster.class);
    AgentValue value = new AgentValue(mockCaster, mockAgent);
    
    assertEquals("TestAgent", value.getUnits().toString());
    assertEquals("TestAgent", value.getLanguageType().toString());
    assertEquals(mockAgent, value.getInnerValue());
  }

  @Test
  void testConversionOperations() {
    Agent mockAgent = Mockito.mock(Agent.class);
    EngineValueCaster mockCaster = Mockito.mock(EngineValueCaster.class);
    AgentValue value = new AgentValue(mockCaster, mockAgent);

    assertThrows(UnsupportedOperationException.class, () -> value.getAsScalar());
    assertThrows(UnsupportedOperationException.class, () -> value.getAsDecimal());
    assertThrows(UnsupportedOperationException.class, () -> value.getAsBoolean());
    assertThrows(UnsupportedOperationException.class, () -> value.getAsString());
    assertThrows(UnsupportedOperationException.class, () -> value.getAsInt());
    assertThrows(UnsupportedOperationException.class, () -> value.getAsDistribution());
    assertThrows(UnsupportedOperationException.class, () -> value.cast(Cast.TO_BOOLEAN));
  }

  @Test
  void testArithmeticOperations() {
    Agent mockAgent = Mockito.mock(Agent.class);
    EngineValueCaster mockCaster = Mockito.mock(EngineValueCaster.class);
    AgentValue value = new AgentValue(mockCaster, mockAgent);
    AgentValue other = new AgentValue(mockCaster, mockAgent);

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
    Agent mockAgent = Mockito.mock(Agent.class);
    EngineValueCaster mockCaster = Mockito.mock(EngineValueCaster.class);
    AgentValue value = new AgentValue(mockCaster, mockAgent);

    assertFalse(value.canBePower());
  }
}
