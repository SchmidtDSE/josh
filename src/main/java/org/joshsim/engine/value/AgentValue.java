
/**
 * Structures describing an individual engine agent value.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.value;

import java.math.BigDecimal;
import org.joshsim.engine.entity.Agent;

/**
 * Engine value which only has a single agent reference.
 */
public class AgentValue extends EngineValue {

  private final Agent innerValue;

  /**
   * Constructs an AgentValue with the specified values.
   *
   * @param caster The caster for this engine value.
   * @param innerValue The inner agent value.
   */
  public AgentValue(EngineValueCaster caster, Agent innerValue) {
    super(caster, new Units(innerValue.getClass().getSimpleName()));
    this.innerValue = innerValue;
  }

  @Override
  public Scalar getAsScalar() {
    throw new UnsupportedOperationException("Agent conversion to scalar is not permitted");
  }

  @Override
  public BigDecimal getAsDecimal() {
    throw new UnsupportedOperationException("Agent conversion to decimal is not permitted");
  }

  @Override
  public boolean getAsBoolean() {
    throw new UnsupportedOperationException("Agent conversion to boolean is not permitted");
  }

  @Override
  public String getAsString() {
    throw new UnsupportedOperationException("Agent conversion to string is not permitted");
  }

  @Override
  public long getAsInt() {
    throw new UnsupportedOperationException("Agent conversion to int is not permitted");
  }

  @Override
  public Distribution getAsDistribution() {
    throw new UnsupportedOperationException("Agent conversion to distribution is not permitted");
  }

  @Override
  public LanguageType getLanguageType() {
    return new LanguageType(innerValue.getClass().getSimpleName());
  }

  @Override
  public EngineValue cast(Cast strategy) {
    throw new UnsupportedOperationException("Agent casting is not permitted");
  }

  @Override
  public Object getInnerValue() {
    return innerValue;
  }

  @Override
  protected EngineValue unsafeAdd(EngineValue other) {
    throw new UnsupportedOperationException("Agent addition is not permitted");
  }

  @Override
  protected EngineValue unsafeSubtract(EngineValue other) {
    throw new UnsupportedOperationException("Agent subtraction is not permitted");
  }

  @Override
  protected EngineValue unsafeMultiply(EngineValue other) {
    throw new UnsupportedOperationException("Agent multiplication is not permitted");
  }

  @Override
  protected EngineValue unsafeDivide(EngineValue other) {
    throw new UnsupportedOperationException("Agent division is not permitted");
  }

  @Override
  protected EngineValue unsafeRaiseToPower(EngineValue other) {
    throw new UnsupportedOperationException("Agent exponentiation is not permitted");
  }

  @Override
  protected EngineValue unsafeSubtractFrom(EngineValue other) {
    throw new UnsupportedOperationException("Agent subtraction is not permitted");
  }

  @Override
  protected EngineValue unsafeDivideFrom(EngineValue other) {
    throw new UnsupportedOperationException("Agent division is not permitted");
  }

  @Override
  protected EngineValue unsafeRaiseAllToPower(EngineValue other) {
    throw new UnsupportedOperationException("Agent exponentiation is not permitted");
  }

  @Override
  protected boolean canBePower() {
    return false;
  }
}
