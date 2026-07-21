/**
 * Utilities to help initialize entities after simulation start.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret.machine;

import java.util.List;
import org.joshsim.engine.entity.base.MutableEntity;


/**
 * Utility to ensure entities are "fast forwarded" if created after simulation start.
 *
 * <p>Structure which helps "fast forward" new entities, initializing them if created after
 * the simulation has already started.</p>
 */
public class EntityFastForwarder {

  private static final List<String> DEFAULT_SUBSTEP_ORDER = List.of("start", "step", "end");

  /**
   * Fast forwards an entity through simulation steps up to the specified substep.
   *
   * <p>Fast forwards an entity through simulation steps up to the specified substep, ensuring
   * entities created after simulation start are properly initialized.</p>
   *
   * @param entity The entity to fast forward
   * @param subStep The target substep to reach
   * @throws IllegalArgumentException if subStep is invalid
   */
  public static void fastForward(MutableEntity entity, String subStep) {
    fastForward(entity, subStep, "init", DEFAULT_SUBSTEP_ORDER);
  }

  /**
   * Fast forwards an entity, running a specific init event at the init stage.
   *
   * <p>Identical to {@link #fastForward(MutableEntity, String)} except that {@code initEvent} is
   * run at the init stage instead of the base {@code "init"}. A {@code create ... through
   * "<origin>"} passes the origin's variant init event here so its handlers run <em>instead of</em>
   * base {@code init} (pure replace); an untagged create passes {@code "init"}.</p>
   *
   * @param entity The entity to fast forward
   * @param subStep The target substep to reach
   * @param initEvent The event to run at the init stage
   * @throws IllegalArgumentException if subStep is invalid
   */
  public static void fastForward(MutableEntity entity, String subStep, String initEvent) {
    fastForward(entity, subStep, initEvent, DEFAULT_SUBSTEP_ORDER);
  }

  /**
   * Fast forwards an entity through a declared substep order, running a specific init event.
   *
   * <p>Identical to {@link #fastForward(MutableEntity, String, String)} except that the substeps
   * run after init are taken from {@code substepOrder} instead of the default {@code start}/
   * {@code step}/{@code end}, so a simulation's declared {@code phases} are honored.</p>
   *
   * @param entity The entity to fast forward
   * @param subStep The target substep to reach
   * @param initEvent The event to run at the init stage
   * @param substepOrder The ordered substeps that run after init, up to and including subStep
   * @throws IllegalArgumentException if subStep is invalid
   */
  public static void fastForward(MutableEntity entity, String subStep, String initEvent,
        List<String> substepOrder) {
    runStep(entity, "constant", "constant".equals(subStep));
    if ("constant".equals(subStep)) {
      return;
    }

    runStep(entity, initEvent, "init".equals(subStep));
    if ("init".equals(subStep)) {
      return;
    }

    for (String candidate : substepOrder) {
      boolean isTarget = candidate.equals(subStep);
      runStep(entity, candidate, isTarget);
      if (isTarget) {
        return;
      }
    }

    throw new IllegalArgumentException("Cannot fast forward to " + subStep);
  }

  /**
   * Executes a single simulation step on the entity.
   *
   * <p>Forces attribute resolution for all attributes using integer-based iteration. This
   * ensures all attributes are evaluated during the substep.</p>
   *
   * @param entity The entity to run the step on
   * @param subStep The substep name to execute
   * @param leaveOpen Whether to leave the substep open after execution
   */
  private static void runStep(MutableEntity entity, String subStep, boolean leaveOpen) {
    entity.startSubstep(subStep);

    // Force attribute resolution for all attributes using integer indexing
    int attributeCount = entity.getAttributeNameToIndex().size();
    for (int i = 0; i < attributeCount; i++) {
      entity.getAttributeValue(i);
    }

    if (!leaveOpen) {
      entity.endSubstep();
    }
  }

}
