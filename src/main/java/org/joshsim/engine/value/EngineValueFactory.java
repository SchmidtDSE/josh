
/**
 * Data structures describing initialization helpers for EngineValues.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.value;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.joshsim.engine.entity.Entity;
import org.joshsim.engine.entity.MutableEntity;


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
   * @param caster EngineValueCaster to cast within operations involving the EngineValue.
   */
  public EngineValueFactory(EngineValueCaster caster) {
    this.caster = caster;
  }

  /**
   * Build a new EngineValue from an integer.
   *
   * @param innerValue the value to decorate in an EngineValue.
   * @param units the units for the value.
   * @returns decorated version of innerValue.
   */
  public EngineValue build(long innerValue, Units units) {
    return new IntScalar(caster, innerValue, units);
  }

  /**
   * Build a new EngineValue from a string.
   *
   * @param innerValue the value to decorate in an EngineValue.
   * @param units the units for the value.
   * @returns decorated version of innerValue.
   */
  public EngineValue build(String innerValue, Units units) {
    return new StringScalar(caster, innerValue, units);
  }

  /**
   * Build a new EngineValue from a boolean.
   *
   * @param innerValue the value to decorate in an EngineValue.
   * @param units the units for the value.
   * @returns decorated version of innerValue.
   */
  public EngineValue build(boolean innerValue, Units units) {
    return new BooleanScalar(caster, innerValue, units);
  }

  /**
   * Build a new EngineValue from a BigDecimal.
   *
   * @param innerValue the value to decorate in an EngineValue.
   * @param units the units for the value.
   * @returns decorated version of innerValue.
   */
  public EngineValue build(BigDecimal innerValue, Units units) {
    return new DecimalScalar(caster, innerValue, units);
  }

  /**
   * Build a new EngineValue from an Entity.
   *
   * @param entity the value to decorate in an EngineValue.
   * @returns decorated version of entity.
   */
  public EngineValue build(Entity entity) {
    return new EntityValue(caster, entity);
  }

  /**
   * Build a new EngineValue from an Entity.
   *
   * @param entity the value to decorate in an EngineValue.
   * @returns decorated version of entity.
   */
  public EngineValue build(MutableEntity entity) {
    return new MutableEntityValue(caster, entity);
  }

  /**
   * Build a new EngineValue distribution from a list.
   *
   * @param innerValue the values to include in the distribution.
   * @param units the units for the values in the distribution.
   * @returns decorated version of innerValue as a distribution.
   */
  public RealizedDistribution buildRealizedDistribution(
      List<EngineValue> innerValue,
      Units units
  ) {
    if (innerValue.size() == 0) {
      throw new IllegalArgumentException("Distributions cannot be empty.");
    }

    ArrayList<EngineValue> innerArrayList = new ArrayList<EngineValue>(innerValue);;
    return new RealizedDistribution(caster, innerArrayList, units);

  }

}
