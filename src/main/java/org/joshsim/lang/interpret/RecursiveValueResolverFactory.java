/**
 * Factory implementation for building RecursiveValueResolver instances.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret;

import org.joshsim.engine.value.engine.EngineValueFactory;

/**
 * Factory that builds a RecursiveValueResolver without decoration.
 */
public class RecursiveValueResolverFactory implements ValueResolverFactory {

  /**
   * Build a RecursiveValueResolver configured to resolve the given path.
   *
   * @param valueFactory The factory to use when building engine values during resolution.
   * @param path The dot-separated path to resolve (e.g. "entity.attribute").
   * @return RecursiveValueResolver configured for the given path.
   */
  @Override
  public ValueResolver build(EngineValueFactory valueFactory, String path) {
    return new RecursiveValueResolver(valueFactory, path);
  }

}
