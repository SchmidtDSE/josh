/**
 * Tests for EngineBridgeSimulationStore.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.bridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.Map;

import org.joshsim.engine.entity.EntityBuilder;
import org.joshsim.engine.entity.EntityType;
import org.joshsim.engine.entity.prototype.EntityPrototype;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


/**
 * Tests for a structure keeping track of multiple simulation implementations.
 */
public class EngineBridgeSimulationStoreTest {

  private EngineBridgeSimulationStore store;
  private Map<String, EngineBridgeOperation> simulationSteps;
  private Map<String, EntityPrototype> simulationPrototypes;
  private EngineBridgeOperation mockOperation;

  @BeforeEach
  void setUp() {
    simulationSteps = new HashMap<>();
    mockOperation = bridge -> java.util.Optional.empty();
    simulationSteps.put("testSimulation", mockOperation);

    simulationPrototypes = new HashMap<>();
    simulationPrototypes.put("testSimulation", new EntityPrototype(
        "testSimluation",
        EntityType.SIMULATION,
        new EntityBuilder())
    );

    store = new EngineBridgeSimulationStore(simulationSteps, simulationPrototypes);
  }

  @Test
  void testGetKnownSimulationStep() {
    EngineBridgeOperation operation = store.getStepFunction("testSimulation");
    assertNotNull(operation, "Should return operation for existing simulation");
    assertEquals(mockOperation, operation, "Should return the correct operation");
  }

  @Test
  void testGetUnknownSimulationStep() {
    Exception exception = assertThrows(UnsupportedOperationException.class, () -> {
      store.getStepFunction("nonExistingSimulation");
    });

    String expectedMessage = "Unknown simulation: nonExistingSimulation";
    String actualMessage = exception.getMessage();
    assertEquals(expectedMessage, actualMessage, "Should throw correct exception message");
  }

  @Test
  void testGetKnownSimulationPrototype() {
    EntityPrototype prototype = store.getProtoype("testSimulation");
    assertNotNull(prototype, "Should return prototype for existing simulation");
  }

  @Test
  void testGetUnknownSimulationPrototype() {
    Exception exception = assertThrows(UnsupportedOperationException.class, () -> {
      store.getProtoype("nonExistingSimulation");
    });

    String expectedMessage = "Unknown simulation: nonExistingSimulation";
    String actualMessage = exception.getMessage();
    assertEquals(expectedMessage, actualMessage, "Should throw correct exception message");
  }
}
