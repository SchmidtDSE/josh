/**
 * Description of a distribution with finite size.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.value;

import java.lang.foreign.Linker.Option;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
  private ArrayList<EngineValue> values;
  private Optional<DoubleSummaryStatistics> stats = Optional.empty();

  /**
   * Compute statistics on demand using parallel stream.
   * 
   *
   * @return DoubleSummaryStatistics for the values
   */
  private void computeStats() {
    DoubleSummaryStatistics newStats = values.parallelStream()
        .map(EngineValue::getAsScalar)
        .map(Scalar::getAsDecimal)
        .collect(Collectors.summarizingDouble(BigDecimal::doubleValue));
    stats = Optional.of(newStats);
  }

  /**
   * Create a new RealizedDistribution.
   *
   * @param newCaster The EngineValueCaster to use for casting.
   * @param newInnerValue The values to be stored in the distribution.
   * @param newUnits The units of the distribution.
   */
  public RealizedDistribution(
      EngineValueCaster newCaster,
      ArrayList<EngineValue> newInnerValue,
      String newUnits
  ) {
    super(newCaster, newUnits);
    values = newInnerValue;
  }

  

  @Override
  public RealizedDistribution add(EngineValue other) {
    ArrayList<EngineValue> result = values.stream()
        .map(value -> value.add(other))
        .collect(Collectors.toCollection(ArrayList::new));
    return new RealizedDistribution(getCaster(), result, getUnits());
  }

  @Override
  public EngineValue subtract(EngineValue other) {
    ArrayList<EngineValue> result = values.stream()
        .map(value -> value.subtract(other))
        .collect(Collectors.toCollection(ArrayList::new));
    return new RealizedDistribution(getCaster(), result, getUnits());
  }

  @Override
  public EngineValue multiply(EngineValue other) {
    ArrayList<EngineValue> result = values.stream()
        .map(value -> value.multiply(other))
        .collect(Collectors.toCollection(ArrayList::new));
    return new RealizedDistribution(getCaster(), result, getUnits());
  }

  @Override
  public EngineValue divide(EngineValue other) {
    ArrayList<EngineValue> result = values.stream()
        .map(value -> value.divide(other))
        .collect(Collectors.toCollection(ArrayList::new));
    return new RealizedDistribution(getCaster(), result, getUnits());
  }

  @Override
  public EngineValue raiseToPower(EngineValue other) {
    ArrayList<EngineValue> result = values.stream()
        .map(value -> value.raiseToPower(other))
        .collect(Collectors.toCollection(ArrayList::new));
    return new RealizedDistribution(getCaster(), result, getUnits());
  }

  @Override
  public String getUnits() {
    return getUnits(); 
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
  public String getLanguageType() {
    return "RealizedDistribution";
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
  public String determineMultipliedUnits(String left, String right) {
    return null;
  }

  @Override
  public String determineDividedUnits(String left, String right) {
    return null;
  }

  @Override
  public Scalar sample() {
    return null;
  }

  @Override
  public Optional<Integer> getSize() {
    return Optional.of(values.size());
  }

  @Override
  public Iterable<EngineValue> getContents(int count, boolean withReplacement) {
    if (withReplacement) {
      ArrayList<EngineValue> result = new ArrayList<>();
      for (int i = 0; i < count; i++) {
        result.add(values.get(i % values.size()));
      }
      return result;
    } else {
      return values.subList(0, Math.min(count, values.size()));
    }
  }

  // @Override
  // public Optional<Scalar> getMean() {
  //   if (stats.isEmpty()) {
  //     computeStats();
  //   }
  //   double mean = BigDecimal(stats.get().getAverage());
  //   return new Optional.of(DecimalScalar(getCaster(), mean, getUnits()));
  // }

  @Override
  public Optional<Scalar> getStd() {
    return null;
  }

  @Override
  public Optional<Scalar> getMin() {
    return null;
  }

  @Override
  public Optional<Scalar> getMax() {
    return null;
  }

  @Override
  public Optional<Scalar> getSum() {
    return null;
  }
}
