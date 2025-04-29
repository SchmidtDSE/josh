package org.joshsim.geo.external.readers;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.engine.value.type.EngineValue;
import org.joshsim.geo.external.ExternalSpatialDimensions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class NetcdfValueReadingTest {

  private static final String FEB_FILE = "/workspaces/josh/assets/test/netcdf/MISR_AM1_JOINT_AS_FEB_2022_F02_0002.nc";
  private static final String MAR_FILE = "/workspaces/josh/assets/test/netcdf/MISR_AM1_JOINT_AS_MAR_2022_F02_0002.nc";
  
  private NetcdfExternalDataReader reader;
  private EngineValueFactory valueFactory;
  
  @BeforeEach
  public void setUp() {
    // Use the actual implementation instead of a mock
    valueFactory = new EngineValueFactory(); // Replace with your actual implementation
    reader = new NetcdfExternalDataReader(valueFactory);
  }
  
  @AfterEach
  public void tearDown() throws IOException {
    reader.close();
  }
  
  static Stream<Arguments> fileAndCoordinateProvider() throws IOException {
    return Stream.of(
      Arguments.of(FEB_FILE, 0),
      Arguments.of(MAR_FILE, 0)
    );
  }
  
  @ParameterizedTest
  @MethodSource("fileAndCoordinateProvider")
  public void testReadingValuesAtVariousPoints(String filePath, int timeStep) throws IOException {
    reader.open(filePath);
    reader.detectSpatialDimensions();
    
    ExternalSpatialDimensions dims = reader.getSpatialDimensions();
    List<String> variables = reader.getVariableNames();
    
    System.out.println("Testing file: " + filePath);
    System.out.println("Variables: " + variables);
    
    // Try 3 different points in the grid for the first variable
    if (!variables.isEmpty()) {
      String variable = variables.get(0);
      
      List<BigDecimal> coordsX = dims.getCoordinatesX();
      List<BigDecimal> coordsY = dims.getCoordinatesY();
      
      int xSize = coordsX.size();
      int ySize = coordsY.size();
      
      // Test points at beginning, middle, and end of grid
      assertReadValueSucceedsWithRealData(variable, coordsX.get(0), coordsY.get(0), timeStep);
      assertReadValueSucceedsWithRealData(variable, coordsX.get(xSize/2), coordsY.get(ySize/2), timeStep);
      assertReadValueSucceedsWithRealData(variable, coordsX.get(xSize-1), coordsY.get(ySize-1), timeStep);
    }
  }
  
  private void assertReadValueSucceedsWithRealData(String variable, BigDecimal x, BigDecimal y, int timeStep) throws IOException {
    System.out.printf("Reading value for var=%s at x=%s, y=%s, time=%d%n", 
        variable, x.toPlainString(), y.toPlainString(), timeStep);
        
    Optional<EngineValue> value = reader.readValueAt(variable, x, y, timeStep);
    assertTrue(value.isPresent(), "Should return a value at valid coordinates");
    
    // Now actually validate the value instead of just checking presence
    EngineValue engineValue = value.get();
    assertNotNull(engineValue);
    
    // Print the actual value for debugging
    System.out.println("Value read: " + engineValue);
    
    // Add assertions to validate expected ranges or other properties
    // Example (uncomment and adjust as needed):
    // assertNotNull(engineValue.getUnits(), "Units should not be null");
    // assertTrue(engineValue.getValue().compareTo(BigDecimal.ZERO) >= 0, "Value should be non-negative");
  }
}