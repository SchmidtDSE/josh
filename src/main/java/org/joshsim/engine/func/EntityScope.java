/**
 * Structures to describe a scope which contains an entity.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.func;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.joshsim.engine.entity.base.Entity;
import org.joshsim.engine.value.type.EngineValue;


/**
 * Simple scope which contains an entity.
 */
public class EntityScope implements Scope {

  private final Entity value;
  private final Set<String> expectedAttrs;
  private Map<String, Integer> indexCache;

  /**
   * Create a scope decorator around this entity.
   *
   * @param value EngineValue to use for the root.
   */
  public EntityScope(Entity value) {
    this.value = value;
    this.expectedAttrs = value.getAttributeNames();
  }

  @Override
  public EngineValue get(String name) {
    Optional<EngineValue> attrValue = value.getAttributeValue(name);
    if (attrValue.isEmpty()) {
      String message = String.format("Cannot find %s on %s. May be uninitalized.", name, value);
      throw new IllegalArgumentException(message);
    } else {
      return attrValue.get();
    }
  }

  /**
   * Get an attribute value by index.
   *
   * <p>Fast-path method for attribute access when the index is known.
   * Avoids string lookup overhead by directly accessing the array.</p>
   *
   * @param index the attribute index (from getAttributeNameToIndex())
   * @return the attribute value
   * @throws IllegalArgumentException if index is invalid or attribute is uninitialized
   */
  public EngineValue get(int index) {
    Optional<EngineValue> attrValue = value.getAttributeValue(index);
    if (attrValue.isEmpty()) {
      String message = String.format(
          "Cannot find attribute at index %d on %s. May be uninitialized or invalid.",
          index,
          value
      );
      throw new IllegalArgumentException(message);
    }
    return attrValue.get();
  }

  @Override
  public boolean has(String name) {
    return expectedAttrs.contains(name);
  }


  @Override
  public Set<String> getAttributes() {
    return expectedAttrs;
  }

  /**
   * Get the attribute name to index mapping for this entity.
   *
   * <p>This map can be used to cache index lookups for repeated access.</p>
   *
   * @return immutable map from attribute name to array index
   */
  public Map<String, Integer> getAttributeNameToIndex() {
    return value.getAttributeNameToIndex();
  }

  /**
   * Get an attribute value by index, returning Optional.
   *
   * <p>Unlike get(int), this method returns Optional.empty() rather than throwing
   * an exception when the attribute is uninitialized or the index is invalid.
   * This avoids exception-based control flow for better performance.</p>
   *
   * @param index the attribute index (from getAttributeNameToIndex())
   * @return Optional containing the attribute value, or empty if invalid/uninitialized
   */
  public Optional<EngineValue> getOptional(int index) {
    return value.getAttributeValue(index);
  }

  /**
   * Attempt to retrieve a value using a cached integer index for fast array access.
   *
   * <p>Caches the resolved attribute index by name so repeated lookups for the same attribute
   * avoid redundant map traversals. Returns {@code Optional.empty()} when the entity has no
   * index map, when {@code name} is not present in the index, or when the stored value is
   * uninitialized (signalling the caller to fall through to the slow path).</p>
   *
   * @param name the attribute name to look up.
   * @return Optional containing the resolved value, or empty to signal fall-through to slow path.
   */
  @Override
  public Optional<EngineValue> tryIndexedGet(String name) {
    Map<String, Integer> indexMap = value.getAttributeNameToIndex();

    if (indexMap == null || indexMap.isEmpty()) {
      return Optional.empty();
    }

    if (indexCache == null) {
      indexCache = new HashMap<>();
    }

    Integer cachedIndex = indexCache.get(name);

    if (cachedIndex != null) {
      return getOptional(cachedIndex);
    }

    Integer index = indexMap.get(name);
    if (index == null) {
      return Optional.empty();
    }

    indexCache.put(name, index);
    return getOptional(index);
  }

}
