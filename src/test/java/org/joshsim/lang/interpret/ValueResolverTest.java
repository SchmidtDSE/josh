
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

  @Mock(lenient = true) private Scope mockScope;
  @Mock(lenient = true) private Entity mockEntity;
  @Mock(lenient = true) private EngineValue mockDirectValue;
  @Mock(lenient = true) private EngineValue mockEntityValue;
  @Mock(lenient = true) private EngineValue mockNestedValue;

  private ValueResolver resolver;

  @BeforeEach
  void setUp() {
    // Configure mock for direct value test
    when(mockScope.get("direct")).thenReturn(mockDirectValue);

    // Configure mocks for nested value test
    when(mockScope.get("entity")).thenReturn(mockEntityValue);
    when(mockEntityValue.getAsEntity()).thenReturn(mockEntity);

    EntityScope entityScope = new EntityScope(mockEntity);
    when(mockEntity.getAttributeValue("nested")).thenReturn(Optional.of(mockNestedValue));

    // Configure mock for local.value test
    when(mockScope.get("local.value")).thenReturn(mockDirectValue);

    // Configure mocks to throw for invalid paths
    when(mockScope.get("invalid")).thenThrow(new IllegalArgumentException());
    when(mockScope.get("entity.invalid")).thenThrow(new IllegalArgumentException());
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

  @Test
  void testLocalDotValueResolution() {
    resolver = new ValueResolver("local.value");
    Optional<EngineValue> result = resolver.get(mockScope);
    
    assertTrue(result.isPresent(), "Should resolve local.value");
    assertEquals(mockDirectValue, result.get(), "Should return correct local value");
  }
}
