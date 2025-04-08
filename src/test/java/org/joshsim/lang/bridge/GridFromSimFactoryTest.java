
/**
 * Tests for GridFromSimFactory which builds Grid objects from simulation entities.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.bridge;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import org.joshsim.engine.entity.base.Entity;
import org.joshsim.engine.entity.prototype.EntityPrototype;
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

  @Mock(lenient = true) private EngineBridge mockBridge;
  @Mock(lenient = true) private Entity mockSimulation;
  @Mock(lenient = true) private EntityPrototype mockPrototype;

  private EngineValueFactory valueFactory;
  private GridFromSimFactory factory;

  /**
   * Sets up test environment before each test.
   */
  @BeforeEach
  void setUp() {
    valueFactory = new EngineValueFactory();
    factory = new GridFromSimFactory(mockBridge, valueFactory);
    when(mockBridge.getPrototype("Default")).thenReturn(mockPrototype);
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
    EngineValueFactory defaultFactory = new EngineValueFactory();
    EngineValue crsStr = defaultFactory.build("EPSG:4326", Units.EMPTY);
    EngineValue gridStartStr = defaultFactory.build(
        "30 degrees latitude, -100 degrees longitude",
        Units.EMPTY
    );
    EngineValue gridEndStr = defaultFactory.build(
        "25 degrees latitude, -95 degrees longitude",
        Units.EMPTY
    );
    EngineValue sizeVal = defaultFactory.build(1, Units.METERS);

    EngineValue size = valueFactory.build(new BigDecimal("30"), new Units("meters"));
    when(mockSimulation.getAttributeValue("grid.inputCrs")).thenReturn(Optional.of(crsStr));
    when(mockSimulation.getAttributeValue("grid.targetCrs")).thenReturn(Optional.of(crsStr));
    when(mockSimulation.getAttributeValue("grid.start")).thenReturn(Optional.of(gridStartStr));
    when(mockSimulation.getAttributeValue("grid.end")).thenReturn(Optional.of(gridEndStr));
    when(mockSimulation.getAttributeValue("grid.size")).thenReturn(Optional.of(sizeVal));

    Grid result = factory.build(mockSimulation);

    assertNotNull(result, "Grid should be created with custom values");
    assertFalse(result.getPatches().isEmpty(), "Grid should contain patches");
  }
}
