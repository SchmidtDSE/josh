
/**
 * Tests for EntityFastForwarder.
 *
 * <p>Verifies the functionality of fast-forwarding entities through simulation steps,
 * ensuring proper initialization of entities created after simulation start.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret.machine;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Optional;
import org.joshsim.engine.entity.base.MutableEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Test suite for the EntityFastForwarder utility class.
 *
 * <p>Tests the fast-forwarding functionality that ensures entities are properly
 * initialized when created after simulation start, verifying correct execution
 * of simulation steps and handling of substeps.</p>
 */
@ExtendWith(MockitoExtension.class)
public class EntityFastForwarderTest {

  @Mock(lenient = true) private MutableEntity mockEntity;

  @BeforeEach
  void setUp() {
    when(mockEntity.getAttributeNameToIndex()).thenReturn(Collections.emptyMap());
    when(mockEntity.getAttributeValue(org.mockito.ArgumentMatchers.anyInt()))
        .thenReturn(Optional.empty());
  }

  @Test
  void testFastForwardToConstant() {
    EntityFastForwarder.fastForward(mockEntity, "constant");
    verify(mockEntity).startSubstep("constant");
    verify(mockEntity, times(0)).endSubstep();
  }

  @Test
  void testFastForwardToInit() {
    EntityFastForwarder.fastForward(mockEntity, "init");
    verify(mockEntity).startSubstep("constant");
    verify(mockEntity).startSubstep("init");
    verify(mockEntity, times(1)).endSubstep();
  }

  @Test
  void testFastForwardToStart() {
    EntityFastForwarder.fastForward(mockEntity, "start");
    verify(mockEntity).startSubstep("constant");
    verify(mockEntity).startSubstep("init");
    verify(mockEntity).startSubstep("start");
    verify(mockEntity, times(2)).endSubstep();
  }

  @Test
  void testFastForwardToInvalidStep() {
    assertThrows(
        IllegalArgumentException.class,
        () -> EntityFastForwarder.fastForward(mockEntity, "invalid"),
        "Should throw when substep is invalid"
    );
  }
}
