
package org.joshsim.lang.bridge;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Optional;
import org.joshsim.engine.entity.Entity;
import org.joshsim.engine.entity.EventHandlerGroup;
import org.joshsim.engine.entity.Patch;
import org.joshsim.engine.entity.Simulation;
import org.joshsim.engine.entity.SpatialEntity;
import org.joshsim.engine.value.EngineValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ShadowingEntityTest {

    @Mock private Patch mockPatch;
    @Mock private SpatialEntity mockSpatialEntity;
    @Mock private Simulation mockSimulation;
    @Mock private EventHandlerGroup mockEventHandlerGroup;
    @Mock private EngineValue mockEngineValue;
    
    private ShadowingEntity patchEntity;
    private ShadowingEntity spatialEntity;

    @BeforeEach
    void setUp() {
        when(mockPatch.getName()).thenReturn("TestPatch");
        when(mockSpatialEntity.getName()).thenReturn("TestEntity");
        
        // Setup event handlers
        when(mockPatch.getEventHandlers()).thenReturn(Arrays.asList(mockEventHandlerGroup));
        when(mockSpatialEntity.getEventHandlers()).thenReturn(Arrays.asList(mockEventHandlerGroup));
        
        patchEntity = new ShadowingEntity(mockPatch, mockSimulation);
        spatialEntity = new ShadowingEntity(mockSpatialEntity, patchEntity, mockSimulation);
    }

    @Test
    void testSubstepLifecycle() {
        String substepName = "testSubstep";
        
        // Start substep
        spatialEntity.startSubstep(substepName);
        
        // Verify cannot start another substep
        assertThrows(IllegalStateException.class, () -> 
            spatialEntity.startSubstep("anotherSubstep")
        );
        
        // End substep
        spatialEntity.endSubstep();
        
        // Can start new substep after ending
        spatialEntity.startSubstep("newSubstep");
        spatialEntity.endSubstep();
    }

    @Test
    void testAttributeManagement() {
        String attrName = "testAttr";
        
        // Setup mock behavior
        when(mockSpatialEntity.getAttributeValue(attrName))
            .thenReturn(Optional.of(mockEngineValue));
        
        // Test getting prior attribute
        EngineValue priorValue = spatialEntity.getPriorAttribute(attrName);
        assertEquals(mockEngineValue, priorValue);
        
        // Test setting current attribute
        spatialEntity.setCurrentAttribute(attrName, mockEngineValue);
        verify(mockSpatialEntity).setAttributeValue(attrName, mockEngineValue);
    }

    @Test
    void testGetHandlersFailsOutsideSubstep() {
        assertThrows(IllegalStateException.class, () ->
            spatialEntity.getHandlers("testAttr")
        );
    }

    @Test
    void testGetHandlersDuringSubstep() {
        String attrName = "testAttr";
        String substepName = "testSubstep";
        
        when(mockSpatialEntity.getEventHandlers(attrName, substepName))
            .thenReturn(Arrays.asList(mockEventHandlerGroup));
            
        spatialEntity.startSubstep(substepName);
        Iterable<EventHandlerGroup> handlers = spatialEntity.getHandlers(attrName);
        assertNotNull(handlers);
        spatialEntity.endSubstep();
    }

    @Test
    void testGetCurrentAttributeUnresolved() {
        String attrName = "testAttr";
        Optional<EngineValue> result = spatialEntity.getCurrentAttribute(attrName);
        assertTrue(result.isEmpty());
    }

    @Test
    void testLocationAccessors() {
        assertEquals(patchEntity, spatialEntity.getHere());
        assertEquals(mockSimulation, spatialEntity.getMeta());
    }

    @Test
    void testNonexistentAttributeAccess() {
        String nonexistentAttr = "nonexistent";
        
        assertThrows(IllegalArgumentException.class, () ->
            spatialEntity.setCurrentAttribute(nonexistentAttr, mockEngineValue)
        );
        
        assertThrows(IllegalArgumentException.class, () ->
            spatialEntity.getPriorAttribute(nonexistentAttr)
        );
    }
}
