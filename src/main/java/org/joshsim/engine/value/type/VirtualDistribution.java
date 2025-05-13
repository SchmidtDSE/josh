package org.joshsim.engine.value.type;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.joshsim.engine.entity.base.Entity;
import org.joshsim.engine.entity.base.MutableEntity;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueCaster;

/**
 * Distribution which, described theoretically, does not have discrete elements.
 *
 * <p>A distribution which is described by parameters to define a certain mathematical shape but
 * which does not have specific individual elements. These distributions can still be sampled and
 * used for generating summary statistics.</p>
 */
public abstract class VirtualDistribution extends Distribution {

  /**
   * Create a new virtual distribution.
   *
   * @param caster The value caster to use for this distribution.
   * @param units The units of this distribution.
   */
  public VirtualDistribution(EngineValueCaster caster, Units units) {
    super(caster, units);
  }

  /**
   * Samples a single value from the virtual distribution. Implementations
   * should provide a way to sample a scalar value from the distribution,
   * everything else is handled by the parent class assuming that this method
   * is implemented correctly.
   *
   * @return A sampled scalar value.
   */
  public abstract EngineValue sample();

  @Override
  public Distribution sampleMultiple(long count, boolean withReplacement) {
    List<EngineValue> samples = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      samples.add(sample());
    }
    return new RealizedDistribution(getCaster(), samples, getUnits());
  }

  @Override
  public List<EngineValue> getContents(int count, boolean withReplacement) {
    List<EngineValue> values = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      values.add(sample());
    }
    return values;
  }

  @Override
  public Object getInnerValue() {
    throw new UnsupportedOperationException("Cannot get inner value of a virtual distribution.");
  }

  @Override
  public LanguageType getLanguageType() {
    EngineValue exampleValue = sample();

    Iterable<String> innerDistributions = exampleValue.getLanguageType().getDistributionTypes();
    List<String> distributions = new ArrayList<>();
    distributions.add("VirtualDistribution");
    innerDistributions.forEach(distributions::add);

    return new LanguageType(distributions, exampleValue.getLanguageType().getRootType());
  }

  @Override
  public EngineValue replaceUnits(Units newUnits) {
    throw new UnsupportedOperationException("Cannot replace units of a virtual distribution.");
  }

  @Override
  public EngineValue cast(Cast strategy) {
    throw new UnsupportedOperationException("Cannot cast a virtual distribution.");
  }

  /**
   * Realizes the virtual distribution into a realized distribution with a
   * specified number of samples.
   *
   * @param count The number of samples to generate.
   * @param withReplacement Whether sampling is done with replacement.
   * @return A realized distribution containing the generated samples.
   */
  public RealizedDistribution realize(int count, boolean withReplacement) {
    List<EngineValue> values = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      values.add(sample());
    }
    return new RealizedDistribution(getCaster(), values, getUnits());
  }

  /**
   * Realizes the virtual distribution to match the size of another EngineValue.
   *
   * @param other The EngineValue whose size will be matched.
   * @return A realized distribution with the same size as the given EngineValue.
   */
  public RealizedDistribution realizeToMatchOther(EngineValue other) {
    Optional<Integer> otherSize = other.getSize();
    return realize(
        otherSize.orElseThrow(),
        false
    );
  }

  @Override
  public Optional<Scalar> getMean() {
    throw new UnsupportedOperationException(
      "Cannot calculate mean of a virtual distribution, unless defined mathematically."
    );
  }

  @Override
  public Optional<Scalar> getStd() {
    throw new UnsupportedOperationException(
      "Cannot calculate standard deviation of a virtual distribution,"
      + " unless defined mathematically."
    );
  }

  @Override
  public Optional<Scalar> getMin() {
    throw new UnsupportedOperationException(
      "Cannot calculate minimum of a virtual distribution, unless defined mathematically."
    );
  }

  @Override
  public Optional<Scalar> getMax() {
    throw new UnsupportedOperationException(
      "Cannot calculate maximum of a virtual distribution, unless defined mathematically."
    );
  }

  @Override
  public Optional<Scalar> getSum() {
    throw new UnsupportedOperationException(
      "Cannot calculate sum of a virtual distribution, unless defined mathematically."
    );
  }

  @Override
  public Optional<Integer> getSize() {
    return Optional.empty();
  }

  @Override
  public Scalar getAsScalar() {
    return sample().getAsScalar();
  }

  @Override
  public BigDecimal getAsDecimal() {
    return getAsScalar().getAsDecimal();
  }

  @Override
  public boolean getAsBoolean() {
    return getAsScalar().getAsBoolean();
  }

  @Override
  public String getAsString() {
    return getAsScalar().getAsString();
  }

  @Override
  public long getAsInt() {
    return getAsScalar().getAsInt();
  }

  @Override
  public Entity getAsEntity() {
    return getAsScalar().getAsEntity();
  }

  @Override
  public MutableEntity getAsMutableEntity() {
    return getAsScalar().getAsMutableEntity();
  }

  @Override
  public Distribution getAsDistribution() {
    return this;
  }

  @Override
  protected EngineValue unsafeAdd(EngineValue other) {
    RealizedDistribution realized = realizeToMatchOther(other);
    return realized.unsafeAdd(other);
  }

  @Override
  protected EngineValue unsafeSubtract(EngineValue other) {
    RealizedDistribution realized = realizeToMatchOther(other);
    return realized.unsafeSubtract(other);
  }

  @Override
  protected EngineValue unsafeMultiply(EngineValue other) {
    RealizedDistribution realized = realizeToMatchOther(other);
    return realized.unsafeMultiply(other);
  }

  @Override
  protected EngineValue unsafeDivide(EngineValue other) {
    RealizedDistribution realized = realizeToMatchOther(other);
    return realized.unsafeDivide(other);
  }

  @Override
  protected EngineValue unsafeRaiseToPower(EngineValue other) {
    RealizedDistribution realized = realizeToMatchOther(other);
    return realized.unsafeRaiseToPower(other);
  }

  @Override
  protected EngineValue unsafeSubtractFrom(EngineValue other) {
    RealizedDistribution realized = realizeToMatchOther(other);
    return realized.unsafeSubtractFrom(other);
  }

  @Override
  protected EngineValue unsafeDivideFrom(EngineValue other) {
    RealizedDistribution realized = realizeToMatchOther(other);
    return realized.unsafeDivideFrom(other);
  }

  @Override
  protected EngineValue unsafeRaiseAllToPower(EngineValue other) {
    RealizedDistribution realized = realizeToMatchOther(other);
    return realized.unsafeRaiseAllToPower(other);
  }

  @Override
  protected EngineValue unsafeGreaterThan(EngineValue other) {
    RealizedDistribution realized = realizeToMatchOther(other);
    return realized.unsafeGreaterThan(other);
  }

  @Override
  protected EngineValue unsafeGreaterThanOrEqualTo(EngineValue other) {
    RealizedDistribution realized = realizeToMatchOther(other);
    return realized.unsafeGreaterThanOrEqualTo(other);
  }

  @Override
  protected EngineValue unsafeLessThan(EngineValue other) {
    RealizedDistribution realized = realizeToMatchOther(other);
    return realized.unsafeLessThan(other);
  }

  @Override
  protected EngineValue unsafeLessThanOrEqualTo(EngineValue other) {
    RealizedDistribution realized = realizeToMatchOther(other);
    return realized.unsafeLessThanOrEqualTo(other);
  }

  @Override
  protected EngineValue unsafeEqualTo(EngineValue other) {
    RealizedDistribution realized = realizeToMatchOther(other);
    return realized.unsafeEqualTo(other);
  }

  @Override
  protected EngineValue unsafeNotEqualTo(EngineValue other) {
    RealizedDistribution realized = realizeToMatchOther(other);
    return realized.unsafeNotEqualTo(other);
  }

}
