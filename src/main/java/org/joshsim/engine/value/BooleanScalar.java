
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

  private final EngineValueCaster caster;
  private final boolean innerValue;

  public BooleanScalar(EngineValueCaster newCaster, boolean newInnerValue, String newUnits) {
    super(newCaster, newUnits);
    caster = newCaster;
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
  public String getType() {
    return "boolean";
  }

  @Override
  public Comparable getInnerValue() {
    return innerValue;
  }
  
  @Override
  public EngineValue add(BooleanScalar other) {
    return new BooleanScalar(caster, getAsBoolean() || other.getAsBoolean(), getUnits());
  }
  
  @Override
  public EngineValue subtract(BooleanScalar other) {
    throw new UnsupportedOperationException("Cannot subtract booleans.");
  }
  
  @Override
  public EngineValue multiply(BooleanScalar other) {
    return new BooleanScalar(caster, getAsBoolean() && other.getAsBoolean(), getUnits());
  }
  
  @Override
  public EngineValue divide(BooleanScalar other) {
    throw new UnsupportedOperationException("Cannot divide booleans.");
  }
  
  @Override
  public EngineValue raiseToPower(BooleanScalar other) {
    throw new UnsupportedOperationException("Cannot raise booleans to powers.");
  }

  @Override
  public Distribution getAsDistribution() {
    EngineValueFactory factory = new EngineValueFactory(caster);
    List<Boolean> values = new ArrayList<>(1);
    values.add(innerValue);
    return factory.build(values);
  }
}
