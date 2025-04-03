
package org.joshsim.engine.func;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.joshsim.engine.entity.base.Entity;
import org.joshsim.engine.value.type.Distribution;
import org.joshsim.engine.value.type.EngineValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DistributionScopeTest {

  @Mock(lenient = true) private Distribution mockDistribution;
  @Mock(lenient = true) private Entity mockEntity;
  @Mock(lenient = true) private EngineValue mockEntityValue;
  @Mock(lenient = true) private EngineValue mockAttributeValue;
  @Mock(lenient = true) private EngineValue mockTransformedValue;

  private DistributionScope scope;

  @BeforeEach
  void setUp() {
    when(mockEntityValue.getAsEntity()).thenReturn(mockEntity);
    when(mockEntity.getAttributeNames()).thenReturn(Arrays.asList("testAttr"));
    when(mockEntity.getAttributeValue("testAttr")).thenReturn(Optional.of(mockAttributeValue));
    when(mockDistribution.sample()).thenReturn(mockEntityValue);
    when(mockDistribution.getSize()).thenReturn(Optional.of(3));
    when(mockDistribution.getContents(3, false)).thenReturn(Arrays.asList(mockEntityValue));
    when(mockAttributeValue.getUnits()).thenReturn(null);

    scope = new DistributionScope(mockDistribution);
  }

  @Test
  void testGetExistingAttribute() {
    EngineValue result = scope.get("testAttr");
    assertTrue(result instanceof Distribution);
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
