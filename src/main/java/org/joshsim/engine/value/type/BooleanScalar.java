
/**
 * Structures describing an individual engine boolean value.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.value.type;

import java.math.BigDecimal;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueCaster;


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
  public BooleanScalar(EngineValueCaster caster, boolean innerValue, Units units) {
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
  public LanguageType getLanguageType() {
    return new LanguageType("boolean");
  }

  @Override
  public Comparable<?> getInnerValue() {
    return innerValue;
  }

  @Override
  public EngineValue replaceUnits(Units newUnits) {
    return new BooleanScalar(getCaster(), getAsBoolean(), newUnits);
  }

}
