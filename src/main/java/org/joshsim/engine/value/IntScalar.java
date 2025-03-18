/**
 * Structures describing an individual engine value.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.value;

import java.lang.UnsupportedOperationException;
import java.math.BigDecimal;


/**
 * Engine value which only has a single discrete value.
 */
public class IntScalar extends Scalar {

  private final EngineValueCaster caster;
  private final Integer innerValue;

  public IntScalar(EngineValueCaster newCaster, int newInnerValue) {
    caster = newCaster;
    innerValue = newInnerValue;
  }

  @Overrides
  public BigDecimal getAsDecimal() {
    return new BigDecimal(innerValue);
  }

  @Overrides
  public boolean getAsBoolean() {
    throw new UnsupportedOperationException("Cannot convert an int to boolean.");
  }

  @Overrides
  public String getAsString() {
    return innerValue.toString();
  }

  @Overrides
  public int getAsInt() {
    return innerValue;
  }

  @Overrides
  public String getType() {
    return "int";
  }

  @Overrides
  public Comparable getInnerValue() {
    return innerValue;
  }
  
  @Overrides
  public EngineValue add(IntScalar other) {
    return new IntScalar(caster, getAsInt() + other.getAsInt());
  }
  
  @Overrides
  EngineValue subtract(EngineValue other);
  
  @Overrides
  EngineValue multiply(EngineValue other);
  
  @Overrides
  EngineValue divide(EngineValue other);
  
  @Overrides
  EngineValue raiseToPower(EngineValue other);
  
  @Overrides
  String getUnits();
  
  @Overrides
  Scalar getAsScalar();
  
  @Overrides
  Distribution getAsDistribution();

  
}
