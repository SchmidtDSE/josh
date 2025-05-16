/**
 * Tests for DistributionScope.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.func;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import org.joshsim.engine.entity.base.Entity;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.engine.value.type.Distribution;
import org.joshsim.engine.value.type.EngineValue;
import org.joshsim.engine.value.type.Scalar;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


/**
 * Test a scope which allows for retrieving the same attribute from a distribution of entities.
 */
@ExtendWith(MockitoExtension.class)
class DistributionScopeTest {

  @Mock(lenient = true) private Distribution mockDistribution;
  @Mock(lenient = true) private Entity mockEntity;
  @Mock(lenient = true) private Scalar mockEntityValue;
  @Mock(lenient = true) private EngineValue attributeValue;
  @Mock(lenient = true) private EngineValue mockInnerEntity;

  private DistributionScope scope;

  /**
   * Setup common values for tests.
   */
  @BeforeEach
  void setUp() {
    EngineValueFactory factory = new EngineValueFactory();
    attributeValue = factory.build(5L, Units.EMPTY);

    when(mockEntityValue.getAsEntity()).thenReturn(mockEntity);
    when(mockEntity.getAttributeNames()).thenReturn(Set.of("testAttr"));
    when(mockEntity.getAttributeValue("testAttr")).thenReturn(Optional.of(attributeValue));
    when(mockDistribution.sample()).thenReturn(mockEntityValue);
    when(mockDistribution.getSize()).thenReturn(Optional.of(3));
    when(mockDistribution.getContents(3, false)).thenReturn(
        Arrays.asList(mockEntityValue, mockEntityValue, mockEntityValue)
    );

    scope = new DistributionScope(new EngineValueFactory(), mockDistribution);
  }

  @Test
  void testGetExistingAttribute() {
    EngineValue result = scope.get("testAttr");
    assertTrue(result instanceof Distribution);
    assertEquals(result.getAsDistribution().getMean().get().getAsDecimal().doubleValue(), 5, 0.001);
  }

  @Test
  void testHasAttribute() {
    assertTrue(scope.has("testAttr"));
    assertFalse(scope.has("nonexistent"));
  }

  @Test
  void testGetAttributes() {
    Iterable<String> attributes = scope.getAttributes();
    assertTrue(attributes.iterator().hasNext());
    assertEquals("testAttr", attributes.iterator().next());
  }
}
