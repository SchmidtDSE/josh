
/**
 * Tests for GridFromSimFactory which builds Grid objects from simulation entities.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.bridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import org.joshsim.engine.entity.base.Entity;
import org.joshsim.engine.geometry.Grid;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValue;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Test for GridFromSimFactory which helps create Grid objects from simulation entities.
 */
@ExtendWith(MockitoExtension.class)
class GridFromSimFactoryTest {

    @Mock private EngineBridge mockBridge;
    @Mock private Entity mockSimulation;
    @Mock private EngineValue mockValue;
    
    private EngineValueFactory valueFactory;
    private GridFromSimFactory factory;

    /**
     * Sets up test environment before each test.
     */
    @BeforeEach
    void setUp() {
        valueFactory = new EngineValueFactory();
        factory = new GridFromSimFactory(mockBridge, valueFactory);
    }

    @Test
    @DisplayName("build() with default values creates valid Grid")
    void buildWithDefaultValues() {
        // Setup empty optional returns for all grid attributes
        when(mockSimulation.getAttributeValue("grid.inputCrs")).thenReturn(Optional.empty());
        when(mockSimulation.getAttributeValue("grid.targetCrs")).thenReturn(Optional.empty());
        when(mockSimulation.getAttributeValue("grid.start")).thenReturn(Optional.empty());
        when(mockSimulation.getAttributeValue("grid.end")).thenReturn(Optional.empty());
        when(mockSimulation.getAttributeValue("grid.size")).thenReturn(Optional.empty());

        Grid result = factory.build(mockSimulation);
        
        assertNotNull(result, "Grid should be created with default values");
        assertNotNull(result.getPatches(), "Grid should contain patches");
        assertNotNull(result.getSpacing(), "Grid should have spacing defined");
    }

    @Test
    @DisplayName("build() with custom values creates expected Grid")
    void buildWithCustomValues() {
        // Mock custom values for grid attributes
        EngineValue size = valueFactory.build(new BigDecimal("30"), new Units("meters"));
        when(mockSimulation.getAttributeValue("grid.inputCrs")).thenReturn(Optional.of(valueFactory.build("EPSG:4326")));
        when(mockSimulation.getAttributeValue("grid.targetCrs")).thenReturn(Optional.of(valueFactory.build("EPSG:32611")));
        when(mockSimulation.getAttributeValue("grid.start")).thenReturn(Optional.of(valueFactory.build("34 degrees latitude, -116 degrees longitude")));
        when(mockSimulation.getAttributeValue("grid.end")).thenReturn(Optional.of(valueFactory.build("35 degrees latitude, -115 degrees longitude")));
        when(mockSimulation.getAttributeValue("grid.size")).thenReturn(Optional.of(size));
        
        when(mockBridge.convert(size, new Units("meters"))).thenReturn(size);

        Grid result = factory.build(mockSimulation);
        
        assertNotNull(result, "Grid should be created with custom values");
        assertTrue(result.getPatches().size() > 0, "Grid should contain patches");
        assertEquals(new BigDecimal("30"), result.getSpacing(), "Grid spacing should match input");
    }
}
