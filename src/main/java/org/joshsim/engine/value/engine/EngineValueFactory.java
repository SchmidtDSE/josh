/**
 * Data structures describing initialization helpers for EngineValues.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.value.engine;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.joshsim.engine.entity.base.Entity;
import org.joshsim.engine.entity.base.MutableEntity;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.type.BooleanScalar;
import org.joshsim.engine.value.type.DecimalScalar;
import org.joshsim.engine.value.type.DoubleScalar;
import org.joshsim.engine.value.type.EngineValue;
import org.joshsim.engine.value.type.EntityValue;
import org.joshsim.engine.value.type.IntScalar;
import org.joshsim.engine.value.type.MutableEntityValue;
import org.joshsim.engine.value.type.RealizedDistribution;
import org.joshsim.engine.value.type.StringScalar;


/**
 * Factory to build new EngineValues from Java types.
 */
public class EngineValueFactory {

  private final EngineValueCaster caster;
  private final boolean favorBigDecimal;

  /**
   * Create a new EnigneValueFactory using a default casting strategy and favoring BigDecimal.
   */
  public EngineValueFactory() {
    favorBigDecimal = true;
    caster = new EngineValueWideningCaster(this);
  }


  /**
   * Create a new EnigneValueFactory using a default casting strategy.
   *
   * @param favorBigDecimal Flag indicating if decimal values produced by this factory should favor
   *     BigDecimal or double if not specified. True if favor BigDecimal and false if favor double.
   */
  public EngineValueFactory(boolean favorBigDecimal) {
    this.favorBigDecimal = favorBigDecimal;
    caster = new EngineValueWideningCaster(this);
  }

  /**
   * Constructor for EngineValueFactory.
   *
   * @param favorBigDecimal Flag indicating if decimal values produced by this factory should favor
   *     BigDecimal or double if not specified. True if favor BigDecimal and false if favor double.
   * @param caster EngineValueCaster to cast within operations involving the EngineValue.
   */
  public EngineValueFactory(boolean favorBigDecimal, EngineValueCaster caster) {
    this.favorBigDecimal = favorBigDecimal;
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
   * Build a new EngineValue from a double.
   *
   * @param innerValue The value to decorate in an EngineValue.
   * @param units The units to use for this value.
   * @return The decorated version of innerValue.
   */
  public EngineValue build(double innerValue, Units units) {
    return new DoubleScalar(caster, innerValue, units);
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
    ArrayList<EngineValue> innerArrayList = new ArrayList<EngineValue>(innerValue);;
    return new RealizedDistribution(caster, innerArrayList, units);
  }

  /**
   * Parse a number from a string.
   *
   * @param target The string to be parsed.
   * @param units The units to be associated with the returned value.
   * @return An EngineValue backed by either a double or BigDecimal depending on factory settings.
   */
  public EngineValue parseNumber(String target, Units units) {
    if (favorBigDecimal) {
      return build(new BigDecimal(target), units);
    } else {
      return build(Double.parseDouble(target), units);
    }
  }

  /**
   * Build an EngineValue for a number.
   *
   * <p>Builds an EngineValue using the provided number and units. Determines whether to use a
   * BigDecimal or a double for the EngineValue based on the factory settings.</p>
   *
   * @param number The numeric value to be decorated into an EngineValue.
   * @param units The units associated with the value.
   * @return An EngineValue representing the specified number and units.
   */
  public EngineValue buildForNumber(double number, Units units) {
    if (favorBigDecimal) {
      return build(new BigDecimal(number), units);
    } else {
      return build(number, units);
    }
  }

  /**
   * Determine if BigDecimal is favored in this factory.
   *
   * <p>Indicates whether the factory is configured to favor BigDecimal over double
   * when producing decimal values.</p>
   *
   * @return true if the factory favors BigDecimal for decimal values; false otherwise.
   */
  public boolean isFavoringBigDecimal() {
    return favorBigDecimal;
  }

}
