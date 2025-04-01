package org.joshsim.engine.simulation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Optional;
import org.joshsim.engine.entity.base.Entity;
import org.joshsim.engine.entity.base.GeoKey;
import org.joshsim.engine.geometry.Geometry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TimeStepTest {

  private TimeStep timeStep;
  private HashMap<GeoKey, Entity> patches;
  private GeoKey mockKey1;
  private GeoKey mockKey2;
  private Entity mockPatch1;
  private Entity mockPatch2;
  private Geometry mockGeometry1;
  private Geometry mockGeometry2;

  @BeforeEach
  void setUp() {
    // Create mocks
    mockKey1 = mock(GeoKey.class);
    mockKey2 = mock(GeoKey.class);
    mockPatch1 = mock(Entity.class);
    mockPatch2 = mock(Entity.class);
    mockGeometry1 = mock(Geometry.class);
    mockGeometry2 = mock(Geometry.class);

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
    timeStep = new TimeStep(42, patches);
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
    Geometry queryGeometry = mock(Geometry.class);
    when(mockGeometry1.intersects(queryGeometry)).thenReturn(true);
    when(mockGeometry2.intersects(queryGeometry)).thenReturn(true);

    Iterable<Entity> result = timeStep.getPatches(queryGeometry);
    
    assertNotNull(result);
    assertEquals(2, countElements(result));
  }

  @Test
  void getPatchesWithPartiallyIntersectingGeometry() {
    Geometry queryGeometry = mock(Geometry.class);
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
    Geometry queryGeometry = mock(Geometry.class);
    when(mockGeometry1.intersects(queryGeometry)).thenReturn(false);
    when(mockGeometry2.intersects(queryGeometry)).thenReturn(false);

    Iterable<Entity> result = timeStep.getPatches(queryGeometry);
    
    assertNotNull(result);
    assertEquals(0, countElements(result));
  }

  @Test
  void getPatchesWithMatchingGeometryAndName() {
    Geometry queryGeometry = mock(Geometry.class);
    when(mockGeometry1.intersects(queryGeometry)).thenReturn(true);
    when(mockGeometry2.intersects(queryGeometry)).thenReturn(true);

    Iterable<Entity> result = timeStep.getPatches(queryGeometry, "Patch1");
    
    assertNotNull(result);
    assertEquals(1, countElements(result));
    assertTrue(containsEntity(result, mockPatch1));
  }

  @Test
  void getPatchesWithNoMatchingName() {
    Geometry queryGeometry = mock(Geometry.class);
    when(mockGeometry1.intersects(queryGeometry)).thenReturn(true);
    when(mockGeometry2.intersects(queryGeometry)).thenReturn(true);

    Iterable<Entity> result = timeStep.getPatches(queryGeometry, "NonExistentName");
    
    assertNotNull(result);
    assertEquals(0, countElements(result));
  }

  @Test
  void getPatchesWithMatchingNameButNoIntersection() {
    Geometry queryGeometry = mock(Geometry.class);
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
    TimeStep timeStepWithNoGeo = new TimeStep(42, patchesWithNoGeo);
    
    // Should not include patch with no geometry in results
    Geometry queryGeometry = mock(Geometry.class);
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