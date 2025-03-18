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

  private final Integer innerValue;

  public IntScalar(EngineValueCaster newCaster, int newInnerValue, String newUnits) {
    super(newCaster, newUnits);
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
  public EngineValue subtract(IntScalar other) {
    return new IntScalar(caster, getAsInt() - other.getAsInt(), getUnits());
  }
  
  @Override
  public EngineValue multiply(IntScalar other) {
    return new IntScalar(
      caster,
      getAsInt() * other.getAsInt(),
      determineMultipliedUnits(getUnits(), other.getUnits())
    );
  }
  
  @Override
  public EngineValue divide(IntScalar other) {
    return new IntScalar(
      caster,
      getAsInt() / other.getAsInt(),
      determineDividedUnits(getUnits(), other.getUnits())
    );
  }
  
  @Override
  public EngineValue raiseToPower(IntScalar other) {
    return new IntScalar(caster, Math.pow(getAsInt(), other.getAsInt()));
  }

  @Override
  public Distribution getAsDistribution() {
    EngineValueFactory factory = new EngineValueFactory(getCaster());

    List<Integer> values = new ArrayList<>(1);
    values.add(innerValue);

    return factory.build(values);
  }
  
}
