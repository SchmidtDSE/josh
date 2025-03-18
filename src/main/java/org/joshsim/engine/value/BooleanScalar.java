
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

  public BooleanScalar(EngineValueCaster newCaster, boolean newInnerValue, String newUnits) {
    super(newCaster, newUnits);
    innerValue = newInnerValue;
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
  public int getAsInt() {
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
