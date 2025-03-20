
package org.joshsim.engine.simulation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.joshsim.engine.geometry.Geometry;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QueryTest {
  
  @Mock
  private Geometry mockGeometry;

  @Test
  void testQueryWithStepOnly() {
    Query query = new Query(5);
    assertEquals(5, query.getStep());
    assertFalse(query.getGeometry().isPresent());
  }

  @Test
  void testQueryWithStepAndGeometry() {
    Query query = new Query(10, mockGeometry);
    assertEquals(10, query.getStep());
    assertTrue(query.getGeometry().isPresent());
    assertEquals(mockGeometry, query.getGeometry().get());
  }
}
