/**
 * Tests for GridFromSimFactory which builds PatchSet objects from simulation entities.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.bridge;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.joshsim.engine.entity.base.MutableEntity;
import org.joshsim.engine.entity.prototype.EntityPrototype;
import org.joshsim.engine.geometry.PatchSet;
import org.joshsim.engine.geometry.grid.GridGeometryFactory;
import org.joshsim.engine.value.engine.EngineValueFactory;
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
    when(mockSimulation.getAttributeValue("grid.patch")).thenReturn(Optional.empty());

    PatchSet result = factory.build(mockSimulation);

    assertNotNull(result, "PatchSet should be created with default values");
    assertNotNull(result.getPatches(), "PatchSet should contain patches");
    assertNotNull(result.getSpacing(), "PatchSet should have spacing defined");
  }

  // TODO: Determine whether we want to generate earth patches from earth side
  // or create them on the grid side and convert when needed.

  // @Test
  // void buildWithCustomValues() throws FactoryException {
  //   // Using Apache SIS for CRS handling instead of GeoTools
  //   CoordinateReferenceSystem crs = CRS.forCode("EPSG:4326");
  //   when(mockBridge.getGeometryFactory()).thenReturn(new EarthGeometryFactory(crs));

  //   // Mock custom values for grid attributes
  //   EngineValueFactory defaultFactory = new EngineValueFactory();
  //   EngineValue crsStr = defaultFactory.build("EPSG:4326", Units.EMPTY);
  //   EngineValue gridStartStr = defaultFactory.build(
  //       "34 degrees latitude, -116 degrees longitude",
  //       Units.EMPTY
  //   );
  //   EngineValue gridEndStr = defaultFactory.build(
  //       "35 degrees latitude, -115 degrees longitude",
  //       Units.EMPTY
  //   );
  //   EngineValue sizeVal = defaultFactory.build(30, Units.METERS);
  //   EngineValue patchName = defaultFactory.build("Default", Units.EMPTY);

  //   when(mockSimulation.getAttributeValue("grid.inputCrs")).thenReturn(Optional.of(crsStr));
  //   when(mockSimulation.getAttributeValue("grid.targetCrs")).thenReturn(Optional.of(crsStr));
  //   when(mockSimulation.getAttributeValue("grid.low")).thenReturn(Optional.of(gridStartStr));
  //   when(mockSimulation.getAttributeValue("grid.high")).thenReturn(Optional.of(gridEndStr));
  //   when(mockSimulation.getAttributeValue("grid.size")).thenReturn(Optional.of(sizeVal));
  //   when(mockSimulation.getAttributeValue("grid.patch")).thenReturn(Optional.of(patchName));

  //   PatchSet result = factory.build(mockSimulation);

  //   assertNotNull(result, "PatchSet should be created with custom values");
  //   assertFalse(result.getPatches().isEmpty(), "PatchSet should contain patches");
  // }

  // @Test
  // void buildWithCustomTransverseMercator() throws FactoryException {
  //   // Get a Transverse Mercator projection centered on the area of interest
  //   CoordinateReferenceSystem baseCrs = CommonCRS.WGS84.geographic();
  //   when(mockBridge.getGeometryFactory()).thenReturn(new EarthGeometryFactory(baseCrs));

  //   // Mock custom values for grid attributes - using the example from the prompt
  //   EngineValueFactory defaultFactory = new EngineValueFactory();
  //   EngineValue crsStr = defaultFactory.build("EPSG:4326", Units.EMPTY);
  //   EngineValue gridStartStr = defaultFactory.build(
  //       "34 degrees latitude, -116 degrees longitude",
  //       Units.EMPTY
  //   );
  //   EngineValue gridEndStr = defaultFactory.build(
  //       "35 degrees latitude, -115 degrees longitude",
  //       Units.EMPTY
  //   );
  //   EngineValue sizeVal = defaultFactory.build(30, Units.METERS);

  //   when(mockSimulation.getAttributeValue("grid.inputCrs")).thenReturn(Optional.of(crsStr));
  //   when(mockSimulation.getAttributeValue("grid.targetCrs")).thenReturn(Optional.of(crsStr));
  //   when(mockSimulation.getAttributeValue("grid.low")).thenReturn(Optional.of(gridStartStr));
  //   when(mockSimulation.getAttributeValue("grid.high")).thenReturn(Optional.of(gridEndStr));
  //   when(mockSimulation.getAttributeValue("grid.size")).thenReturn(Optional.of(sizeVal));
  //   when(mockSimulation.getAttributeValue("grid.patch")).thenReturn(Optional.empty());

  //   PatchSet result = factory.build(mockSimulation);

  //   assertNotNull(result, "PatchSet should be created with custom Transverse Mercator projection");
  //   assertFalse(result.getPatches().isEmpty(), "PatchSet should contain patches");
  // }
}
