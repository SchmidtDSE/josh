/**
 * Structures describing distributions with only unique values.
 *
 * @license BSD-3-Clause
 */
package org.joshsim.engine.value;

/**
 * Distribution in which each unique value can only zero or one times.
 */
public abstract class Set extends Distribution {

  /**
   * Constructor.
   *
   * @param caster The caster to use.
   * @param units The units of the distribution.
   */
  public Set(EngineValueCaster caster, Units units) {
    super(caster, units);
  }
}
