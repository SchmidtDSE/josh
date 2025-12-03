package org.joshsim.engine.entity.base;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.joshsim.engine.entity.handler.EventHandlerGroup;
import org.joshsim.engine.entity.handler.EventKey;
import org.joshsim.engine.entity.type.EntityType;
import org.joshsim.engine.geometry.EngineGeometry;
import org.joshsim.engine.value.type.EngineValue;

/**
 * Geometric entity that serves as a reference to a generic geometry.
 */
public class ReferenceGeometryEntity extends RootSpatialEntity {

  /**
   * Constructs a RootSpatialEntity with the specified geometry.
   *
   * @param geometry The geometry associated with this entity.
   */
  public ReferenceGeometryEntity(EngineGeometry geometry) {
    // ReferenceGeometryEntity has no handlers, so pass empty array and maps
    super(geometry, new EntityInitializationInfo() {
      @Override
      public String getName() {
        return "reference";
      }

      @Override
      public Map<EventKey, EventHandlerGroup> getEventHandlerGroups() {
        return Collections.emptyMap();
      }

      @Override
      public EngineValue[] createAttributesArray() {
        return new EngineValue[0];
      }

      @Override
      public Map<String, Integer> getAttributeNameToIndex() {
        return Collections.emptyMap();
      }

      @Override
      public String[] getIndexToAttributeName() {
        return new String[0];
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

      @Override
      public boolean getUsesState() {
        return false;
      }

      @Override
      public int getStateIndex() {
        return -1;
      }
    });
  }

  @Override
  public EntityType getEntityType() {
    return EntityType.REFERENCE;
  }
}
