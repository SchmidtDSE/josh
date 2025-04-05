
/**
 * Tests for FutureBridgeGetter.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret;

import org.joshsim.engine.entity.base.MutableEntity;
import org.joshsim.engine.entity.prototype.EntityPrototype;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import org.joshsim.engine.entity.prototype.EntityPrototypeStore;
import org.joshsim.engine.simulation.Replicate;
import org.joshsim.lang.bridge.EngineBridge;
import org.joshsim.lang.bridge.EngineBridgeSimulationStore;
import org.joshsim.engine.value.converter.Converter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class FutureBridgeGetterTest {

    @Mock(lenient = true) private JoshProgram mockProgram;
    @Mock(lenient = true) private EngineBridgeSimulationStore mockSimStore;
    @Mock(lenient = true) private EntityPrototypeStore mockPrototypeStore;
    @Mock(lenient = true) private Converter mockConverter;
    @Mock(lenient = true) private MutableEntity mockSimulation;
    @Mock(lenient = true) private EntityPrototype mockPrototype;
    private FutureBridgeGetter bridgeGetter;
    
    @BeforeEach
    void setUp() {
        bridgeGetter = new FutureBridgeGetter();
        
        when(mockProgram.getSimulations()).thenReturn(mockSimStore);
        when(mockProgram.getPrototypes()).thenReturn(mockPrototypeStore);
        when(mockProgram.getConverter()).thenReturn(mockConverter);
        when(mockSimStore.getProtoype("testSim")).thenReturn(mockPrototype);
        when(mockPrototype.build()).thenReturn(mockSimulation);
    }

    @Test
    void testGetBridgeWithoutProgram() {
        bridgeGetter.setSimulationName("testSim");
        assertThrows(
            IllegalStateException.class,
            () -> bridgeGetter.get(),
            "Should throw when program not set"
        );
    }

    @Test
    void testGetBridgeWithoutSimulationName() {
        bridgeGetter.setProgram(mockProgram);
        assertThrows(
            IllegalStateException.class,
            () -> bridgeGetter.get(),
            "Should throw when simulation name not set"
        );
    }

    @Test
    void testSuccessfulBridgeCreation() {
        bridgeGetter.setProgram(mockProgram);
        bridgeGetter.setSimulationName("testSim");
        
        EngineBridge bridge = bridgeGetter.get();
        assertEquals(bridge, bridgeGetter.get(), "Should return same bridge instance");
    }

    @Test
    void testSetProgramAfterBridgeBuilt() {
        bridgeGetter.setProgram(mockProgram);
        bridgeGetter.setSimulationName("testSim");
        bridgeGetter.get();

        assertThrows(
            IllegalStateException.class,
            () -> bridgeGetter.setProgram(mockProgram),
            "Should throw when setting program after bridge built"
        );
    }

    @Test
    void testSetSimulationNameAfterBridgeBuilt() {
        bridgeGetter.setProgram(mockProgram);
        bridgeGetter.setSimulationName("testSim");
        bridgeGetter.get();

        assertThrows(
            IllegalStateException.class,
            () -> bridgeGetter.setSimulationName("testSim"),
            "Should throw when setting simulation name after bridge built"
        );
    }
}
