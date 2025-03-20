/**
 * Structures to represent units.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.value;

import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeMap;


/**
 * Strategy which represents a set of units which performs dimensional analysis.
 */
public class Units {

  private final Map<String, Integer> numeratorUnits;
  private final Map<String, Integer> denominatorUnits;

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
   * @param numeratorUnits a Map representing the units in the numerator, mapping from name to count.
   * @param denominatorUnits a Map representing the units in the denominator, mapping from name to
   *     count.
   */
  public Units(Map<String, Integer> numeratorUnits, Map<String, Integer> denominatorUnits) {
    this.numeratorUnits = numeratorUnits;
    this.denominatorUnits = denominatorUnits;
  }

  /**
   * Gets the numerator units.
   *
   * @return a Map representing the units in the numerator, mapping from name to count.
   */
  public Map<String, Integer> getNumeratorUnits() {
    return numeratorUnits;
  }

  /**
   * Gets the denominator units.
   *
   * @return a Map representing the units in the denominator, mapping from name to count.
   */
  public Map<String, Integer> getDenominatorUnits() {
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
    Map<String, Integer> newNumeratorUnits = new TreeMap<>(numeratorUnits);
    Map<String, Integer> newDenominatorUnits = new TreeMap<>(denominatorUnits);
  
    Map<String, Integer> otherNumeratorUnits = other.getNumeratorUnits();
    for (String units : otherNumeratorUnits.keySet()) {
      int priorCount = newNumeratorUnits.getOrDefault(units, 0);
      int otherCount = otherNumeratorUnits.get(units);
      int newCount = priorCount + otherCount;
      newNumeratorUnits.put(units, newCount);
    }

    Map<String, Integer> otherDenominatorUnits = other.getNumeratorUnits();
    for (String units : otherDenominatorUnits.keySet()) {
      int priorCount = newDenominatorUnits.getOrDefault(units, 0);
      int otherCount = otherDenominatorUnits.get(units);
      int newCount = priorCount + otherCount;
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
   * Simplify the units by canceling out common units in the numerator and denominator.
   *
   * @return a new Units instance where common units in the numerator and denominator are canceled
   *     out.
   */
  public Units simplify() {
    Map<String, Integer> newNumeratorUnits = new TreeMap<>();
    Map<String, Integer> newDenominatorUnits = new TreeMap<>();
    
    Set<String> sharedUnits = numeratorUnits.keySet();
    sharedUnits.retainAll(denominatorUnits.keySet());

    for (String units : sharedUnits) {
      int numeratorCount = numeratorUnits.get(units);
      int denominatorCount = denominatorUnits.get(units);
      int simplifiedCount = numeratorCount - denominatorCount;
      
      if (simplifiedCount > 0) {
        newNumeratorUnits.put(units, simplifiedCount);
      } else {
        newDenominatorUnits.put(units, simplifiedCount * -1);
      }
    }

    Set<String> numeratorOnlyUnits = numeratorUnits.keySet();
    numeratorOnlyUnits.removeAll(denominatorUnits.keySet());

    for (String units : numeratorOnlyUnits) {
      newNumeratorUnits.put(units, numeratorUnits.get(units));
    }
    
    Set<String> denominatorOnlyUnits = denominatorUnits.keySet();
    denominatorOnlyUnits.removeAll(numeratorUnits.keySet());

    for (String units : denominatorOnlyUnits) {
      newDenominatorUnits.put(units, denominatorUnits.get(units));
    }

    return new Units(newNumeratorUnits, newDenominatorUnits);
  }

  /**
   * Check if this units and another units are the same without simplification.
   *
   * @param other the other units.
   * @returns true if this units and the other units are the same without simplification and false
   *     otherwise.
   */
  public boolean equals(Units other) {
    return toString().equals(other.toString());
  }

  @Override
  public String toString() {
    String numeratorString = serializeMultiplyString(numeratorUnits);
    String denominatorString = serializeMultiplyString(denominatorUnits);
    boolean noDenominator = denominatorString.isEmpty();
    return noDenominator ? numeratorString : numeratorString + "/" + denominatorString;
  }

  @Override
  public int hashCode() {
    return toString().hashCode();
  }

  private Map<String, Integer> parseMultiplyString(String target) {
    Map<String, Integer> unitCounts = new TreeMap<>();

    for (String unit : target.split(" * ")) {
      unitCounts.put(unit, unitCounts.getOrDefault(unit, 0) + 1);
    }
    return unitCounts;
  }

  private String serializeMultiplyString(Map<String, Integer> target) {
    StringJoiner joiner = new StringJoiner(" * ");

    for (String unit : target.keySet()) {
      int numInstances = target.get(unit);
      for (int i = 0; i < numInstances; i++) {
        joiner.add(unit);
      }
    }

    return joiner.toString();
  }
  
}
