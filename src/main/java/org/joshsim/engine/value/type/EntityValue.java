
/**
 * Structures describing an individual entity as engine value.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.value.type;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.joshsim.engine.entity.base.Entity;
import org.joshsim.engine.entity.base.MutableEntity;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueCaster;


/**
 * Engine value which only has a single entity or entity reference.
 */
public class EntityValue extends EngineValue {

  private final Entity innerValue;

  /**
   * Constructs an EntityValue with the specified values.
   *
   * @param caster The caster for this engine value.
   * @param innerValue The inner entity value.
   */
  public EntityValue(EngineValueCaster caster, Entity innerValue) {
    super(caster, Units.of(innerValue.getName()));
    this.innerValue = innerValue;
  }

  @Override
  public Optional<Integer> getSize() {
    return Optional.of(1);
  }

  @Override
  public Scalar getAsScalar() {
    throw new UnsupportedOperationException("Entity conversion to scalar is not defined.");
  }

  @Override
  public BigDecimal getAsDecimal() {
    throw new UnsupportedOperationException("Entity conversion to decimal is not defined.");
  }

  @Override
  public double getAsDouble() {
    throw new UnsupportedOperationException("Entity conversion to decimal is not defined.");
  }

  @Override
  public boolean getAsBoolean() {
    throw new UnsupportedOperationException("Entity conversion to boolean is not defined.");
  }

  @Override
  public String getAsString() {
    throw new UnsupportedOperationException("Entity conversion to string is not defined.");
  }

  @Override
  public long getAsInt() {
    throw new UnsupportedOperationException("Entity conversion to int is not defined.");
  }

  @Override
  public Distribution getAsDistribution() {
    return new RealizedDistribution(caster, List.of(this), units);
  }

  @Override
  public Entity getAsEntity() {
    return innerValue;
  }

  @Override
  public MutableEntity getAsMutableEntity() {
    throw new UnsupportedOperationException("Entity not mutable: " + innerValue);
  }

  @Override
  public LanguageType getLanguageType() {
    return new LanguageType(innerValue.getName(), true);
  }

  @Override
  public EngineValue cast(Cast strategy) {
    throw new UnsupportedOperationException("Entity casting is not defined.");
  }

  @Override
  public Object getInnerValue() {
    return innerValue;
  }

  @Override
  public EngineValue replaceUnits(Units newUnits) {
    throw new UnsupportedOperationException("Entity name and type cannot be changed.");
  }

  @Override
  protected EngineValue unsafeAdd(EngineValue other) {
    throw new UnsupportedOperationException("Entity addition is not defined.");
  }

  @Override
  protected EngineValue unsafeSubtract(EngineValue other) {
    throw new UnsupportedOperationException("Entity subtraction is not defined.");
  }

  @Override
  protected EngineValue unsafeMultiply(EngineValue other) {
    throw new UnsupportedOperationException("Entity multiplication is not defined.");
  }

  @Override
  protected EngineValue unsafeDivide(EngineValue other) {
    throw new UnsupportedOperationException("Entity division is not defined.");
  }

  @Override
  protected EngineValue unsafeRaiseToPower(EngineValue other) {
    throw new UnsupportedOperationException("Entity exponentiation is not defined.");
  }

  @Override
  protected EngineValue unsafeSubtractFrom(EngineValue other) {
    throw new UnsupportedOperationException("Entity subtraction is not defined.");
  }

  @Override
  protected EngineValue unsafeDivideFrom(EngineValue other) {
    throw new UnsupportedOperationException("Entity division is not defined.");
  }

  @Override
  protected EngineValue unsafeRaiseAllToPower(EngineValue other) {
    throw new UnsupportedOperationException("Entity exponentiation is not defined.");
  }

  @Override
  protected EngineValue unsafeGreaterThan(EngineValue other) {
    throw new UnsupportedOperationException("Entity comparison is not defined.");
  }

  @Override
  protected EngineValue unsafeGreaterThanOrEqualTo(EngineValue other) {
    throw new UnsupportedOperationException("Entity comparison is not defined.");
  }

  @Override
  protected EngineValue unsafeLessThan(EngineValue other) {
    throw new UnsupportedOperationException("Entity comparison is not defined.");
  }

  @Override
  protected EngineValue unsafeLessThanOrEqualTo(EngineValue other) {
    throw new UnsupportedOperationException("Entity comparison is not defined.");
  }

  @Override
  protected EngineValue unsafeEqualTo(EngineValue other) {
    throw new UnsupportedOperationException("Entity comparison is not defined.");
  }

  @Override
  protected EngineValue unsafeNotEqualTo(EngineValue other) {
    throw new UnsupportedOperationException("Entity comparison is not defined.");
  }

  @Override
  protected boolean canBePower() {
    return false;
  }

  @Override
  public EngineValue freeze() {
    return new EntityValue(caster, innerValue.freeze());
  }
}
