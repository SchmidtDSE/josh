/**
 * Structures to help find values within a scope or nested scopes.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret;

import java.util.Optional;
import org.joshsim.engine.func.Scope;
import org.joshsim.engine.value.type.EngineValue;

/**
 * Interface for an object which resolves a value within a scope.
 *
 * <p>Following the dot chaining pattern of the Josh language definition, implementors resolve
 * a value within a scope and may memoize path resolution after the first lookup.</p>
 */
public interface ValueResolver {

  /**
   * Attempts to get a value from the target scope using the configured path.
   *
   * @param target The scope to search for the value.
   * @return Optional containing the resolved value if found, empty otherwise.
   */
  Optional<EngineValue> get(Scope target);

  /**
   * Get the path being resolved.
   *
   * @return the dot-separated path string.
   */
  String getPath();
}
