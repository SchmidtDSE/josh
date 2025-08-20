/**
 * Immutable value object representing a named map for wire format conversion.
 *
 * <p>This class encapsulates a name and a map of key-value pairs, providing a clean
 * way to pass data to and from wire format serialization methods. The class is
 * immutable with final fields to ensure thread safety.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable value object for named map data used in wire format conversion.
 *
 * <p>This class represents a named collection of key-value pairs that can be
 * serialized to and deserialized from wire format. Both the name and the map
 * are immutable to ensure thread safety and prevent accidental modification.</p>
 */
public class NamedMap {

  private final String name;
  private final Map<String, String> target;

  /**
   * Creates a new NamedMap with the specified name and map data.
   *
   * @param name The identifier name for this map
   * @param target The map of key-value pairs to store (will be defensively copied)
   * @throws IllegalArgumentException if name is empty, or if target is null
   */
  public NamedMap(String name, Map<String, String> target) {
    if (name.trim().isEmpty()) {
      throw new IllegalArgumentException("Name cannot be empty");
    }
    if (target == null) {
      throw new IllegalArgumentException("Target map cannot be null");
    }
    
    this.name = name;
    this.target = Collections.unmodifiableMap(new HashMap<>(target));
  }

  /**
   * Gets the name identifier for this map.
   *
   * @return The name identifier
   */
  public String getName() {
    return name;
  }

  /**
   * Gets an unmodifiable view of the target map.
   *
   * @return An unmodifiable map containing the key-value pairs
   */
  public Map<String, String> getTarget() {
    return target;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    NamedMap namedMap = (NamedMap) obj;
    return name.equals(namedMap.name) && target.equals(namedMap.target);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, target);
  }

  @Override
  public String toString() {
    return String.format("NamedMap{name='%s', target=%s}", name, target);
  }
}