package org.joshsim.engine.simulation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Optional;
import org.joshsim.engine.entity.base.Entity;
import org.joshsim.engine.entity.base.GeoKey;
import org.joshsim.engine.entity.base.MutableEntity;
import org.joshsim.engine.entity.type.Patch;
import org.joshsim.engine.geometry.EngineGeometry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


/**
 * Unit tests for the {@link Replicate} class.
 * This class uses Mockito for mocking dependencies and JUnit 5 for testing.
 */
@ExtendWith(MockitoExtension.class)
public class ReplicateTest {

  @Mock(lenient = true) private Patch mockPatch1;
  @Mock(lenient = true) private Patch mockPatch2;
  @Mock private Entity frozenPatch1;
  @Mock private Entity frozenPatch2;
  @Mock private GeoKey mockKey1;
  @Mock private GeoKey mockKey2;
  @Mock private EngineGeometry mockGeometry;
  @Mock private Query mockQuery;

  private HashMap<GeoKey, MutableEntity> patches;
  private Replicate replicate;

  @BeforeEach
  void setUp() {
    patches = new HashMap<>();
    when(mockPatch1.getKey()).thenReturn(Optional.of(mockKey1));
    when(mockPatch2.getKey()).thenReturn(Optional.of(mockKey2));
    when(mockPatch1.freeze()).thenReturn(frozenPatch1);
    when(mockPatch2.freeze()).thenReturn(frozenPatch2);

    patches.put(mockKey1, mockPatch1);
    patches.put(mockKey2, mockPatch2);

    replicate = new Replicate(patches);
  }

  @Test
  void testInitialStepNumber() {
    assertEquals(0, replicate.getStepNumber());
  }

  @Test
  void testSaveTimeStep() {
    replicate.saveTimeStep(1);
    Optional<TimeStep> timeStep = replicate.getTimeStep(1);
    assertTrue(timeStep.isPresent());
    assertEquals(1, timeStep.get().getStep());

    // Verify the entities were frozen
    verify(mockPatch1).freeze();
    verify(mockPatch2).freeze();
  }

  @Test
  void testSaveTimeStepAlreadyExists() {
    replicate.saveTimeStep(1);
    assertThrows(IllegalArgumentException.class, () -> replicate.saveTimeStep(1));
  }

  @Test
  void testGetTimeStepNotFound() {
    Optional<TimeStep> timeStep = replicate.getTimeStep(999);
    assertFalse(timeStep.isPresent());
  }

  @Test
  void testQuery() {
    // Setup
    replicate.saveTimeStep(1);
    when(mockQuery.getStep()).thenReturn(1L);
    when(mockQuery.getGeometry()).thenReturn(Optional.of(mockGeometry));

    // Execute
    Iterable<Entity> result = replicate.query(mockQuery);

    // Verify
    assertNotNull(result);
  }

  @Test
  void testQueryCurrentStepNotAllowed() {
    when(mockQuery.getStep()).thenReturn(0L); // Current step
    assertThrows(IllegalArgumentException.class, () -> replicate.query(mockQuery));
  }

  @Test
  void testQueryNonExistentStep() {
    when(mockQuery.getStep()).thenReturn(999L); // Non-existent step
    assertThrows(IllegalArgumentException.class, () -> replicate.query(mockQuery));
  }

  @Test
  void testGetPatchByKey() {
    assertEquals(mockPatch1, replicate.getPatchByKey(mockKey1, 0));
  }

  @Test
  void testGetPatchByKeyInvalidStep() {
    assertThrows(IllegalArgumentException.class, () -> replicate.getPatchByKey(mockKey1, 1));
  }

  @Test
  void testGetCurrentPatches() {
    Iterable<MutableEntity> currentPatches = replicate.getCurrentPatches();

    assertNotNull(currentPatches);
    Iterator<MutableEntity> iterator = currentPatches.iterator();
    assertTrue(iterator.hasNext());

    // Verify the expected patch is returned
    boolean foundPatch1 = false;
    for (MutableEntity patch : currentPatches) {
      if (patch.equals(mockPatch1)) {
        foundPatch1 = true;
        break;
      }
    }
    assertTrue(foundPatch1, "Expected to find mockPatch1 in current patches");
  }
}
