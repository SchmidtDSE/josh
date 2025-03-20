
/**
 * Structures describing an individual engine value.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.value;

import java.math.BigDecimal;


/**
 * Engine value which only has a single discrete boolean value.
 */
public class BooleanScalar extends Scalar {

  private final boolean innerValue;

  /**
   * Constructs a BooleanScalar with the specified values.
   *
   * @param newCaster The caster for this engine value.
   * @param newInnerValue The inner boolean value.
   * @param newUnits The units associated with this engine value.
   */
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
  public long getAsInt() {
    return innerValue ? 1 : 0;
  }

  @Override
  public String getLanguageType() {
    return "boolean";
  }

  @Override
  public Comparable<?> getInnerValue() {
    return innerValue;
  }
  
  /**
   * Indicate that add is not supported for this type.
   *
   * @param other the other operand.
   */
  protected EngineValue fulfillAdd(EngineValue other) {
    throw new UnsupportedOperationException("Cannot add booleans.");
  }
  
  /**
   * Indicate that subtract is not supported for this type.
   *
   * @param other the other operand.
   */
  protected EngineValue fulfillSubtract(EngineValue other) {
    throw new UnsupportedOperationException("Cannot subtract booleans.");
  }
  
  /**
   * Indicate that multiply is not supported for this type.
   *
   * @param other the other operand.
   */
  protected EngineValue fulfillMultiply(EngineValue other) {
    throw new UnsupportedOperationException("Cannot multiply booleans.");
  }
  
  /**
   * Indicate that divide is not supported for this type.
   *
   * @param other the other operand.
   */
  protected EngineValue fulfillDivide(EngineValue other) {
    throw new UnsupportedOperationException("Cannot divide booleans.");
  }
  
  /**
   * Indicate that raise to power is not supported for this type.
   *
   * @param other the other operand.
   */
  protected EngineValue fulfillRaiseToPower(EngineValue other) {
    throw new UnsupportedOperationException("Cannot raise booleans to powers.");
  }
}
