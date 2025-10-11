
/**
 * Tests for InnerEntityGetter.
 *
 * <p>Verifies the functionality of retrieving inner entities from an entity's attributes,
 * ensuring proper handling of single entities and distributions.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.bridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.joshsim.engine.entity.base.Entity;
import org.joshsim.engine.entity.base.MutableEntity;
import org.joshsim.engine.value.type.EngineValue;
import org.joshsim.engine.value.type.LanguageType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Test suite for the InnerEntityGetter utility class.
 *
 * <p>Tests the extraction of inner MutableEntity instances from an entity's attributes,
 * verifying correct handling of both single entities and entity collections.</p>
 */
@ExtendWith(MockitoExtension.class)
public class InnerEntityGetterTest {

  @Mock(lenient = true) private MutableEntity mockEntity;
  @Mock(lenient = true) private MutableEntity mockInnerEntity;
  @Mock(lenient = true) private MutableEntity mockNestedEntity;
  @Mock(lenient = true) private Entity mockFrozenEntity;
  @Mock(lenient = true) private Entity mockFrozenInnerEntity;
  @Mock(lenient = true) private EngineValue mockValue;
  @Mock(lenient = true) private EngineValue mockNestedValue;
  @Mock(lenient = true) private LanguageType mockLanguageType;

  /**
   * Sets up the test environment before each test.
   *
   * <p>Initializes mock objects and configures their behavior to simulate
   * an entity containing another entity as an attribute value.</p>
   */
  @BeforeEach
  void setUp() {
    when(mockEntity.getAttributeNames()).thenReturn(Set.of("inner"));
    when(mockEntity.getAttributeValue("inner")).thenReturn(Optional.of(mockValue));
    when(mockValue.getLanguageType()).thenReturn(mockLanguageType);
    when(mockLanguageType.containsAttributes()).thenReturn(true);
    when(mockValue.getSize()).thenReturn(Optional.of(1));
    when(mockValue.getAsMutableEntity()).thenReturn(mockInnerEntity);
    when(mockValue.getAsEntity()).thenReturn(mockInnerEntity);

    // Setup for nested entity testing
    when(mockInnerEntity.getAttributeNames()).thenReturn(Set.of("nested"));
    when(mockInnerEntity.getAttributeValue("nested")).thenReturn(Optional.of(mockNestedValue));
    when(mockNestedValue.getLanguageType()).thenReturn(mockLanguageType);
    when(mockNestedValue.getSize()).thenReturn(Optional.of(1));
    when(mockNestedValue.getAsMutableEntity()).thenReturn(mockNestedEntity);
    when(mockNestedValue.getAsEntity()).thenReturn(mockFrozenEntity);
  }

  @Test
  void testGetInnerEntitiesWithSingleEntity() {
    Iterable<MutableEntity> innerEntitiesIterable = InnerEntityGetter.getInnerEntities(mockEntity);
    List<MutableEntity> innerEntities = new ArrayList<>();
    for (MutableEntity entity : innerEntitiesIterable) {
      innerEntities.add(entity);
    }

    assertEquals(1, innerEntities.size());
  }

  @Test
  void testGetInnerFrozenEntities() {
    Iterable<Entity> frozenEntitiesIterable = InnerEntityGetter
        .getInnerFrozenEntities(mockEntity);
    List<Entity> frozenEntities = new ArrayList<>();
    for (Entity entity : frozenEntitiesIterable) {
      frozenEntities.add(entity);
    }

    assertEquals(1, frozenEntities.size());
  }

  @Test
  void testGetInnerEntitiesRecursive() {
    List<MutableEntity> recursiveEntities = InnerEntityGetter
        .getInnerEntitiesRecursive(mockEntity)
        .collect(Collectors.toList());

    assertEquals(2, recursiveEntities.size());
  }

  @Test
  void testGetInnerFrozenEntitiesRecursive() {
    List<Entity> recursiveFrozenEntities = InnerEntityGetter
        .getInnerFrozenEntitiesRecursive(mockEntity)
        .collect(Collectors.toList());

    assertEquals(2, recursiveFrozenEntities.size());
  }
}
