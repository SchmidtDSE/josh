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
import org.joshsim.engine.entity.base.EntityBuilder;
import org.joshsim.engine.entity.prototype.EntityPrototype;
import org.joshsim.engine.entity.prototype.ParentlessEntityPrototype;
import org.joshsim.engine.entity.type.EntityType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


/**
 * Tests for a structure keeping track of multiple simulation implementations.
 */
public class EngineBridgeSimulationStoreTest {

  private EngineBridgeSimulationStore store;
  private Map<String, EntityPrototype> simulationPrototypes;

  @BeforeEach
  void setUp() {
    simulationPrototypes = new HashMap<>();
    simulationPrototypes.put("testSimulation", new ParentlessEntityPrototype(
        "testSimluation",
        EntityType.SIMULATION,
        new EntityBuilder())
    );

    store = new EngineBridgeSimulationStore(simulationPrototypes);
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
