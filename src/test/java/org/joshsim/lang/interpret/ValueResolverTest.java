
/**
 * Tests for ValueResolver.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.joshsim.engine.entity.Entity;
import org.joshsim.engine.func.EntityScope;
import org.joshsim.engine.func.Scope;
import org.joshsim.engine.value.EngineValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for the ValueResolver which resolves dot-chained paths in scopes.
 */
@ExtendWith(MockitoExtension.class)
public class ValueResolverTest {

  @Mock private Scope mockScope;
  @Mock private Entity mockEntity;
  @Mock private EngineValue mockDirectValue;
  @Mock private EngineValue mockEntityValue;
  @Mock private EngineValue mockNestedValue;

  private ValueResolver resolver;

  @BeforeEach
  void setUp() {
    // Configure mock for direct value test
    lenient().when(mockScope.has("direct")).thenReturn(true);
    lenient().when(mockScope.get("direct")).thenReturn(mockDirectValue);

    // Configure mocks for nested value test
    lenient().when(mockScope.has("entity")).thenReturn(true);
    lenient().when(mockScope.get("entity")).thenReturn(mockEntityValue);
    lenient().when(mockEntityValue.getAsEntity()).thenReturn(mockEntity);

    EntityScope entityScope = new EntityScope(mockEntity);
    lenient().when(mockEntity.getScope()).thenReturn(entityScope);
    lenient().when(mockEntity.has("nested")).thenReturn(true);
    lenient().when(mockEntity.get("nested")).thenReturn(mockNestedValue);
  }

  @Test
  void testDirectValueResolution() {
    resolver = new ValueResolver("direct");
    Optional<EngineValue> result = resolver.get(mockScope);
    
    assertTrue(result.isPresent(), "Should resolve direct value");
    assertEquals(mockDirectValue, result.get(), "Should return correct direct value");
  }

  @Test
  void testNestedValueResolution() {
    resolver = new ValueResolver("entity.nested");
    Optional<EngineValue> result = resolver.get(mockScope);
    
    assertTrue(result.isPresent(), "Should resolve nested value");
    assertEquals(mockNestedValue, result.get(), "Should return correct nested value");
  }
}
