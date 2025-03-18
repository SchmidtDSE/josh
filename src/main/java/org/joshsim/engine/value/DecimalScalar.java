
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
 * Engine value which only has a single discrete decimal value.
 */
public class DecimalScalar extends Scalar {

  private final EngineValueCaster caster;
  private final BigDecimal innerValue;

  public DecimalScalar(EngineValueCaster newCaster, BigDecimal newInnerValue, String newUnits) {
    super(newCaster, newUnits);
    caster = newCaster;
    innerValue = newInnerValue;
  }

  @Override
  public BigDecimal getAsDecimal() {
    return innerValue;
  }

  @Override
  public boolean getAsBoolean() {
    return !innerValue.equals(BigDecimal.ZERO);
  }

  @Override
  public String getAsString() {
    return innerValue.toString();
  }

  @Override
  public int getAsInt() {
    return innerValue.intValue();
  }

  @Override
  public String getType() {
    return "decimal";
  }

  @Override
  public Comparable getInnerValue() {
    return innerValue;
  }
  
  @Override
  public EngineValue add(DecimalScalar other) {
    return new DecimalScalar(caster, getAsDecimal().add(other.getAsDecimal()), getUnits());
  }
  
  @Override
  public EngineValue subtract(DecimalScalar other) {
    return new DecimalScalar(caster, getAsDecimal().subtract(other.getAsDecimal()), getUnits());
  }
  
  @Override
  public EngineValue multiply(DecimalScalar other) {
    return new DecimalScalar(
      caster,
      getAsDecimal().multiply(other.getAsDecimal()),
      determineMultipliedUnits(getUnits(), other.getUnits())
    );
  }
  
  @Override
  public EngineValue divide(DecimalScalar other) {
    return new DecimalScalar(
      caster,
      getAsDecimal().divide(other.getAsDecimal()),
      determineDividedUnits(getUnits(), other.getUnits())
    );
  }
  
  @Override
  public EngineValue raiseToPower(DecimalScalar other) {
    double base = getAsDecimal().doubleValue();
    double exponent = other.getAsDecimal().doubleValue();
    return new DecimalScalar(caster, BigDecimal.valueOf(Math.pow(base, exponent)), getUnits());
  }

  @Override
  public Distribution getAsDistribution() {
    EngineValueFactory factory = new EngineValueFactory(caster);
    List<BigDecimal> values = new ArrayList<>(1);
    values.add(innerValue);
    return factory.build(values);
  }
}
