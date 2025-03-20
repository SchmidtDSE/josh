
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
  public LanguageType getLanguageType() {
    return new LanguageType("boolean");
  }

  @Override
  public Comparable<?> getInnerValue() {
    return innerValue;
  }

}
