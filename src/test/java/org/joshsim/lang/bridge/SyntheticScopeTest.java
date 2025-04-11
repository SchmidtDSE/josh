
/**
 * Tests for SyntheticScope.
 *
 * <p>Tests the scope which adds synthetic attributes to an entity, allowing access to
 * hierarchy-related entities through keywords like current, prior, here, and meta.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.bridge;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
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
 * <p>Verifies the behavior of synthetic attributes (current, prior, here, meta)
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
}
