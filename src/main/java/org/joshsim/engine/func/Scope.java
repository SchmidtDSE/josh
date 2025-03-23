/**
 * Structures to describe Josh language or simulation local scope.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.func;

import org.joshsim.engine.value.EngineValue;


/**
 * Description of a variable scope.
 *
 * <p>Structure maintaining state over local variables used in Josh simulation or Josh langage full
 * body evaluations or inlined lambdas.
 * </p>
 */
public interface Scope {

  /**
   * Get a value within this scope.
   *
   * @param name of the attribute which must be accessible on this scope's root.
   */
  EngineValue get(String name);

}
