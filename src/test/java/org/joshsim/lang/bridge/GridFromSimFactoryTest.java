
/**
 * Tests for GridFromSimFactory which builds Grid objects from simulation entities.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.bridge;

import org.joshsim.engine.value.type.EngineValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import org.joshsim.engine.entity.base.Entity;
import org.joshsim.engine.geometry.Grid;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.engine.value.type.EngineValue;
import org.junit.jupiter.api.BeforeEach;
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
  void buildWithCustomValues() {
    // Mock custom values for grid attributes
    EngineValue size = valueFactory.build(new BigDecimal("30"), new Units("meters"));
    when(mockSimulation.getAttributeValue("grid.inputCrs"))
        .thenReturn(Optional.of(valueFactory.build("EPSG:4326", Units.EMPTY)));
    when(mockSimulation.getAttributeValue("grid.targetCrs"))
        .thenReturn(Optional.of(valueFactory.build("EPSG:32611", Units.EMPTY)));
    when(mockSimulation.getAttributeValue("grid.start"))
        .thenReturn(Optional.of(valueFactory.build(
            "30 degrees latitude, -100 degrees longitude",
            Units.EMPTY
        )));
    when(mockSimulation.getAttributeValue("grid.end"))
        .thenReturn(Optional.of(valueFactory.build(
            "25 degrees latitude, -95 degrees longitude",
            Units.EMPTY
        )));
    when(mockSimulation.getAttributeValue("grid.size"))
      .thenReturn(Optional.of(size));

    Grid result = factory.build(mockSimulation);
    
    assertNotNull(result, "Grid should be created with custom values");
    assertTrue(result.getPatches().size() > 0, "Grid should contain patches");
    assertEquals(new BigDecimal("30"), result.getSpacing(), "Grid spacing should match input");
  }
}
