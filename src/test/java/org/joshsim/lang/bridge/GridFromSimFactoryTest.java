
/**
 * Tests for GridFromSimFactory which builds PatchSet objects from simulation entities.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.bridge;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.referencing.CRS;
import org.joshsim.engine.entity.base.MutableEntity;
import org.joshsim.engine.entity.prototype.EntityPrototype;
import org.joshsim.engine.geometry.PatchSet;
import org.joshsim.engine.geometry.grid.GridGeometryFactory;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.engine.value.type.EngineValue;
import org.joshsim.geo.geometry.EarthGeometryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


/**
 * Test for GridFromSimFactory which helps create PatchSet objects from simulation entities.
 */
@ExtendWith(MockitoExtension.class)
class GridFromSimFactoryTest {

  @Mock(lenient = true) private EngineBridge mockBridge;
  @Mock(lenient = true) private MutableEntity mockSimulation;
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
    when(mockBridge.getGeometryFactory()).thenReturn(new GridGeometryFactory());

    when(mockSimulation.getAttributeValue("grid.inputCrs")).thenReturn(Optional.empty());
    when(mockSimulation.getAttributeValue("grid.targetCrs")).thenReturn(Optional.empty());
    when(mockSimulation.getAttributeValue("grid.low")).thenReturn(Optional.empty());
    when(mockSimulation.getAttributeValue("grid.high")).thenReturn(Optional.empty());
    when(mockSimulation.getAttributeValue("grid.size")).thenReturn(Optional.empty());

    PatchSet result = factory.build(mockSimulation);

    assertNotNull(result, "PatchSet should be created with default values");
    assertNotNull(result.getPatches(), "PatchSet should contain patches");
    assertNotNull(result.getSpacing(), "PatchSet should have spacing defined");
  }

  @Test
  void testConvertCoordinatesToMeters() {
    when(mockBridge.getGeometryFactory()).thenReturn(new GridGeometryFactory());

    EngineValueFactory testFactory = new EngineValueFactory();
    EngineValue gridStartStr = testFactory.build(
        "1.23 degrees latitude, 1.4 degrees longitude",
        Units.EMPTY
    );
    EngineValue gridEndStr = testFactory.build(
        "1.3 degrees latitude, 1.5 degrees longitude",
        Units.EMPTY
    );
    EngineValue sizeVal = testFactory.build(1000, Units.METERS);

    when(mockSimulation.getAttributeValue("grid.inputCrs")).thenReturn(Optional.empty());
    when(mockSimulation.getAttributeValue("grid.targetCrs")).thenReturn(Optional.empty());
    when(mockSimulation.getAttributeValue("grid.low")).thenReturn(Optional.of(gridStartStr));
    when(mockSimulation.getAttributeValue("grid.high")).thenReturn(Optional.of(gridEndStr));
    when(mockSimulation.getAttributeValue("grid.size")).thenReturn(Optional.of(sizeVal));

    PatchSet result = factory.build(mockSimulation);

    assertNotNull(result);
    assertTrue(result.getSpacing().compareTo(BigDecimal.valueOf(1)) == 0);
  }

  @Test
  void buildWithCustomValues() throws FactoryException {
    CoordinateReferenceSystem crs = CRS.decode("EPSG:4326", true);
    when(mockBridge.getGeometryFactory()).thenReturn(new EarthGeometryFactory(crs));

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
    when(mockSimulation.getAttributeValue("grid.low")).thenReturn(Optional.of(gridStartStr));
    when(mockSimulation.getAttributeValue("grid.high")).thenReturn(Optional.of(gridEndStr));
    when(mockSimulation.getAttributeValue("grid.size")).thenReturn(Optional.of(sizeVal));

    PatchSet result = factory.build(mockSimulation);

    assertNotNull(result, "PatchSet should be created with custom values");
    assertFalse(result.getPatches().isEmpty(), "PatchSet should contain patches");
  }
}
