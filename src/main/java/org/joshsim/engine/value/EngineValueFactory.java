
/**
 * Data structures describing initialization helpers for EngineValues.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.value;

import java.lang.UnsupportedOperationException;
import java.math.BigDecimal;
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
   * @param units the units for the value.
   * @returns decorated version of innerValue.
   */
  public EngineValue build(long innerValue, String units) {
    return new IntScalar(caster, innerValue, units);
  } 

  /**
   * Build a new EngineValue from a string.
   *
   * @param innerValue the value to decorate in an EngineValue.
   * @param units the units for the value.
   * @returns decorated version of innerValue.
   */
  public EngineValue build(String innerValue, String units) {
    return new StringScalar(caster, innerValue, units);
  }

  /**
   * Build a new EngineValue from a boolean.
   *
   * @param innerValue the value to decorate in an EngineValue.
   * @param units the units for the value.
   * @returns decorated version of innerValue.
   */
  public EngineValue build(boolean innerValue, String units) {
    return new BooleanScalar(caster, innerValue, units);
  }

  /**
   * Build a new EngineValue from a BigDecimal.
   *
   * @param innerValue the value to decorate in an EngineValue.
   * @param units the units for the value.
   * @returns decorated version of innerValue.
   */
  public EngineValue build(BigDecimal innerValue, String units) {
    return new DecimalScalar(caster, innerValue, units);
  }

  /**
   * Build a new EngineValue distribution from a list.
   *
   * @param innerValue the values to include in the distribution.
   * @param units the units for the values in the distribution.
   * @returns decorated version of innerValue as a distribution.
   */
  public Distribution buildDistribution(List innerValue, String units) {
    if (innerValue.size() == 0) {
      throw new IllegalArgumentException("Distributions cannot be empty.");
    }

    throw new UnsupportedOperationException("Not implemented.");
  }
  
}
