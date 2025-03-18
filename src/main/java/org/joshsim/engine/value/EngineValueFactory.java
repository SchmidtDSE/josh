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
   * Create a new EnigneValueFactory using a default casting strategy.
   */
  public EngineValueFactory() {
    caster = new EngineValueWideningCaster();
  }
  
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

  /**
   * Build a new EngineValue from a string.
   *
   * @param innerValue the value to decorate in an EngineValue.
   * @returns decorated version of innerValue.
   */
  public EngineValue build(String innerValue) {
    return new StringScalar(caster, innerValue, "");
  }

  /**
   * Build a new EngineValue from a list of strings.
   *
   * @param innerValue the value to decorate in an EngineValue.
   * @returns decorated version of innerValue.
   */
  public EngineValue build(List<String> innerValue) {
    return null;  // Need realized distribution
  }
  
}
