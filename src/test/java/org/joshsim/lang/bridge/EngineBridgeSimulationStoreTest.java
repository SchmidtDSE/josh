
package org.joshsim.lang.bridge;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import java.util.HashMap;
import java.util.Map;

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
    void testGetStepFunction_ExistingSimulation() {
        EngineBridgeOperation operation = store.getStepFunction("testSimulation");
        assertNotNull(operation, "Should return operation for existing simulation");
        assertEquals(mockOperation, operation, "Should return the correct operation");
    }
    
    @Test
    void testGetStepFunction_NonExistingSimulation() {
        Exception exception = assertThrows(UnsupportedOperationException.class, () -> {
            store.getStepFunction("nonExistingSimulation");
        });
        
        String expectedMessage = "Unknown simulation: nonExistingSimulation";
        String actualMessage = exception.getMessage();
        assertEquals(expectedMessage, actualMessage, "Should throw correct exception message");
    }
    
    @Test
    void testConstructor_WithEmptyMap() {
        Map<String, EngineBridgeOperation> emptyMap = new HashMap<>();
        EngineBridgeSimulationStore emptyStore = new EngineBridgeSimulationStore(emptyMap);
        
        Exception exception = assertThrows(UnsupportedOperationException.class, () -> {
            emptyStore.getStepFunction("anySimulation");
        });
        
        assertNotNull(exception, "Should throw exception for empty store");
    }
}
