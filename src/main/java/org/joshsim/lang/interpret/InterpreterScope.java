/**
 * Structures describing a scope for an InterpreterAction.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret;
import org.joshsim.engine.func.Scope;
import org.joshsim.engine.value.EngineValue;


/**
 * Structure describing a scope and engine bridge to use for taking an InterpreterAction.
 */
public class InterpreterScope implements Scope {

  private final Scope memory;

  /**
   * Get a value within this scope.
   *
   * @param name of the attribute which must be accessible on this scope's root.
   */
  public EngineValue get(String name) {
    return memory.get(name);
  }

  /**
   * Check if a value is within this scope.
   *
   * @param name of the attribute to look for.
   * @return true if present and false otherwise.
   */
  public boolean has(String name) {
    return memory.has(name);
  }

  /**
   * Determine what values are on this scope.
   *
   * @return all attributes within this scope.
   */
  public Iterable<String> getAttributes() {
    return memory.getAttributes();
  }

}
