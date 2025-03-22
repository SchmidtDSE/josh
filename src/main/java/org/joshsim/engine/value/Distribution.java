/**
 * Structures to represent distributions of values.
 *
 * @license BSD-3-Clause
 */
package org.joshsim.engine.value;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Structure representing a distribution of values.
 *
 * <p>Structure representing a distribution of values where this may be a finite collection of
 * specific discrete values or it may be a theoreticcal distribution with undefined size.
 * </p>
 */
public abstract class Distribution extends EngineValue {

  /**
   * Constructor for a distribution.
   *
   * @param caster the engine value caster to use for this distribution.
   * @param units the units of this distribution.
   */
  public Distribution(EngineValueCaster caster, Units units) {
    super(caster, units);
  }

  /**
   * Sample a single value from this distribution.
   *
   * <p>Sample a single value from this distribution where each element has a probability of
   * selection propotional to the frequency with which that value appears in the distribution.</p>
   *
   * @return Scalar value which is sampled from this distribution with frequency-proportional
   *      selection probability.
   */
  public abstract Scalar sample();

  /**
   * Get the size of the distribution if known.
   *
   * @return the number of elements in the distribution, or empty if virtualized
   */
  public abstract Optional<Integer> getSize();

  /**
   * Get a specified number of values from the distribution.
   *
   * <p>Sample repeatedly from this distribution given a number of samples to retrieve. In each
   * sampling this draw can either happen with or without replacement. With replacement, each
   * value has a probability of selection proportional to the frequency of that value. Without
   * replacement, each value has a probability of selection proprotion to the frequency of that
   * value minus the number of times it was drawn in this invocation of getContents.</p>
   *
   * @param count number of values to retrieve.
   * @param withReplacement boolean flag indicating whether to sample with replacement. True if
   *      sample with replacement and False otherwise. If this distribution does not have a finite
   *      number of elements (virtualized), this flag will be ignored.
   * @return an iterable of engine values from the distribution.
   * @throws IllegalArgumentException if count exceeds the cardinality of this distribution
   *      and sampling without replacement (like there are too few elements if the distribution is
   *      realized).
   */
  public abstract Iterable<EngineValue> getContents(int count, boolean withReplacement);

  /**
   * Get the mean value of this distribution.
   *
   * @returns mean value of this distribution either actual or hypothetical depending on
   *      distribution type. Empty if not defined for this distribution.
   */
  public abstract Optional<Scalar> getMean();

  /**
   * Get the standard deviation of this distribution.
   *
   * @returns standard deviation this distribution either actual or hypothetical depending on
   *      distribution type. Empty if not defined for this distribution.
   */
  public abstract Optional<Scalar> getStd();

  /**
   * Get the minimum value of this distribution.
   *
   * @returns minimum value of this distribution either actual or hypothetical depending on
   *      distribution type. Empty if not defined for this distribution.
   */
  public abstract Optional<Scalar> getMin();

  /**
   * Get the maximum value of this distribution.
   *
   * @returns maximum value of this distribution either actual or hypothetical depending on
   *      distribution type. Empty if not defined for this distribution.
   */
  public abstract Optional<Scalar> getMax();

  /**
   * Get the sum value of this distribution.
   *
   * @returns sum value of all elements in this distribution either actual or hypothetical
   *      depending on distribution type. Empty if not defined for this distribution.
   */
  public abstract Optional<Scalar> getSum();

  @Override
  public BigDecimal getAsDecimal() {
    return sample().getAsDecimal();
  }

  @Override
  public boolean getAsBoolean() {
    return sample().getAsBoolean();
  }

  @Override
  public String getAsString() {
    return sample().getAsString();
  }

  @Override
  public long getAsInt() {
    return sample().getAsInt();
  }
}
