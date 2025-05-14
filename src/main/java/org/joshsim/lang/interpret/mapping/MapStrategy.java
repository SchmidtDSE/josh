/**
 * Strategy for performing a map between a given domain and range.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret.mapping;

import org.joshsim.engine.value.type.EngineValue;


/**
 * Interface for a strategy which maps from a given domain to a given range.
 *
 * <p>Strategy which takes in a given domain and performs a mapping from values in that domain to a
 * given range. Each strategy implements a different mathematical function.</p>
 */
public interface MapStrategy {

  /**
   * Apply this mapping to the given value.
   *
   * @param target The value to be mapped through this strategy's given mathematical function.
   * @return Value after mapping. If the value is outside the map's domain, values may be returned
   *     outside its range.
   */
  EngineValue apply(EngineValue target);

}
