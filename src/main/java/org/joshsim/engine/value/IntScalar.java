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

  public IntScalar(EngineValueCaster newCaster, int newInnerValue, String newUnits) {
    super(newUnits);
    caster = newCaster;
    innerValue = newInnerValue;
  }

  @Override
  public BigDecimal getAsDecimal() {
    return new BigDecimal(innerValue);
  }

  @Override
  public boolean getAsBoolean() {
    throw new UnsupportedOperationException("Cannot convert an int to boolean.");
  }

  @Override
  public String getAsString() {
    return innerValue.toString();
  }

  @Override
  public int getAsInt() {
    return innerValue;
  }

  @Override
  public String getType() {
    return "int";
  }

  @Override
  public Comparable getInnerValue() {
    return innerValue;
  }

  @Override
  public EngineValue add(IntScalar other) {
    return new IntScalar(caster, getAsInt() + other.getAsInt(), getUnits());
  }

  @Override
  EngineValue subtract(IntScalar other) {
    return new IntScalar(caster, getAsInt() - other.getAsInt(), getUnits());
  }

  @Override
  EngineValue multiply(IntScalar other) {
    return new IntScalar(caster, getAsInt() * other.getAsInt(), getUnits());
  }

  @Override
  EngineValue divide(IntScalar other) {
    return new IntScalar(caster, getAsInt() / other.getAsInt(), getUnits());
  }

  @Override
  EngineValue raiseToPower(EngineValue other) {
    return new IntScalar(caster, Math.pow(getAsInt(), other.getAsInt()), getUnits());
  }

  @Override
  public Distribution getAsDistribution() {
    EngineValueFactory factory = new EngineValueFactory(caster);

    List<Integer> values = new ArrayList<>(1);
    values.add(innerValue);

    return factory.build(values);
  }
  
}
