/**
 * Data structures describing initialization helpers for EngineValues.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.value;

import java.util.List;


/**
 * Factory to build new EngineValues from Java types.
 */
public class EngineValueFactory {

  private final EngineValueCaster caster;
  
  /**
   * Constructor for EngineValueFactory.
   *
   * @param newCaster EngineValueCaster to cast within operations involving the EngineValue.
   */
  public EngineValueFactory(EngineValueCaster newCaster) {
    caster = newCaster;
  }

  /**
   * Build a new EngineValue from an integer.
   *
   * @param innerValue the value to decorate in an EngineValue.
   * @returns decorated version of innerValue.
   */
  public EngineValue build(int innerValue) {
    return new IntScalar(caster, innerValue);
  }

  /**
   * Build a new EngineValue from an integer.
   *
   * @param innerValue the value to decorate in an EngineValue.
   * @returns decorated version of innerValue.
   */
  public EngineValue build(List<Integer> innerValue) {
    return null;  // Need realized distribution
  }
  
}
