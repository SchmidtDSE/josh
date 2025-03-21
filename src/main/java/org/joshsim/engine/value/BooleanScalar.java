
/**
 * Structures describing an individual engine value.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.value;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;


/**
 * Engine value which only has a single discrete boolean value.
 */
public class BooleanScalar extends Scalar {

  private final boolean innerValue;

  /**
   * Constructs a BooleanScalar with the specified values.
   *
   * @param caster The caster for this engine value.
   * @param innerValue The inner boolean value.
   * @param units The units associated with this engine value.
   */
  public BooleanScalar(EngineValueCaster caster, boolean innerValue, String units) {
    super(caster, units);
    this.innerValue = innerValue;
  }

  @Override
  public BigDecimal getAsDecimal() {
    return new BigDecimal(getAsInt());
  }

  @Override
  public boolean getAsBoolean() {
    return innerValue;
  }

  @Override
  public String getAsString() {
    return Boolean.toString(innerValue);
  }

  @Override
  public long getAsInt() {
    return innerValue ? 1 : 0;
  }

  @Override
  public String getLanguageType() {
    return "boolean";
  }

  @Override
  public Comparable getInnerValue() {
    return innerValue;
  }
  
  /**
   * Indicate that add is not supported for this type.
   *
   * @param other the other operand.
   */
  public EngineValue add(BooleanScalar other) {
    throw new UnsupportedOperationException("Cannot add booleans.");
  }
  
  /**
   * Indicate that subtract is not supported for this type.
   *
   * @param other the other operand.
   */
  public EngineValue subtract(BooleanScalar other) {
    throw new UnsupportedOperationException("Cannot subtract booleans.");
  }
  
  /**
   * Indicate that multiply is not supported for this type.
   *
   * @param other the other operand.
   */
  public EngineValue multiply(BooleanScalar other) {
    return new BooleanScalar(getCaster(), getAsBoolean() && other.getAsBoolean(), getUnits());
  }
  
  /**
   * Indicate that divide is not supported for this type.
   *
   * @param other the other operand.
   */
  public EngineValue divide(BooleanScalar other) {
    throw new UnsupportedOperationException("Cannot divide booleans.");
  }
  
  /**
   * Indicate that raise to power is not supported for this type.
   *
   * @param other the other operand.
   */
  public EngineValue raiseToPower(BooleanScalar other) {
    throw new UnsupportedOperationException("Cannot raise booleans to powers.");
  }

  @Override
  public Distribution getAsDistribution() {
    EngineValueFactory factory = new EngineValueFactory(getCaster());
    List<Boolean> values = new ArrayList<>(1);
    values.add(innerValue);
    return factory.buildDistribution(values, getUnits());
  }
}
