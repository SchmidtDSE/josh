/**
 * Factory interface for building ValueResolver instances.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret;

import org.joshsim.engine.value.engine.EngineValueFactory;

/**
 * Factory responsible for creating instances of ValueResolver for a given path.
 */
public interface ValueResolverFactory {

  /**
   * Build a ValueResolver configured to resolve the given path.
   *
   * @param valueFactory The factory to use when building engine values during resolution.
   * @param path The dot-separated path to resolve (e.g. "entity.attribute").
   * @return ValueResolver configured for the given path.
   */
  ValueResolver build(EngineValueFactory valueFactory, String path);

}
