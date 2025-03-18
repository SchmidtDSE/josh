/**
 * Structures describing an individual engine value.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.value;

import java.lang.UnsupportedOperationException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;


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
  public String getLanguageType() {
    return "int";
  }

  @Override
  public Comparable getInnerValue() {
    return innerValue;
  }

  
  /**
   * Compares this IntScalar instance with another for equality.
   *
   * @param obj the object to compare to
   * @return true if the specified object is equal to this IntScalar; false otherwise.
   */
  public EngineValue add(IntScalar other) {
    return new IntScalar(caster, getAsInt() + other.getAsInt(), getUnits());
  }

  
  /**
   * Checks equality of this IntScalar instance with another object.
   * 
   * @param obj the object to compare to
   * @return true if the specified object is equal to this IntScalar; false otherwise.
   */
  public EngineValue subtract(IntScalar other) {
    return new IntScalar(caster, getAsInt() - other.getAsInt(), getUnits());
  }

  
  /**
   * Multiplies this IntScalar instance with another IntScalar.
   *
   * @param other the IntScalar to multiply with
   * @return a new IntScalar that is the product of this and the other IntScalar
   */
  public EngineValue multiply(IntScalar other) {
    return new IntScalar(
      caster,
      getAsInt() * other.getAsInt(),
      determineMultipliedUnits(getUnits(), other.getUnits())
    );
  }
  
  public EngineValue divide(IntScalar other) {
    return new IntScalar(
      caster,
      getAsInt() / other.getAsInt(),
      determineDividedUnits(getUnits(), other.getUnits())
    );
  }
  
  public EngineValue raiseToPower(IntScalar other) {
    return new DecimalScalar(caster, Math.pow(getAsInt(), other.getAsInt()), getUnits());
  }

  @Override
  public Distribution getAsDistribution() {
    EngineValueFactory factory = new EngineValueFactory(getCaster());

    List<Integer> values = new ArrayList<>(1);
    values.add(innerValue);

    return factory.buildDistribution(values);
  }
  
}
