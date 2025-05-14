/**
 * Structure describing the domain or range of a mapping function.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret.mapping;

import org.joshsim.engine.value.type.EngineValue;


/**
 * Structure describing bounds to be used in a mapping function.
 *
 * <p>Structure describing bounds to be used in a mapping function such as a function's domain or
 * its range.</p>
 */
public class MapBounds {

  private final EngineValue low;
  private final EngineValue high;

  /**
   * Create a new record of map bounds.
   *
   * @param low The lower end of the bounds like the start of a domain or range.
   * @param high The upper end of the bounds like the end of a domain or range.
   */
  public MapBounds(EngineValue low, EngineValue high) {
    this.low = low;
    this.high = high;
  }

  /**
   * Get the lower end of these bounds.
   *
   * @returns The lower end of the bounds like the start of a domain or range.
   */
  public EngineValue getLow() {
    return low;
  }

  /**
   * Get the upper end of these bounds.
   *
   * @returns The upper end of the bounds like the end of a domain or range.
   */
  public EngineValue getHigh() {
    return high;
  }
  
}

