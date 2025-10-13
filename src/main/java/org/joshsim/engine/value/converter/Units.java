/**
 * Structures to represent units.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.value.converter;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import org.joshsim.compat.CompatibilityLayerKeeper;
import org.joshsim.compat.CompatibleStringJoiner;


/**
 * Strategy which represents a set of units which performs dimensional analysis.
 */
public class Units {

  // Cache for parsed units to avoid redundant parsing and object creation.
  // ConcurrentHashMap provides thread-safe access without synchronization overhead.
  private static final Map<String, Units> UNITS_CACHE = new ConcurrentHashMap<>();

  public static final Units EMPTY;
  public static final Units COUNT;
  public static final Units METERS;
  public static final Units DEGREES;

  // Static initializer to pre-populate cache with commonly used constants.
  // This ensures zero-allocation lookups for the most frequent cases.
  static {
    // Create and cache the constants, ensuring they go through simplify()
    EMPTY = createAndCacheConstant("");
    // COUNT simplifies to empty string, so it's the same as EMPTY
    COUNT = EMPTY;
    UNITS_CACHE.put("count", EMPTY);
    METERS = createAndCacheConstant("meters");
    DEGREES = createAndCacheConstant("degrees");
  }

  private static Units createAndCacheConstant(String description) {
    Units units = new Units(description).simplify();
    UNITS_CACHE.put(description, units);
    // Also cache under canonical form if different from input
    String canonical = units.toString();
    if (!canonical.equals(description)) {
      UNITS_CACHE.put(canonical, units);
    }
    return units;
  }

  private final String description;
  private final Map<String, Long> numeratorUnits;
  private final Map<String, Long> denominatorUnits;

  /**
   * Constructs Units from a description string.
   *
   * @param description a string representation of the units, with numerator and optionally a
   *     denominator.
   * @return Units from the description.
   * @throws IllegalArgumentException if more than one denominator is specified.
   */
  public static Units of(String description) {
    // Check cache first for O(1) lookup
    Units cached = UNITS_CACHE.get(description);
    if (cached != null) {
      return cached;
    }

    // Cache miss: parse and simplify as before
    Units unsimplified = new Units(description);
    Units simplified = unsimplified.simplify();

    // Cache the simplified result under the input description
    UNITS_CACHE.put(description, simplified);

    // Also cache under canonical form if different from input
    // This handles cases like "m * m" vs "m^2" representation
    String canonical = simplified.toString();
    if (!canonical.equals(description)) {
      UNITS_CACHE.put(canonical, simplified);
    }

    return simplified;
  }

  /**
   * Constructs Units with the specified numerator and denominator unit maps.
   *
   * @param numeratorUnits a Map representing the units in the numerator, mapping from name to
   *     count.
   * @param denominatorUnits a Map representing the units in the denominator, mapping from name to
   *     count.
   * @return Units from the given units.
   */
  public static Units of(Map<String, Long> numeratorUnits, Map<String, Long> denominatorUnits) {
    Units unsimplified = new Units(numeratorUnits, denominatorUnits);
    return unsimplified.simplify();
  }

  /**
   * Constructs Units from a description string.
   *
   * @param description a string representation of the units, with numerator and optionally a
   *     denominator.
   * @throws IllegalArgumentException if more than one denominator is specified.
   */
  private Units(String description) {
    String numerator = "";
    String denominator = "";

    if (description.contains(" / ")) {
      String[] pieces = description.split(" / ");
      if (pieces.length > 2) {
        String message = "No more than one numerator and denominator allowed: " + description;
        throw new IllegalArgumentException(message);
      }

      numerator = pieces[0];
      denominator = pieces.length > 1 ? pieces[1] : "";
    } else {
      numerator = description;
      denominator = "";
    }

    numeratorUnits = parseMultiplyString(numerator);
    denominatorUnits = parseMultiplyString(denominator);
    this.description = serializeString();
  }

  /**
   * Constructs Units with the specified numerator and denominator unit maps.
   *
   * @param numeratorUnits a Map representing the units in the numerator, mapping from name to
   *     count.
   * @param denominatorUnits a Map representing the units in the denominator, mapping from name to
   *     count.
   */
  private Units(Map<String, Long> numeratorUnits, Map<String, Long> denominatorUnits) {
    this.numeratorUnits = numeratorUnits;
    this.denominatorUnits = denominatorUnits;
    this.description = serializeString();
  }

  /**
   * Gets the numerator units.
   *
   * @return a Map representing the units in the numerator, mapping from name to count.
   */
  public Map<String, Long> getNumeratorUnits() {
    return numeratorUnits;
  }

  /**
   * Gets the denominator units.
   *
   * @return a Map representing the units in the denominator, mapping from name to count.
   */
  public Map<String, Long> getDenominatorUnits() {
    return denominatorUnits;
  }

  /**
   * Flip the units such that the numerator and denominator are inverted.
   *
   * @returns inverted copy of these units.
   */
  public Units invert() {
    return new Units(denominatorUnits, numeratorUnits);
  }

  /**
   * Multiply the current units with other units.
   *
   * @param other the other Units to multiply with.
   * @return a new Units instance representing the multiplication of the current and other units.
   */
  public Units multiply(Units other) {
    Map<String, Long> newNumeratorUnits = new TreeMap<>(numeratorUnits);
    Map<String, Long> newDenominatorUnits = new TreeMap<>(denominatorUnits);

    Map<String, Long> otherNumeratorUnits = other.getNumeratorUnits();
    for (String units : otherNumeratorUnits.keySet()) {
      long priorCount = newNumeratorUnits.getOrDefault(units, 0L);
      long otherCount = otherNumeratorUnits.get(units);
      long newCount = priorCount + otherCount;
      newNumeratorUnits.put(units, newCount);
    }

    Map<String, Long> otherDenominatorUnits = other.getDenominatorUnits();
    for (String units : otherDenominatorUnits.keySet()) {
      long priorCount = newDenominatorUnits.getOrDefault(units, 0L);
      long otherCount = otherDenominatorUnits.get(units);
      long newCount = priorCount + otherCount;
      newDenominatorUnits.put(units, newCount);
    }

    return Units.of(newNumeratorUnits, newDenominatorUnits);
  }

  /**
   * Divide the current units with other units.
   *
   * @param other the other Units to divide with.
   * @return a new Units instance representing the division of the current and other units.
   */
  public Units divide(Units other) {
    return multiply(other.invert());
  }

  /**
   * Raise the current units to a power.
   *
   * @param power the power to raise the units to.
   * @return a new Units instance representing the current units raised to the power.
   */
  public Units raiseToPower(Long power) {
    Map<String, Long> newNumeratorUnits = new TreeMap<>(numeratorUnits);
    Map<String, Long> newDenominatorUnits = new TreeMap<>(denominatorUnits);

    for (String units : newNumeratorUnits.keySet()) {
      long newCount = newNumeratorUnits.getOrDefault(units, 0L) * power;
      newNumeratorUnits.put(units, newCount);
    }

    for (String units : newDenominatorUnits.keySet()) {
      long newCount = newDenominatorUnits.getOrDefault(units, 0L) * power;
      newDenominatorUnits.put(units, newCount);
    }

    return Units.of(newNumeratorUnits, newDenominatorUnits);
  }

  /**
   * Simplify the units by canceling out common units in the numerator and denominator.
   *
   * @return a new Units instance where common units in the numerator and denominator are canceled
   *     out.
   */
  public Units simplify() {
    if (numeratorUnits.isEmpty() || denominatorUnits.isEmpty()) {
      return this;
    }

    Map<String, Long> newNumeratorUnits = new TreeMap<>();
    Map<String, Long> newDenominatorUnits = new TreeMap<>();

    // Create a copy instead of a view to avoid modifying original collections
    Set<String> sharedUnits = new HashSet<>(numeratorUnits.keySet());
    sharedUnits.retainAll(denominatorUnits.keySet());

    for (String units : sharedUnits) {
      long numeratorCount = numeratorUnits.get(units);
      long denominatorCount = denominatorUnits.get(units);
      long simplifiedCount = numeratorCount - denominatorCount;

      if (simplifiedCount > 0) {
        newNumeratorUnits.put(units, simplifiedCount);
      } else if (simplifiedCount < 0) {
        newDenominatorUnits.put(units, Math.abs(simplifiedCount));
      }
    }

    Set<String> numeratorOnlyUnits = new HashSet<>(numeratorUnits.keySet());
    numeratorOnlyUnits.removeAll(denominatorUnits.keySet());

    for (String units : numeratorOnlyUnits) {
      newNumeratorUnits.put(units, numeratorUnits.get(units));
    }

    Set<String> denominatorOnlyUnits = new HashSet<>(denominatorUnits.keySet());
    denominatorOnlyUnits.removeAll(numeratorUnits.keySet());

    for (String units : denominatorOnlyUnits) {
      newDenominatorUnits.put(units, denominatorUnits.get(units));
    }

    return new Units(newNumeratorUnits, newDenominatorUnits);
  }

  @Override
  public boolean equals(Object other) {
    return toString().equals(other.toString());
  }

  @Override
  public String toString() {
    return description;
  }

  @Override
  public int hashCode() {
    return toString().hashCode();
  }

  private Map<String, Long> parseMultiplyString(String target) {
    Map<String, Long> unitCounts = new TreeMap<>();

    StringTokenizer tokenizer = new StringTokenizer(target, " * ");
    while (tokenizer.hasMoreTokens()) {
      String unit = tokenizer.nextToken();

      if (unit.isEmpty()) {
        continue;
      }

      if (!isFormOfCount(unit)) {
        unitCounts.put(unit, unitCounts.getOrDefault(unit, 0L) + 1);
      }
    }
    return unitCounts;
  }

  private String serializeMultiplyString(Map<String, Long> target) {
    CompatibleStringJoiner joiner = CompatibilityLayerKeeper.get().createStringJoiner(" * ");

    for (String unit : target.keySet()) {
      long numInstances = target.get(unit);
      for (long i = 0; i < numInstances; i++) {
        joiner.add(unit);
      }
    }

    return joiner.toString();
  }

  private String serializeString() {
    String numeratorString = serializeMultiplyString(numeratorUnits);
    String denominatorString = serializeMultiplyString(denominatorUnits);
    boolean noDenominator = denominatorString.isEmpty();
    return noDenominator ? numeratorString : numeratorString + " / " + denominatorString;
  }

  /**
   * Determine if the given unit is a form of count.
   *
   * @param unit The single component unit String to check if it is a form of count.
   * @return True if the given unit is a form of count and can be ignored.
   */
  private boolean isFormOfCount(String unit) {
    return switch (unit) {
      case ("count") -> true;
      case ("counts") -> true;
      default -> false;
    };
  }

}
