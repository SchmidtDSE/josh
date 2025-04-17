package org.joshsim.engine.simulation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Optional;
import org.joshsim.engine.entity.base.Entity;
import org.joshsim.engine.entity.base.GeoKey;
import org.joshsim.engine.geometry.EngineGeometry;
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
    Entity meta = mock(Entity.class);
    Entity patchNoGeo = mock(Entity.class);
    GeoKey keyNoGeo = mock(GeoKey.class);
    when(patchNoGeo.getGeometry()).thenReturn(Optional.empty());
    when(patchNoGeo.getName()).thenReturn("PatchNoGeo");

    // Add to a new map and create a new timestep
    HashMap<GeoKey, Entity> patchesWithNoGeo = new HashMap<>(patches);
    patchesWithNoGeo.put(keyNoGeo, patchNoGeo);
    TimeStep timeStepWithNoGeo = new TimeStep(42, meta, patchesWithNoGeo);

    // Should not include patch with no EngineGeometry in results
    EngineGeometry queryGeometry = mock(EngineGeometry.class);
    when(mockGeometry1.intersects(queryGeometry)).thenReturn(true);
    when(mockGeometry2.intersects(queryGeometry)).thenReturn(true);

    Iterable<Entity> result = timeStepWithNoGeo.getPatches(queryGeometry);

    assertEquals(2, countElements(result));
    assertFalse(containsEntity(result, patchNoGeo));
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
}
