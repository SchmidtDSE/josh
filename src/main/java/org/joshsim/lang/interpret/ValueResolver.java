/**
 * Structures to help find values within a scope or nested scopes.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret;

import java.util.Optional;
import org.joshsim.engine.func.Scope;
import org.joshsim.engine.value.EngineValue;



/**
 * Strategy which resolves a value within a scope, memoizing the path after resolution.
 */
public interface ValueResolver {

  /**
   * Get the value indicated by this resolver within the given scope.
   *
   * @param target Scope in which to get value.
   * @return Value if found or none if the value is not defined or not initalized in the scope.
   */
  Optional<EngineValue> get(Scope target);

}
