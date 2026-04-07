/**
 * Factory implementation for building TimedValueResolver-wrapped RecursiveValueResolver instances.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret;

import org.joshsim.engine.value.engine.ValueSupportFactory;

/**
 * Factory that builds a RecursiveValueResolver decorated with a TimedValueResolver.
 *
 * <p>Each call to {@link #build(ValueSupportFactory, String)} constructs a new
 * {@link RecursiveValueResolver} for the given path and wraps it in a {@link TimedValueResolver}
 * so that resolution time is captured and available via the synthetic {@code evalDuration}
 * attribute. The factory itself is stateless and therefore thread-safe.</p>
 */
public class TimedRecursiveValueResolverFactory implements ValueResolverFactory {

  /**
   * Build a TimedValueResolver wrapping a RecursiveValueResolver for the given path.
   *
   * @param valueFactory The factory to use when building engine values during resolution and
   *     when constructing the millisecond EngineValue returned for evalDuration queries.
   * @param path The dot-separated path to resolve (e.g. "entity.attribute").
   * @return TimedValueResolver decorating a RecursiveValueResolver configured for the given path.
   */
  @Override
  public ValueResolver build(ValueSupportFactory valueFactory, String path) {
    ValueResolver inner = new RecursiveValueResolver(valueFactory, path);
    return new TimedValueResolver(valueFactory, inner);
  }

}
