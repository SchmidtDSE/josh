/**
 * Initialization information for entity construction.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.entity.base;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.joshsim.engine.entity.handler.EventHandlerGroup;
import org.joshsim.engine.entity.handler.EventKey;
import org.joshsim.engine.value.type.EngineValue;

/**
 * Interface providing initialization information for entity construction.
 *
 * <p>This interface encapsulates all the shared, immutable initialization data needed to
 * construct entity instances. By grouping these parameters into a single interface, we reduce
 * the number of parameters passed to entity constructors from 8 to 2 (parent + info), making
 * the code less error-prone and easier to maintain.</p>
 *
 * <p>All methods return immutable, shared data structures that can be safely reused across
 * multiple entity instances of the same type without defensive copying.</p>
 */
public interface EntityInitializationInfo {
  /**
   * Gets the entity name.
   *
   * @return the name of the entity
   */
  String getName();

  /**
   * Gets the immutable map of event keys to event handler groups.
   *
   * <p>This map is shared across all instances of this entity type for performance.</p>
   *
   * @return immutable map of event keys to event handler groups
   */
  Map<EventKey, EventHandlerGroup> getEventHandlerGroups();

  /**
   * Creates an attributes array for a new entity instance.
   *
   * <p>Unlike other methods which return shared immutable data, this method creates
   * a new array for each entity instance since attributes are mutable per instance.</p>
   *
   * @return a new array of EngineValue objects indexed by attributeNameToIndex
   */
  EngineValue[] createAttributesArray();

  /**
   * Gets the shared immutable map from attribute name to array index.
   *
   * <p>This map is shared across all instances of this entity type.</p>
   *
   * @return immutable map from attribute name to array index
   */
  Map<String, Integer> getAttributeNameToIndex();

  /**
   * Gets the shared immutable array from index to attribute name.
   *
   * <p>This array is shared across all instances of this entity type.</p>
   *
   * @return immutable array where array[index] = attribute name
   */
  String[] getIndexToAttributeName();

  /**
   * Gets the precomputed map from substep name to boolean arrays.
   *
   * <p>Each boolean array indicates which attributes (by index) have no handlers
   * for that substep. This map is shared across all instances of this entity type.</p>
   *
   * @return immutable map from substep name to boolean arrays
   */
  Map<String, boolean[]> getAttributesWithoutHandlersBySubstep();

  /**
   * Gets the precomputed handler cache.
   *
   * <p>This map caches all handler lookups by cache key strings in the format
   * "attribute:substep" or "attribute:substep:state". The cache is shared across
   * all instances of this entity type.</p>
   *
   * @return immutable map from cache key string to list of EventHandlerGroups
   */
  Map<String, List<EventHandlerGroup>> getCommonHandlerCache();

  /**
   * Gets the precomputed immutable set of attribute names.
   *
   * <p>This set is shared across all instances of this entity type.</p>
   *
   * @return immutable set of attribute names
   */
  Set<String> getSharedAttributeNames();
}
