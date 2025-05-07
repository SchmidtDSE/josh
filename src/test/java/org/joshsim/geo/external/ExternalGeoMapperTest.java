package org.joshsim.geo.external;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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

  /**
   * Sets up the test environment by initializing required components and resources.
   *
   * @throws IOException if the test resource file cannot be found or accessed.
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

    // Create mapper with real components
    mapper = new ExternalGeoMapperBuilder()
        .addCoordinateTransformer(new GridExternalCoordinateTransformer())
        .addInterpolationStrategy(new NearestNeighborInterpolationStrategy())
        .addDimensions(DIM_X, DIM_Y, DIM_TIME)
        .build();

    // Create a real PatchSet for testing with smallAoi
    patchSet = createRiversidePatchSet(smallAoi);

    // Initialize variable names
    variableNames = Arrays.asList(VAR_NAME);

    System.out.println("Created PatchSet with " + patchSet.getPatches().size() + " patches");
  }

  /**
   * Small area of interest (AOI) for Riverside, CA.
   * This is used to define the extents of the grid.
   */
  private static final BigDecimal[] smallAoi = {
      new BigDecimal("-117.400"), // westLon
      new BigDecimal("-117.399"), // eastLon
      new BigDecimal("33.900"),   // southLat
      new BigDecimal("33.905")    // northLat
  };

  /**
   * Medium area of interest (AOI) for Riverside, CA.
   * This is used to define the extents of the grid.
   */
  private static final BigDecimal[] mediumAoi = {
      new BigDecimal("-117.400"), // westLon
      new BigDecimal("-117.370"), // eastLon
      new BigDecimal("33.900"),   // southLat
      new BigDecimal("33.930")    // northLat
  };

  /**
   * Large area of interest (AOI) for Riverside, CA.
   * This is used to define the extents of the grid.
   */
  private static final BigDecimal[] largeAoi = {
      new BigDecimal("-117.400"), // westLon
      new BigDecimal("-117.300"), // eastLon
      new BigDecimal("33.900"),   // southLat
      new BigDecimal("34.000")    // northLat
  };


  /**
   * Create a real PatchSet for the Riverside area with specified AOI.
   */
  private PatchSet createRiversidePatchSet(BigDecimal[] aoi) {
    // Define the area of interest (AOI) for Riverside, CA
    BigDecimal westLon = aoi[0];
    BigDecimal eastLon = aoi[1];
    BigDecimal southLat = aoi[2];
    BigDecimal northLat = aoi[3];

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
   * Helper method to execute mapping with given parameters.
   * This allows reuse between tests without duplication.
   */
  private Map<String, Map<Integer, Map<GeoKey, EngineValue>>> executeMapping(
      boolean useParallel, int minTimestep, int maxTimestep) throws IOException {

    // Configure parallel processing
    mapper.setUseParallelProcessing(useParallel);

    // Execute the method
    return mapper.mapDataToPatchValues(
        riversideFilePath, variableNames, patchSet, minTimestep, maxTimestep);
  }

  /**
   * Helper method to execute mapping with a custom patch set.
   */
  private Map<String, Map<Integer, Map<GeoKey, EngineValue>>> executeWithPatchSet(
      PatchSet customPatchSet, boolean useParallel, int minTimestep, int maxTimestep)
      throws IOException {

    // Configure parallel processing
    mapper.setUseParallelProcessing(useParallel);

    // Execute the method with the specified patch set
    return mapper.mapDataToPatchValues(
        riversideFilePath, variableNames, customPatchSet, minTimestep, maxTimestep);
  }

  /**
   * Helper method to validate standard expectations for mapping results.
   */
  private void validateMappingResults(
      Map<String, Map<Integer, Map<GeoKey, EngineValue>>> result,
      int expectedTimeSteps) {

    assertNotNull(result);
    assertEquals(1, result.size());

    Map<Integer, Map<GeoKey, EngineValue>> timeStepMaps = result.get(VAR_NAME);
    assertEquals(expectedTimeSteps, timeStepMaps.size(),
        String.format("Should have %d time steps", expectedTimeSteps));

    // Validate each time step has data
    for (Map.Entry<Integer, Map<GeoKey, EngineValue>> entry : timeStepMaps.entrySet()) {
      int timeStep = entry.getKey();
      Map<GeoKey, EngineValue> values = entry.getValue();

      assertFalse(values.isEmpty(), "Should have values for time step " + timeStep);
      assertTrue(values.size() > patchSet.getPatches().size() * 0.9,
          "Should have values for at least 90% of patches at time step " + timeStep);
    }
  }

  /**
   * Helper method to validate standard expectations for mapping results with a custom patch set.
   */
  private void validateMappingResultsWithCustomPatchSet(
      Map<String, Map<Integer, Map<GeoKey, EngineValue>>> result,
      int expectedTimeSteps,
      PatchSet customPatchSet) {

    assertNotNull(result);
    assertEquals(1, result.size());

    Map<Integer, Map<GeoKey, EngineValue>> timeStepMaps = result.get(VAR_NAME);
    assertEquals(expectedTimeSteps, timeStepMaps.size(),
        String.format("Should have %d time steps", expectedTimeSteps));

    // Validate each time step has data
    for (Map.Entry<Integer, Map<GeoKey, EngineValue>> entry : timeStepMaps.entrySet()) {
      int timeStep = entry.getKey();
      Map<GeoKey, EngineValue> values = entry.getValue();

      assertFalse(values.isEmpty(), "Should have values for time step " + timeStep);
      assertTrue(values.size() > customPatchSet.getPatches().size() * 0.9,
          "Should have values for at least 90% of patches at time step " + timeStep);
    }
  }

  /**
   * Test mapping data to patch values with specific variables.
   */
  @Test
  public void testMapDataToPatchValuesWithSpecifiedVariables() throws IOException {
    // Execute with time range 0 to 1 (2 timesteps)
    Map<String, Map<Integer, Map<GeoKey, EngineValue>>> result =
        executeMapping(false, 0, 1);

    // Verify results
    validateMappingResults(result, 2);
  }

  /**
   * Test with empty variable list (should fetch all variables).
   */
  @Test
  public void testMapDataToPatchValuesWithEmptyVariableList() throws IOException {
    // Execute with empty variable list - should get all variables from reader
    mapper.setUseParallelProcessing(false);
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
   * Test requesting all available time steps.
   */
  @Test
  public void testMapDataToPatchValuesWithAllTimeSteps() throws IOException {
    // Execute with -1 for maxTimestep (all available)
    Map<String, Map<Integer, Map<GeoKey, EngineValue>>> result =
        executeMapping(false, -1, -1);

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
   * Test requesting a specific time range.
   */
  @Test
  public void testMapDataToPatchValuesWithSpecificTimeRange() throws IOException {
    // Execute with specific time range (1 to 2 inclusive)
    Map<String, Map<Integer, Map<GeoKey, EngineValue>>> result =
        executeMapping(false, 1, 2);

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
   * Test handling of invalid file path.
   */
  @Test
  public void testMapDataToPatchValuesHandlesInvalidFilepath() {
    // Verify exception is thrown for invalid file path
    mapper.setUseParallelProcessing(false);
    assertThrows(IOException.class, () ->
        mapper.mapDataToPatchValues(INVALID_RESOURCE_PATH, variableNames, patchSet, 0, 1));

    // Should also fail with parallel processing
    mapper.setUseParallelProcessing(true);
    assertThrows(IOException.class, () ->
        mapper.mapDataToPatchValues(INVALID_RESOURCE_PATH, variableNames, patchSet, 0, 1));
  }

  /**
   * Test that parallel and sequential processing produce the same results.
   */
  @Test
  public void testParallelVsSequentialProcessing() throws IOException {
    // Execute with both sequential and parallel processing with time steps 0 to 2
    Map<String, Map<Integer, Map<GeoKey, EngineValue>>> seqResults =
        executeMapping(false, 0, 2);
    Map<String, Map<Integer, Map<GeoKey, EngineValue>>> parResults =
        executeMapping(true, 0, 2);

    // Validate both approaches produce expected results
    validateMappingResults(seqResults, 3);
    validateMappingResults(parResults, 3);

    // Compare results from both approaches
    assertEquals(seqResults.keySet(), parResults.keySet(),
        "Results should have same variable names");

    for (String varName : seqResults.keySet()) {
      Map<Integer, Map<GeoKey, EngineValue>> seqTimeSteps = seqResults.get(varName);
      Map<Integer, Map<GeoKey, EngineValue>> parTimeSteps = parResults.get(varName);

      assertEquals(seqTimeSteps.keySet(), parTimeSteps.keySet(),
          "Results should have same time steps");

      for (Integer timeStep : seqTimeSteps.keySet()) {
        Map<GeoKey, EngineValue> seqValues = seqTimeSteps.get(timeStep);
        Map<GeoKey, EngineValue> parValues = parTimeSteps.get(timeStep);

        // Use Set comparison instead of direct equals to ignore order
        assertEquals(seqValues.keySet().size(), parValues.keySet().size(),
            "Results should have same number of patch keys for time step " + timeStep);
        assertTrue(seqValues.keySet().containsAll(parValues.keySet()),
            "Sequential results should contain all parallel result keys");
        assertTrue(parValues.keySet().containsAll(seqValues.keySet()),
            "Parallel results should contain all sequential result keys");

        // Now check that the values match for each key
        for (GeoKey key : seqValues.keySet()) {
          assertEquals(seqValues.get(key), parValues.get(key),
              "Values should match for patch " + key + " at time step " + timeStep);
        }
      }
    }
  }

  /**
   * Test parallel performance with a slightly larger workload.
   */
  @Test
  public void testParallelProcessingPerformance() throws IOException {
    // Set up a performance test with more time steps to better observe
    // potential parallel processing benefits
    int testTimeSteps = 15; // More time steps than other tests, but not full dataset

    // Warm up the JVM first to avoid measuring JIT compilation time
    executeMapping(false, 0, 0);

    // Test sequential performance
    long seqStart = System.nanoTime();
    Map<String, Map<Integer, Map<GeoKey, EngineValue>>> seqResult =
        executeMapping(false, 0, testTimeSteps - 1);
    long seqDuration = System.nanoTime() - seqStart;

    // Test parallel performance
    long parStart = System.nanoTime();
    Map<String, Map<Integer, Map<GeoKey, EngineValue>>> parResult =
        executeMapping(true, 0, testTimeSteps - 1);
    long parDuration = System.nanoTime() - parStart;

    // Verify both produced the same results
    validateMappingResults(seqResult, testTimeSteps);
    validateMappingResults(parResult, testTimeSteps);

    // Log performance metrics
    System.out.printf("Sequential: %.2f ms, Parallel: %.2f ms%n",
        seqDuration / 1_000_000.0, parDuration / 1_000_000.0);
  }

  /*
   * Compare the performance of parallel processing with different size AOIs.
   * This test only uses parallel processing since sequential would be too slow for larger AOIs.
   */
  /*@Test
  public void testParallelProcessingWithDifferentAoiSizes() throws IOException {
    // Ensure parallel processing is enabled
    mapper.setUseParallelProcessing(true);

    // Time steps to process (keep small to avoid test taking too long)
    final int testTimeSteps = 2;

    // Create PatchSets with different AOI sizes
    PatchSet smallPatchSet = createRiversidePatchSet(smallAoi);
    PatchSet mediumPatchSet = createRiversidePatchSet(mediumAoi);
    // PatchSet largePatchSet = createRiversidePatchSet(largeAoi);

    System.out.println("Small AOI has " + smallPatchSet.getPatches().size() + " patches");
    System.out.println("Medium AOI has " + mediumPatchSet.getPatches().size() + " patches");
    // System.out.println("Large AOI has " + largePatchSet.getPatches().size() + " patches");

    // Warm up the JVM first
    executeWithPatchSet(smallPatchSet, true, 0, 0);

    // Test with small AOI
    long smallStart = System.nanoTime();
    Map<String, Map<Integer, Map<GeoKey, EngineValue>>> smallResults =
        executeWithPatchSet(smallPatchSet, true, 0, testTimeSteps - 1);
    final long smallDuration = System.nanoTime() - smallStart;
    validateMappingResultsWithCustomPatchSet(smallResults, testTimeSteps, smallPatchSet);

    // Test with medium AOI
    long mediumStart = System.nanoTime();
    Map<String, Map<Integer, Map<GeoKey, EngineValue>>> mediumResults =
        executeWithPatchSet(mediumPatchSet, true, 0, testTimeSteps - 1);
    final long mediumDuration = System.nanoTime() - mediumStart;
    validateMappingResultsWithCustomPatchSet(mediumResults, testTimeSteps, mediumPatchSet);

    // // Test with large AOI
    // long largeStart = System.nanoTime();
    // Map<String, Map<Integer, Map<GeoKey, EngineValue>>> largeResults =
    //     executeWithPatchSet(largePatchSet, true, 0, testTimeSteps - 1);
    // final long largeDuration = System.nanoTime() - largeStart;
    // validateMappingResultsWithCustomPatchSet(largeResults, testTimeSteps, largePatchSet);

    // Log performance metrics
    System.out.println("AOI Size Comparison (Parallel Processing):");
    System.out.printf("Small AOI: %.2f ms (%d patches)%n",
        smallDuration / 1_000_000.0, smallPatchSet.getPatches().size());
    System.out.printf("Medium AOI: %.2f ms (%d patches)%n",
        mediumDuration / 1_000_000.0, mediumPatchSet.getPatches().size());
    // System.out.printf("Large AOI: %.2f ms (%d patches)%n",
    //     largeDuration / 1_000_000.0, largePatchSet.getPatches().size());

    // Optional: Calculate and log throughput metrics (patches processed per second)
    double smallThroughput = smallPatchSet.getPatches().size() / (smallDuration / 1_000_000_000.0);
    double mediumThroughput =
        mediumPatchSet.getPatches().size() / (mediumDuration / 1_000_000_000.0);
    // double largeThroughput = largePatchSet.getPatches().size() /
    //     (largeDuration / 1_000_000_000.0);

    System.out.println("Throughput Comparison (patches/second):");
    System.out.printf("Small AOI: %.2f patches/second%n", smallThroughput);
    System.out.printf("Medium AOI: %.2f patches/second%n", mediumThroughput);
    //   System.out.printf("Large AOI: %.2f patches/second%n", largeThroughput);
  }*/

  /**
   * Test basic streaming functionality for a single variable and time step.
   */
  @Test
  public void testStreamVariableTimeStepToPatches() throws IOException {
    // Execute streaming method
    int timeStep = 0;

    try (Stream<Map.Entry<GeoKey, EngineValue>> stream = mapper.streamVariableTimeStepToPatches(
        riversideFilePath, VAR_NAME, timeStep, patchSet)) {

      Map<GeoKey, EngineValue> patchValueMap = stream.collect(Collectors.toMap(
          Map.Entry::getKey,
          Map.Entry::getValue
      ));

      // Verify we got values
      assertFalse(patchValueMap.isEmpty(), "Should have values for patches");
      assertTrue(patchValueMap.size() > patchSet.getPatches().size() * 0.9,
          "Should have values for at least 90% of patches");
    }
  }

  /**
   * Test resource cleanup by intentionally trying to use a stream after it's closed.
   */
  @Test
  public void testStreamResourceCleanup() throws IOException {
    // Get a stream
    Stream<Map.Entry<GeoKey, EngineValue>> stream = mapper.streamVariableTimeStepToPatches(
        riversideFilePath, VAR_NAME, 0, patchSet);

    // Close it
    stream.close();

    // Verify it throws exception when we try to use it after closing
    assertThrows(IllegalStateException.class, () -> {
      stream.findFirst();
    });
  }

  /**
   * Test parallel streaming produces correct results.
   */
  @Test
  public void testParallelStreaming() throws IOException {
    // Configure mapper for parallel processing
    mapper.setUseParallelProcessing(true);
    int timeStep = 0;

    // Execute with parallel stream
    try (Stream<Map.Entry<GeoKey, EngineValue>> stream = mapper.streamVariableTimeStepToPatches(
        riversideFilePath, VAR_NAME, timeStep, patchSet)) {

      Map<GeoKey, EngineValue> patchValueMap = stream.collect(Collectors.toMap(
          Map.Entry::getKey,
          Map.Entry::getValue
      ));

      // Verify we got values
      assertFalse(patchValueMap.isEmpty(), "Should have values for patches");
      assertTrue(patchValueMap.size() > patchSet.getPatches().size() * 0.9,
          "Should have values for at least 90% of patches");
    }

    // Compare with sequential results
    mapper.setUseParallelProcessing(false);

    try (Stream<Map.Entry<GeoKey, EngineValue>> stream = mapper.streamVariableTimeStepToPatches(
        riversideFilePath, VAR_NAME, timeStep, patchSet)) {

      Map<GeoKey, EngineValue> seqPatchValueMap = stream.collect(Collectors.toMap(
          Map.Entry::getKey,
          Map.Entry::getValue
      ));

      // Execute with parallel processing again to compare results
      mapper.setUseParallelProcessing(true);

      try (Stream<Map.Entry<GeoKey, EngineValue>> parStream =
          mapper.streamVariableTimeStepToPatches(
          riversideFilePath, VAR_NAME, timeStep, patchSet
      )) {

        Map<GeoKey, EngineValue> parPatchValueMap = parStream.collect(Collectors.toMap(
            Map.Entry::getKey,
            Map.Entry::getValue
        ));

        // Compare sequential and parallel results
        assertEquals(seqPatchValueMap.keySet(), parPatchValueMap.keySet(),
            "Sequential and parallel results should have the same keys");

        for (GeoKey key : seqPatchValueMap.keySet()) {
          assertEquals(seqPatchValueMap.get(key), parPatchValueMap.get(key),
              "Values should match for key " + key);
        }
      }
    }
  }

  /**
   * Test lazy evaluation of stream by processing only a subset of patches.
   */
  @Test
  public void testLazyEvaluationOfStream() throws IOException {
    int timeStep = 0;

    // Measure time to find first 10 values that meet a condition
    long start = System.nanoTime();

    try (Stream<Map.Entry<GeoKey, EngineValue>> stream = mapper.streamVariableTimeStepToPatches(
        riversideFilePath, VAR_NAME, timeStep, patchSet)) {

      List<Map.Entry<GeoKey, EngineValue>> firstTen = stream
          .filter(entry -> {
            try {
              // Apply some filter condition (accessing the value will trigger interpolation)
              BigDecimal value = entry.getValue().getAsDecimal();
              BigDecimal threshold = new BigDecimal(0.0);
              return value.compareTo(threshold) > 0;
            } catch (Exception e) {
              return false;
            }
          })
          .limit(10) // Only process until we find 10 matching entries
          .collect(Collectors.toList());

      long duration = System.nanoTime() - start;

      // Verify we got some results
      assertFalse(firstTen.isEmpty(), "Should find at least some entries");
      assertTrue(firstTen.size() <= 10, "Should not process more than requested limit");

      System.out.printf("Time to find first 10 matching entries: %.2f ms%n",
          duration / 1_000_000.0);
    }

    // Compare with eager evaluation time (processing all patches)
    start = System.nanoTime();

    try (Stream<Map.Entry<GeoKey, EngineValue>> stream = mapper.streamVariableTimeStepToPatches(
        riversideFilePath, VAR_NAME, timeStep, patchSet)) {

      List<Map.Entry<GeoKey, EngineValue>> all = stream
          .filter(entry -> {
            try {
              BigDecimal value = entry.getValue().getAsDecimal();
              BigDecimal threshold = new BigDecimal(0.0);
              return value.compareTo(threshold) > 0;
            } catch (Exception e) {
              return false;
            }
          })
          .collect(Collectors.toList());

      long fullDuration = System.nanoTime() - start;

      System.out.printf("Time to process all entries: %.2f ms%n",
          fullDuration / 1_000_000.0);
    }
  }

  /**
   * Test filtered streaming to demonstrate processing efficiency.
   *
   * @throws Exception if an error occurs during processing.
   */
  @Test
  public void testFilteredStreamProcessing() throws Exception {
    // Define a threshold value for filtering
    final BigDecimal threshold = new BigDecimal(300.0);  // Filter for precipitation above value

    // Process multiple time steps
    try (ExternalDataReader reader = ExternalDataReaderFactory.createReader(riversideFilePath)) {
      reader.open(riversideFilePath);
      reader.setDimensions(DIM_X, DIM_Y, Optional.ofNullable(DIM_TIME));

      int timeSteps = reader.getTimeDimensionSize().orElse(30);
      ExternalSpatialDimensions dimensions = reader.getSpatialDimensions();

      // Count high precipitation patches at each time step
      System.out.println("Time steps with high precipitation (>" + threshold + " mm):");

      for (int t = 0; t < Math.min(timeSteps, 10); t++) {  // Limit to first 10 time steps
        // Stream just this time step
        try (Stream<Map.Entry<GeoKey, EngineValue>> stream = mapper.streamVariableTimeStepToPatches(
            reader, riversideFilePath, VAR_NAME, t, dimensions, patchSet)) {

          long highPrecipCount = stream
              .filter(entry -> {
                try {
                  BigDecimal value = entry.getValue().getAsDecimal();
                  return value.compareTo(threshold) > 0.0;
                } catch (Exception e) {
                  return false;
                }
              })
              .count();

          System.out.printf("Time step %d: %d patches with precipitation > %.1f mm%n",
              t, highPrecipCount, threshold);
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
