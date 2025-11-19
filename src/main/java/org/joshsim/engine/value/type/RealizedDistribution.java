/**
 * Description of a distribution with finite size.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.value.type;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueCaster;

/**
 * Distribution with a finite number of elements.
 *
 * <p>Distribution which contains a finite number of EngineValue elements which can be enumerated in
 * memory as distinct to a VirtualizedDistribution which describes a collection of an indeterminate
 * number of elements for which summary statistics like mean can be derived but individual elements
 * cannot be iterated through.
 * </p>
 */
public class RealizedDistribution extends Distribution {
  // Cached LanguageType for empty distributions to avoid repeated allocations
  private static final LanguageType EMPTY_TYPE = new LanguageType("empty", false);

  private final List<EngineValue> values;
  private Optional<DoubleSummaryStatistics> stats = Optional.empty();

  /**
   * Create a new RealizedDistribution.
   *
   * @param caster The EngineValueCaster to use for casting.
   * @param values The values to be stored in the distribution.
   * @param units The units of the distribution.
   */
  public RealizedDistribution(
      EngineValueCaster caster,
      List<EngineValue> values,
      Units units
  ) {
    super(caster, units);
    this.values = values;
  }

  /**
   * Creates a new RealizedDistribution from a list of BigDecimal values. This is
   * useful for converting a list of decimal values into a distribution format when
   * reading external resources, where we know the units of the target.
   *
   * @param caster The EngineValueCaster to use for casting.
   * @param decimalValues The BigDecimal values to be stored in the distribution.
   * @param units The units of the distribution.
   * @return A new RealizedDistribution containing the converted values.
   */
  public static RealizedDistribution fromDecimalValues(
      EngineValueCaster caster,
      List<BigDecimal> decimalValues,
      Units units
  ) {

    List<EngineValue> engineValues = decimalValues.stream()
        .map(value -> new DecimalScalar(caster, value, units))
        .collect(Collectors.toList());

    return new RealizedDistribution(caster, engineValues, units);
  }

  /**
   * Compute statistics on demand using parallel stream.
   *
   * @return DoubleSummaryStatistics for the values
   */
  private void computeStats() {
    requireNonEmpty();
    DoubleSummaryStatistics newStats = values.stream()
        .map(EngineValue::getAsScalar)
        .map(Scalar::getAsDouble)
        .collect(Collectors.summarizingDouble(Double::doubleValue));
    stats = Optional.of(newStats);
  }

  @Override
  protected EngineValue unsafeAdd(EngineValue other) {
    List<EngineValue> result = values.stream()
        .map(value -> value.add(other))
        .collect(Collectors.toCollection(ArrayList::new));
    return new RealizedDistribution(getCaster(), result, getUnits());
  }

  @Override
  protected EngineValue unsafeSubtract(EngineValue other) {
    List<EngineValue> result = values.stream()
        .map(value -> value.subtract(other))
        .collect(Collectors.toCollection(ArrayList::new));
    return new RealizedDistribution(getCaster(), result, getUnits());
  }

  @Override
  protected EngineValue unsafeMultiply(EngineValue other) {
    List<EngineValue> result = values.stream()
        .map(value -> value.multiply(other))
        .collect(Collectors.toCollection(ArrayList::new));
    return new RealizedDistribution(getCaster(), result, getUnits().multiply(other.getUnits()));
  }

  @Override
  protected EngineValue unsafeDivide(EngineValue other) {
    List<EngineValue> result = values.stream()
        .map(value -> value.divide(other))
        .collect(Collectors.toCollection(ArrayList::new));
    return new RealizedDistribution(getCaster(), result, getUnits().divide(other.getUnits()));
  }

  @Override
  protected EngineValue unsafeRaiseToPower(EngineValue other) {
    List<EngineValue> result = values.stream()
        .map(value -> value.raiseToPower(other))
        .collect(Collectors.toCollection(ArrayList::new));
    return new RealizedDistribution(getCaster(), result, getUnits().raiseToPower(other.getAsInt()));
  }

  @Override
  protected EngineValue unsafeSubtractFrom(EngineValue other) {
    List<EngineValue> result = values.stream()
        .map(value -> other.subtract(value))
        .collect(Collectors.toCollection(ArrayList::new));
    return new RealizedDistribution(getCaster(), result, getUnits());
  }

  @Override
  protected EngineValue unsafeDivideFrom(EngineValue other) {
    List<EngineValue> result = values.stream()
        .map(value -> other.divide(value))
        .collect(Collectors.toCollection(ArrayList::new));
    Units newUnits = getUnits().divide(other.getUnits()).invert();
    return new RealizedDistribution(getCaster(), result, newUnits);
  }

  @Override
  protected EngineValue unsafeRaiseAllToPower(EngineValue other) {
    List<EngineValue> result = values.stream()
        .map(value -> other.raiseToPower(value))
        .collect(Collectors.toCollection(ArrayList::new));
    return new RealizedDistribution(getCaster(), result, getUnits().raiseToPower(other.getAsInt()));
  }

  @Override
  protected EngineValue unsafeGreaterThan(EngineValue other) {
    List<EngineValue> result = values.stream()
        .map(value -> value.greaterThan(other))
        .collect(Collectors.toCollection(ArrayList::new));
    // Comparison operators return boolean values with no units per language spec
    return new RealizedDistribution(getCaster(), result, Units.EMPTY);
  }

  @Override
  protected EngineValue unsafeGreaterThanOrEqualTo(EngineValue other) {
    List<EngineValue> result = values.stream()
        .map(value -> value.greaterThanOrEqualTo(other))
        .collect(Collectors.toCollection(ArrayList::new));
    // Comparison operators return boolean values with no units per language spec
    return new RealizedDistribution(getCaster(), result, Units.EMPTY);
  }

  @Override
  protected EngineValue unsafeLessThan(EngineValue other) {
    List<EngineValue> result = values.stream()
        .map(value -> value.lessThan(other))
        .collect(Collectors.toCollection(ArrayList::new));
    // Comparison operators return boolean values with no units per language spec
    return new RealizedDistribution(getCaster(), result, Units.EMPTY);
  }

  @Override
  protected EngineValue unsafeLessThanOrEqualTo(EngineValue other) {
    List<EngineValue> result = values.stream()
        .map(value -> value.lessThanOrEqualTo(other))
        .collect(Collectors.toCollection(ArrayList::new));
    // Comparison operators return boolean values with no units per language spec
    return new RealizedDistribution(getCaster(), result, Units.EMPTY);
  }

  @Override
  protected EngineValue unsafeEqualTo(EngineValue other) {
    List<EngineValue> result = values.stream()
        .map(value -> value.equalTo(other))
        .collect(Collectors.toCollection(ArrayList::new));
    // Comparison operators return boolean values with no units per language spec
    return new RealizedDistribution(getCaster(), result, Units.EMPTY);
  }

  @Override
  protected EngineValue unsafeNotEqualTo(EngineValue other) {
    List<EngineValue> result = values.stream()
        .map(value -> value.notEqualTo(other))
        .collect(Collectors.toCollection(ArrayList::new));
    // Comparison operators return boolean values with no units per language spec
    return new RealizedDistribution(getCaster(), result, Units.EMPTY);
  }

  @Override
  protected EngineValue unsafeAnd(EngineValue other) {
    List<EngineValue> result = values.stream()
        .map(value -> value.and(other))
        .collect(Collectors.toCollection(ArrayList::new));
    // Logical operators return boolean values with no units
    return new RealizedDistribution(getCaster(), result, Units.EMPTY);
  }

  @Override
  protected EngineValue unsafeOr(EngineValue other) {
    List<EngineValue> result = values.stream()
        .map(value -> value.or(other))
        .collect(Collectors.toCollection(ArrayList::new));
    return new RealizedDistribution(getCaster(), result, Units.EMPTY);
  }

  @Override
  protected EngineValue unsafeXor(EngineValue other) {
    List<EngineValue> result = values.stream()
        .map(value -> value.xor(other))
        .collect(Collectors.toCollection(ArrayList::new));
    return new RealizedDistribution(getCaster(), result, Units.EMPTY);
  }

  @Override
  public Scalar getAsScalar() {
    throw new UnsupportedOperationException("Cannot convert multiple values to a single scalar.");
  }

  @Override
  public Distribution getAsDistribution() {
    return this;
  }

  @Override
  public LanguageType getLanguageType() {
    if (values.isEmpty()) {
      return EMPTY_TYPE;
    }

    EngineValue exampleValue = values.get(0);

    Iterable<String> innerDistributions = exampleValue.getLanguageType().getDistributionTypes();
    List<String> distributions = new ArrayList<>();
    distributions.add("RealizedDistribution");
    innerDistributions.forEach(distributions::add);

    LanguageType innerLanguageType = exampleValue.getLanguageType();

    return new LanguageType(
        distributions,
        innerLanguageType.getRootType(),
        innerLanguageType.containsAttributes()
    );
  }

  @Override
  public EngineValue cast(Cast strategy) {
    ArrayList<EngineValue> result = values.stream()
        .map(value -> value.cast(strategy))
        .collect(Collectors.toCollection(ArrayList::new));
    return new RealizedDistribution(getCaster(), result, getUnits());
  }

  @Override
  public Object getInnerValue() {
    return values;
  }

  @Override
  public EngineValue replaceUnits(Units newUnits) {
    return new RealizedDistribution(getCaster(), values, newUnits);
  }

  @Override
  public EngineValue sample() {
    requireNonEmpty();
    int index = (int) (Math.random() * values.size());
    return values.get(index);
  }

  @Override
  public Distribution sampleMultiple(long count, boolean withReplacement) {
    return withReplacement ? sampleWithReplacement(count) : sampleWithoutReplacement(count);
  }

  @Override
  public Optional<Integer> getSize() {
    return Optional.of(values.size());
  }

  @Override
  public Iterable<EngineValue> getContents(int count, boolean withReplacement) {
    // If exactly all requested, can return immediately to save some memory and time.
    if (values.size() == count) {
      return values;
    }

    requireNonEmpty();

    // Otherwise, sublist or wrap.
    if (withReplacement) {
      ArrayList<EngineValue> result = new ArrayList<>();
      for (int i = 0; i < count; i++) {
        result.add(values.get(i % values.size()));
      }
      return result;
    } else {
      if (values.size() < count) {
        throw new IllegalArgumentException(
          "Cannot get more elements than present in a realized distribution without replacement."
        );
      }
      return values.subList(0, count);
    }
  }

  @Override
  public Optional<Scalar> getMean() {
    requireNonEmpty();
    if (stats.isEmpty()) {
      computeStats();
    }
    BigDecimal meanDecimal = new BigDecimal(stats.get().getAverage());
    return Optional.of(new DecimalScalar(getCaster(), meanDecimal, getUnits()));
  }

  @Override
  public Optional<Scalar> getStd() {
    requireNonEmpty();
    if (stats.isEmpty()) {
      computeStats();
    }
    double mean = stats.get().getAverage();
    double variance = values.stream()
        .map(EngineValue::getAsScalar)
        .map(Scalar::getAsDouble)
        .map(value -> Math.pow(value - mean, 2))
        .mapToDouble(Double::doubleValue)
        .sum() / (values.size() - 1);
    double stdDev = Math.sqrt(variance);
    DecimalScalar result = new DecimalScalar(getCaster(), BigDecimal.valueOf(stdDev), getUnits());
    return Optional.of(result);
  }

  @Override
  public Optional<Scalar> getMin() {
    requireNonEmpty();
    if (stats.isEmpty()) {
      computeStats();
    }
    DecimalScalar result = new DecimalScalar(
        getCaster(),
        BigDecimal.valueOf(stats.get().getMin()),
        getUnits()
    );
    return Optional.of(result);
  }

  @Override
  public Optional<Scalar> getMax() {
    if (stats.isEmpty()) {
      computeStats();
    }
    DecimalScalar result = new DecimalScalar(
        getCaster(),
        BigDecimal.valueOf(stats.get().getMax()),
        getUnits()
    );
    return Optional.of(result);
  }

  @Override
  public Optional<Scalar> getSum() {
    requireNonEmpty();
    if (stats.isEmpty()) {
      computeStats();
    }
    DecimalScalar result = new DecimalScalar(
        getCaster(),
        BigDecimal.valueOf(stats.get().getSum()),
        getUnits()
    );
    return Optional.of(result);
  }

  @Override
  public EngineValue freeze() {
    List<EngineValue> frozenValues = values.stream()
        .map((x) -> x.freeze())
        .collect(Collectors.toList());

    return new RealizedDistribution(caster, frozenValues, units);
  }

  /**
   * Samples a specified number of elements from the current distribution without replacement.
   *
   * <p>Randomly select a subset of elements from the distribution without replacement,
   * meaning each selected element is removed from the pool of potential subsequent selections.</p>
   *
   * @param count The number of elements to sample. Must not exceed the total number of elements
   *              in the distribution.
   * @return A new Distribution containing the sampled elements.
   * @throws IllegalArgumentException if {@code count} is greater than the total size of elements
   *     in the distribution.
   */
  private Distribution sampleWithoutReplacement(long count) {
    requireNonEmpty();
    if (count > values.size()) {
      String message = String.format(
          "Cannot sample %d elements from a distribution with %d elements without replacement.",
          count,
          values.size()
      );
      throw new IllegalArgumentException(message);
    }
    List<EngineValue> sampledValues = new ArrayList<>(values);
    Collections.shuffle(sampledValues);
    return new RealizedDistribution(getCaster(), sampledValues.subList(0, (int) count), getUnits());
  }

  /**
   * Generates a new distribution by sampling values from the current distribution with replacement.
   *
   * @param count The number of samples to be drawn randomly from the distribution.
   * @return A new distribution containing the sampled values.
   */
  private Distribution sampleWithReplacement(long count) {
    requireNonEmpty();
    List<EngineValue> sampledValues = new ArrayList<>();
    for (long i = 0; i < count; i++) {
      sampledValues.add(sample());
    }
    return new RealizedDistribution(getCaster(), sampledValues, getUnits());
  }

  /**
   * Validates that the distribution is not empty.
   *
   * <p>This method ensures that the list of values in the distribution is not empty. If the
   * distribution contains no values, an {@code IllegalArgumentException} is thrown to indicate
   * that operations on an empty distribution are not allowed.
   *
   * @throws IllegalArgumentException if the distribution is empty.
   */
  private void requireNonEmpty() {
    if (values.isEmpty()) {
      throw new IllegalArgumentException("Cannot perform this operation on an empty distribution.");
    }
  }

}
