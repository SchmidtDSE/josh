/**
 * Utilities to help initialize entities after simulation start.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret.machine;

import org.joshsim.engine.entity.base.MutableEntity;
import org.joshsim.lang.bridge.InnerEntityGetter;


/**
 * Utility to ensure entities are "fast forwarded" if created after simulation start.
 *
 * <p>Structure which helps "fast forward" new entities, initializing them if created after
 * the simulation has already started.</p>
 */
public class EntityFastForwarder {

  private static final int CONSTANT_SUBSTEP = -1;
  private static final int INIT_SUBSTEP = 0;
  private static final int START_SUBSTEP = 1;
  private static final int STEP_SUBSTEP = 2;
  private static final int END_SUBSTEP = 3;

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
    int substepNum = getSubstepNum(subStep);

    if (substepNum >= CONSTANT_SUBSTEP) {
      runStep(entity, "constant", substepNum == CONSTANT_SUBSTEP);
    }

    if (substepNum >= INIT_SUBSTEP) {
      runStep(entity, "init", substepNum == INIT_SUBSTEP);
    }

    if (substepNum >= START_SUBSTEP) {
      runStep(entity, "start", substepNum == START_SUBSTEP);
    }

    if (substepNum >= STEP_SUBSTEP) {
      runStep(entity, "step", substepNum == STEP_SUBSTEP);
    }

    if (substepNum >= END_SUBSTEP) {
      runStep(entity, "end", substepNum == END_SUBSTEP);
    }
  }

  /**
   * Executes a single simulation step on the entity.
   *
   * @param entity The entity to run the step on
   * @param subStep The substep name to execute
   * @param leaveOpen Whether to leave the substep open after execution
   */
  private static void runStep(MutableEntity entity, String subStep, boolean leaveOpen) {
    entity.startSubstep(subStep);
    
    entity.getAttributeNames().stream()
        .map(entity::getAttributeValue)
        .forEach((x) -> {assert x != null;});
    
    if (!leaveOpen) {
      entity.endSubstep();
    }
  }

  /**
   * Converts a substep name to its corresponding numeric value.
   *
   * @param subStep The name of the substep
   * @return The numeric value of the substep
   * @throws IllegalArgumentException if the substep name is invalid
   */
  private static int getSubstepNum(String subStep) {
    return switch (subStep) {
      case "constant" -> CONSTANT_SUBSTEP;
      case "init" -> INIT_SUBSTEP;
      case "start" -> START_SUBSTEP;
      case "step" -> STEP_SUBSTEP;
      case "end" -> END_SUBSTEP;
      default -> throw new IllegalArgumentException("Cannot fast forward to " + subStep);
    };
  }
  
}