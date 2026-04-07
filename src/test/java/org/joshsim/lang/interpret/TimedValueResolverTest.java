/**
 * Tests for TimedValueResolver.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.joshsim.engine.func.Scope;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.ValueSupportFactory;
import org.joshsim.engine.value.type.EngineValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


/**
 * Tests for TimedValueResolver which decorates a ValueResolver with timing support.
 */
@ExtendWith(MockitoExtension.class)
public class TimedValueResolverTest {

  @Mock(lenient = true) private Scope mockScope;
  @Mock(lenient = true) private ValueResolver mockInner;
  @Mock(lenient = true) private ValueResolver mockEvalDurationInner;
  @Mock(lenient = true) private EngineValue mockEngineValue;

  private ValueSupportFactory valueFactory;
  private TimedValueResolver timedResolver;

  /**
   * Set up shared instances before each test.
   */
  @BeforeEach
  void setUp() {
    valueFactory = new ValueSupportFactory();
    when(mockInner.getPath()).thenReturn("height");
    when(mockInner.get(mockScope)).thenReturn(Optional.of(mockEngineValue));
    when(mockEvalDurationInner.getPath()).thenReturn("evalDuration");
    timedResolver = new TimedValueResolver(valueFactory, mockInner);
  }

  @Test
  void testGetPathDelegatesToInner() {
    assertEquals(
        "height",
        timedResolver.getPath(),
        "getPath should return the inner resolver's path"
    );
  }

  @Test
  void testNonEvalDurationDelegatesToInner() {
    Optional<EngineValue> result = timedResolver.get(mockScope);

    assertTrue(result.isPresent(), "result should be present for a normal path");
    assertEquals(
        mockEngineValue,
        result.get(),
        "result should equal what the inner resolver returned"
    );
    verify(mockInner).get(mockScope);
  }

  @Test
  void testEvalDurationReturnsLastDuration() {
    TimedValueResolver evalDurationResolver = new TimedValueResolver(
        valueFactory,
        mockEvalDurationInner
    );

    Optional<EngineValue> result = evalDurationResolver.get(mockScope);

    assertTrue(result.isPresent(), "evalDuration should return a value");
    assertEquals(0L, result.get().getAsInt(), "initial evalDuration should be 0");
    assertEquals(
        Units.MILLISECONDS,
        result.get().getUnits(),
        "evalDuration should use MILLISECONDS units"
    );
  }

  @Test
  void testDurationIsRecordedAfterCall() {
    timedResolver.get(mockScope);

    TimedValueResolver evalDurationResolver = new TimedValueResolver(
        valueFactory,
        mockEvalDurationInner
    );

    Optional<EngineValue> durationResult = evalDurationResolver.get(mockScope);

    assertTrue(durationResult.isPresent(), "evalDuration resolver should return a value");
    assertTrue(
        durationResult.get().getAsInt() >= 0,
        "duration value should be non-negative"
    );
    assertEquals(
        Units.MILLISECONDS,
        durationResult.get().getUnits(),
        "duration should be in milliseconds"
    );
  }

  @Test
  void testEvalDurationDoesNotCallInner() {
    TimedValueResolver evalDurationResolver = new TimedValueResolver(
        valueFactory,
        mockEvalDurationInner
    );

    evalDurationResolver.get(mockScope);

    verify(mockEvalDurationInner, never()).get(mockScope);
  }

  @Test
  void testDurationUnitsAreMilliseconds() {
    TimedValueResolver evalDurationResolver = new TimedValueResolver(
        valueFactory,
        mockEvalDurationInner
    );

    Optional<EngineValue> result = evalDurationResolver.get(mockScope);

    assertEquals(
        Units.MILLISECONDS,
        result.get().getUnits(),
        "duration units should be MILLISECONDS"
    );
  }

  @Test
  void testToStringIncludesPath() {
    String toStringResult = timedResolver.toString();
    assertTrue(
        toStringResult.contains("height"),
        "toString should contain the inner resolver's path"
    );
  }

}
