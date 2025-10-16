/**
 * Structures describing a read only external data soruce.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.entity.type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.joshsim.engine.entity.base.DirectLockMutableEntity;
import org.joshsim.engine.entity.base.EntityInitializationInfo;
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
    super(createInitInfo(name, eventHandlerGroups, attributes));
  }

  /**
   * Create initialization info for ExternalResource.
   */
  private static EntityInitializationInfo createInitInfo(
      String name,
      HashMap<EventKey, EventHandlerGroup> eventHandlerGroups,
      HashMap<String, EngineValue> attributes
  ) {
    final EngineValue[] attributesArray = attributesArrayFromMap(attributes);
    final Map<String, Integer> attributeIndex = attributeIndexFromMap(attributes);
    final String[] indexToAttributeName = indexToAttributeNameFromMap(attributes);

    return new EntityInitializationInfo() {
      @Override
      public String getName() {
        return name;
      }

      @Override
      public Map<EventKey, EventHandlerGroup> getEventHandlerGroups() {
        return eventHandlerGroups;
      }

      @Override
      public EngineValue[] createAttributesArray() {
        return attributesArray;
      }

      @Override
      public Map<String, Integer> getAttributeNameToIndex() {
        return attributeIndex;
      }

      @Override
      public String[] getIndexToAttributeName() {
        return indexToAttributeName;
      }

      @Override
      public Map<String, boolean[]> getAttributesWithoutHandlersBySubstep() {
        return Collections.emptyMap();
      }

      @Override
      public Map<String, List<EventHandlerGroup>> getCommonHandlerCache() {
        return Collections.emptyMap();
      }

      @Override
      public Set<String> getSharedAttributeNames() {
        return Collections.emptySet();
      }
    };
  }

  /**
   * Convert attributes map to array using alphabetical ordering.
   */
  private static EngineValue[] attributesArrayFromMap(HashMap<String, EngineValue> map) {
    if (map.isEmpty()) {
      return new EngineValue[0];
    }
    List<String> sortedNames = new ArrayList<>(map.keySet());
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
  private static Map<String, Integer> attributeIndexFromMap(
      HashMap<String, EngineValue> map) {
    if (map.isEmpty()) {
      return Collections.emptyMap();
    }
    List<String> sortedNames = new ArrayList<>(map.keySet());
    Collections.sort(sortedNames);
    Map<String, Integer> result = new HashMap<>();
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
    List<String> sortedNames = new ArrayList<>(map.keySet());
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
