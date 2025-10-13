package org.joshsim.engine.entity.base;

import java.util.Collections;
import org.joshsim.engine.entity.type.EntityType;
import org.joshsim.engine.geometry.EngineGeometry;

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
    super(geometry, "reference", Collections.emptyMap(),
        new org.joshsim.engine.value.type.EngineValue[0],
        Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(),
        Collections.emptySet());
  }

  @Override
  public EntityType getEntityType() {
    return EntityType.REFERENCE;
  }
}
