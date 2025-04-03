
/**
 * Structures to describe language types.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.value.type;

import java.util.ArrayList;
import java.util.Collection;
import java.util.StringJoiner;


/**
 * Data structure describing a language data type like decimal.
 */
public class LanguageType {

  private final Collection<String> distributionTypes;
  private final String rootType;
  private final boolean containsAttributes;

  /**
   * Creates a new LanguageType for a value not in a distribution without inner attributes.
   *
   * @param rootType The base type (e.g., "decimal", "string", etc.).
   */
  public LanguageType(String rootType) {
    this.rootType = rootType;
    this.distributionTypes = new ArrayList<>();
    containsAttributes = false;
  }

  /**
   * Creates a new LanguageType for a value that is not in a distribution.
   *
   * @param rootType The base type (e.g., "decimal", "string", etc.).
   * @param containsAttributes A flag indicating if this type contains other attributes. True if
   *     contains attributes and false if is a simple primitive.
   */
  public LanguageType(String rootType, boolean containsAttributes) {
    this.rootType = rootType;
    this.distributionTypes = new ArrayList<>();
    this.containsAttributes = containsAttributes;
  }

  /**
   * Creates a new LanguageType with distribution types and a root type without inner attributes.
   *
   * @param distributionTypes Collection of distribution type identifiers.
   * @param rootType The base type (e.g., "decimal", "string", etc.).
   */
  public LanguageType(Collection<String> distributionTypes, String rootType) {
    this.distributionTypes = distributionTypes;
    this.rootType = rootType;
    this.containsAttributes = false;
  }

  /**
   * Creates a new LanguageType with distribution types and a root type.
   *
   * @param distributionTypes Collection of distribution type identifiers.
   * @param rootType The base type (e.g., "decimal", "string", etc.).
   * @param containsAttributes A flag indicating if this type contains other attributes. True if
   *     contains attributes and false if is a distribution of simple primitives.
   */
  public LanguageType(Collection<String> distributionTypes, String rootType,
      boolean containsAttributes) {
    this.distributionTypes = distributionTypes;
    this.rootType = rootType;
    this.containsAttributes = containsAttributes;
  }

  /**
   * Checks if this type represents a distribution.
   *
   * @return true if this type has distribution types, false otherwise
   */
  public boolean isDistribution() {
    return !distributionTypes.isEmpty();
  }

  /**
   * Gets the base type of this language type.
   *
   * @return the root type string
   */
  public String getRootType() {
    return rootType;
  }

  /**
   * Gets the collection of distribution types for this language type.
   *
   * @return Iterable of distribution type strings
   */
  public Iterable<String> getDistributionTypes() {
    return distributionTypes;
  }

  /**
   * Determine if this type contains attributes.
   *
   * @return True if this contains attributes or contains a distribution of values that contains
   *     attributes. False if this is a simple value / primitives or a distribution of simple
   *     values.
   */
  public boolean containsAttributes() {
    return containsAttributes;
  }

  @Override
  public String toString() {
    if (isDistribution()) {
      StringJoiner joiner = new StringJoiner(" > ");
      for (String distributionType : getDistributionTypes()) {
        joiner.add(distributionType);
      }
      return joiner.toString();
    } else {
      return rootType;
    }
  }

  @Override
  public boolean equals(Object other) {
    return toString().equals(other.toString());
  }

  @Override
  public int hashCode() {
    return toString().hashCode();
  }

}
