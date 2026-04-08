/**
 * Tests for correct evalDuration behavior when resolved through a distribution of entities.
 *
 * <p>When a user writes {@code mean(TimedTree.height.evalDuration)} in a Josh simulation,
 * the resolution chain must handle the case where "height" resolves to a distribution of
 * scalar values (one per entity) and the continuation path is "evalDuration". The correct
 * behavior is to return a distribution of 0-millisecond values (one per entity) when
 * profiling is disabled, matching the scalar (size==1) behavior.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.joshsim.engine.entity.base.MutableEntity;
import org.joshsim.engine.entity.handler.EventHandler;
import org.joshsim.engine.entity.handler.EventHandlerGroup;
import org.joshsim.engine.func.DistributionScope;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.ValueSupportFactory;
import org.joshsim.engine.value.type.EngineValue;
import org.joshsim.engine.value.type.EntityValue;
import org.joshsim.engine.value.type.RealizedDistribution;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


/**
 * Tests that evalDuration correctly resolves through distributions.
 *
 * <p>The resolution of {@code height.evalDuration} on a DistributionScope of entities proceeds:
 * <ol>
 *   <li>RecursiveValueResolver splits into foundPath="height", continuation="evalDuration"</li>
 *   <li>DistributionScope.get("height") returns a RealizedDistribution of scalar height values</li>
 *   <li>The scalar distribution has size &gt; 1, so the code at RecursiveValueResolver line 125
 *       creates a new DistributionScope wrapping the scalar distribution</li>
 *   <li>DistributionScope constructor calls sample().getAsEntity() on a scalar → throws</li>
 * </ol>
 *
 * <p>The correct behavior should be: when the continuation path is "evalDuration" and the resolved
 * value is a distribution of non-entity values, return a distribution of 0-millisecond values
 * (one per element), consistent with the scalar special case at line 109-113.</p>
 */
@ExtendWith(MockitoExtension.class)
public class EvalDurationDistributionTest {

  @Mock(lenient = true) private MutableEntity mockEntity1;
  @Mock(lenient = true) private MutableEntity mockEntity2;
  @Mock(lenient = true) private MutableEntity mockEntity3;
  @Mock(lenient = true) private EventHandlerGroup mockGroup1;
  @Mock(lenient = true) private EventHandlerGroup mockGroup2;
  @Mock(lenient = true) private EventHandlerGroup mockGroup3;
  @Mock(lenient = true) private EventHandler mockHandler1;
  @Mock(lenient = true) private EventHandler mockHandler2;
  @Mock(lenient = true) private EventHandler mockHandler3;

  /**
   * Wire a mock entity so it has a "height" attribute returning the given scalar value.
   */
  private void wireEntity(
      MutableEntity entity,
      EventHandlerGroup group,
      EventHandler handler,
      EngineValue heightValue
  ) {
    when(handler.getAttributeName()).thenReturn("height");
    when(handler.getEventName()).thenReturn("step");
    when(group.getEventHandlers()).thenReturn(Arrays.asList(handler));
    when(entity.getEventHandlers()).thenReturn(Arrays.asList(group));
    when(entity.getAttributeNames()).thenReturn(Set.of("height"));
    when(entity.getAttributeValue("height")).thenReturn(Optional.of(heightValue));
    when(entity.getName()).thenReturn("TimedTree");
  }

  /**
   * Resolving "height.evalDuration" on a DistributionScope of entities should return a
   * distribution of 0-millisecond values, one per entity in the distribution.
   *
   * <p>Currently this fails because the resolver wraps the intermediate scalar distribution
   * in a DistributionScope whose constructor calls getAsEntity() on scalars.</p>
   */
  @Test
  void evalDurationOnEntityDistributionReturnsZeroMillisecondsDistribution() {
    ValueSupportFactory factory = new ValueSupportFactory();

    // Build scalar height values
    EngineValue h1 = factory.build(1.0, Units.METERS);
    EngineValue h2 = factory.build(2.0, Units.METERS);
    EngineValue h3 = factory.build(3.0, Units.METERS);

    // Wire three entities each with a "height" attribute
    wireEntity(mockEntity1, mockGroup1, mockHandler1, h1);
    wireEntity(mockEntity2, mockGroup2, mockHandler2, h2);
    wireEntity(mockEntity3, mockGroup3, mockHandler3, h3);

    // Build a distribution of EntityValues (simulating a collection of TimedTree organisms)
    EntityValue ev1 = (EntityValue) factory.build(mockEntity1);
    EntityValue ev2 = (EntityValue) factory.build(mockEntity2);
    EntityValue ev3 = (EntityValue) factory.build(mockEntity3);
    RealizedDistribution entityDist = factory.buildRealizedDistribution(
        List.of(ev1, ev2, ev3),
        Units.of("TimedTree")
    );

    // Create a DistributionScope as the resolver would see it
    DistributionScope distScope = new DistributionScope(factory, entityDist);

    // Resolve "height.evalDuration" — this is the path taken for
    // mean(TimedTree.height.evalDuration) in profiler.josh
    ValueResolver resolver = new RecursiveValueResolver(factory, "height.evalDuration");
    Optional<EngineValue> result = resolver.get(distScope);

    // Correct behavior: should return a distribution of 0ms values
    assertTrue(result.isPresent(), "height.evalDuration should resolve on a distribution");

    EngineValue resolved = result.get();
    assertEquals(
        Units.MILLISECONDS,
        resolved.getUnits(),
        "Result should have milliseconds units"
    );

    // Should be a distribution with one value per entity
    Optional<Integer> size = resolved.getSize();
    assertTrue(size.isPresent(), "Result should have a definite size");
    assertEquals(3, size.get(), "Result should have one value per entity in the distribution");

    // Each value should be 0 milliseconds
    Iterable<EngineValue> contents = resolved.getAsDistribution().getContents(3, false);
    for (EngineValue val : contents) {
      assertEquals(0L, val.getAsInt(), "Each evalDuration value should be 0");
      assertEquals(Units.MILLISECONDS, val.getUnits(), "Each value should be in milliseconds");
    }
  }

  /**
   * Resolving bare "evalDuration" on a DistributionScope should also work, returning
   * 0 milliseconds without attempting to iterate entities.
   */
  @Test
  void bareEvalDurationOnDistributionScopeReturnsZero() {
    ValueSupportFactory factory = new ValueSupportFactory();

    // Build an entity distribution (we need valid entities for the DistributionScope constructor)
    EngineValue h1 = factory.build(1.0, Units.METERS);
    wireEntity(mockEntity1, mockGroup1, mockHandler1, h1);
    EntityValue ev1 = (EntityValue) factory.build(mockEntity1);
    RealizedDistribution entityDist = factory.buildRealizedDistribution(
        List.of(ev1),
        Units.of("TimedTree")
    );
    DistributionScope distScope = new DistributionScope(factory, entityDist);

    ValueResolver resolver = new RecursiveValueResolver(factory, "evalDuration");
    Optional<EngineValue> result = resolver.get(distScope);

    assertTrue(result.isPresent(), "evalDuration should resolve");
    assertEquals(0L, result.get().getAsInt(), "evalDuration should be 0");
    assertEquals(Units.MILLISECONDS, result.get().getUnits(), "Should be in milliseconds");
  }
}
