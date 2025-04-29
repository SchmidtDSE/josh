package org.joshsim.geo.external.readers;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.engine.value.type.EngineValue;
import org.joshsim.geo.external.ExternalSpatialDimensions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Integration test that simulates streaming over patches and building a map of GeoKey 
 * to EngineValue. This test demonstrates the expected usage pattern described in the requirements.
 */
public class NetcdfPatchStreamingTest {
  
  private static final String TEST_FILE =
      "/workspaces/josh/assets/test/netcdf/MISR_AM1_JOINT_AS_FEB_2022_F02_0002.nc";
  
  /**
   * Simple mock class to represent a geographic patch with a center point.
   */
  static class MockPatch {
    private final BigDecimal centerX;
    private final BigDecimal centerY;
    
    public MockPatch(BigDecimal centerX, BigDecimal centerY) {
      this.centerX = centerX;
      this.centerY = centerY;
    }
    
    public BigDecimal getCenterX() { return centerX; }
    public BigDecimal getCenterY() { return centerY; }
  }
  
  /**
   * Simple mock class to represent a GeoKey (patch identifier).
   */
  static class GeoKey {
    private final String patchId;
    
    public GeoKey(String patchId) {
      this.patchId = patchId;
    }
    
    @Override
    public int hashCode() {
      return patchId.hashCode();
    }
    
    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null || getClass() != obj.getClass()) return false;
      GeoKey other = (GeoKey) obj;
      return patchId.equals(other.patchId);
    }
  }
  
  /**
   * Test that demonstrates how to process a collection of patches and build up the required map.
   */
  @Test
  public void testProcessPatchesIntoMap() throws IOException {
    // Create a real EngineValueFactory for this integration test
    EngineValueFactory valueFactory = new EngineValueFactory();
    
    try (NetcdfExternalDataReader reader = new NetcdfExternalDataReader(valueFactory)) {
      reader.open(TEST_FILE);
      assertTrue(reader.detectSpatialDimensions(), "Should detect dimensions");
      
      // Get spatial dimensions and variables from the file
      ExternalSpatialDimensions dims = reader.getSpatialDimensions();
      List<String> variables = reader.getVariableNames();
      
      // Create some mock patches based on the actual coordinate space in the file
      List<BigDecimal> coordsX = dims.getCoordinatesX();
      List<BigDecimal> coordsY = dims.getCoordinatesY();
      List<MockPatch> patches = createMockPatches(coordsX, coordsY);
      
      // Create our result map: Map<variable, Map<geoKey, value>>
      Map<String, Map<GeoKey, EngineValue>> resultMap = new HashMap<>();
      
      // Process each variable
      for (String variable : variables) {
        Map<GeoKey, EngineValue> variableValues = new HashMap<>();
        
        // Process each patch for this variable
        for (int i = 0; i < patches.size(); i++) {
          MockPatch patch = patches.get(i);
          GeoKey geoKey = new GeoKey("patch_" + i);
          
          // Try to read a value at the patch center
          Optional<EngineValue> value = reader.readValueAt(
              variable, patch.getCenterX(), patch.getCenterY(), 0);
          
          // Add to map if value exists
          value.ifPresent(v -> variableValues.put(geoKey, v));
        }
        
        // Add to main result map
        resultMap.put(variable, variableValues);
      }
      
      // Verify results
      assertFalse(resultMap.isEmpty(), "Result map should not be empty");
      
      // Check that every variable has some values
      for (String variable : variables) {
        assertTrue(resultMap.containsKey(variable), "Result map should contain " + variable);
        Map<GeoKey, EngineValue> varValues = resultMap.get(variable);
        
        // There should be at least some patches with values
        assertFalse(varValues.isEmpty(), "Should have at least some values for " + variable);
        
        // Print some statistics
        System.out.printf("Variable %s has values for %d out of %d patches%n", 
            variable, varValues.size(), patches.size());
      }
    }
  }
  
  /**
   * Creates mock patches based on the coordinate space.
   */
  private List<MockPatch> createMockPatches(
        List<BigDecimal> coordsX, List<BigDecimal> coordsY) {
    List<MockPatch> patches = new java.util.ArrayList<>();
    
    // Create a limited number of patches for testing
    // In a real scenario, these would be provided by the application
    int stepX = Math.max(1, coordsX.size() / 5);
    int stepY = Math.max(1, coordsY.size() / 5);
    
    for (int x = 0; x < coordsX.size(); x += stepX) {
      for (int y = 0; y < coordsY.size(); y += stepY) {
        patches.add(new MockPatch(coordsX.get(x), coordsY.get(y)));
      }
    }
    
    return patches;
  }
}