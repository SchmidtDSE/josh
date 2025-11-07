
/**
 * Tests for SyntheticScope.
 *
 * <p>Tests the scope which adds synthetic attributes to an entity, allowing access to
 * hierarchy-related entities through keywords like current, prior, here, meta, and parent.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.bridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.joshsim.engine.entity.base.Entity;
import org.joshsim.engine.entity.type.Agent;
import org.joshsim.engine.entity.type.Patch;
import org.joshsim.engine.simulation.Simulation;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.engine.value.type.EngineValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for the SyntheticScope which provides synthetic attribute access for entities.
 *
 * <p>Verifies the behavior of synthetic attributes (current, prior, here, meta, parent)
 * and their integration with the underlying entity attributes.</p>
 */
@ExtendWith(MockitoExtension.class)
public class SyntheticScopeTest {

  @Mock(lenient = true) private ShadowingEntity mockInner;
  @Mock(lenient = true) private ShadowingEntity mockHere;
  @Mock(lenient = true) private Simulation mockMeta;
  @Mock(lenient = true) private ShadowingEntity mockPrior;
  @Mock(lenient = true) private EngineValue mockValue;
  @Mock(lenient = true) private EngineValueFactory mockValueFactory;

  private SyntheticScope scope;

  /**
   * Set up the test environment before each test.
   *
   * <p>Initializes mocks and creates a new SyntheticScope instance with the mocked dependencies.
   * Sets up common behaviors for the mocked objects that will be used across multiple tests.</p>
   */
  @BeforeEach
  void setUp() {
    when(mockInner.getValueFactory()).thenReturn(new EngineValueFactory());
    when(mockInner.getAttributeNames()).thenReturn(Set.of("testAttr"));
    when(mockInner.hasAttribute("testAttr")).thenReturn(true);
    when(mockInner.getAttributeValue("testAttr")).thenReturn(Optional.of(mockValue));
    when(mockInner.getHere()).thenReturn(mockHere);
    when(mockInner.getMeta()).thenReturn(mockMeta);
    when(mockInner.getPrior()).thenReturn(mockPrior);
    when(mockInner.getName()).thenReturn("Test");
    when(mockHere.getName()).thenReturn("Test");
    when(mockMeta.getName()).thenReturn("Test");
    when(mockPrior.getName()).thenReturn("Test");

    scope = new SyntheticScope(mockInner);
  }

  @Test
  void testGetSyntheticCurrent() {
    assertNotNull(scope.get("current"));
  }

  @Test
  void testGetSyntheticPrior() {
    assertNotNull(scope.get("prior"));
  }

  @Test
  void testGetSyntheticHere() {
    assertNotNull(scope.get("here"));
  }

  @Test
  void testGetSyntheticMeta() {
    assertNotNull(scope.get("meta"));
  }

  @Test
  void testGetRegularAttribute() {
    assertNotNull(scope.get("testAttr"));
  }

  @Test
  void testHasSyntheticAttribute() {
    assertTrue(scope.has("current"));
    assertTrue(scope.has("prior"));
    assertTrue(scope.has("here"));
    assertTrue(scope.has("meta"));
  }

  @Test
  void testHasRegularAttribute() {
    assertTrue(scope.has("testAttr"));
  }

  @Test
  void testGetAttributes() {
    Iterable<String> attributes = scope.getAttributes();
    assertTrue(StreamSupport.stream(attributes.spliterator(), false)
        .collect(Collectors.toSet())
        .containsAll(Arrays.asList("current", "prior", "here", "meta", "testAttr")));
  }

  @Test
  void testHasParentAttribute() {
    assertTrue(scope.has("parent"), "parent should be a synthetic attribute");
  }

  @Test
  void testGetParentForAgent() {
    // Setup: Create mock Agent (MemberSpatialEntity) with parent
    Agent mockAgent = mock(Agent.class);
    Entity mockParent = mock(Entity.class);
    ShadowingEntity mockAgentShadow =
        mock(ShadowingEntity.class, org.mockito.Mockito.withSettings().lenient());
    EngineValueFactory factory = new EngineValueFactory();

    when(mockAgentShadow.getInner()).thenReturn(mockAgent);
    when(mockAgent.getParent()).thenReturn(mockParent);
    when(mockAgentShadow.getValueFactory()).thenReturn(factory);
    when(mockAgentShadow.getAttributeNames()).thenReturn(Set.of());
    when(mockParent.getName()).thenReturn("ParentEntity");

    SyntheticScope agentScope = new SyntheticScope(mockAgentShadow);

    // Execute: Get parent attribute
    EngineValue parentValue = agentScope.get("parent");

    // Verify: Parent is returned and wrapped correctly
    assertNotNull(parentValue, "parent should return a value for Agent");
    assertEquals(mockParent, parentValue.getAsEntity(),
        "parent should return the correct parent entity");
  }

  @Test
  void testGetParentForPatch() {
    // Setup: Create mock Patch (RootSpatialEntity - no parent)
    Patch mockPatch = mock(Patch.class);
    ShadowingEntity mockPatchShadow =
        mock(ShadowingEntity.class, org.mockito.Mockito.withSettings().lenient());
    EngineValueFactory factory = new EngineValueFactory();

    when(mockPatchShadow.getInner()).thenReturn(mockPatch);
    when(mockPatchShadow.getValueFactory()).thenReturn(factory);
    when(mockPatchShadow.getAttributeNames()).thenReturn(Set.of());

    SyntheticScope patchScope = new SyntheticScope(mockPatchShadow);

    // Execute & Verify: Should throw RuntimeException when accessing parent
    // (because parent returns Optional.empty() and scope.get() throws on empty)
    assertThrows(RuntimeException.class, () -> patchScope.get("parent"),
        "parent should throw exception for Patch (no parent available)");
  }

  @Test
  void testGetAttributesIncludesParent() {
    Set<String> attributes = scope.getAttributes();
    assertTrue(attributes.contains("parent"),
        "getAttributes should include parent synthetic attribute");
  }
}
