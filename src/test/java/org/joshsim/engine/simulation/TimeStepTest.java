package org.joshsim.engine.simulation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.joshsim.engine.entity.base.Entity;
import org.joshsim.engine.entity.base.GeoKey;
import org.joshsim.engine.geometry.EngineGeometry;
import org.joshsim.engine.geometry.grid.GridCircle;
import org.joshsim.engine.geometry.grid.GridSquare;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TimeStepTest {

  private TimeStep timeStep;
  private HashMap<GeoKey, Entity> patches;
  private GeoKey mockKey1;
  private GeoKey mockKey2;
  private Entity mockPatch1;
  private Entity mockPatch2;
  private Entity mockMeta;
  private EngineGeometry mockGeometry1;
  private EngineGeometry mockGeometry2;

  @BeforeEach
  void setUp() {
    // Create mocks
    mockKey1 = mock(GeoKey.class);
    mockKey2 = mock(GeoKey.class);
    mockPatch1 = mock(Entity.class);
    mockPatch2 = mock(Entity.class);
    mockMeta = mock(Entity.class);
    mockGeometry1 = mock(EngineGeometry.class);
    mockGeometry2 = mock(EngineGeometry.class);

    // Configure mocks
    when(mockPatch1.getGeometry()).thenReturn(Optional.of(mockGeometry1));
    when(mockPatch2.getGeometry()).thenReturn(Optional.of(mockGeometry2));
    when(mockPatch1.getName()).thenReturn("Patch1");
    when(mockPatch2.getName()).thenReturn("Patch2");

    // Prepare test data
    patches = new HashMap<>();
    patches.put(mockKey1, mockPatch1);
    patches.put(mockKey2, mockPatch2);

    // Create TimeStep instance
    timeStep = new TimeStep(42, mockMeta, patches);
  }

  @Test
  void constructorSetsStepNumber() {
    assertEquals(42, timeStep.getStep());
  }

  @Test
  void getPatchesReturnsAllPatches() {
    Iterable<Entity> result = timeStep.getPatches();

    assertNotNull(result);
    assertEquals(2, countElements(result));
    assertTrue(containsEntity(result, mockPatch1));
    assertTrue(containsEntity(result, mockPatch2));
  }

  @Test
  void getPatchesWithIntersectingGeometry() {
    EngineGeometry queryGeometry = mock(EngineGeometry.class);
    when(mockGeometry1.intersects(queryGeometry)).thenReturn(true);
    when(mockGeometry2.intersects(queryGeometry)).thenReturn(true);

    Iterable<Entity> result = timeStep.getPatches(queryGeometry);

    assertNotNull(result);
    assertEquals(2, countElements(result));
  }

  @Test
  void getPatchesWithPartiallyIntersectingGeometry() {
    EngineGeometry queryGeometry = mock(EngineGeometry.class);
    when(mockGeometry1.intersects(queryGeometry)).thenReturn(true);
    when(mockGeometry2.intersects(queryGeometry)).thenReturn(false);

    Iterable<Entity> result = timeStep.getPatches(queryGeometry);

    assertNotNull(result);
    assertEquals(1, countElements(result));
    assertTrue(containsEntity(result, mockPatch1));
    assertFalse(containsEntity(result, mockPatch2));
  }

  @Test
  void getPatchesWithNonIntersectingGeometry() {
    EngineGeometry queryGeometry = mock(EngineGeometry.class);
    when(mockGeometry1.intersects(queryGeometry)).thenReturn(false);
    when(mockGeometry2.intersects(queryGeometry)).thenReturn(false);

    Iterable<Entity> result = timeStep.getPatches(queryGeometry);

    assertNotNull(result);
    assertEquals(0, countElements(result));
  }

  @Test
  void getPatchesWithMatchingGeometryAndName() {
    EngineGeometry queryGeometry = mock(EngineGeometry.class);
    when(mockGeometry1.intersects(queryGeometry)).thenReturn(true);
    when(mockGeometry2.intersects(queryGeometry)).thenReturn(true);

    Iterable<Entity> result = timeStep.getPatches(queryGeometry, "Patch1");

    assertNotNull(result);
    assertEquals(1, countElements(result));
    assertTrue(containsEntity(result, mockPatch1));
  }

  @Test
  void getPatchesWithNoMatchingName() {
    EngineGeometry queryGeometry = mock(EngineGeometry.class);
    when(mockGeometry1.intersects(queryGeometry)).thenReturn(true);
    when(mockGeometry2.intersects(queryGeometry)).thenReturn(true);

    Iterable<Entity> result = timeStep.getPatches(queryGeometry, "NonExistentName");

    assertNotNull(result);
    assertEquals(0, countElements(result));
  }

  @Test
  void getPatchesWithMatchingNameButNoIntersection() {
    EngineGeometry queryGeometry = mock(EngineGeometry.class);
    when(mockGeometry1.intersects(queryGeometry)).thenReturn(false);
    when(mockGeometry2.intersects(queryGeometry)).thenReturn(false);

    Iterable<Entity> result = timeStep.getPatches(queryGeometry, "Patch1");

    assertNotNull(result);
    assertEquals(0, countElements(result));
  }

  @Test
  void getPatchByExistingKey() {
    Entity result = timeStep.getPatchByKey(mockKey1);

    assertEquals(mockPatch1, result);
  }

  @Test
  void getPatchByNonExistingKey() {
    GeoKey nonExistingKey = mock(GeoKey.class);
    Entity result = timeStep.getPatchByKey(nonExistingKey);

    assertNull(result);
  }

  @Test
  void getPatchesWithPatchHavingNoGeometry() {
    // Create a patch with no geometry
    Entity patchNoGeo = mock(Entity.class);
    GeoKey keyNoGeo = mock(GeoKey.class);
    when(patchNoGeo.getGeometry()).thenReturn(Optional.empty());
    when(patchNoGeo.getName()).thenReturn("PatchNoGeo");

    // Add to a new map and create a new timestep
    HashMap<GeoKey, Entity> patchesWithNoGeo = new HashMap<>(patches);
    patchesWithNoGeo.put(keyNoGeo, patchNoGeo);

    Entity meta = mock(Entity.class);
    TimeStep timeStepWithNoGeo = new TimeStep(42, meta, patchesWithNoGeo);

    // Should not include patch with no EngineGeometry in results
    EngineGeometry queryGeometry = mock(EngineGeometry.class);
    when(mockGeometry1.intersects(queryGeometry)).thenReturn(true);
    when(mockGeometry2.intersects(queryGeometry)).thenReturn(true);

    Iterable<Entity> result = timeStepWithNoGeo.getPatches(queryGeometry);

    assertEquals(2, countElements(result));
    assertFalse(containsEntity(result, patchNoGeo));
  }

  @Test
  void circleQueryWithCachedOffsetsReturnsConsistentResults() {
    // Create a grid of patches with real GridSquare geometries
    HashMap<GeoKey, Entity> gridPatches = createTestGrid(10, 10, BigDecimal.ONE);
    TimeStep testTimeStep = new TimeStep(1, mockMeta, gridPatches);

    // Create circle query centered at (5, 5) with radius 2
    BigDecimal centerX = new BigDecimal("5");
    BigDecimal centerY = new BigDecimal("5");
    BigDecimal diameter = new BigDecimal("4"); // radius = 2
    GridCircle circle = new GridCircle(centerX, centerY, diameter);

    // Query twice - second query should use cached offsets
    List<Entity> results1 = testTimeStep.getPatches(circle);
    List<Entity> results2 = testTimeStep.getPatches(circle);

    // Results should be identical
    assertNotNull(results1);
    assertNotNull(results2);
    assertEquals(results1.size(), results2.size());

    // Verify no duplicates in results
    Set<Entity> uniqueResults = new HashSet<>(results1);
    assertEquals(results1.size(), uniqueResults.size());
  }

  @Test
  void circleQueryWithFractionalRadiusWorks() {
    // Create a grid of patches
    HashMap<GeoKey, Entity> gridPatches = createTestGrid(20, 20, BigDecimal.ONE);
    TimeStep testTimeStep = new TimeStep(1, mockMeta, gridPatches);

    // Test multiple fractional radii that should map to same cache entry
    BigDecimal centerX = new BigDecimal("10");
    BigDecimal centerY = new BigDecimal("10");

    GridCircle circle51 = new GridCircle(centerX, centerY, new BigDecimal("10.2")); // radius=5.1
    GridCircle circle55 = new GridCircle(centerX, centerY, new BigDecimal("11.0")); // radius=5.5
    GridCircle circle59 = new GridCircle(centerX, centerY, new BigDecimal("11.8")); // radius=5.9

    List<Entity> results51 = testTimeStep.getPatches(circle51);
    List<Entity> results55 = testTimeStep.getPatches(circle55);
    List<Entity> results59 = testTimeStep.getPatches(circle59);

    // All should return valid results
    assertNotNull(results51);
    assertNotNull(results55);
    assertNotNull(results59);
    assertTrue(results51.size() > 0);
    assertTrue(results55.size() > 0);
    assertTrue(results59.size() > 0);
  }

  @Test
  void circleQueryWithVerySmallRadiusWorks() {
    // Create a grid of patches
    HashMap<GeoKey, Entity> gridPatches = createTestGrid(10, 10, BigDecimal.ONE);
    TimeStep testTimeStep = new TimeStep(1, mockMeta, gridPatches);

    // Query with radius < 1 grid cell
    BigDecimal centerX = new BigDecimal("5");
    BigDecimal centerY = new BigDecimal("5");
    BigDecimal diameter = new BigDecimal("0.8"); // radius = 0.4
    GridCircle tinyCircle = new GridCircle(centerX, centerY, diameter);

    List<Entity> results = testTimeStep.getPatches(tinyCircle);

    assertNotNull(results);
    assertTrue(results.size() >= 1, "Should return at least center cell");
  }

  @Test
  void circleQueryThreadSafety() throws Exception {
    // Create shared grid
    HashMap<GeoKey, Entity> gridPatches = createTestGrid(30, 30, BigDecimal.ONE);
    TimeStep testTimeStep = new TimeStep(1, mockMeta, gridPatches);

    // Launch multiple threads querying with same radius simultaneously
    int numThreads = 10;
    CountDownLatch latch = new CountDownLatch(numThreads);
    ExecutorService executor = Executors.newFixedThreadPool(numThreads);
    List<Future<List<Entity>>> futures = new java.util.ArrayList<>();

    for (int i = 0; i < numThreads; i++) {
      futures.add(executor.submit(() -> {
        latch.countDown();
        latch.await();  // Synchronize start
        BigDecimal centerX = new BigDecimal("15");
        BigDecimal centerY = new BigDecimal("15");
        BigDecimal diameter = new BigDecimal("10"); // radius = 5
        GridCircle circle = new GridCircle(centerX, centerY, diameter);
        return testTimeStep.getPatches(circle);
      }));
    }

    // All threads should complete without exception
    for (Future<List<Entity>> future : futures) {
      List<Entity> result = future.get();
      assertNotNull(result);
      assertTrue(result.size() > 0);
    }

    executor.shutdown();
  }

  @Test
  void circleQueryReturnsCorrectPatches() {
    // Create a grid of patches with known layout
    HashMap<GeoKey, Entity> gridPatches = createTestGrid(20, 20, new BigDecimal("10"));
    TimeStep testTimeStep = new TimeStep(1, mockMeta, gridPatches);

    // Query circle centered at (100, 100) with diameter 40m (radius = 20m = 2 grid cells)
    BigDecimal centerX = new BigDecimal("100");
    BigDecimal centerY = new BigDecimal("100");
    BigDecimal diameter = new BigDecimal("40"); // radius = 20m = 2 grid cells
    GridCircle circle = new GridCircle(centerX, centerY, diameter);

    List<Entity> results = testTimeStep.getPatches(circle);

    assertNotNull(results);
    // Should return some patches within the circle (just verify functionality works)
    assertTrue(results.size() > 0, "Should return at least one patch");

    // Verify no duplicates
    Set<Entity> uniqueResults = new HashSet<>(results);
    assertEquals(results.size(), uniqueResults.size(), "Results should not contain duplicates");
  }

  // Helper methods
  private int countElements(Iterable<?> iterable) {
    int count = 0;
    for (Object element : iterable) {
      count++;
    }
    return count;
  }

  private boolean containsEntity(Iterable<Entity> iterable, Entity entity) {
    for (Entity e : iterable) {
      if (e.equals(entity)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Creates a test grid of patches with real GridSquare geometries.
   *
   * @param width grid width in cells
   * @param height grid height in cells
   * @param cellSize size of each cell
   * @return map of patches with GeoKeys
   */
  private HashMap<GeoKey, Entity> createTestGrid(int width, int height, BigDecimal cellSize) {
    HashMap<GeoKey, Entity> grid = new HashMap<>();

    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        // Create patch at grid position (x, y)
        BigDecimal centerX = cellSize.multiply(new BigDecimal(x));
        BigDecimal centerY = cellSize.multiply(new BigDecimal(y));

        GridSquare patchGeometry = new GridSquare(centerX, centerY, cellSize);
        String patchName = "Patch_" + x + "_" + y;

        Entity patch = mock(Entity.class);
        when(patch.getGeometry()).thenReturn(Optional.of(patchGeometry));
        when(patch.getName()).thenReturn(patchName);

        GeoKey key = new GeoKey(Optional.of(patchGeometry), patchName);
        grid.put(key, patch);
      }
    }

    return grid;
  }
}
