package org.joshsim.geo.external;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.joshsim.engine.entity.base.Entity;
import org.joshsim.engine.entity.base.GeoKey;
import org.joshsim.engine.entity.base.MutableEntity;
import org.joshsim.engine.entity.prototype.EntityPrototype;
import org.joshsim.engine.entity.type.EntityType;
import org.joshsim.engine.entity.type.Patch;
import org.joshsim.engine.geometry.EngineGeometry;
import org.joshsim.engine.geometry.PatchBuilder;
import org.joshsim.engine.geometry.PatchBuilderExtents;
import org.joshsim.engine.geometry.PatchSet;
import org.joshsim.engine.geometry.grid.GridCrsDefinition;
import org.joshsim.engine.geometry.grid.GridPatchBuilder;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.engine.value.type.EngineValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for ExternalGeoMapper using a real NetCDF file and real PatchSet.
 */
public class ExternalGeoMapperIntegrationTest {

  private static final String RIVERSIDE_RESOURCE_PATH = "netcdf/precip_riverside_annual_agg.nc";
  private static final String DIM_X = "lon";
  private static final String DIM_Y = "lat";
  private static final String DIM_TIME = "calendar_year";
  private static final String VAR_NAME = "Precipitation_(total)";

  private String riversideFilePath;
  private EngineValueFactory valueFactory;
  private ExternalGeoMapper mapper;
  private PatchSet patchSet;

  /**
   * Sets up the test environment by initializing the value factory, mapper, and PatchSet.
   *
   * @throws IOException if the test resource cannot be found or accessed.
   */
  @BeforeEach
  public void setUp() throws IOException {
    // Set up the value factory
    valueFactory = new EngineValueFactory();

    // Get resource path
    URL resourceUrl = getClass().getClassLoader().getResource(RIVERSIDE_RESOURCE_PATH);
    if (resourceUrl == null) {
      throw new IOException("Cannot find test resource: " + RIVERSIDE_RESOURCE_PATH);
    }
    riversideFilePath = new File(resourceUrl.getFile()).getAbsolutePath();

    mapper = new ExternalGeoMapperBuilder()
        .addCoordinateTransformer(new GridExternalCoordinateTransformer())
        .addInterpolationStrategy(new NearestNeighborInterpolationStrategy())
        .addDimensions(DIM_X, DIM_Y, DIM_TIME)
        .build();

    // Create a real PatchSet
    patchSet = createRiversidePatchSet();
    System.out.println("Created PatchSet with " + patchSet.getPatches().size() + " patches");
  }

  /**
   * Create a real PatchSet for the Riverside area.
   */
  private PatchSet createRiversidePatchSet() {
    // Define Riverside area coordinates
    BigDecimal westLon = new BigDecimal("-117.400");
    BigDecimal eastLon = new BigDecimal("-117.395");
    BigDecimal southLat = new BigDecimal("33.900");
    BigDecimal northLat = new BigDecimal("33.905");

    // Create extents for the grid - note the orientation for proper grid construction
    // In grid space, topLeft has smaller Y value than bottomRight
    PatchBuilderExtents extents = new PatchBuilderExtents(
        westLon,    // topLeftX
        southLat,   // topLeftY
        eastLon,    // bottomRightX
        northLat    // bottomRightY
    );

    // Define cell size (approximately 25m in decimal degrees)
    // At this latitude, ~0.00025 degrees is roughly 25 meters
    BigDecimal cellSize = new BigDecimal("0.00025");

    // Create GridCrsDefinition
    GridCrsDefinition gridCrs = new GridCrsDefinition(
        "RiversideGrid",  // name
        "EPSG:4326",      // baseCrsCode (WGS84)
        extents,          // extents
        cellSize,         // cellSize
        "degrees"         // cellSizeUnits
    );

    // Create prototype for patches
    EntityPrototype prototype = new RiversideEntityPrototype();

    // Build the PatchSet
    PatchBuilder builder = new GridPatchBuilder(gridCrs, prototype);
    return builder.build();
  }

  @Test
  public void testMapDataToPatchValuesWithRealPatchSet() throws IOException {
    // Get precipitation variable
    List<String> variableNames = new ArrayList<>();
    variableNames.add(VAR_NAME);

    // Execute the method under test
    Map<String, Map<Integer, Map<GeoKey, EngineValue>>> result =
        mapper.mapDataToPatchValues(riversideFilePath, variableNames, patchSet, 1);

    // Verify result contains data
    assertNotNull(result);
    assertFalse(result.isEmpty());
    assertTrue(result.containsKey(VAR_NAME));

    // Check data for precipitation variable
    Map<Integer, Map<GeoKey, EngineValue>> timeStepMaps = result.get(VAR_NAME);
    assertNotNull(timeStepMaps);
    assertEquals(1, timeStepMaps.size()); // Should have only time step 1

    // Check values for patches
    Map<GeoKey, EngineValue> patchValueMap = timeStepMaps.get(1);
    assertFalse(patchValueMap.isEmpty());

    // Print sample values
    System.out.println("Number of patches with precipitation values: " + patchValueMap.size());
    System.out.println("Sample precipitation values at time step 1:");

    int count = 0;
    for (Map.Entry<GeoKey, EngineValue> entry : patchValueMap.entrySet()) {
      System.out.println("  " + entry.getKey() + ": " + entry.getValue());
      if (++count >= 5) {
        break;
      }
    }

    // Verify we have values for most patches
    assertTrue(patchValueMap.size() > patchSet.getPatches().size() * 0.9,
            "Should have values for at least 90% of patches");
  }

  @Test
  public void testMapDataToPatchValuesWithMultipleTimeSteps() throws IOException {
    // Test with multiple time steps
    List<String> variableNames = new ArrayList<>();
    variableNames.add(VAR_NAME);

    // Execute with request for 2 time steps
    Map<String, Map<Integer, Map<GeoKey, EngineValue>>> result =
        mapper.mapDataToPatchValues(riversideFilePath, variableNames, patchSet, 0, 1);

    // Verify result contains data for 2 time steps
    assertNotNull(result);
    Map<Integer, Map<GeoKey, EngineValue>> timeStepMaps = result.get(VAR_NAME);
    assertNotNull(timeStepMaps);
    assertEquals(2, timeStepMaps.size());
    assertTrue(timeStepMaps.containsKey(0));
    assertTrue(timeStepMaps.containsKey(1));

    // Compare values between time steps
    Map<GeoKey, EngineValue> values0 = timeStepMaps.get(0);
    Map<GeoKey, EngineValue> values1 = timeStepMaps.get(1);

    // Both time steps should have values
    assertFalse(values0.isEmpty());
    assertFalse(values1.isEmpty());

    // Print some comparison values
    System.out.println("Comparison of values between time steps for same patches:");
    int count = 0;
    for (GeoKey key : values0.keySet()) {
      if (values1.containsKey(key)) {
        System.out.println("  Patch " + key + ": "
            + "Time 0 = " + values0.get(key) + ", Time 1 = " + values1.get(key));
        if (++count >= 3) {
          break;
        }
      }
    }
  }

  /**
   * Real entity prototype for creating Patch instances.
   */
  private class RiversideEntityPrototype implements EntityPrototype {
    private int patchCounter = 0;

    @Override
    public String getIdentifier() {
      return "RiversidePatch";
    }

    @Override
    public EntityType getEntityType() {
      return EntityType.PATCH;
    }

    @Override
    public MutableEntity build() {
      throw new UnsupportedOperationException("Use buildSpatial with geometry");
    }

    @Override
    public MutableEntity buildSpatial(Entity parent) {
      throw new UnsupportedOperationException("Use buildSpatial with geometry");
    }

    @Override
    public MutableEntity buildSpatial(EngineGeometry geometry) {
      String patchId = "riverside_patch_" + (++patchCounter);
      return new Patch(geometry, patchId, new HashMap<>(), new HashMap<>());
    }

    @Override
    public boolean requiresParent() {
      return false;
    }

    @Override
    public boolean requiresGeometry() {
      return true;
    }
  }
}
