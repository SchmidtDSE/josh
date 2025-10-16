
/**
 * Structures to describe language types.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.value.type;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.joshsim.compat.CompatibilityLayerKeeper;
import org.joshsim.compat.CompatibleStringJoiner;


/**
 * Data structure describing a language data type like decimal.
 */
public class LanguageType {

  private final Collection<String> distributionTypes;
  private final String rootType;
  private final boolean containsAttributes;

  // Interning cache for simple single-argument LanguageType instances
  // Only caches types created with LanguageType(String) constructor for primitive types
  private static final Map<String, LanguageType> SIMPLE_TYPE_CACHE = new ConcurrentHashMap<>();

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
   * Factory method to get or create a simple LanguageType without distributions or attributes.
   *
   * <p>This method implements an interning pattern where LanguageType instances for simple
   * types (int, decimal, string, boolean) are cached and reused. This significantly reduces
   * allocations for frequently-used scalar types.</p>
   *
   * <p>Only use this for simple primitive types. For entity types or distributions, use the
   * appropriate constructor directly.</p>
   *
   * @param rootType The base type (e.g., "int", "decimal", "string", "boolean").
   * @return cached or new LanguageType instance
   */
  public static LanguageType of(String rootType) {
    return SIMPLE_TYPE_CACHE.computeIfAbsent(rootType, LanguageType::new);
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
      CompatibleStringJoiner joiner = CompatibilityLayerKeeper.get().createStringJoiner(" > ");
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
