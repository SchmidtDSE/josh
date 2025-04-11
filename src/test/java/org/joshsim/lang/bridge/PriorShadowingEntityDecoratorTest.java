/**
 * Tests for the decorator allowing ShadowingEntity to provide prior values like a frozen entity.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.bridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;
import org.joshsim.engine.entity.base.Entity;
import org.joshsim.engine.entity.base.GeoKey;
import org.joshsim.engine.entity.type.EntityType;
import org.joshsim.engine.geometry.EngineGeometry;
import org.joshsim.engine.value.type.EngineValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


/**
 * Tests for the decorator allowing ShadowingEntity to provide prior values like a frozen entity.
 */
@ExtendWith(MockitoExtension.class)
public class PriorShadowingEntityDecoratorTest {

  @Mock(lenient = true) private ShadowingEntity mockInner;
  @Mock(lenient = true) private EngineGeometry mockGeometry;
  @Mock(lenient = true) private EntityType mockEntityType;
  @Mock(lenient = true) private Entity mockFrozenEntity;
  @Mock(lenient = true) private GeoKey mockGeoKey;
  @Mock(lenient = true) private EngineValue mockEngineValue;

  private PriorShadowingEntityDecorator decorator;

  /**
   * Mock the inner ShadowingEntity.
   */
  @BeforeEach
  void setUp() {
    when(mockInner.getName()).thenReturn("TestEntity");
    when(mockInner.getGeometry()).thenReturn(Optional.of(mockGeometry));
    when(mockInner.getEntityType()).thenReturn(mockEntityType);
    when(mockInner.freeze()).thenReturn(mockFrozenEntity);
    when(mockInner.getKey()).thenReturn(Optional.of(mockGeoKey));
    when(mockInner.getAttributeNames()).thenReturn(Set.of("testAttr"));
    when(mockInner.getPriorAttribute("testAttr")).thenReturn(Optional.of(mockEngineValue));

    decorator = new PriorShadowingEntityDecorator(mockInner);
  }

  @Test
  void testGetGeometry() {
    Optional<EngineGeometry> result = decorator.getGeometry();
    assertTrue(result.isPresent());
    assertEquals(mockGeometry, result.get());
  }

  @Test
  void testGetName() {
    assertEquals("TestEntity", decorator.getName());
  }

  @Test
  void testGetAttributeValue() {
    Optional<EngineValue> result = decorator.getAttributeValue("testAttr");
    assertTrue(result.isPresent());
    assertEquals(mockEngineValue, result.get());
  }

  @Test
  void testGetAttributeNames() {
    Iterable<String> names = decorator.getAttributeNames();
    assertEquals(Set.of("testAttr"), names);
  }

  @Test
  void testGetEntityType() {
    assertEquals(mockEntityType, decorator.getEntityType());
  }

  @Test
  void testFreeze() {
    assertEquals(mockFrozenEntity, decorator.freeze());
  }

  @Test
  void testGetKey() {
    Optional<GeoKey> result = decorator.getKey();
    assertTrue(result.isPresent());
    assertEquals(mockGeoKey, result.get());
  }
}
