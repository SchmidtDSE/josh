
/**
 * Structures to describe language types.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.value.type;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.joshsim.compat.CompatibilityLayerKeeper;
import org.joshsim.compat.CompatibleStringJoiner;


/**
 * Data structure describing a language data type like decimal.
 */
public class LanguageType {

  private static int nextId = 1;

  private final Collection<String> distributionTypes;
  private final String rootType;
  private final boolean containsAttributes;
  private final int id;

  // Memoized types describing a distribution of this type. Both are a pure function of this
  // type, so they are derived once and shared. Keeping them here rather than rebuilding them per
  // call is what allows callers to hand out a stable instance, which the identity keyed tuple
  // caches require in order to ever hit. Benign race on first access: a losing thread rebuilds
  // an equivalent type, and LanguageType's own fields are final so either is safely published.
  private LanguageType realizedDistributionType;
  private LanguageType virtualDistributionType;

  // Interning cache for simple single-argument LanguageType instances
  // Only caches types created with LanguageType(String) constructor for primitive types
  private static final Map<String, LanguageType> SIMPLE_TYPE_CACHE = new ConcurrentHashMap<>();

  // Interning cache for types which carry attributes, kept separate from SIMPLE_TYPE_CACHE
  // because both are keyed on root type alone and the two differ only by that flag.
  private static final Map<String, LanguageType> ATTRIBUTED_TYPE_CACHE = new ConcurrentHashMap<>();

  /**
   * Creates a new LanguageType for a value not in a distribution without inner attributes.
   *
   * @param rootType The base type (e.g., "decimal", "string", etc.).
   */
  public LanguageType(String rootType) {
    this.rootType = rootType;
    this.distributionTypes = new ArrayList<>();
    containsAttributes = false;
    id = takeNextId();
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
    id = takeNextId();
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
    id = takeNextId();
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
    id = takeNextId();
  }

  private static synchronized int takeNextId() {
    int id = nextId;
    nextId += 1;
    return id;
  }

  /**
   * Get the unique numeric identity of this instance.
   *
   * <p>Identity is unique per instance regardless of content, unlike equals / hashCode which
   * compare content. Used to build collision-free composite cache keys without boxing.</p>
   *
   * @return unique positive identifier for this exact LanguageType instance.
   */
  public int getId() {
    return id;
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
    LanguageType cached = SIMPLE_TYPE_CACHE.get(rootType);
    if (cached != null) {
      return cached;
    }
    LanguageType newType = new LanguageType(rootType);
    LanguageType existing = SIMPLE_TYPE_CACHE.putIfAbsent(rootType, newType);
    return existing != null ? existing : newType;
  }

  /**
   * Factory method to get or create a simple LanguageType, interning by root type and attributes.
   *
   * <p>Extends the interning of {@link #of(String)} to types which carry attributes, such as the
   * type of an entity value. Entity values report their type on each request, so without
   * interning each request would yield a distinct instance and the identity keyed tuple caches
   * would never hit for them.</p>
   *
   * @param rootType The base type, such as a primitive name or an entity name.
   * @param containsAttributes True if this type contains other attributes and false if it is a
   *     simple primitive.
   * @return cached or new LanguageType instance.
   */
  public static LanguageType of(String rootType, boolean containsAttributes) {
    if (!containsAttributes) {
      return of(rootType);
    }

    LanguageType cached = ATTRIBUTED_TYPE_CACHE.get(rootType);
    if (cached != null) {
      return cached;
    }
    LanguageType newType = new LanguageType(rootType, true);
    LanguageType existing = ATTRIBUTED_TYPE_CACHE.putIfAbsent(rootType, newType);
    return existing != null ? existing : newType;
  }

  /**
   * Get the type describing a realized distribution whose members are of this type.
   *
   * <p>Prepends RealizedDistribution to this type's distribution chain, carrying this type's root
   * type and attribute flag through. Derived once and reused so that every realized distribution
   * over the same member type reports the same type instance.</p>
   *
   * @return type describing a realized distribution of this type.
   */
  public LanguageType asRealizedDistribution() {
    LanguageType cached = realizedDistributionType;
    if (cached == null) {
      LanguageType computed = new LanguageType(
          buildDistributionChain("RealizedDistribution"),
          rootType,
          containsAttributes
      );
      realizedDistributionType = computed;
      return computed;
    } else {
      return cached;
    }
  }

  /**
   * Get the type describing a virtual distribution whose members are of this type.
   *
   * <p>Prepends VirtualDistribution to this type's distribution chain. Unlike
   * {@link #asRealizedDistribution()} the attribute flag is not carried through, preserving the
   * long standing behavior of virtual distributions reporting no attributes.</p>
   *
   * @return type describing a virtual distribution of this type.
   */
  public LanguageType asVirtualDistribution() {
    LanguageType cached = virtualDistributionType;
    if (cached == null) {
      LanguageType computed =
          new LanguageType(buildDistributionChain("VirtualDistribution"), rootType);
      virtualDistributionType = computed;
      return computed;
    } else {
      return cached;
    }
  }

  /**
   * Build the distribution chain for a distribution of this type.
   *
   * @param outerType Name of the distribution type to place at the head of the chain.
   * @return This type's distribution chain with the given type prepended.
   */
  private Collection<String> buildDistributionChain(String outerType) {
    List<String> chain = new ArrayList<>();
    chain.add(outerType);
    for (String distributionType : distributionTypes) {
      chain.add(distributionType);
    }
    return chain;
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
