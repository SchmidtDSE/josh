/**
 * Structures to represent units.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.value;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeMap;


/**
 * Strategy which represents a set of units which performs dimensional analysis.
 */
public class Units {

  private final Map<String, Long> numeratorUnits;
  private final Map<String, Long> denominatorUnits;

  /**
   * Constructs Units from a description string.
   *
   * @param description a string representation of the units, with numerator and optionally a
   *     denominator.
   * @throws IllegalArgumentException if more than one denominator is specified.
   */
  public Units(String description) {
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
  }

  /**
   * Constructs Units with the specified numerator and denominator unit maps.
   *
   * @param numeratorUnits a Map representing the units in the numerator, mapping from name to
   *     count.
   * @param denominatorUnits a Map representing the units in the denominator, mapping from name to
   *     count.
   */
  public Units(Map<String, Long> numeratorUnits, Map<String, Long> denominatorUnits) {
    this.numeratorUnits = numeratorUnits;
    this.denominatorUnits = denominatorUnits;
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

    return new Units(newNumeratorUnits, newDenominatorUnits);
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

    return new Units(newNumeratorUnits, newDenominatorUnits);
  }

  /**
   * Simplify the units by canceling out common units in the numerator and denominator.
   *
   * @return a new Units instance where common units in the numerator and denominator are canceled
   *     out.
   */
  public Units simplify() {
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
    String numeratorString = serializeMultiplyString(numeratorUnits);
    String denominatorString = serializeMultiplyString(denominatorUnits);
    boolean noDenominator = denominatorString.isEmpty();
    return noDenominator ? numeratorString : numeratorString + " / " + denominatorString;
  }

  @Override
  public int hashCode() {
    return toString().hashCode();
  }

  private Map<String, Long> parseMultiplyString(String target) {
    Map<String, Long> unitCounts = new TreeMap<>();

    for (String unit : target.split("\\s*\\*\\s*")) {
      if (unit.isEmpty()) {
        continue;
      }
      unitCounts.put(unit, unitCounts.getOrDefault(unit, 0L) + 1);
    }
    return unitCounts;
  }

  private String serializeMultiplyString(Map<String, Long> target) {
    StringJoiner joiner = new StringJoiner(" * ");

    for (String unit : target.keySet()) {
      long numInstances = target.get(unit);
      for (long i = 0; i < numInstances; i++) {
        joiner.add(unit);
      }
    }

    return joiner.toString();
  }

}
