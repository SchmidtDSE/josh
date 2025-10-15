/**
 * Base entity which is a mutable entity.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.entity.base;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.joshsim.engine.entity.handler.EventHandlerGroup;
import org.joshsim.engine.entity.type.EntityType;
import org.joshsim.engine.geometry.EngineGeometry;
import org.joshsim.engine.value.type.EngineValue;


/**
 * Represents a base entity that is mutable.
 * This class provides mechanisms for managing attributes and event handlers,
 * and supports locking to be thread-safe.
 */
public interface Entity {

  /**
   * Get the geographic location of this spatial entity.
   *
   * @return The geographic point representing this entity's location.
   */
  Optional<EngineGeometry> getGeometry();

  /**
   * Get the name of this type of entity.
   *
   * @returns unique name of this entity type.
   */
  String getName();

  /**
   * Get the value of an attribute by name.
   *
   * @param name the attribute name
   * @return an Optional containing the attribute value, or empty if not found
   */
  Optional<EngineValue> getAttributeValue(String name);

  /**
   * Get the value of an attribute by index.
   *
   * <p>This method provides fast array-based access to attributes when the index
   * is known. The index corresponds to the position in the attributeNameToIndex
   * map, which uses alphabetical ordering.</p>
   *
   * @param index the attribute index (from attributeNameToIndex map)
   * @return an Optional containing the attribute value, or empty if index is invalid
   */
  Optional<EngineValue> getAttributeValue(int index);

  /**
   * Get the array index for an attribute name.
   *
   * <p>This method allows callers to look up an attribute's index once and then
   * use integer-based access for subsequent calls. Returns empty if the attribute
   * does not exist on this entity type.</p>
   *
   * @param name the attribute name
   * @return an Optional containing the attribute index, or empty if not found
   */
  Optional<Integer> getAttributeIndex(String name);

  /**
   * Get the complete attribute name to index mapping for this entity type.
   *
   * <p>This map is shared across all instances of this entity type and maps
   * attribute names to their array indices. The map is immutable and uses
   * alphabetical ordering for deterministic indexing.</p>
   *
   * <p>This method is useful for caching index lookups across multiple
   * attribute accesses on entities of the same type.</p>
   *
   * @return immutable map from attribute name to array index
   */
  Map<String, Integer> getAttributeNameToIndex();

  /**
   * Get the index-to-name array for reverse lookup.
   *
   * <p>This array maps attribute indices to their corresponding names,
   * providing efficient reverse lookup from index to attribute name.</p>
   *
   * <p>For entity types that do not support index-based access, this
   * method returns null to avoid allocation overhead.</p>
   *
   * @return immutable array where array[index] = attribute name, or null
   *     if this entity type does not support index-based reverse lookup
   */
  String[] getIndexToAttributeName();

  /**
   * Get the names of all attributes associated with this entity.
   *
   * @return All attribute names available on this entity as strings. This may include non-
   *     initialized names.
   */
  Set<String> getAttributeNames();

  /**
   * Get the type of this entity.
   *
   * @return The EntityType for this entity.
   */
  EntityType getEntityType();

  /**
   * An immutable copy of this entity's attributes or a reference to this entity if already frozen.
   *
   * @return An immutable FrozenEntity containing the attributes of this entity.
   */
  Entity freeze();

  /**
   * Get a key that uniquely identifies the location of this entity within a replicate.
   *
   * @return Uniquely identifying key which can be hashed and used in equality operations or empty
   *     if there is no location associated with this entity.
   */
  Optional<GeoKey> getKey();

  /**
   * Get the pre-computed handler cache for this entity type.
   *
   * <p>This cache maps cache key strings (format: "attribute:substep" or
   * "attribute:substep:state") to lists of matching EventHandlerGroups. The cache
   * is computed once during entity type construction and shared across all instances.</p>
   *
   * <p>For entity types without handlers, this returns an empty map.</p>
   *
   * @return Immutable map from cache key string to list of matching EventHandlerGroups,
   *     or empty map if no handlers are defined
   */
  Map<String, List<EventHandlerGroup>> getResolvedHandlers();
}
