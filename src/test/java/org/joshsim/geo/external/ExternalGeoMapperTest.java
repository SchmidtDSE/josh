package org.joshsim.geo.external;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
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
 * Tests for the ExternalGeoMapper class using real components.
 */
public class ExternalGeoMapperTest {

  private static final String RIVERSIDE_RESOURCE_PATH = "netcdf/precip_riverside_annual_agg.nc";
  private static final String INVALID_RESOURCE_PATH = "invalid/path/to/resource.nc";
  private static final String DIM_X = "lon";
  private static final String DIM_Y = "lat";
  private static final String DIM_TIME = "calendar_year";
  private static final String VAR_NAME = "Precipitation_(total)";
  
  private String riversideFilePath;
  private EngineValueFactory valueFactory;
  private ExternalGeoMapper mapper;
  private PatchSet patchSet;
  private List<String> variableNames;
  
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
    
    // Create mapper with real components
    mapper = new ExternalGeoMapperBuilder()
        .addCoordinateTransformer(new GridExternalCoordinateTransformer())
        .addInterpolationStrategy(new NearestNeighborInterpolationStrategy())
        .addDimensions(DIM_X, DIM_Y, DIM_TIME)
        .build();
    
    // Create a real PatchSet for testing
    patchSet = createRiversidePatchSet();
    
    // Initialize variable names
    variableNames = Arrays.asList(VAR_NAME);
    
    System.out.println("Created PatchSet with " + patchSet.getPatches().size() + " patches");
  }
  
  /**
   * Create a real PatchSet for the Riverside area
   */
  private PatchSet createRiversidePatchSet() {
    // Define Riverside area coordinates
    BigDecimal westLon = new BigDecimal("-117.400");
    BigDecimal eastLon = new BigDecimal("-117.395");
    BigDecimal southLat = new BigDecimal("33.900");
    BigDecimal northLat = new BigDecimal("33.905");
    
    // Create extents for the grid
    PatchBuilderExtents extents = new PatchBuilderExtents(
        westLon,    // topLeftX
        southLat,   // topLeftY 
        eastLon,    // bottomRightX
        northLat    // bottomRightY
    );
    
    // Define cell size (approximately 25m in decimal degrees)
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

  /**
   * Test mapping data to patch values with specific variables.
   */
  @Test
  public void testMapDataToPatchValues_WithSpecifiedVariables() throws IOException {
    // Execute the method with time range 0 to 1 (2 timesteps)
    Map<String, Map<Integer, Map<GeoKey, EngineValue>>> result =
        mapper.mapDataToPatchValues(riversideFilePath, variableNames, patchSet, 0, 1);

    // Verify results
    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals(2, result.get(VAR_NAME).size());  // 2 time steps
    
    // Check that we have values for patches
    Map<GeoKey, EngineValue> timeStep0Values = result.get(VAR_NAME).get(0);
    Map<GeoKey, EngineValue> timeStep1Values = result.get(VAR_NAME).get(1);
    
    assertFalse(timeStep0Values.isEmpty(), "Should have values for time step 0");
    assertFalse(timeStep1Values.isEmpty(), "Should have values for time step 1");
    
    // Verify we have values for most patches
    assertTrue(timeStep0Values.size() > patchSet.getPatches().size() * 0.9,
            "Should have values for at least 90% of patches at time step 0");
  }
  
  /**
   * Test with empty variable list (should fetch all variables)
   */
  @Test
  public void testMapDataToPatchValues_WithEmptyVariableList() throws IOException {
    // Execute with empty variable list - should get all variables from reader
    Map<String, Map<Integer, Map<GeoKey, EngineValue>>> result =
        mapper.mapDataToPatchValues(riversideFilePath, List.of(), patchSet, 0, 1);

    // Verify results
    assertNotNull(result);
    assertEquals(1, result.size());  // Should have map for one variable (precipitation)
    assertTrue(result.containsKey(VAR_NAME), "Should contain the precipitation variable");
    
    // Verify data for the discovered variable
    Map<Integer, Map<GeoKey, EngineValue>> timeStepMaps = result.get(VAR_NAME);
    assertEquals(2, timeStepMaps.size(), "Should have two time steps");
    
    // Check data at each time step
    assertFalse(timeStepMaps.get(0).isEmpty(), "Should have data for time step 0");
    assertFalse(timeStepMaps.get(1).isEmpty(), "Should have data for time step 1");
  }
  
  /**
   * Test requesting all available time steps
   */
  @Test
  public void testMapDataToPatchValues_WithAllTimeSteps() throws IOException {
    // Execute with -1 for maxTimestep (all available)
    Map<String, Map<Integer, Map<GeoKey, EngineValue>>> result =
        mapper.mapDataToPatchValues(riversideFilePath, variableNames, patchSet, -1);

    // Verify results - should process all available timesteps
    assertNotNull(result);
    assertEquals(1, result.size());  // Should have map for one variable
    Map<Integer, Map<GeoKey, EngineValue>> timeStepMaps = result.get(VAR_NAME);
    
    // The dataset has 30 time steps
    assertEquals(30, timeStepMaps.size(), "Should contain all 30 time steps");
    
    // Check for data at a few sample time steps
    assertTrue(timeStepMaps.containsKey(0), "Should have data for time step 0");
    assertTrue(timeStepMaps.containsKey(15), "Should have data for time step 15");
    assertTrue(timeStepMaps.containsKey(29), "Should have data for time step 29");
  }
  
  /**
   * Test requesting a specific time range
   */
  @Test
  public void testMapDataToPatchValues_WithSpecificTimeRange() throws IOException {    
    // Execute with specific time range (1 to 2 inclusive)
    Map<String, Map<Integer, Map<GeoKey, EngineValue>>> result =
        mapper.mapDataToPatchValues(riversideFilePath, variableNames, patchSet, 1, 2);

    // Verify results - should process timesteps 1 and 2 (total of 2 steps)
    assertNotNull(result);
    assertEquals(1, result.size());
    Map<Integer, Map<GeoKey, EngineValue>> timeStepMaps = result.get(VAR_NAME);
    assertEquals(2, timeStepMaps.size());
    
    // Verify the specific keys are present
    assertTrue(timeStepMaps.containsKey(1), "Should contain time step 1");
    assertTrue(timeStepMaps.containsKey(2), "Should contain time step 2");
    assertFalse(timeStepMaps.containsKey(0), "Should not contain time step 0");
    assertFalse(timeStepMaps.containsKey(3), "Should not contain time step 3");
  }

  /**
   * Test handling of invalid file path
   */
  @Test
  public void testMapDataToPatchValues_HandlesInvalidFilepath() {
    // Verify exception is thrown for invalid file path
    assertThrows(IOException.class, () -> 
        mapper.mapDataToPatchValues(INVALID_RESOURCE_PATH, variableNames, patchSet, 0, 1));
  }
  
  /**
   * Real entity prototype for creating Patch instances
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