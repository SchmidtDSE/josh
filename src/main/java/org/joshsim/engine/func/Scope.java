/**
 * Structures to describe Josh language or simulation local scope.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.func;

import java.util.Optional;
import java.util.Set;
import org.joshsim.engine.value.type.EngineValue;


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

  /**
   * Check if a value is within this scope.
   *
   * @param name of the attribute to look for.
   * @return true if present and false otherwise.
   */
  boolean has(String name);

  /**
   * Determine what values are on this scope.
   *
   * @return all attributes within this scope.
   */
  Set<String> getAttributes();

  /**
   * Attempt to retrieve a value using an integer index cache for fast array access.
   *
   * <p>Implementations that maintain an attribute index (e.g. EntityScope) should resolve
   * {@code name} via that index and return the value when present. Implementations that do not
   * support indexed access must return {@code Optional.empty()} so callers fall through to the
   * standard slow path.</p>
   *
   * @param name the attribute name to look up.
   * @return Optional containing the value if the fast path succeeded, or empty to signal the
   *     caller should fall back to the slow path.
   */
  Optional<EngineValue> tryIndexedGet(String name);

}
