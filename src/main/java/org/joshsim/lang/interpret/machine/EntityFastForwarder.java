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

  public static void fastForward(MutableEntity entity, String subStep) {
    int substepNum = getSubstepNum(subStep);

    if (substepNum <= CONSTANT_SUBSTEP) {
      runStep(entity, "constant", substepNum == CONSTANT_SUBSTEP);
    }

    if (substepNum <= INIT_SUBSTEP) {
      runStep(entity, "init", substepNum <= INIT_SUBSTEP);
    }

    if (substepNum <= START_SUBSTEP) {
      runStep(entity, "start", substepNum == START_SUBSTEP);
    }

    if (substepNum <= STEP_SUBSTEP) {
      runStep(entity, "step", substepNum == STEP_SUBSTEP);
    }

    if (substepNum <= END_SUBSTEP) {
      runStep(entity, "end", substepNum == END_SUBSTEP);
    }
  }

  private static void runStep(MutableEntity entity, String subStep, boolean leaveOpen) {
    entity.startSubstep(subStep);
    
    entity.getAttributeNames().stream()
        .map(entity::getAttributeValue)
        .forEach((x) -> {assert x != null;});
    
    if (!leaveOpen) {
      entity.endSubstep();
    }
  }

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