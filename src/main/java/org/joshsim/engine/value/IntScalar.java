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
  EngineValue subtract(EngineValue other) {
    return new IntScalar(caster, getAsInt() - other.getAsInt());
  }
  
  @Overrides
  EngineValue multiply(EngineValue other) {
    return new IntScalar(caster, getAsInt() * other.getAsInt());
  }
  
  @Overrides
  EngineValue divide(EngineValue other) {
    return new IntScalar(caster, getAsInt() / other.getAsInt());
  }
  
  @Overrides
  EngineValue raiseToPower(EngineValue other) {
    return new IntScalar(caster, Math.pow(getAsInt(), other.getAsInt()));
  }

  @Overrides
  public Distribution getAsDistribution() {
    EngineValueFactory factory = new EngineValueFactory(caster);

    List<Integer> values = new ArrayList<>(1);
    values.add(innerValue);

    return factory.build(values);
  }
  
}
