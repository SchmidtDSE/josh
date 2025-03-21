
package org.joshsim.lang.bridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class EngineBridgeSimulationStoreTest {
    
    private EngineBridgeSimulationStore store;
    private Map<String, EngineBridgeOperation> simulationSteps;
    private EngineBridgeOperation mockOperation;
    
    @BeforeEach
    void setUp() {
        simulationSteps = new HashMap<>();
        mockOperation = bridge -> java.util.Optional.empty();
        simulationSteps.put("testSimulation", mockOperation);
        store = new EngineBridgeSimulationStore(simulationSteps);
    }
    
    @Test
    void testGetKnownSimulation() {
        EngineBridgeOperation operation = store.getStepFunction("testSimulation");
        assertNotNull(operation, "Should return operation for existing simulation");
        assertEquals(mockOperation, operation, "Should return the correct operation");
    }
    
    @Test
    void testGetUnknownSimulation() {
        Exception exception = assertThrows(UnsupportedOperationException.class, () -> {
            store.getStepFunction("nonExistingSimulation");
        });
        
        String expectedMessage = "Unknown simulation: nonExistingSimulation";
        String actualMessage = exception.getMessage();
        assertEquals(expectedMessage, actualMessage, "Should throw correct exception message");
    }
}
