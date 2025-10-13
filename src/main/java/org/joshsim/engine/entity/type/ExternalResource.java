/**
 * Structures describing a read only external data soruce.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.entity.type;

import java.util.Collections;
import java.util.HashMap;
import org.joshsim.engine.entity.base.DirectLockMutableEntity;
import org.joshsim.engine.entity.handler.EventHandlerGroup;
import org.joshsim.engine.entity.handler.EventKey;
import org.joshsim.engine.geometry.EngineGeometry;
import org.joshsim.engine.value.type.Distribution;
import org.joshsim.engine.value.type.EngineValue;


/**
 * Immutable external data source.
 *
 * <p>Represents an external resource entity in the system which provides access to distributed
 * values based on a geometry and attribute-sensitive paths.</p>
 */
public abstract class ExternalResource extends DirectLockMutableEntity {
  /**
   * Constructor for a ExternalResource, which allows access to external data to influence Entities.
   *
   * @param name Name of the entity.
   * @param eventHandlerGroups A map of event keys to their corresponding EventHandlerGroups.
   * @param attributes A map of attribute names to their corresponding EngineValues.
   */
  public ExternalResource(
      String name,
      HashMap<EventKey, EventHandlerGroup> eventHandlerGroups,
      HashMap<String, EngineValue> attributes
  ) {
    // ExternalResource has no handlers, so convert to array and pass empty maps
    super(name, eventHandlerGroups,
        attributesArrayFromMap(attributes),
        attributeIndexFromMap(attributes),
        indexToAttributeNameFromMap(attributes),
        Collections.emptyMap(), Collections.emptyMap(), Collections.emptySet());
  }

  /**
   * Convert attributes map to array using alphabetical ordering.
   */
  private static EngineValue[] attributesArrayFromMap(HashMap<String, EngineValue> map) {
    if (map.isEmpty()) {
      return new EngineValue[0];
    }
    java.util.List<String> sortedNames = new java.util.ArrayList<>(map.keySet());
    Collections.sort(sortedNames);
    EngineValue[] result = new EngineValue[sortedNames.size()];
    for (int i = 0; i < sortedNames.size(); i++) {
      result[i] = map.get(sortedNames.get(i));
    }
    return result;
  }

  /**
   * Create index map from attribute names using alphabetical ordering.
   */
  private static java.util.Map<String, Integer> attributeIndexFromMap(
      HashMap<String, EngineValue> map) {
    if (map.isEmpty()) {
      return Collections.emptyMap();
    }
    java.util.List<String> sortedNames = new java.util.ArrayList<>(map.keySet());
    Collections.sort(sortedNames);
    java.util.Map<String, Integer> result = new HashMap<>();
    for (int i = 0; i < sortedNames.size(); i++) {
      result.put(sortedNames.get(i), i);
    }
    return Collections.unmodifiableMap(result);
  }

  /**
   * Create index-to-name array from attribute names using alphabetical ordering.
   */
  private static String[] indexToAttributeNameFromMap(HashMap<String, EngineValue> map) {
    if (map.isEmpty()) {
      return new String[0];
    }
    java.util.List<String> sortedNames = new java.util.ArrayList<>(map.keySet());
    Collections.sort(sortedNames);
    return sortedNames.toArray(new String[0]);
  }

  /**
   * Get distribution values for the specified geometry.
   *
   * @param geometry the geometry to query values for
   * @return the distribution of values for the given geometry
   */
  abstract Distribution getDistribution(EngineGeometry geometry);

  @Override
  public EntityType getEntityType() {
    return EntityType.EXTERNAL_RESOURCE;
  }
}
